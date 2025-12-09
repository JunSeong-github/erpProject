package erp.backEnd.repository;

import erp.backEnd.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long>, VendorRepositoryCustom {
    Optional<Vendor> findByVendorCode(String vendorCode);
}
