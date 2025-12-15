package erp.backEnd.service;

import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.repository.MemberRepository;

import java.util.List;

public interface MemberService{

   FindMemberResponseDto findMember(Long memberId);

   List<FindMemberResponseDto> findMemberList();

}
