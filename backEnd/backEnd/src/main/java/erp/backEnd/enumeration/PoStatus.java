package erp.backEnd.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PoStatus {

    DRAFT("DRAFT"),                 // 발주요청 / 작성중
    APPROVED("APPROVED"),           // 승인됨
    ORDERED("ORDERED"),             // 발주서 공급사에 전달됨
    PARTIAL_RECEIVED("PARTIAL_RECEIVED"), // 부분입고
    RECEIVED("RECEIVED"),           // 전체입고
    COMPLETED("COMPLETED"),         // 검수/정산까지 완료
    CANCELLED("CANCELLED");         // 발주 취소

    private final String value;
}
