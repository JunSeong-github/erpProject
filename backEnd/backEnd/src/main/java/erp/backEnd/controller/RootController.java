package erp.backEnd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 백엔드 루트(/) 안내용. API 전용 서버라 루트에 화면이 없어
 * 직접 접속 시 NoResourceFoundException 이 뜨는 것을 막기 위한 단순 응답.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public String root() {
        return "ERP backend is running";
    }
}
