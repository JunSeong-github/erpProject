package erp.backEnd.service;

import erp.backEnd.dto.po.*;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.ItemBulkRepository;
import erp.backEnd.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemExcelParser itemExcelParser;
    private final ItemBulkRepository itemBulkRepository;

    public List<ItemResponse> itemFindAll() {
        List<Item> itemDdlList = itemRepository.findAll();
        return ItemResponse.toListDto(itemDdlList);
    }

    @Override
    public Boolean existsByItemCode(String itemCode) {
        return itemRepository.existsByItemCode(itemCode);
    }

    @Override
    public Boolean existsByItemName(String itemName) {
        return itemRepository.existsByItemName(itemName);
    }

    @Override
    @Transactional
    public Item save(ItemCreateRequest req) {

        if (itemRepository.existsByItemCode(req.getItemCode())) {
            throw new IllegalArgumentException("이미 존재하는 품목코드입니다.");
        }
        if (itemRepository.existsByItemName(req.getItemName())) {
            throw new IllegalArgumentException("이미 존재하는 품목이름입니다.");
        }

        Item item = Item.of(
                req.getItemCode(),
                req.getItemName(),
                req.getStandardPrice()
        );

        Item savedItem = itemRepository.save(item);

        return savedItem;
    }

    @Override
    @Transactional
    public void update(Long id, ItemCreateRequest req) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("저장된 품목을 찾을 수 없습니다."));

        String oldItemCode = item.getItemCode();
        String oldItemName = item.getItemName();

        if (!oldItemCode.equals(req.getItemCode())) {
            if (itemRepository.existsByItemCode(req.getItemCode())) {
                throw new IllegalArgumentException("이미 존재하는 품목코드입니다.");
            }
        }
        if (!oldItemName.equals(req.getItemName())) {
            if (itemRepository.existsByItemName(req.getItemName())) {
                throw new IllegalArgumentException("이미 존재하는 품목이름입니다.");
            }
        }

        item.updateForm(req);

    }

    @Override
    @Transactional
    public ItemResponse getDetail(Long id) {
        Optional<Item> optionalItem = itemRepository.findById(id);

        Item item = optionalItem.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ItemResponse.toDto(item);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("저장된 품목을 찾을 수 없습니다."));

        // 재고(입고합 - 승인사용합)가 남아있으면 삭제 불가
        Long stock = itemRepository.getCurrentStock(id);
        if (stock != null && stock > 0) {
            throw new BusinessException(ErrorCode.ITEM_HAS_STOCK);
        }

        itemRepository.deleteById(id);
    }

    public Page<ItemResponse> findSearchPageComplex(ItemSearchCondition itemSearchCondition, Pageable pageable){
        return itemRepository.searchPageComplex(itemSearchCondition, pageable);
    }

    @Override
    public Page<StockResponse> findStockPage(ItemSearchCondition itemSearchCondition, Pageable pageable){
        return itemRepository.searchStockPage(itemSearchCondition, pageable);
    }

    // ==================== 대량 품목 업로드(엑셀) ====================

    @Override
    @Transactional(readOnly = true)
    public BulkItemPreviewResponse bulkPreview(MultipartFile file) {
        List<ItemExcelParser.ParsedRow> parsed = itemExcelParser.parse(file).rows;
        List<ItemRowCheck> checks = validateItems(parsed);

        BulkItemPreviewResponse resp = new BulkItemPreviewResponse();
        List<BulkItemPreviewResponse.PreviewRow> rows = new ArrayList<>(checks.size());
        int errorCount = 0;
        for (ItemRowCheck c : checks) {
            boolean ok = (c.error == null);
            if (!ok) errorCount++;
            BulkItemRow r = c.raw;
            rows.add(new BulkItemPreviewResponse.PreviewRow(
                    c.rowNo, r.getItemCode(), r.getItemName(), r.getStandardPrice(), ok, c.error));
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
    public BulkItemResponse bulkUpload(MultipartFile file) {
        List<ItemExcelParser.ParsedRow> parsed = itemExcelParser.parse(file).rows;
        List<ItemRowCheck> checks = validateItems(parsed);

        List<BulkItemResponse.RowError> errors = new ArrayList<>();
        List<BulkItemRow> valids = new ArrayList<>();
        for (ItemRowCheck c : checks) {
            if (c.error != null) errors.add(new BulkItemResponse.RowError(c.rowNo, c.error));
            else valids.add(c.raw);
        }
        int total = checks.size();
        if (!errors.isEmpty()) {
            return new BulkItemResponse(total, 0, errors.size(), errors);
        }

        LocalDateTime now = LocalDateTime.now();
        List<ItemBulkRepository.ItemRow> insertRows = new ArrayList<>(valids.size());
        for (BulkItemRow v : valids) {
            insertRows.add(new ItemBulkRepository.ItemRow(v.getItemCode(), v.getItemName(), v.getStandardPrice(), now));
        }
        itemBulkRepository.batchInsert(insertRows);

        return new BulkItemResponse(total, valids.size(), 0, errors);
    }

    private List<ItemRowCheck> validateItems(List<ItemExcelParser.ParsedRow> parsed) {
        // 업로드된 코드/이름 중 DB에 이미 있는 것들을 한 번에 조회
        Set<String> codes = parsed.stream().map(p -> p.row.getItemCode())
                .filter(v -> v != null).collect(Collectors.toSet());
        Set<String> names = parsed.stream().map(p -> p.row.getItemName())
                .filter(v -> v != null).collect(Collectors.toSet());
        Set<String> existingCodes = codes.isEmpty() ? Set.of()
                : itemRepository.findByItemCodeIn(codes).stream().map(Item::getItemCode).collect(Collectors.toSet());
        Set<String> existingNames = names.isEmpty() ? Set.of()
                : itemRepository.findByItemNameIn(names).stream().map(Item::getItemName).collect(Collectors.toSet());

        Set<String> seenCodes = new HashSet<>();
        Set<String> seenNames = new HashSet<>();
        List<ItemRowCheck> checks = new ArrayList<>(parsed.size());

        for (ItemExcelParser.ParsedRow p : parsed) {
            BulkItemRow r = p.row;
            String err = null;

            if (p.parseError != null) {
                err = p.parseError;
            } else if (r.getItemCode() == null) {
                err = "품목코드는 필수입니다.";
            } else if (r.getItemName() == null) {
                err = "품목이름은 필수입니다.";
            } else if (r.getStandardPrice() == null) {
                err = "품목가격은 필수입니다.";
            } else if (r.getStandardPrice().signum() < 0) {
                err = "품목가격은 0 이상이어야 합니다.";
            } else if (existingCodes.contains(r.getItemCode())) {
                err = "이미 존재하는 품목코드입니다.";
            } else if (existingNames.contains(r.getItemName())) {
                err = "이미 존재하는 품목이름입니다.";
            } else if (!seenCodes.add(r.getItemCode())) {
                err = "파일 내 품목코드가 중복됩니다.";
            } else if (!seenNames.add(r.getItemName())) {
                err = "파일 내 품목이름이 중복됩니다.";
            }

            checks.add(new ItemRowCheck(p.rowNo, r, err));
        }
        return checks;
    }

    private static class ItemRowCheck {
        final int rowNo;
        final BulkItemRow raw;
        final String error; // null 이면 정상

        ItemRowCheck(int rowNo, BulkItemRow raw, String error) {
            this.rowNo = rowNo;
            this.raw = raw;
            this.error = error;
        }
    }

}
