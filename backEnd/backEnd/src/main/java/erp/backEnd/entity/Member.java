package erp.backEnd.entity;

import erp.backEnd.enumeration.AuthProvider;
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

    @Column(nullable = false, comment = "유저 명")
    private String username;

    @Column(nullable = false, comment = "유저나이")
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(comment = "SNS 가입여부?")
    private AuthProvider authProvider;
}
