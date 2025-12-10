package erp.backEnd.controller;

import erp.backEnd.dto.po.PoCreateRequest;
import erp.backEnd.dto.po.PoResponse;
import erp.backEnd.dto.po.PoSearchCondition;
import erp.backEnd.entity.Po;
import erp.backEnd.service.PoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("po")
@RequestMapping("/po")
@RequiredArgsConstructor
public class PoController {

    private final PoService poService;

//    @GetMapping("list")
//    public ResponseEntity<List<PoResponse>> findPoList() {
//        List<PoResponse> poList = poService.findPoList();
//        return ResponseEntity.ok(poList);
//    }

    @GetMapping("/list")
    public ResponseEntity<Page<PoResponse>> list(PoSearchCondition poSearchCondition, Pageable pageable) {
        Page<PoResponse> List = poService.findSearchPageComplex(poSearchCondition, pageable);
        return ResponseEntity.ok(List);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody PoCreateRequest poCreateRequest) {
        Po save = poService.save(poCreateRequest);
        return ResponseEntity.ok(save);
    }

}
