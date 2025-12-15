package erp.backEnd.dto.member;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.entity.Member;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class FindMemberResponseDto {

    private Long id;
    private String username;
    private int age;

    @QueryProjection
    public FindMemberResponseDto(Long id, String username, int age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }




    public static FindMemberResponseDto toDto(Member member) {
        return FindMemberResponseDto.builder()
                .id(member.getId())
                .username(member.getUsername())
                .age(member.getAge())
                .build();
    }

    public static List<FindMemberResponseDto> toListDto(List<Member> members) {
        return members.stream().map(FindMemberResponseDto::toDto).collect(Collectors.toList());
    }

}
