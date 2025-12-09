package erp.backEnd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="item_id", comment = "품목번호") // db테이블명_// pk컬럼명
    private Long id;

    @Column(name ="item_code", nullable = false, comment = "품목코드 문서작성할때 필요함")
    private String itemCode;
    
    @Column(name ="item_name", nullable = false, comment = "품목명")
    private String itemName;

    @Column(name ="standard_price", precision = 15, scale = 2, nullable = false, comment = "품목가격(변동가능)")
    private BigDecimal standardPrice;

    @OneToMany(mappedBy = "item") // 일대다관계 이런식으로 매칭되어있는경우에는 fk없는쪽에 mappedby해준다
    private List<PoItem> poItems = new ArrayList<>();
}
