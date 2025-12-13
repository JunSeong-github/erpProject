package erp.backEnd.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private Po po;

    @Column(nullable = false)
    private LocalDate receiptDate;

    @Column(length = 1000)
    private String remark;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceiptLine> lines = new ArrayList<>();

    public static Receipt create(Po po, LocalDate receiptDate, String remark) {
        Receipt r = new Receipt();
        r.po = po;
        r.receiptDate = receiptDate;
        r.remark = remark;
        return r;
    }

    public void addLine(ReceiptLine line) {
        lines.add(line);
        line.setReceipt(this);
    }
}
