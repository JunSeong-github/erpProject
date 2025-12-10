package erp.backEnd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PoItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="po_item_id", comment = "발주라인번호") // db테이블명_// pk컬럼명
    private Long poItemId;

    @ManyToOne(fetch = FetchType.LAZY) // 다대일관계 기본적으로 실무에선 무조건 fetch = FetchType.LAZY 지연로딩으로 세팅해야 성능최적화하기 좋다 / 기본값은 즉시로딩인 eager임
    // member값을 꺼낼떄 member는 가져오고 team값은 프록시라는 가짜객체만 만들고 team내용을 가져올때 그때 쿼리보내서 로딩하는게 지연로딩이고 fetch = FetchType.LAZY 이부분이다
    @JoinColumn(name = "po_id")
    private Po po;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false, comment = "수량")
    private Long quantity;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false, comment = "발주당시가격")
    private BigDecimal unitPrice;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false, comment = "수량 * 가격 합계값")
    private BigDecimal amount;

    public static PoItem of(Po po, Item item, Long quantity, BigDecimal unitPrice, BigDecimal amount) {
        PoItem poItem = new PoItem();
        poItem.po = po;
        poItem.item = item;
        poItem.quantity = quantity;
        poItem.unitPrice = unitPrice;
        poItem.amount = amount;
        return poItem;
    }

}
