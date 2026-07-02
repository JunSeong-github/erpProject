package erp.backEnd.service;

import erp.backEnd.dto.po.*;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ItemService {

    List<ItemResponse> itemFindAll();

    Boolean existsByItemCode(String itemCode);

    Boolean existsByItemName(String itemName);

    Item save(ItemCreateRequest itemCreateRequest);

    void update(Long id, ItemCreateRequest itemCreateRequest);

    ItemResponse getDetail(Long id);

    void delete(Long id);

    Page<ItemResponse> findSearchPageComplex(ItemSearchCondition condition, Pageable pageable);

    Page<StockResponse> findStockPage(ItemSearchCondition condition, Pageable pageable);

    /** [대량 품목 - 미리보기] 엑셀을 파싱+검증만 하고 저장하지 않는다. */
    BulkItemPreviewResponse bulkPreview(MultipartFile file);

    /** [대량 품목 - 확정 저장] 재검증 후 batch insert. 오류가 하나라도 있으면 전체 롤백. */
    BulkItemResponse bulkUpload(MultipartFile file);

}
