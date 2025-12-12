package erp.backEnd.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
public enum PoStatus {

    DRAFT("DRAFT", "발주요청"),
    APPROVED("APPROVED", "승인"),
    REJECTED("REJECTED", "반려"),
    ORDERED("ORDERED", "입고진행"),
    PARTIAL_RECEIVED("PARTIAL_RECEIVED", "부분입고"),
    RECEIVED("RECEIVED", "전체입고"),
    CANCELLED("CANCELLED", "취소");

    private final String code;
    private final String label;  // 🔥 한글명

    PoStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static PoStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid PoStatus code: " + code));
    }
}
