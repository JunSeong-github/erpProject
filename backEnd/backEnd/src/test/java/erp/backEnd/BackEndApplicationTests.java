package erp.backEnd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // 외부 MySQL/Redis 없이 H2 로 전체 컨텍스트 로딩(CI 안전)
class BackEndApplicationTests {

	@Test
	void contextLoads() {
	}

}
