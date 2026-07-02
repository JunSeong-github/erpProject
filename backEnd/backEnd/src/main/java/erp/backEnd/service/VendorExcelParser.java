package erp.backEnd.service;

import erp.backEnd.dto.po.BulkVendorRow;
import erp.backEnd.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대량 공급사 업로드용 엑셀 파서. 첫 시트 1행을 헤더로 보고 헤더명으로 컬럼을 매핑한다.
 * 중복/필수 검증은 서비스에서 수행한다.
 */
@Component
public class VendorExcelParser {

    private static final String[] ALIAS_VENDOR_CODE = {"vendorcode", "공급사코드", "공급처코드", "거래처코드", "코드"};
    private static final String[] ALIAS_VENDOR_NAME = {"vendorname", "공급사명", "공급사이름", "공급처명", "거래처명", "이름"};

    public ParseResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("업로드된 파일이 비어있습니다.");
        }
        String name = file.getOriginalFilename();
        if (name != null && !(name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"))) {
            throw new ExcelParseException("엑셀 파일(.xlsx, .xls)만 업로드할 수 있습니다.");
        }

        try (InputStream in = file.getInputStream();
             Workbook wb = WorkbookFactory.create(in)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new ExcelParseException("시트를 찾을 수 없습니다.");
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new ExcelParseException("헤더 행(1행)이 비어있습니다.");

            DataFormatter fmt = new DataFormatter();
            Map<String, Integer> headerIndex = new HashMap<>();
            for (Cell c : header) {
                String key = normalize(getStringValue(c, fmt));
                if (key != null && !headerIndex.containsKey(key)) headerIndex.put(key, c.getColumnIndex());
            }

            int colCode = findColumn(headerIndex, ALIAS_VENDOR_CODE);
            int colName = findColumn(headerIndex, ALIAS_VENDOR_NAME);

            List<String> missing = new ArrayList<>();
            if (colCode < 0) missing.add("공급사코드");
            if (colName < 0) missing.add("공급사명");
            if (!missing.isEmpty()) {
                throw new ExcelParseException("필수 컬럼이 없습니다: " + String.join(", ", missing)
                        + " (1행 헤더를 확인해 주세요)");
            }

            List<ParsedRow> rows = new ArrayList<>();
            int first = sheet.getFirstRowNum() + 1;
            int last = sheet.getLastRowNum();
            for (int r = first; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row, fmt)) continue;

                int excelRowNo = r + 1;
                BulkVendorRow dto = new BulkVendorRow();
                dto.setVendorCode(readString(row, colCode, fmt));
                dto.setVendorName(readString(row, colName, fmt));
                rows.add(new ParsedRow(excelRowNo, dto, null));
            }

            if (rows.isEmpty()) {
                throw new ExcelParseException("데이터 행이 없습니다. (헤더 아래에 공급사 데이터를 입력해 주세요)");
            }
            return new ParseResult(rows);

        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelParseException("엑셀 파일을 읽을 수 없습니다. 형식이 올바른 엑셀인지 확인해 주세요.");
        }
    }

    private String readString(Row row, int col, DataFormatter fmt) {
        if (col < 0) return null;
        Cell cell = row.getCell(col);
        String s = getStringValue(cell, fmt);
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String getStringValue(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        return fmt.formatCellValue(cell);
    }

    private boolean isBlankRow(Row row, DataFormatter fmt) {
        if (row == null) return true;
        for (Cell c : row) {
            String s = getStringValue(c, fmt);
            if (s != null && !s.isBlank()) return false;
        }
        return true;
    }

    private int findColumn(Map<String, Integer> headerIndex, String[] aliases) {
        for (String a : aliases) {
            Integer idx = headerIndex.get(a);
            if (idx != null) return idx;
        }
        return -1;
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.replaceAll("\\s+", "").toLowerCase();
        return t.isEmpty() ? null : t;
    }

    public static class ParsedRow {
        public final int rowNo;
        public final BulkVendorRow row;
        public final String parseError;

        public ParsedRow(int rowNo, BulkVendorRow row, String parseError) {
            this.rowNo = rowNo;
            this.row = row;
            this.parseError = parseError;
        }
    }

    public static class ParseResult {
        public final List<ParsedRow> rows;
        public ParseResult(List<ParsedRow> rows) { this.rows = rows; }
    }
}
