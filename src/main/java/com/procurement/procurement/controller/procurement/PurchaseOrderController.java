package com.procurement.procurement.controller.procurement;

import com.procurement.procurement.dto.procurement.PurchaseOrderRequestDTO;
import com.procurement.procurement.dto.procurement.PurchaseOrderResponseDTO;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.mapper.PurchaseOrderMapper;
import com.procurement.procurement.service.procurement.PurchaseOrderService;
import com.procurement.procurement.service.procurement.RequisitionService;
import com.procurement.procurement.service.vendor.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/procurement/purchase-order")
@PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final VendorService vendorService;
    private final RequisitionService requisitionService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService,
                                   VendorService vendorService,
                                   RequisitionService requisitionService) {
        this.purchaseOrderService = purchaseOrderService;
        this.vendorService = vendorService;
        this.requisitionService = requisitionService;
    }

    // ===================== Create Purchase Order =====================
    @PostMapping("/create")
    public ResponseEntity<PurchaseOrderResponseDTO> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequestDTO request) {
        Vendor vendor = vendorService.getVendorById(request.getVendorId());
        PurchaseOrder po = PurchaseOrderMapper.toEntity(request, vendor);

        if (request.getRequisitionId() != null) {
            Requisition req = requisitionService.getRequisitionById(request.getRequisitionId());
            po.setRequisition(req);
        }

        PurchaseOrder saved = purchaseOrderService.createPurchaseOrder(po);
        return ResponseEntity.ok(PurchaseOrderMapper.toResponseDTO(saved));
    }

    // ===================== Get all Purchase Orders =====================
    @GetMapping("/all")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getAllPurchaseOrders() {
        List<PurchaseOrderResponseDTO> list = purchaseOrderService.getAllPurchaseOrders().stream()
                .map(PurchaseOrderMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ===================== Get Purchase Order by ID =====================
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDTO> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrder po = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(PurchaseOrderMapper.toResponseDTO(po));
    }

    // ===================== Update Purchase Order Status =====================
    @PatchMapping("/update-status/{id}")
    public ResponseEntity<String> updateStatus(@PathVariable Long id,
                                               @RequestParam String status) {
        PurchaseOrder updated = purchaseOrderService.updatePurchaseOrderStatus(id, status);
        return ResponseEntity.ok("Purchase Order status updated to " + updated.getStatus());
    }
}
