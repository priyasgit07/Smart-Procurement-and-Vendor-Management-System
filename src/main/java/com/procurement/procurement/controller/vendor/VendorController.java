package com.procurement.procurement.controller.vendor;

import com.procurement.procurement.dto.vendor.VendorRequestDTO;
import com.procurement.procurement.dto.vendor.VendorResponseDTO;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.mapper.VendorMapper;
import com.procurement.procurement.service.vendor.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendor")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    // ================= CREATE VENDOR =================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    public ResponseEntity<VendorResponseDTO> createVendor(@Valid @RequestBody VendorRequestDTO request) {
        Vendor entity = VendorMapper.toEntity(request);
        Vendor saved = vendorService.createVendor(entity);
        return ResponseEntity.ok(VendorMapper.toResponseDTO(saved));
    }

    // ================= GET ALL VENDORS =================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    public ResponseEntity<List<VendorResponseDTO>> getAllVendors() {
        List<VendorResponseDTO> list = vendorService.getAllVendors().stream()
                .map(VendorMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ================= GET VENDOR BY ID =================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
    public ResponseEntity<VendorResponseDTO> getVendorById(@PathVariable Long id) {
        Vendor vendor = vendorService.getVendorById(id);
        return ResponseEntity.ok(VendorMapper.toResponseDTO(vendor));
    }

    // ================= UPDATE VENDOR =================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VendorResponseDTO> updateVendor(@PathVariable Long id,
                                                          @Valid @RequestBody VendorRequestDTO request) {
        Vendor entity = VendorMapper.toEntity(request);
        Vendor updated = vendorService.updateVendor(id, entity);
        return ResponseEntity.ok(VendorMapper.toResponseDTO(updated));
    }

    // ================= DELETE VENDOR =================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok("Vendor deleted successfully");
    }
}
