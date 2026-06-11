package erp.backEnd.enumeration;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum UsageStatus {

    REQUESTED("REQUESTED", "요청"),
    APPROVED("APPROVED", "승인"),
    REJECTED("REJECTED", "반려");

    private final String code;
    private final String label;

    UsageStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static UsageStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid UsageStatus code: " + code));
    }
}
