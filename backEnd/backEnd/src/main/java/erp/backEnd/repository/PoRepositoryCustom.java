package erp.backEnd.repository;

import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.Po;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PoRepositoryCustom {

    List<PoResponse> search();
//    List<PoResponse> basicSelect();
    Page<PoResponse> searchPageComplex(PoSearchCondition condition, Pageable pageable);

    Po findDetail(Long id);
}
