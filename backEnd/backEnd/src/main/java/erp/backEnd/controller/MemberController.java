package erp.backEnd.controller;

import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "멤버(Member)", description = "회원 단건·목록 조회 API")
@RestController("members")
@RequestMapping("members")
@RequiredArgsConstructor()
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 단건 조회", description = "회원 ID로 단건 정보를 조회한다.")
    @GetMapping("{id}")
    public ResponseEntity<FindMemberResponseDto> findMemberById(
            @Parameter(description = "회원 ID", example = "1") @PathVariable Long id) {

        FindMemberResponseDto result = memberService.findMember(id);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "회원 목록 조회", description = "전체 회원 목록을 반환한다.")
    @GetMapping("list")
    public ResponseEntity<List<FindMemberResponseDto>> findMemberList() {
        List<FindMemberResponseDto> result = memberService.findMemberList();
        return ResponseEntity.ok(result);
    }

}
