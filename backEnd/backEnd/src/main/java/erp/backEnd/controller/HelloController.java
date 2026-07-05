package erp.backEnd.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시스템(System)", description = "헬스체크·서버 상태 확인용 엔드포인트")
@RestController
@RequestMapping("/api")
public class HelloController {

    @Operation(summary = "헬스 체크", description = "서버가 살아있는지 확인하는 간단한 응답을 반환한다.")
    @GetMapping("/hello")
    public String hello() {
        return "Hello From Spring!";
    }
}
