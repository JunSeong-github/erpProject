package erp.backEnd.service;

import erp.backEnd.dto.po.ItemCreateRequest;
import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;

import java.util.List;

public interface ItemService {

    List<ItemResponse> itemFindAll();

    Item save(ItemCreateRequest itemCreateRequest);
}
