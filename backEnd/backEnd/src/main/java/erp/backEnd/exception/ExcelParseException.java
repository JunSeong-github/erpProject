package erp.backEnd.exception;

/**
 * 엑셀 업로드 파싱 실패(파일 손상, 필수 컬럼 없음, 데이터 행 없음 등).
 * 사유 메시지를 그대로 응답 본문(message)에 담아 사용자에게 보여주기 위해 별도 예외로 둔다.
 * (IllegalArgumentException 은 한글 메시지가 X-Error-Detail 헤더에서 유실되어 일반 메시지만 노출됨)
 */
public class ExcelParseException extends RuntimeException {
    public ExcelParseException(String message) {
        super(message);
    }
}
