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

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody ItemCreateRequest itemCreateRequest
    ) {
        itemService.update(id, itemCreateRequest);
        return ResponseEntity.ok().build();
    }

}
