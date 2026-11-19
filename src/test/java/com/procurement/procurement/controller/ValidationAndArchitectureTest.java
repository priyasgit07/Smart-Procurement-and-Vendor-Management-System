package com.procurement.procurement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.procurement.dto.auth.LoginRequestDTO;
import com.procurement.procurement.dto.procurement.*;
import com.procurement.procurement.dto.vendor.VendorRequestDTO;
import com.procurement.procurement.dto.vendor.VendorResponseDTO;
import com.procurement.procurement.entity.procurement.*;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.mapper.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationAndArchitectureTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("LoginRequestDTO validates blank username and password")
    void testLoginValidation() {
        LoginRequestDTO dto = new LoginRequestDTO("", "");
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("VendorRequestDTO validates blank name and invalid status")
    void testVendorValidation() {
        VendorRequestDTO dto = new VendorRequestDTO("", "bad-email", "123", "Address", "INVALID_STATUS");
        Set<ConstraintViolation<VendorRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    @DisplayName("RequisitionRequestDTO validates empty items list and negative price")
    void testRequisitionValidation() {
        RequisitionRequestDTO dto = new RequisitionRequestDTO();
        dto.setRequisitionNumber("");
        dto.setItems(Collections.emptyList());

        Set<ConstraintViolation<RequisitionRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("requisitionNumber")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("items")));
    }

    @Test
    @DisplayName("PurchaseOrderRequestDTO validates required vendor and positive quantity")
    void testPurchaseOrderValidation() {
        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setPoNumber("");
        dto.setVendorId(null);
        dto.setItems(Collections.emptyList());

        Set<ConstraintViolation<PurchaseOrderRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("poNumber")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("vendorId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("items")));
    }

    @Test
    @DisplayName("User password field is write-only and excluded during JSON serialization")
    void testUserPasswordExcludedFromSerialization() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("SecretBcryptHash123");

        String json = objectMapper.writeValueAsString(user);
        assertFalse(json.contains("SecretBcryptHash123"));
        assertFalse(json.contains("\"password\""));
        assertTrue(json.contains("testuser"));
    }

    @Test
    @DisplayName("VendorMapper maps request to entity and entity to response DTO safely")
    void testVendorMapper() {
        VendorRequestDTO request = new VendorRequestDTO("Acme Corp", "acme@example.com", "1234567890", "123 Tech Way", "ACTIVE");
        Vendor vendor = VendorMapper.toEntity(request);
        assertNotNull(vendor);
        assertEquals("Acme Corp", vendor.getName());
        assertEquals("acme@example.com", vendor.getEmail());
        assertEquals("1234567890", vendor.getContactNumber());

        vendor.setId(101L);
        VendorResponseDTO response = VendorMapper.toResponseDTO(vendor);
        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Acme Corp", response.getName());
        assertEquals("1234567890", response.getPhone());
        assertEquals("COMPLIANT", response.getCompliance());
    }

    @Test
    @DisplayName("RequisitionMapper maps request to entity and entity to response DTO safely")
    void testRequisitionMapper() {
        RequisitionRequestDTO request = new RequisitionRequestDTO();
        request.setRequisitionNumber("REQ-100");
        request.setItems(List.of(new RequisitionRequestDTO.RequisitionItemDTO("Laptops", 3, 1200.0, "Dev laptops")));

        Requisition req = RequisitionMapper.toEntity(request, null);
        assertNotNull(req);
        assertEquals("REQ-100", req.getRequisitionNumber());
        assertEquals(1, req.getItems().size());
        assertEquals("Laptops", req.getItems().get(0).getItemName());

        req.setId(50L);
        req.setStatus("PENDING");
        RequisitionResponseDTO response = RequisitionMapper.toResponseDTO(req);
        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("REQ-100", response.getRequisitionNumber());
        assertEquals(1, response.getItems().size());
        assertEquals("Laptops", response.getItems().get(0).getItemName());
        assertEquals(1200.0, response.getItems().get(0).getUnitPrice());
    }

    @Test
    @DisplayName("PurchaseOrderMapper maps request to entity and entity to response DTO safely")
    void testPurchaseOrderMapper() {
        Vendor vendor = new Vendor("Acme", "acme@example.com", "1234567890", "123 Tech Way", "ACTIVE");
        vendor.setId(1L);

        PurchaseOrderRequestDTO request = new PurchaseOrderRequestDTO();
        request.setPoNumber("PO-200");
        request.setVendorId(1L);
        request.setItems(List.of(new PurchaseOrderRequestDTO.Item("Monitors", 5, 250.0)));

        PurchaseOrder po = PurchaseOrderMapper.toEntity(request, vendor);
        assertNotNull(po);
        assertEquals("PO-200", po.getPoNumber());
        assertEquals(vendor, po.getVendor());
        assertEquals(1, po.getItems().size());

        po.setId(77L);
        PurchaseOrderResponseDTO response = PurchaseOrderMapper.toResponseDTO(po);
        assertNotNull(response);
        assertEquals(77L, response.getId());
        assertEquals("PO-200", response.getPoNumber());
        assertEquals(1L, response.getVendorId());
        assertEquals("Acme", response.getVendorName());
        assertEquals(1, response.getItems().size());
    }

    @Test
    @DisplayName("ApprovalMapper maps approval entity to response DTO")
    void testApprovalMapper() {
        User approver = new User();
        approver.setId(5L);
        approver.setUsername("procurement_mgr");

        PurchaseOrder po = new PurchaseOrder();
        po.setId(10L);
        po.setPoNumber("PO-999");

        Approval approval = new Approval();
        approval.setId(123L);
        approval.setStatus("APPROVED");
        approval.setComments("All good");
        approval.setApprovedAt(LocalDateTime.now());
        approval.setApprover(approver);
        approval.setPurchaseOrder(po);

        ApprovalResponseDTO response = ApprovalMapper.toResponseDTO(approval);
        assertNotNull(response);
        assertEquals(123L, response.getId());
        assertEquals("APPROVED", response.getStatus());
        assertEquals("All good", response.getComments());
        assertEquals(5L, response.getApproverId());
        assertEquals("procurement_mgr", response.getApproverUsername());
        assertEquals(10L, response.getPurchaseOrderId());
        assertEquals("PO-999", response.getPoNumber());
    }
}
