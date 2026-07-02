package erp.backEnd.repository;

import erp.backEnd.entity.Po;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoRepository extends JpaRepository<Po, Long>, PoRepositoryCustom {

    /** 해당 공급사코드를 사용하는 발주가 존재하는지 (공급사 삭제 제한용) */
    boolean existsByVendor_VendorCode(String vendorCode);

}
