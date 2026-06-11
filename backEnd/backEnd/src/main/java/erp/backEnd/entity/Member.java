package erp.backEnd.entity;

import erp.backEnd.enumeration.AuthProvider;
import erp.backEnd.enumeration.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="member_id", comment = "유저 id") // db테이블명_// pk컬럼명
    private Long id;

    @Column(name = "login_id", unique = true, comment = "로그인 아이디")
    private String loginId;

    @Column(comment = "비밀번호(BCrypt 해시)")
    private String password;

    @Column(nullable = false, comment = "유저 명")
    private String username;

    @Column(comment = "유저나이")
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(comment = "권한(관리자/직원)")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(comment = "SNS 가입여부?")
    private AuthProvider authProvider;

    /** ERP 로그인 계정 생성 */
    public static Member createErpUser(String loginId, String encodedPassword, String username, Role role) {
        return Member.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .username(username)
                .role(role)
                .authProvider(AuthProvider.ERP)
                .build();
    }
}
