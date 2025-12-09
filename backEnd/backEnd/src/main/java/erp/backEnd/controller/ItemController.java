package erp.backEnd.controller;

import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.entity.Item;
import erp.backEnd.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("item")
@RequestMapping("items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public List<ItemResponse> getItems() {
        return itemService.itemFindAll();
    }

}
