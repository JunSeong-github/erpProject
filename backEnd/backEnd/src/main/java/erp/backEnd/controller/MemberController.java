package erp.backEnd.controller;

import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.service.MemberService;
//import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("members")
@RequestMapping("members")
@RequiredArgsConstructor()
//@Tag(name = "멤버", description = "멤버 조회")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("{id}")
    public ResponseEntity<FindMemberResponseDto> findMemberById(@PathVariable Long id) {

        FindMemberResponseDto result = memberService.findMember(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("list")
    public ResponseEntity<List<FindMemberResponseDto>> findMemberList() {
        List<FindMemberResponseDto> result = memberService.findMemberList();
        return ResponseEntity.ok(result); //굿 테스트 코드 작성하든지 스웨거 들어가보면될거같은데 스웨거 ㄴㄴ 그거 기본주소 잇을텐데 http://localhost:8080/swagger-ui/index.html 스웨거 됨? 지금 빌드오류남 너도 보이나 오류내용 난 몰루 성공이라 뜨는데
    }

}
