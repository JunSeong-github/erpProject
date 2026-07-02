package erp.backEnd.service;

import erp.backEnd.dto.po.BulkItemRow;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대량 품목 업로드용 엑셀 파서. 첫 시트 1행을 헤더로 보고 헤더명으로 컬럼을 매핑한다.
 * 형식 오류(가격 칸에 문자 등)만 행 단위로 잡고, 중복/필수 검증은 서비스에서 수행한다.
 */
@Component
public class ItemExcelParser {

    private static final String[] ALIAS_ITEM_CODE  = {"itemcode", "품목코드", "품번", "코드"};
    private static final String[] ALIAS_ITEM_NAME  = {"itemname", "품목이름", "품목명", "이름"};
    private static final String[] ALIAS_PRICE       = {"standardprice", "품목가격", "가격", "단가", "표준가"};

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

            int colCode  = findColumn(headerIndex, ALIAS_ITEM_CODE);
            int colName  = findColumn(headerIndex, ALIAS_ITEM_NAME);
            int colPrice = findColumn(headerIndex, ALIAS_PRICE);

            List<String> missing = new ArrayList<>();
            if (colCode < 0)  missing.add("품목코드");
            if (colName < 0)  missing.add("품목이름");
            if (colPrice < 0) missing.add("품목가격");
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
                BulkItemRow dto = new BulkItemRow();
                StringBuilder err = new StringBuilder();

                dto.setItemCode(readString(row, colCode, fmt));
                dto.setItemName(readString(row, colName, fmt));
                dto.setStandardPrice(readDecimal(row, colPrice, fmt, "품목가격", err));

                String parseError = (err.length() == 0) ? null : err.toString();
                rows.add(new ParsedRow(excelRowNo, dto, parseError));
            }

            if (rows.isEmpty()) {
                throw new ExcelParseException("데이터 행이 없습니다. (헤더 아래에 품목 데이터를 입력해 주세요)");
            }
            return new ParseResult(rows);

        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelParseException("엑셀 파일을 읽을 수 없습니다. 형식이 올바른 엑셀인지 확인해 주세요.");
        }
    }

    private BigDecimal readDecimal(Row row, int col, DataFormatter fmt, String label, StringBuilder err) {
        if (col < 0) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String s = getStringValue(cell, fmt);
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            appendErr(err, label + "이(가) 숫자가 아닙니다('" + s.trim() + "')");
            return null;
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

    private void appendErr(StringBuilder err, String msg) {
        if (err.length() > 0) err.append("; ");
        err.append(msg);
    }

    public static class ParsedRow {
        public final int rowNo;
        public final BulkItemRow row;
        public final String parseError;

        public ParsedRow(int rowNo, BulkItemRow row, String parseError) {
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
