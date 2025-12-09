package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import erp.backEnd.dto.member.FindMemberResponseDto;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Member;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
public class ItemResponse {
    private Long id;                // Item PK
    private String itemCode;        // 품목코드
    private String itemName;        // 품목명
    private BigDecimal standardPrice; // 기준단가(변동 가능)

    @QueryProjection
    public ItemResponse(Long id, String itemCode, String itemName, BigDecimal standardPrice) {
        this.id = id;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.standardPrice = standardPrice;
    }

      public static ItemResponse toDto(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .standardPrice(item.getStandardPrice())
                .build();
      }

      public static List<ItemResponse> toListDto(List<Item> items) {
        return items.stream().map(ItemResponse::toDto).collect(Collectors.toList());
      }



}
