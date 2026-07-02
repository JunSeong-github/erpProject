package erp.backEnd.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
 * 대량 발주 저장 전용 JDBC 배치 리포지토리.
 * JPA(IDENTITY 전략)로는 insert 배치가 불가능하므로 JDBC batch 로 헤더(po)/라인(po_item)을 묶어서 저장한다.
 * (MySQL 은 rewriteBatchedStatements=true, PostgreSQL 은 reWriteBatchedInserts=true 가 켜져 있어야 실제로 묶임)
 *
 * 주의: JPA 감사(auditing)를 타지 않으므로 created_date / last_modified_date 는 직접 채운다.
 * created_by / last_modified_by 는 기존 JPA 저장과 동일하게 NULL 로 둔다.
 */
@Repository
@RequiredArgsConstructor
public class PoBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    private static final String INSERT_PO =
            "INSERT INTO po (delivery_date, po_status, etc, vendor_code, created_date, last_modified_date) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_PO_ITEM =
            "INSERT INTO po_item (po_id, item_id, quantity, unit_price, amount, created_date, last_modified_date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    /** 발주 헤더들을 배치 insert 하고, 생성된 po_id 를 입력 순서대로 반환한다. */
    public List<Long> batchInsertPos(List<PoHeaderRow> headers) {
        List<Long> ids = new ArrayList<>(headers.size());

        for (int start = 0; start < headers.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, headers.size());
            List<PoHeaderRow> chunk = headers.subList(start, end);

            List<Long> chunkIds = jdbcTemplate.execute((Connection con) -> {
                List<Long> keys = new ArrayList<>(chunk.size());
                try (PreparedStatement ps = con.prepareStatement(INSERT_PO, Statement.RETURN_GENERATED_KEYS)) {
                    for (PoHeaderRow h : chunk) {
                        ps.setDate(1, Date.valueOf(h.deliveryDate));
                        ps.setString(2, h.poStatus);
                        if (h.etc != null) ps.setString(3, h.etc); else ps.setNull(3, Types.VARCHAR);
                        ps.setString(4, h.vendorCode);
                        ps.setTimestamp(5, Timestamp.valueOf(h.now));
                        ps.setTimestamp(6, Timestamp.valueOf(h.now));
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
                    "생성된 발주 헤더 키 개수 불일치: expected=" + headers.size() + ", actual=" + ids.size());
        }
        return ids;
    }

    /** 발주 라인들을 배치 insert 한다. */
    public void batchInsertPoItems(List<PoItemRow> lines) {
        for (int start = 0; start < lines.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, lines.size());
            final List<PoItemRow> chunk = lines.subList(start, end);

            jdbcTemplate.batchUpdate(INSERT_PO_ITEM, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    PoItemRow r = chunk.get(i);
                    ps.setLong(1, r.poId);
                    ps.setLong(2, r.itemId);
                    ps.setLong(3, r.quantity);
                    ps.setBigDecimal(4, r.unitPrice);
                    ps.setBigDecimal(5, r.amount);
                    ps.setTimestamp(6, Timestamp.valueOf(r.now));
                    ps.setTimestamp(7, Timestamp.valueOf(r.now));
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }

    /** 발주 헤더 insert 용 값 홀더 */
    public static class PoHeaderRow {
        public final LocalDate deliveryDate;
        public final String poStatus;
        public final String etc;
        public final String vendorCode;
        public final LocalDateTime now;

        public PoHeaderRow(LocalDate deliveryDate, String poStatus, String etc, String vendorCode, LocalDateTime now) {
            this.deliveryDate = deliveryDate;
            this.poStatus = poStatus;
            this.etc = etc;
            this.vendorCode = vendorCode;
            this.now = now;
        }
    }

    /** 발주 라인 insert 용 값 홀더 */
    public static class PoItemRow {
        public final Long poId;
        public final Long itemId;
        public final Long quantity;
        public final BigDecimal unitPrice;
        public final BigDecimal amount;
        public final LocalDateTime now;

        public PoItemRow(Long poId, Long itemId, Long quantity, BigDecimal unitPrice, BigDecimal amount, LocalDateTime now) {
            this.poId = poId;
            this.itemId = itemId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.amount = amount;
            this.now = now;
        }
    }
}
