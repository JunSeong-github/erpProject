package erp.backEnd.service;

import erp.backEnd.dto.po.ItemCreateRequest;
import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    public List<ItemResponse> itemFindAll() {
        List<Item> itemDdlList = itemRepository.findAll();
        return ItemResponse.toListDto(itemDdlList);
    }

    @Override
    public Item save(ItemCreateRequest req) {

        Item item = Item.of(
                req.getItemCode(),
                req.getItemName(),
                req.getStandardPrice()
        );

        Item savedItem = itemRepository.save(item);

        return savedItem;
    }
}
