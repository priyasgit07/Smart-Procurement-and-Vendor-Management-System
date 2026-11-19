package com.procurement.procurement.mapper;

import com.procurement.procurement.dto.procurement.PurchaseOrderRequestDTO;
import com.procurement.procurement.dto.procurement.PurchaseOrderResponseDTO;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderItem;
import com.procurement.procurement.entity.vendor.Vendor;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class PurchaseOrderMapper {

    // ===================== Request DTO → Entity =====================
    public static PurchaseOrder toEntity(PurchaseOrderRequestDTO dto, Vendor vendor) {
        if (dto == null) return null;

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(dto.getPoNumber());
        po.setVendor(vendor);
        po.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());

        // Map items
        if (dto.getItems() != null) {
            po.setItems(
                    dto.getItems().stream()
                            .map(itemDto -> {
                                PurchaseOrderItem item = new PurchaseOrderItem();
                                item.setProductName(itemDto.getProductName());
                                item.setQuantity(itemDto.getQuantity());
                                item.setUnitPrice(itemDto.getUnitPrice());
                                item.setPurchaseOrder(po);
                                return item;
                            })
                            .collect(Collectors.toList())
            );
        }

        return po;
    }

    // ===================== Entity → Response DTO =====================
    public static PurchaseOrderResponseDTO toResponseDTO(PurchaseOrder po) {
        if (po == null) return null;

        PurchaseOrderResponseDTO dto = new PurchaseOrderResponseDTO();
        dto.setId(po.getId());
        dto.setPoNumber(po.getPoNumber());
        dto.setStatus(po.getStatus());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());

        if (po.getVendor() != null) {
            dto.setVendorId(po.getVendor().getId());
            dto.setVendorName(po.getVendor().getName());
        }

        if (po.getRequisition() != null) {
            dto.setRequisitionId(po.getRequisition().getId());
            dto.setRequisitionNumber(po.getRequisition().getRequisitionNumber());
        }

        if (po.getItems() != null) {
            dto.setItems(po.getItems().stream()
                    .map(item -> new PurchaseOrderResponseDTO.Item(
                            item.getId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    ))
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}
