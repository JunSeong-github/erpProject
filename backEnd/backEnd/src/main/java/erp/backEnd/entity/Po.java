package erp.backEnd.entity;

import erp.backEnd.enumeration.PoStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Po extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="po_id", comment = "발주번호") // db테이블명_// pk컬럼명
    private Long id;

//    @Column(nullable = false, comment = "공급사_코드") //  ex) 10001
//    private String vendorCode;

//    @Column(nullable = false, comment = "공급사명")
//    private String vendorName;

    @Column(nullable = false, comment = "납기 요청일")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(comment = "발주상태")
    private PoStatus poStatus;

    @Column(comment = "비고")
    private String etc;

    @OneToMany(mappedBy = "po") // 일대다관계 이런식으로 매칭되어있는경우에는 fk없는쪽에 mappedby해준다
    private List<PoItem> poItems = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_code")
    private Vendor vendor;

    public static Po of(Vendor vendor, LocalDate deliveryDate, PoStatus poStatus, String etc) {
        Po po = new Po();
        po.vendor = vendor;
        po.deliveryDate = deliveryDate;
        po.poStatus = poStatus;
        po.etc = etc;
        return po;
    }
}
