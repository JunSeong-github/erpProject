package erp.backEnd.dto.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Spring 의 Page(PageImpl)는 no-arg 생성자가 없어 JSON 으로 역직렬화하기 어렵다.
 * (Redis 캐시에서 다시 읽을 때 Jackson 이 인스턴스를 못 만들어 실패)
 *
 * 그래서 캐시에는 직렬화가 쉬운 이 래퍼(content + 전체 건수)만 저장하고,
 * 서비스 계층에서 다시 PageImpl 로 복원한다. 컨트롤러/프론트로 나가는
 * 응답(JSON) 형태는 기존 Page 그대로라 프론트 변경이 필요 없다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedPage<T> {
    private List<T> content;
    private long totalElements;

    public static <T> CachedPage<T> of(Page<T> page) {
        return new CachedPage<>(page.getContent(), page.getTotalElements());
    }

    public Page<T> toPage(Pageable pageable) {
        return new PageImpl<>(content, pageable, totalElements);
    }
}
