package erp.backEnd.config;

import erp.backEnd.entity.Member;
import erp.backEnd.enumeration.Role;
import erp.backEnd.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedAccounts() {
        return (ApplicationArguments args) -> {
            createIfAbsent("admin", "admin1234", "관리자", Role.ADMIN);
            createIfAbsent("employee", "employee1234", "직원", Role.EMPLOYEE);
        };
    }

    private void createIfAbsent(String loginId, String rawPassword, String username, Role role) {
        if (memberRepository.existsByLoginId(loginId)) {
            return;
        }
        Member member = Member.createErpUser(loginId, passwordEncoder.encode(rawPassword), username, role);
        memberRepository.save(member);
        log.info("시드 계정 생성: loginId={}, role={}", loginId, role.getCode());
    }
}
