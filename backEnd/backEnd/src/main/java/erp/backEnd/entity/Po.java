package erp.backEnd.entity;

import erp.backEnd.dto.po.PoCreateRequest;
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

    @Column(comment = "반려사유")
    private String rejectReason;

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

    public void approve() {
        if (this.poStatus != PoStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태만 승인 가능합니다.");
        }
        this.poStatus = PoStatus.APPROVED;
    }

    public void reject(String reason) {
        if (this.poStatus != PoStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태만 반려 가능합니다.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("반려 사유는 필수입니다.");
        }
        this.rejectReason = reason.trim();
        this.poStatus = PoStatus.REJECTED;
    }

    public void updateFrom(PoCreateRequest req, Vendor vendor) {
        if (this.poStatus != PoStatus.DRAFT) {
            throw new IllegalStateException("승인된 발주는 수정할 수 없습니다.");
        }

        this.vendor = vendor;
        this.deliveryDate = req.getDeliveryDate();
//        this.poStatus = req.getPoStatus();
        this.etc = req.getEtc();

    }
}
