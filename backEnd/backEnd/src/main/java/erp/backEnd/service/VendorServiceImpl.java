package erp.backEnd.service;

import erp.backEnd.dto.po.BulkVendorPreviewResponse;
import erp.backEnd.dto.po.BulkVendorResponse;
import erp.backEnd.dto.po.BulkVendorRow;
import erp.backEnd.dto.po.VendorCreateRequest;
import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.dto.po.VendorSearchCondition;
import erp.backEnd.entity.Vendor;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.PoRepository;
import erp.backEnd.repository.VendorBulkRepository;
import erp.backEnd.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final PoRepository poRepository;
    private final VendorExcelParser vendorExcelParser;
    private final VendorBulkRepository vendorBulkRepository;

    public List<VendorResponse> findVendorAll() {
        List<Vendor> vendorList = vendorRepository.findAll();
        return VendorResponse.toListDto(vendorList);
    }

    @Override
    public Page<VendorResponse> findSearchPageComplex(VendorSearchCondition condition, Pageable pageable) {
        return vendorRepository.searchPageComplex(condition, pageable);
    }

    @Override
    public Boolean existsByVendorCode(String vendorCode) {
        return vendorRepository.existsByVendorCode(vendorCode);
    }

    @Override
    public Boolean existsByVendorName(String vendorName) {
        return vendorRepository.existsByVendorName(vendorName);
    }

    @Override
    @Transactional
    public Vendor save(VendorCreateRequest req) {

        if (vendorRepository.existsByVendorCode(req.getVendorCode())) {
            throw new IllegalArgumentException("이미 존재하는 공급사코드입니다.");
        }
        if (vendorRepository.existsByVendorName(req.getVendorName())) {
            throw new IllegalArgumentException("이미 존재하는 공급사명입니다.");
        }

        Vendor vendor = Vendor.of(
                req.getVendorCode(),
                req.getVendorName()
        );

        return vendorRepository.save(vendor);
    }

    @Override
    @Transactional
    public void update(String vendorCode, VendorCreateRequest req) {

        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("저장된 공급사를 찾을 수 없습니다."));

        if (!vendor.getVendorName().equals(req.getVendorName())
                && vendorRepository.existsByVendorName(req.getVendorName())) {
            throw new IllegalArgumentException("이미 존재하는 공급사명입니다.");
        }

        vendor.updateForm(req);
    }

    @Override
    @Transactional
    public VendorResponse getDetail(String vendorCode) {

        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("저장된 공급사를 찾을 수 없습니다."));

        return VendorResponse.toDto(vendor);
    }

    @Override
    @Transactional
    public void delete(String vendorCode) {

        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new IllegalArgumentException("저장된 공급사를 찾을 수 없습니다."));

        // 발주에 사용된 공급사는 삭제 불가
        if (poRepository.existsByVendor_VendorCode(vendorCode)) {
            throw new BusinessException(ErrorCode.VENDOR_IN_USE);
        }

        vendorRepository.deleteById(vendorCode);
    }

    // ==================== 대량 공급사 업로드(엑셀) ====================

    @Override
    @Transactional(readOnly = true)
    public BulkVendorPreviewResponse bulkPreview(MultipartFile file) {
        List<VendorExcelParser.ParsedRow> parsed = vendorExcelParser.parse(file).rows;
        List<VendorRowCheck> checks = validateVendors(parsed);

        BulkVendorPreviewResponse resp = new BulkVendorPreviewResponse();
        List<BulkVendorPreviewResponse.PreviewRow> rows = new ArrayList<>(checks.size());
        int errorCount = 0;
        for (VendorRowCheck c : checks) {
            boolean ok = (c.error == null);
            if (!ok) errorCount++;
            rows.add(new BulkVendorPreviewResponse.PreviewRow(
                    c.rowNo, c.raw.getVendorCode(), c.raw.getVendorName(), ok, c.error));
        }
        int total = checks.size();
        resp.setTotalRows(total);
        resp.setErrorRows(errorCount);
        resp.setValidRows(total - errorCount);
        resp.setConfirmable(errorCount == 0 && total > 0);
        resp.setRows(rows);
        return resp;
    }

    @Override
    @Transactional
    public BulkVendorResponse bulkUpload(MultipartFile file) {
        List<VendorExcelParser.ParsedRow> parsed = vendorExcelParser.parse(file).rows;
        List<VendorRowCheck> checks = validateVendors(parsed);

        List<BulkVendorResponse.RowError> errors = new ArrayList<>();
        List<BulkVendorRow> valids = new ArrayList<>();
        for (VendorRowCheck c : checks) {
            if (c.error != null) errors.add(new BulkVendorResponse.RowError(c.rowNo, c.error));
            else valids.add(c.raw);
        }
        int total = checks.size();
        if (!errors.isEmpty()) {
            return new BulkVendorResponse(total, 0, errors.size(), errors);
        }

        LocalDateTime now = LocalDateTime.now();
        List<VendorBulkRepository.VendorRow> insertRows = new ArrayList<>(valids.size());
        for (BulkVendorRow v : valids) {
            insertRows.add(new VendorBulkRepository.VendorRow(v.getVendorCode(), v.getVendorName(), now));
        }
        vendorBulkRepository.batchInsert(insertRows);

        return new BulkVendorResponse(total, valids.size(), 0, errors);
    }

    private List<VendorRowCheck> validateVendors(List<VendorExcelParser.ParsedRow> parsed) {
        Set<String> codes = parsed.stream().map(p -> p.row.getVendorCode())
                .filter(v -> v != null).collect(Collectors.toSet());
        Set<String> names = parsed.stream().map(p -> p.row.getVendorName())
                .filter(v -> v != null).collect(Collectors.toSet());
        Set<String> existingCodes = codes.isEmpty() ? Set.of()
                : vendorRepository.findByVendorCodeIn(codes).stream().map(Vendor::getVendorCode).collect(Collectors.toSet());
        Set<String> existingNames = names.isEmpty() ? Set.of()
                : vendorRepository.findByVendorNameIn(names).stream().map(Vendor::getVendorName).collect(Collectors.toSet());

        Set<String> seenCodes = new HashSet<>();
        Set<String> seenNames = new HashSet<>();
        List<VendorRowCheck> checks = new ArrayList<>(parsed.size());

        for (VendorExcelParser.ParsedRow p : parsed) {
            BulkVendorRow r = p.row;
            String err = null;

            if (p.parseError != null) {
                err = p.parseError;
            } else if (r.getVendorCode() == null) {
                err = "공급사코드는 필수입니다.";
            } else if (r.getVendorName() == null) {
                err = "공급사명은 필수입니다.";
            } else if (existingCodes.contains(r.getVendorCode())) {
                err = "이미 존재하는 공급사코드입니다.";
            } else if (existingNames.contains(r.getVendorName())) {
                err = "이미 존재하는 공급사명입니다.";
            } else if (!seenCodes.add(r.getVendorCode())) {
                err = "파일 내 공급사코드가 중복됩니다.";
            } else if (!seenNames.add(r.getVendorName())) {
                err = "파일 내 공급사명이 중복됩니다.";
            }

            checks.add(new VendorRowCheck(p.rowNo, r, err));
        }
        return checks;
    }

    private static class VendorRowCheck {
        final int rowNo;
        final BulkVendorRow raw;
        final String error;

        VendorRowCheck(int rowNo, BulkVendorRow raw, String error) {
            this.rowNo = rowNo;
            this.raw = raw;
            this.error = error;
        }
    }

}
