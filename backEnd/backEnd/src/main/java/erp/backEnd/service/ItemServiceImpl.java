package erp.backEnd.service;

import erp.backEnd.dto.po.ItemCreateRequest;
import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Boolean existsByItemCode(String itemCode) {
        return itemRepository.existsByItemCode(itemCode);
    }

    @Override
    @Transactional
    public Item save(ItemCreateRequest req) {

        if (itemRepository.existsByItemCode(req.getItemCode())) {
            throw new IllegalArgumentException("이미 존재하는 품목코드입니다.");
        }

        Item item = Item.of(
                req.getItemCode(),
                req.getItemName(),
                req.getStandardPrice()
        );

        Item savedItem = itemRepository.save(item);

        return savedItem;
    }

    @Override
    @Transactional
    public void update(Long id, ItemCreateRequest req) {

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("저장된 품목을 찾을 수 없습니다."));

        String oldItemCode = item.getItemCode();

        if (!oldItemCode.equals(req.getItemCode())) {
            if (itemRepository.existsByItemCode(req.getItemCode())) {
                throw new IllegalArgumentException("이미 존재하는 품목코드입니다.");
            }
        }

        item.updateForm(req);

    }

}
