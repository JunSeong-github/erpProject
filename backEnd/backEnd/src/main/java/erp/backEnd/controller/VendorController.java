package erp.backEnd.controller;

import erp.backEnd.dto.po.VendorResponse;
import erp.backEnd.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("vendor")
@RequestMapping("vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public List<VendorResponse> getVendors() {
        return vendorService.findVendorAll();
    }

}
