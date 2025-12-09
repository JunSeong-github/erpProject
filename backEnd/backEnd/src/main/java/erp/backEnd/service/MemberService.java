package erp.backEnd.service;

import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.repository.MemberRepository;

import java.util.List;

public interface MemberService{
//여기말고 서비스 임플

   FindMemberResponseDto findMember(Long memberId);

   List<FindMemberResponseDto> findMemberList();

}
