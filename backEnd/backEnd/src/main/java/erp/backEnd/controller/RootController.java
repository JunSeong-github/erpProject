package erp.backEnd.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 백엔드 루트(/) 안내용. API 전용 서버라 루트에 화면이 없어
 * 직접 접속 시 NoResourceFoundException 이 뜨는 것을 막기 위한 단순 응답.
 */
@Tag(name = "시스템(System)", description = "헬스체크·서버 상태 확인용 엔드포인트")
@RestController
public class RootController {

    @Operation(summary = "루트 안내", description = "API 전용 서버의 루트(/) 접속 시 동작 여부를 알리는 단순 응답.")
    @GetMapping("/")
    public String root() {
        return "ERP backend is running";
    }
}
