package erp.backEnd.repository;

import erp.backEnd.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, String>, VendorRepositoryCustom {
    Optional<Vendor> findByVendorCode(String vendorCode);

    boolean existsByVendorCode(String vendorCode);

    boolean existsByVendorName(String vendorName);

    /** 대량 발주 업로드: 공급사코드 목록으로 한 번에 조회 */
    List<Vendor> findByVendorCodeIn(Collection<String> vendorCodes);

    /** 대량 공급사 업로드: 공급사명 목록으로 한 번에 조회(중복 검증용) */
    List<Vendor> findByVendorNameIn(Collection<String> vendorNames);
}
