package erp.backEnd.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 대량 공급사 저장 전용 JDBC 배치 리포지토리.
 * vendor_code 가 PK(직접 지정)이므로 생성키 회수는 필요 없다.
 */
@Repository
@RequiredArgsConstructor
public class VendorBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    private static final String INSERT_VENDOR =
            "INSERT INTO vendor (vendor_code, vendor_name, created_date, last_modified_date) " +
            "VALUES (?, ?, ?, ?)";

    public void batchInsert(List<VendorRow> vendors) {
        for (int start = 0; start < vendors.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, vendors.size());
            final List<VendorRow> chunk = vendors.subList(start, end);

            jdbcTemplate.batchUpdate(INSERT_VENDOR, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    VendorRow r = chunk.get(i);
                    ps.setString(1, r.vendorCode);
                    ps.setString(2, r.vendorName);
                    ps.setTimestamp(3, Timestamp.valueOf(r.now));
                    ps.setTimestamp(4, Timestamp.valueOf(r.now));
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }

    public static class VendorRow {
        public final String vendorCode;
        public final String vendorName;
        public final LocalDateTime now;

        public VendorRow(String vendorCode, String vendorName, LocalDateTime now) {
            this.vendorCode = vendorCode;
            this.vendorName = vendorName;
            this.now = now;
        }
    }
}
