package erp.backEnd.entity;

import erp.backEnd.dto.po.ItemCreateRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item", indexes = {
        // 재고/품목 목록 정렬(ORDER BY item_name ASC) + 페이징용 인덱스
        @Index(name = "idx_item_name", columnList = "item_name")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="item_id", comment = "품목번호") // db테이블명_// pk컬럼명
    private Long id;

    @Column(name ="item_code", nullable = false, unique = true, comment = "품목코드 문서작성할때 필요함")
    private String itemCode;
    
    @Column(name ="item_name", nullable = false, comment = "품목명")
    private String itemName;

    @Column(name ="standard_price", precision = 15, scale = 2, nullable = false, comment = "품목가격(변동가능)")
    private BigDecimal standardPrice;

    @OneToMany(mappedBy = "item") // 일대다관계 이런식으로 매칭되어있는경우에는 fk없는쪽에 mappedby해준다
    private List<PoItem> poItems = new ArrayList<>();

    // 현재 재고수량(정답값). 입고 시 +, 재고사용 승인 시 - 로 원자적 갱신한다.
    // 집계(입고합-승인사용합)는 검증/대사(reconciliation)용으로만 남겨둔다.
    @Column(name = "stock_qty", nullable = false, comment = "현재 재고수량(입고합-승인사용합, 정답값)")
    private Long stockQty;

    // 낙관적 락 버전. 품목 편집 등 엔티티 단위 read-modify-write 시 동시성 충돌을 감지한다.
    @Version
    @Column(name = "version", nullable = false, comment = "낙관적 락 버전")
    private Long version;

    public static Item of(String itemCode, String itemName, BigDecimal standardPrice){
        Item item = new Item();
        item.itemCode=itemCode;
        item.itemName=itemName;
        item.standardPrice=standardPrice;
        item.stockQty=0L; // 신규 품목의 초기 재고는 0
        return item;
    }

    public void updateForm(ItemCreateRequest req){
        this.itemCode=req.getItemCode();
        this.itemName=req.getItemName();
        this.standardPrice=req.getStandardPrice();
    }

}
