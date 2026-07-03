package erp.backEnd.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대량 품목 저장 전용 JDBC 배치 리포지토리.
 * created_date / last_modified_date 는 직접 채운다(감사 리스너 미적용).
 */
@Repository
@RequiredArgsConstructor
public class ItemBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    // 신규 품목의 초기 재고(stock_qty)와 낙관적 락 버전(version)은 0 으로 시작한다.
    private static final String INSERT_ITEM =
            "INSERT INTO item (item_code, item_name, standard_price, stock_qty, version, created_date, last_modified_date) " +
            "VALUES (?, ?, ?, 0, 0, ?, ?)";

    public void batchInsert(List<ItemRow> items) {
        for (int start = 0; start < items.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, items.size());
            final List<ItemRow> chunk = items.subList(start, end);

            jdbcTemplate.batchUpdate(INSERT_ITEM, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ItemRow r = chunk.get(i);
                    ps.setString(1, r.itemCode);
                    ps.setString(2, r.itemName);
                    ps.setBigDecimal(3, r.standardPrice);
                    ps.setTimestamp(4, Timestamp.valueOf(r.now));
                    ps.setTimestamp(5, Timestamp.valueOf(r.now));
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }

    public static class ItemRow {
        public final String itemCode;
        public final String itemName;
        public final BigDecimal standardPrice;
        public final LocalDateTime now;

        public ItemRow(String itemCode, String itemName, BigDecimal standardPrice, LocalDateTime now) {
            this.itemCode = itemCode;
            this.itemName = itemName;
            this.standardPrice = standardPrice;
            this.now = now;
        }
    }
}
