package erp.backEnd.service;

import erp.backEnd.dto.po.*;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.enumeration.PoStatus;
import erp.backEnd.exception.BusinessException;
import erp.backEnd.exception.ErrorCode;
import erp.backEnd.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    @Override
    @Transactional
    public ItemResponse getDetail(Long id) {
        Optional<Item> optionalItem = itemRepository.findById(id);

        Item item = optionalItem.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ItemResponse.toDto(item);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("저장된 품목을 찾을 수 없습니다."));

        itemRepository.deleteById(id);
    }

    public Page<ItemResponse> findSearchPageComplex(ItemSearchCondition itemSearchCondition, Pageable pageable){
        return itemRepository.searchPageComplex(itemSearchCondition, pageable);
    }

    @Override
    public Page<StockResponse> findStockPage(ItemSearchCondition itemSearchCondition, Pageable pageable){
        return itemRepository.searchStockPage(itemSearchCondition, pageable);
    }

}
