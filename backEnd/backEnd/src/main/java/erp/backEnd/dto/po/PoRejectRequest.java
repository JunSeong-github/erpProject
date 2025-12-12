package erp.backEnd.dto.po;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PoRejectRequest {
    private String reason;

    @QueryProjection
    public PoRejectRequest(String reason) {
        this.reason = reason;
    }
}
