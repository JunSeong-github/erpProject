package erp.backEnd.entity;

import erp.backEnd.enumeration.UsageStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "stock_usage", indexes = {
        // 재고 조회 시 (품목별 + 승인상태) 사용량 집계를 인덱스로 처리하기 위한 복합 인덱스
        @Index(name = "idx_stock_usage_item_status", columnList = "item_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_usage_id", comment = "재고사용번호")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, comment = "사용용도")
    private String purpose;

    @Column(name = "usage_place", nullable = false, comment = "사용처")
    private String usagePlace;

    @Column(name = "usage_qty", nullable = false, comment = "사용량")
    private Long usageQty;

    @Column(name = "usage_date", comment = "사용일")
    private LocalDate usageDate;

    @Column(length = 500, comment = "비고")
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(comment = "사용승인상태")
    private UsageStatus status;

    @Column(comment = "반려사유")
    private String rejectReason;

    public static StockUsage create(Item item, String purpose, String usagePlace,
                                    Long usageQty, LocalDate usageDate, String remark) {
        StockUsage u = new StockUsage();
        u.item = item;
        u.purpose = purpose;
        u.usagePlace = usagePlace;
        u.usageQty = usageQty;
        u.usageDate = usageDate;
        u.remark = remark;
        u.status = UsageStatus.REQUESTED;
        return u;
    }

    public void approve() {
        if (this.status != UsageStatus.REQUESTED) {
            throw new IllegalStateException("요청 상태만 승인 가능합니다.");
        }
        this.status = UsageStatus.APPROVED;
    }

    public void reject(String reason) {
        if (this.status != UsageStatus.REQUESTED) {
            throw new IllegalStateException("요청 상태만 반려 가능합니다.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("반려 사유는 필수입니다.");
        }
        this.rejectReason = reason.trim();
        this.status = UsageStatus.REJECTED;
    }
}
