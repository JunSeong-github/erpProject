package erp.backEnd.dto.member;

import erp.backEnd.entity.Member;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String loginId;
    private String username;
    private String role;       // ADMIN / EMPLOYEE
    private String roleLabel;  // 관리자 / 직원

    public static LoginResponse from(Member member) {
        return LoginResponse.builder()
                .loginId(member.getLoginId())
                .username(member.getUsername())
                .role(member.getRole() != null ? member.getRole().getCode() : null)
                .roleLabel(member.getRole() != null ? member.getRole().getLabel() : null)
                .build();
    }
}
