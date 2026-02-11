package erp.backEnd.service;

import erp.backEnd.dto.po.*;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemService {

    List<ItemResponse> itemFindAll();

    Boolean existsByItemCode(String itemCode);

    Item save(ItemCreateRequest itemCreateRequest);

    void update(Long id, ItemCreateRequest itemCreateRequest);

    ItemResponse getDetail(Long id);

    void delete(Long id);

    Page<ItemResponse> findSearchPageComplex(ItemSearchCondition condition, Pageable pageable);

}
