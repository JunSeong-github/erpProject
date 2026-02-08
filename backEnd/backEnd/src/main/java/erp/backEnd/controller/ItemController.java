package erp.backEnd.controller;

import erp.backEnd.dto.po.ItemCreateRequest;
import erp.backEnd.dto.po.ItemResponse;
import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.entity.Item;
import erp.backEnd.entity.Po;
import erp.backEnd.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody ItemCreateRequest itemCreateRequest) {
        Item save = itemService.save(itemCreateRequest);
        return ResponseEntity.ok(save);
    }

}
