package com.procurement.procurement.dto.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RequisitionRequestDTO {

    @NotBlank(message = "Requisition number is required")
    @Size(max = 50, message = "Requisition number cannot exceed 50 characters")
    private String requisitionNumber;

    private Long requestedByUserId;
    private String status;

    @NotEmpty(message = "Requisition must contain at least one item")
    @Valid
    private List<RequisitionItemDTO> items;

    public RequisitionRequestDTO() {
    }

    public RequisitionRequestDTO(String requisitionNumber, Long requestedByUserId, String status, List<RequisitionItemDTO> items) {
        this.requisitionNumber = requisitionNumber;
        this.requestedByUserId = requestedByUserId;
        this.status = status;
        this.items = items;
    }

    // ===================== Getters & Setters =====================
    public String getRequisitionNumber() {
        return requisitionNumber;
    }

    public void setRequisitionNumber(String requisitionNumber) {
        this.requisitionNumber = requisitionNumber;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(Long requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<RequisitionItemDTO> getItems() {
        return items;
    }

    public void setItems(List<RequisitionItemDTO> items) {
        this.items = items;
    }

    // ===================== Inner DTO for Requisition Items =====================
    public static class RequisitionItemDTO {

        @NotBlank(message = "Item name is required")
        @Size(max = 100, message = "Item name cannot exceed 100 characters")
        private String itemName;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        private Integer quantity;

        @PositiveOrZero(message = "Unit price cannot be negative")
        private Double unitPrice = 0.0;

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        private String description;

        public RequisitionItemDTO() {
        }

        public RequisitionItemDTO(String itemName, Integer quantity, Double unitPrice, String description) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.description = description;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(Double unitPrice) {
            this.unitPrice = unitPrice;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
