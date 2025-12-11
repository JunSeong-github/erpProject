package erp.backEnd.service;

import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.Po;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PoService {

    List<PoResponse> findPoList();

    Page<PoResponse> findSearchPageComplex(PoSearchCondition condition, Pageable pageable);

    Po save(PoCreateRequest poCreateRequest);

    void approve(Long poId);

    PoResponse getDetail(Long id);

    void update(Long id, PoCreateRequest req);
}
