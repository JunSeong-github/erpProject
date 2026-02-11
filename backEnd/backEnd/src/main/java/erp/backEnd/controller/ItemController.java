package erp.backEnd.controller;

import erp.backEnd.dto.po.*;
import erp.backEnd.entity.Item;
import erp.backEnd.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("item")
@RequestMapping("items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/checkDuplicate")
    public ResponseEntity<Boolean> checkDuplicate(@RequestParam String itemCode) {
        boolean isDuplicate = itemService.existsByItemCode(itemCode);
        return ResponseEntity.ok(isDuplicate);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody ItemCreateRequest itemCreateRequest) {
        Item save = itemService.save(itemCreateRequest);
        return ResponseEntity.ok(save);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody ItemCreateRequest itemCreateRequest
    ) {
        itemService.update(id, itemCreateRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getDetail(@PathVariable Long id) {
        ItemResponse response = itemService.getDetail(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ItemResponse>> list(ItemSearchCondition itemSearchCondition, Pageable pageable) {
        Page<ItemResponse> List = itemService.findSearchPageComplex(itemSearchCondition, pageable);
        return ResponseEntity.ok(List);
    }

}
