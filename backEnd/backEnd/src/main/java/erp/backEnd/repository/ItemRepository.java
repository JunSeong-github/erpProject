package erp.backEnd.repository;

import erp.backEnd.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, ItemRepositoryCustom {
    boolean existsByItemCode(String itemCode);

    Optional<Item> findById(Long id);
}
