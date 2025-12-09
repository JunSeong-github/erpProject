package erp.backEnd.repository;

import erp.backEnd.dto.member.FindMemberResponseDto;

import java.util.List;

public interface MemberRepositoryCustom {
    List<FindMemberResponseDto> search();
}
