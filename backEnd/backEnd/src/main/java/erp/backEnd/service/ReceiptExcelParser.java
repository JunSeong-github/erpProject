package erp.backEnd.service;

import erp.backEnd.dto.po.BulkReceiptRow;
import erp.backEnd.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 대량 입고 업로드용 엑셀(.xlsx/.xls) 파서.
 * 첫 번째 시트의 1행을 헤더로 보고, 헤더명(한글/영문 별칭)으로 컬럼을 매핑한다.
 *
 * <p>이 단계에서는 "형식 오류"(숫자 칸에 문자, 잘못된 날짜 등)만 행 단위로 잡는다.
 * 필수값 누락/발주·품목 존재 여부 같은 "업무 검증"은 서비스(ReceiptServiceImpl)에서 수행한다.</p>
 */
@Component
public class ReceiptExcelParser {

    /** 필수 컬럼 헤더(정규화된 별칭). 하나라도 못 찾으면 파싱 자체를 중단한다. */
    private static final String[] ALIAS_PO_ID       = {"poid", "발주번호", "발주id", "po", "po번호"};
    private static final String[] ALIAS_PO_ITEM_ID  = {"poitemid", "발주라인번호", "발주라인id", "발주라인", "라인번호", "poitem"};
    private static final String[] ALIAS_ITEM_CODE   = {"itemcode", "품목코드", "품번"};
    private static final String[] ALIAS_RECEIVED_QTY= {"receivedqty", "입고수량", "수량", "입고량"};
    private static final String[] ALIAS_RECEIPT_DATE= {"receiptdate", "입고일", "입고일자"};
    private static final String[] ALIAS_REMARK      = {"remark", "입고비고", "헤더비고", "비고"};
    private static final String[] ALIAS_LINE_REMARK = {"lineremark", "라인비고"};

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
    };

    /**
     * 엑셀을 파싱해 행 목록을 만든다.
     * 파일 자체가 열리지 않거나 필수 헤더가 없으면 IllegalArgumentException 을 던진다(전체 실패).
     */
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
            if (sheet == null) {
                throw new ExcelParseException("시트를 찾을 수 없습니다.");
            }

            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new ExcelParseException("헤더 행(1행)이 비어있습니다.");
            }

            DataFormatter fmt = new DataFormatter();
            Map<String, Integer> headerIndex = new HashMap<>();
            for (Cell c : header) {
                String key = normalize(getStringValue(c, fmt));
                if (key != null && !headerIndex.containsKey(key)) {
                    headerIndex.put(key, c.getColumnIndex());
                }
            }

            int colPoId       = findColumn(headerIndex, ALIAS_PO_ID);
            int colPoItemId   = findColumn(headerIndex, ALIAS_PO_ITEM_ID);
            int colItemCode   = findColumn(headerIndex, ALIAS_ITEM_CODE);
            int colQty        = findColumn(headerIndex, ALIAS_RECEIVED_QTY);
            int colDate       = findColumn(headerIndex, ALIAS_RECEIPT_DATE);
            int colRemark     = findColumn(headerIndex, ALIAS_REMARK);
            int colLineRemark = findColumn(headerIndex, ALIAS_LINE_REMARK);

            List<String> missing = new ArrayList<>();
            if (colPoId < 0) missing.add("발주번호");
            if (colQty < 0)  missing.add("입고수량");
            // 발주 라인 식별자는 '발주라인번호' 또는 '품목코드' 중 하나 이상 있어야 함
            if (colPoItemId < 0 && colItemCode < 0) missing.add("발주라인번호 또는 품목코드");
            if (!missing.isEmpty()) {
                throw new ExcelParseException("필수 컬럼이 없습니다: " + String.join(", ", missing)
                        + " (1행 헤더를 확인해 주세요)");
            }

            List<ParsedRow> rows = new ArrayList<>();
            int first = sheet.getFirstRowNum() + 1; // 헤더 다음 행부터
            int last = sheet.getLastRowNum();
            for (int r = first; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row, fmt)) continue;

                int excelRowNo = r + 1; // 엑셀에서 실제 보이는 행 번호(1-based)
                BulkReceiptRow dto = new BulkReceiptRow();
                StringBuilder err = new StringBuilder();

                dto.setPoId(readLong(row, colPoId, fmt, "발주번호", err));
                dto.setPoItemId(readLong(row, colPoItemId, fmt, "발주라인번호", err));
                dto.setItemCode(readString(row, colItemCode, fmt));
                dto.setReceivedQty(readLong(row, colQty, fmt, "입고수량", err));
                dto.setReceiptDate(readDate(row, colDate, fmt, err));
                dto.setRemark(readString(row, colRemark, fmt));
                dto.setLineRemark(readString(row, colLineRemark, fmt));

                String parseError = (err.length() == 0) ? null : err.toString();
                rows.add(new ParsedRow(excelRowNo, dto, parseError));
            }

            if (rows.isEmpty()) {
                throw new ExcelParseException("데이터 행이 없습니다. (헤더 아래에 입고 데이터를 입력해 주세요)");
            }
            return new ParseResult(rows);

        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelParseException("엑셀 파일을 읽을 수 없습니다. 형식이 올바른 엑셀인지 확인해 주세요.");
        }
    }

    // ---- 셀 읽기 헬퍼 ------------------------------------------------------

    /** 숫자 칸을 Long 으로. 비어있으면 null(업무 검증에서 필수 여부 판단), 형식 오류면 err 에 사유 누적 후 null. */
    private Long readLong(Row row, int col, DataFormatter fmt, String label, StringBuilder err) {
        if (col < 0) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        String s = getStringValue(cell, fmt);
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            appendErr(err, label + "이(가) 숫자가 아닙니다('" + s.trim() + "')");
            return null;
        }
    }

    /** 날짜 칸을 LocalDate 로. 비어있으면 null, 형식 오류면 err 에 사유 누적 후 null. */
    private LocalDate readDate(Row row, int col, DataFormatter fmt, StringBuilder err) {
        if (col < 0) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            // 날짜 서식이 아닌 숫자면 날짜로 해석하지 않고 오류 처리
            appendErr(err, "입고일 형식이 올바르지 않습니다");
            return null;
        }
        String s = getStringValue(cell, fmt);
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, f);
            } catch (Exception ignore) { /* 다음 포맷 시도 */ }
        }
        appendErr(err, "입고일 형식이 올바르지 않습니다('" + s + "', 예: 2026-07-02)");
        return null;
    }

    private String readString(Row row, int col, DataFormatter fmt) {
        if (col < 0) return null;
        Cell cell = row.getCell(col);
        String s = getStringValue(cell, fmt);
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String getStringValue(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
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

    /** 헤더/셀 텍스트 정규화: 소문자 + 공백 제거 */
    private String normalize(String s) {
        if (s == null) return null;
        String t = s.replaceAll("\\s+", "").toLowerCase();
        return t.isEmpty() ? null : t;
    }

    private void appendErr(StringBuilder err, String msg) {
        if (err.length() > 0) err.append("; ");
        err.append(msg);
    }

    // ---- 결과 홀더 --------------------------------------------------------

    /** 파싱된 한 행: 엑셀 행번호 + 값 DTO + 형식오류(null 이면 정상) */
    public static class ParsedRow {
        public final int rowNo;
        public final BulkReceiptRow row;
        public final String parseError;

        public ParsedRow(int rowNo, BulkReceiptRow row, String parseError) {
            this.rowNo = rowNo;
            this.row = row;
            this.parseError = parseError;
        }
    }

    public static class ParseResult {
        public final List<ParsedRow> rows;

        public ParseResult(List<ParsedRow> rows) {
            this.rows = rows;
        }
    }
}
