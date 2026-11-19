package com.procurement.procurement.dto.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PurchaseOrderRequestDTO {

    @NotBlank(message = "PO number is required")
    @Size(max = 50, message = "PO number cannot exceed 50 characters")
    private String poNumber;

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    private Long requisitionId;
    private String status;

    @NotEmpty(message = "Purchase order must contain at least one item")
    @Valid
    private List<Item> items;

    public static class Item {

        @NotBlank(message = "Product name is required")
        @Size(max = 100, message = "Product name cannot exceed 100 characters")
        private String productName;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price cannot be negative")
        private Double unitPrice;

        public Item() {
        }

        public Item(String productName, Integer quantity, Double unitPrice) {
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
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
    }

    public PurchaseOrderRequestDTO() {
    }

    public PurchaseOrderRequestDTO(String poNumber, Long vendorId, Long requisitionId, String status, List<Item> items) {
        this.poNumber = poNumber;
        this.vendorId = vendorId;
        this.requisitionId = requisitionId;
        this.status = status;
        this.items = items;
    }

    // Getters & Setters
    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public Long getRequisitionId() {
        return requisitionId;
    }

    public void setRequisitionId(Long requisitionId) {
        this.requisitionId = requisitionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
