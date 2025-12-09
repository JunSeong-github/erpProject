package erp.backEnd.service;

import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.entity.Member;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    public FindMemberResponseDto findMember(Long memberId) {
        //리포지토리에서 Optional로 멤버 꺼냄
        Optional<Member> optionalMember = memberRepository.findByUsername(memberId);

        //멤버 없으면 에러 처리
        Member member = optionalMember.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //Member엔티티를 절대로 바로 컨트롤러로 리턴하면 안됨 -> DTO로 변환 후 리턴
        return FindMemberResponseDto.toDto(member);
    }

    public List<FindMemberResponseDto> findMemberList() {
        //Member엔티티를 절대로 바로 컨트롤러로 리턴하면 안됨 -> 존나 중요
        //1. 멤버 꺼냄
//        List<Member> memberList = memberRepository.findAll();
//        //2. DTO로 변환
//
//        return FindMemberResponseDto.toListDto(memberList);

        //MemberRepository에 상속해야함 추가했는데?.. dto벼ㅓㄴ환 쿼리에서 변환해서 가져오자너 그러네 했넹 지렸따 ㅇㅈ
        return memberRepository.search();
    }

}
