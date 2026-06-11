package erp.backEnd.enumeration;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Role {

    ADMIN("ADMIN", "관리자"),
    EMPLOYEE("EMPLOYEE", "직원");

    private final String code;
    private final String label;

    Role(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** Spring Security 권한 표기(ROLE_ 접두사) */
    public String authority() {
        return "ROLE_" + code;
    }

    public static Role fromCode(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid Role code: " + code));
    }
}
