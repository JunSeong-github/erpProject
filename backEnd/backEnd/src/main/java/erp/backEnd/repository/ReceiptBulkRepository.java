package erp.backEnd.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 대량 입고 저장 전용 JDBC 배치 리포지토리.
 * JPA(IDENTITY 전략)로는 insert 배치가 불가능하므로 여기서 JDBC batch 로 묶어서 처리한다.
 * (MySQL 은 rewriteBatchedStatements=true, PostgreSQL 은 reWriteBatchedInserts=true 가 켜져 있어야 실제로 묶임)
 *
 * 주의: JPA 감사(auditing)를 타지 않으므로 created_date / last_modified_date 는 직접 채운다.
 * created_by / last_modified_by 는 기존 JPA 저장과 동일하게(AuditorAware 미등록) NULL 로 둔다.
 */
@Repository
@RequiredArgsConstructor
public class ReceiptBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    /** 한 번의 executeBatch 로 보낼 최대 행 수 */
    private static final int BATCH_SIZE = 1000;

    private static final String INSERT_RECEIPT =
            "INSERT INTO receipt (po_id, receipt_date, remark, created_date, last_modified_date) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String INSERT_RECEIPT_LINE =
            "INSERT INTO receipt_line (receipt_id, po_item_id, received_qty, line_remark, created_date, last_modified_date) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    // 대량 입고분을 품목 재고 컬럼에 원자적으로 더한다(JDBC 경로라 엔티티를 우회하므로 여기서 직접 UPDATE).
    // version 도 함께 증가시켜 낙관적 락 버전을 일관되게 유지한다.
    private static final String UPDATE_ITEM_STOCK_ADD =
            "UPDATE item SET stock_qty = stock_qty + ?, version = version + 1 WHERE item_id = ?";

    /**
     * 입고 헤더들을 배치 insert 하고, 생성된 receipt_id 를 입력 순서대로 반환한다.
     */
    public List<Long> batchInsertReceipts(List<ReceiptHeaderRow> headers) {
        List<Long> ids = new ArrayList<>(headers.size());

        for (int start = 0; start < headers.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, headers.size());
            List<ReceiptHeaderRow> chunk = headers.subList(start, end);

            List<Long> chunkIds = jdbcTemplate.execute((Connection con) -> {
                List<Long> keys = new ArrayList<>(chunk.size());
                try (PreparedStatement ps = con.prepareStatement(INSERT_RECEIPT, Statement.RETURN_GENERATED_KEYS)) {
                    for (ReceiptHeaderRow h : chunk) {
                        ps.setLong(1, h.poId);
                        ps.setDate(2, Date.valueOf(h.receiptDate));
                        if (h.remark != null) ps.setString(3, h.remark); else ps.setNull(3, Types.VARCHAR);
                        ps.setTimestamp(4, Timestamp.valueOf(h.now));
                        ps.setTimestamp(5, Timestamp.valueOf(h.now));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        while (rs.next()) {
                            keys.add(rs.getLong(1));
                        }
                    }
                }
                return keys;
            });

            ids.addAll(chunkIds);
        }

        if (ids.size() != headers.size()) {
            throw new IllegalStateException(
                    "생성된 입고 헤더 키 개수 불일치: expected=" + headers.size() + ", actual=" + ids.size());
        }
        return ids;
    }

    /**
     * 입고 라인들을 배치 insert 한다.
     */
    public void batchInsertLines(List<ReceiptLineRow> lines) {
        for (int start = 0; start < lines.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, lines.size());
            final List<ReceiptLineRow> chunk = lines.subList(start, end);

            jdbcTemplate.batchUpdate(INSERT_RECEIPT_LINE, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ReceiptLineRow r = chunk.get(i);
                    ps.setLong(1, r.receiptId);
                    ps.setLong(2, r.poItemId);
                    ps.setLong(3, r.receivedQty);
                    if (r.lineRemark != null) ps.setString(4, r.lineRemark); else ps.setNull(4, Types.VARCHAR);
                    ps.setTimestamp(5, Timestamp.valueOf(r.now));
                    ps.setTimestamp(6, Timestamp.valueOf(r.now));
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }

    /**
     * 품목별 재고 증가분을 배치 UPDATE 한다(대량 입고분 반영).
     */
    public void batchIncreaseItemStock(List<ItemStockDelta> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        for (int start = 0; start < deltas.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, deltas.size());
            final List<ItemStockDelta> chunk = deltas.subList(start, end);

            jdbcTemplate.batchUpdate(UPDATE_ITEM_STOCK_ADD, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ItemStockDelta d = chunk.get(i);
                    ps.setLong(1, d.qty);
                    ps.setLong(2, d.itemId);
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }

    /** 품목 재고 증가분 값 홀더 */
    public static class ItemStockDelta {
        public final Long itemId;
        public final Long qty;

        public ItemStockDelta(Long itemId, Long qty) {
            this.itemId = itemId;
            this.qty = qty;
        }
    }

    /** 입고 헤더 insert 용 값 홀더 */
    public static class ReceiptHeaderRow {
        public final Long poId;
        public final LocalDate receiptDate;
        public final String remark;
        public final LocalDateTime now;

        public ReceiptHeaderRow(Long poId, LocalDate receiptDate, String remark, LocalDateTime now) {
            this.poId = poId;
            this.receiptDate = receiptDate;
            this.remark = remark;
            this.now = now;
        }
    }

    /** 입고 라인 insert 용 값 홀더 */
    public static class ReceiptLineRow {
        public final Long receiptId;
        public final Long poItemId;
        public final Long receivedQty;
        public final String lineRemark;
        public final LocalDateTime now;

        public ReceiptLineRow(Long receiptId, Long poItemId, Long receivedQty, String lineRemark, LocalDateTime now) {
            this.receiptId = receiptId;
            this.poItemId = poItemId;
            this.receivedQty = receivedQty;
            this.lineRemark = lineRemark;
            this.now = now;
        }
    }
}
