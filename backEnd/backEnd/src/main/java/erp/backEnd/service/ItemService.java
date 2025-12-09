package erp.backEnd.service;

import erp.backEnd.dto.po.ItemResponse;

import java.util.List;

public interface ItemService {

    List<ItemResponse> itemFindAll();
}
