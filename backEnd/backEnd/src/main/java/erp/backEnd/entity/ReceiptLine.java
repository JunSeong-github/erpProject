package erp.backEnd.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "receipt_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_line_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_item_id", nullable = false)
    private PoItem poItem;

    @Column(nullable = false)
    private Long receivedQty;

    @Column(length = 500)
    private String lineRemark;

    public static ReceiptLine create(PoItem poItem, Long receivedQty, String lineRemark) {
        ReceiptLine l = new ReceiptLine();
        l.poItem = poItem;
        l.receivedQty = receivedQty;
        l.lineRemark = lineRemark;
        return l;
    }

    void setReceipt(Receipt receipt) { this.receipt = receipt; }

}
