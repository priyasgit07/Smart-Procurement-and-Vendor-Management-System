package com.procurement.procurement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurement.procurement.dto.procurement.PurchaseOrderRequestDTO;
import com.procurement.procurement.dto.procurement.RequisitionRequestDTO;
import com.procurement.procurement.dto.vendor.VendorRequestDTO;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderItem;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.procurement.RequisitionItem;
import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.RoleRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import com.procurement.procurement.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProcurementApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String adminToken;
    private String managerToken;
    private String employeeToken;
    private User employeeUser;
    private User otherEmployeeUser;

    @BeforeEach
    void setUp() {
        Role roleEmployee = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(new Role("ROLE_EMPLOYEE", "Employee")));
        Role roleManager = roleRepository.findByName("ROLE_PROCUREMENT_MANAGER").orElseGet(() -> roleRepository.save(new Role("ROLE_PROCUREMENT_MANAGER", "Manager")));
        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin")));

        employeeUser = userRepository.findByUsername("api_emp1").orElseGet(() -> {
            User u = new User("api_emp1", "api_emp1@procurement.com", "pass", "Employee One");
            u.setRoles(Set.of(roleEmployee));
            return userRepository.save(u);
        });

        otherEmployeeUser = userRepository.findByUsername("api_emp2").orElseGet(() -> {
            User u = new User("api_emp2", "api_emp2@procurement.com", "pass", "Employee Two");
            u.setRoles(Set.of(roleEmployee));
            return userRepository.save(u);
        });

        userRepository.findByUsername("api_mgr").orElseGet(() -> {
            User u = new User("api_mgr", "api_mgr@procurement.com", "pass", "Manager User");
            u.setRoles(Set.of(roleManager));
            return userRepository.save(u);
        });

        userRepository.findByUsername("api_admin").orElseGet(() -> {
            User u = new User("api_admin", "api_admin@procurement.com", "pass", "Admin User");
            u.setRoles(Set.of(roleAdmin));
            return userRepository.save(u);
        });

        adminToken = jwtUtil.generateToken("api_admin");
        managerToken = jwtUtil.generateToken("api_mgr");
        employeeToken = jwtUtil.generateToken("api_emp1");
    }

    // =========================================================================
    // 1. VENDOR REST API TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/vendor: Manager creates vendor, returns 200 and VendorResponseDTO")
    void testCreateVendorSuccess() throws Exception {
        VendorRequestDTO dto = new VendorRequestDTO("Prime Hardware Ltd", "prime@hardware.com", "999-888-777", "42 Tech Blvd", "ACTIVE");

        mockMvc.perform(post("/api/vendor")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Prime Hardware Ltd"))
                .andExpect(jsonPath("$.email").value("prime@hardware.com"))
                .andExpect(jsonPath("$.compliance").value("COMPLIANT"));
    }

    @Test
    @DisplayName("POST /api/vendor: Invalid DTO (blank name, invalid email) returns 400 with field errors")
    void testCreateVendorValidationFailure() throws Exception {
        VendorRequestDTO dto = new VendorRequestDTO("", "invalid-email-format", "123", "Addr", "INVALID_STATUS");

        mockMvc.perform(post("/api/vendor")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/vendor: Duplicate vendor name returns 400 Business Error")
    void testCreateDuplicateVendorName() throws Exception {
        Vendor existing = new Vendor("Alpha Supplies", "alpha@supplies.com", "123", "Addr", "ACTIVE");
        vendorRepository.save(existing);

        VendorRequestDTO duplicate = new VendorRequestDTO("Alpha Supplies", "other_alpha@supplies.com", "123", "Addr", "ACTIVE");

        mockMvc.perform(post("/api/vendor")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("PUT /api/vendor/{id}: Admin can update vendor; Manager is Forbidden (403)")
    void testUpdateVendorRoleRestrictions() throws Exception {
        Vendor v = new Vendor("Updatable Vendor", "updatable@vendor.com", "123", "Addr", "ACTIVE");
        Vendor saved = vendorRepository.save(v);

        VendorRequestDTO updateDto = new VendorRequestDTO("Updatable Vendor Renamed", "updatable@vendor.com", "555-999", "New Addr", "ACTIVE");

        // Manager cannot update (Admin only)
        mockMvc.perform(put("/api/vendor/" + saved.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());

        // Admin updates successfully
        mockMvc.perform(put("/api/vendor/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updatable Vendor Renamed"));
    }

    @Test
    @DisplayName("DELETE /api/vendor/{id}: Admin can delete unused vendor; referenced vendor deletion fails")
    void testDeleteVendorSafeDeletion() throws Exception {
        Vendor v = new Vendor("Deletable API Vendor", "del_api@vendor.com", "123", "Addr", "ACTIVE");
        Vendor saved = vendorRepository.save(v);

        // Delete succeeds when unreferenced
        mockMvc.perform(delete("/api/vendor/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertFalse(vendorRepository.existsById(saved.getId()));

        // Create vendor and link to a PO
        Vendor referencedVendor = vendorRepository.save(new Vendor("Referenced Vendor", "ref@vendor.com", "123", "Addr", "ACTIVE"));
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-REF-VEND-01");
        po.setVendor(referencedVendor);
        purchaseOrderRepository.save(po);

        // Deleting referenced vendor fails with 400
        mockMvc.perform(delete("/api/vendor/" + referencedVendor.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"))
                .andExpect(jsonPath("$.message", containsString("referenced by existing purchase orders")));
    }

    // =========================================================================
    // 2. REQUISITION REST API TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/procurement/requisition: Valid request returns 200 and RequisitionResponseDTO")
    void testCreateRequisitionSuccess() throws Exception {
        RequisitionRequestDTO dto = new RequisitionRequestDTO();
        dto.setRequisitionNumber("REQ-API-101");
        dto.setItems(List.of(
                new RequisitionRequestDTO.RequisitionItemDTO("ThinkPad X1", 2, 1800.0, "Engineering laptops")
        ));

        mockMvc.perform(post("/api/procurement/requisition")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.requisitionNumber").value("REQ-API-101"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestedByUsername").value("api_emp1"))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/procurement/requisition: Empty items list returns 400 Validation Error")
    void testCreateRequisitionEmptyItems() throws Exception {
        RequisitionRequestDTO dto = new RequisitionRequestDTO();
        dto.setRequisitionNumber("REQ-API-EMPTY");
        dto.setItems(List.of());

        mockMvc.perform(post("/api/procurement/requisition")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    @DisplayName("GET /api/procurement/requisition: Employee sees only their own requisitions")
    void testRequisitionDataIsolationApi() throws Exception {
        // Create requisition by emp1
        Requisition req1 = new Requisition();
        req1.setRequisitionNumber("REQ-ISO-API-1");
        req1.setRequestedBy(employeeUser);
        req1.setStatus("PENDING");
        req1.setItems(List.of(new RequisitionItem("Item 1", "Desc", 1, 10.0, req1)));
        requisitionRepository.save(req1);

        // Create requisition by emp2
        Requisition req2 = new Requisition();
        req2.setRequisitionNumber("REQ-ISO-API-2");
        req2.setRequestedBy(otherEmployeeUser);
        req2.setStatus("PENDING");
        req2.setItems(List.of(new RequisitionItem("Item 2", "Desc", 1, 20.0, req2)));
        requisitionRepository.save(req2);

        // Employee 1 GET /api/procurement/requisition should see only REQ-ISO-API-1
        mockMvc.perform(get("/api/procurement/requisition")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requisitionNumber", hasItem("REQ-ISO-API-1")))
                .andExpect(jsonPath("$[*].requisitionNumber", not(hasItem("REQ-ISO-API-2"))));

        // Admin GET /api/procurement/requisition sees both
        mockMvc.perform(get("/api/procurement/requisition")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].requisitionNumber", hasItem("REQ-ISO-API-1")))
                .andExpect(jsonPath("$[*].requisitionNumber", hasItem("REQ-ISO-API-2")));
    }

    @Test
    @DisplayName("GET /api/procurement/requisition/{id}: Employee accessing another user's requisition returns 401 Unauthorized")
    void testEmployeeCannotAccessOtherEmployeeRequisition() throws Exception {
        Requisition otherReq = new Requisition();
        otherReq.setRequisitionNumber("REQ-OTHER-01");
        otherReq.setRequestedBy(otherEmployeeUser);
        otherReq.setStatus("PENDING");
        otherReq.setItems(List.of(new RequisitionItem("Private Item", "Desc", 1, 100.0, otherReq)));
        Requisition saved = requisitionRepository.save(otherReq);

        mockMvc.perform(get("/api/procurement/requisition/" + saved.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Access denied: You can only view your own requisitions")));
    }

    @Test
    @DisplayName("PATCH /api/procurement/requisition/{id}: Manager can approve; Employee cannot (403)")
    void testRequisitionStatusTransitionApi() throws Exception {
        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-PATCH-01");
        req.setRequestedBy(employeeUser);
        req.setStatus("PENDING");
        req.setItems(List.of(new RequisitionItem("Patch Item", "Desc", 1, 50.0, req)));
        Requisition saved = requisitionRepository.save(req);

        // Employee cannot change status
        mockMvc.perform(patch("/api/procurement/requisition/" + saved.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .param("status", "APPROVED"))
                .andExpect(status().isForbidden());

        // Manager approves
        mockMvc.perform(patch("/api/procurement/requisition/" + saved.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("APPROVED")));

        // Attempting to reopen/modify APPROVED requisition to REJECTED fails
        mockMvc.perform(patch("/api/procurement/requisition/" + saved.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .param("status", "REJECTED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"));
    }

    // =========================================================================
    // 3. PURCHASE ORDER REST API TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/procurement/purchase-order/create: Employee cannot create PO (403 Forbidden)")
    void testEmployeeCannotCreatePurchaseOrder() throws Exception {
        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setPoNumber("PO-EMP-DENIED");
        dto.setVendorId(1L);
        dto.setItems(List.of(new PurchaseOrderRequestDTO.Item("Item", 1, 10.0)));

        mockMvc.perform(post("/api/procurement/purchase-order/create")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/procurement/purchase-order/create: Manager creates PO successfully from approved requisition")
    void testCreatePurchaseOrderSuccess() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor("PO Supplier Co", "po_supp@co.com", "123", "Addr", "ACTIVE"));

        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-FOR-PO-01");
        req.setRequestedBy(employeeUser);
        req.setStatus("APPROVED");
        req.setItems(List.of(new RequisitionItem("Monitors", "27in", 4, 300.0, req)));
        Requisition savedReq = requisitionRepository.save(req);

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setPoNumber("PO-API-SUCCESS-01");
        dto.setVendorId(vendor.getId());
        dto.setRequisitionId(savedReq.getId());
        dto.setItems(List.of(new PurchaseOrderRequestDTO.Item("Monitors", 4, 300.0)));

        mockMvc.perform(post("/api/procurement/purchase-order/create")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poNumber").value("PO-API-SUCCESS-01"))
                .andExpect(jsonPath("$.vendorName").value("PO Supplier Co"))
                .andExpect(jsonPath("$.requisitionNumber").value("REQ-FOR-PO-01"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Second PO for the SAME requisition must fail (duplicate PO prevention)
        PurchaseOrderRequestDTO dupDto = new PurchaseOrderRequestDTO();
        dupDto.setPoNumber("PO-API-DUP-01");
        dupDto.setVendorId(vendor.getId());
        dupDto.setRequisitionId(savedReq.getId());
        dupDto.setItems(List.of(new PurchaseOrderRequestDTO.Item("Monitors", 4, 300.0)));

        mockMvc.perform(post("/api/procurement/purchase-order/create")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"))
                .andExpect(jsonPath("$.message", containsString("already been generated")));
    }

    @Test
    @DisplayName("POST /api/procurement/purchase-order/create: Rejects INACTIVE vendor")
    void testCreatePurchaseOrderInactiveVendor() throws Exception {
        Vendor inactiveVendor = vendorRepository.save(new Vendor("Inactive Supplier", "inact@supp.com", "123", "Addr", "INACTIVE"));

        PurchaseOrderRequestDTO dto = new PurchaseOrderRequestDTO();
        dto.setPoNumber("PO-INACT-API");
        dto.setVendorId(inactiveVendor.getId());
        dto.setItems(List.of(new PurchaseOrderRequestDTO.Item("Item", 1, 10.0)));

        mockMvc.perform(post("/api/procurement/purchase-order/create")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"))
                .andExpect(jsonPath("$.message", containsString("Only ACTIVE vendors are eligible")));
    }

    // =========================================================================
    // 4. APPROVAL REST API TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/procurement/approval/all: Manager access succeeds; Employee access forbidden (403)")
    void testApprovalApiRbac() throws Exception {
        mockMvc.perform(get("/api/procurement/approval/all")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/procurement/approval/all")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/procurement/approval/approve/{poId}: Manager approves PO, repeat approval is rejected")
    void testApprovePurchaseOrderApiWorkflow() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor("Appr PO Vendor", "appr_po@vendor.com", "123", "Addr", "ACTIVE"));

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-APPR-API-01");
        po.setVendor(vendor);
        po.setStatus("PENDING");
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        // Approve succeeds
        mockMvc.perform(post("/api/procurement/approval/approve/" + savedPo.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("approved successfully by api_mgr")));

        PurchaseOrder updatedPo = purchaseOrderRepository.findById(savedPo.getId()).orElseThrow();
        assertEquals("APPROVED", updatedPo.getStatus());

        // Repeated approval attempt fails with 400 Business Error
        mockMvc.perform(post("/api/procurement/approval/approve/" + savedPo.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"));
    }

    @Test
    @DisplayName("POST /api/procurement/approval/reject/{poId}: Manager rejects PO with comments")
    void testRejectPurchaseOrderApiWorkflow() throws Exception {
        Vendor vendor = vendorRepository.save(new Vendor("Reject PO Vendor", "rej_po@vendor.com", "123", "Addr", "ACTIVE"));

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-REJ-API-01");
        po.setVendor(vendor);
        po.setStatus("PENDING");
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        mockMvc.perform(post("/api/procurement/approval/reject/" + savedPo.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .param("reason", "Budget allocation exceeded"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("rejected successfully by api_mgr")));

        PurchaseOrder updatedPo = purchaseOrderRepository.findById(savedPo.getId()).orElseThrow();
        assertEquals("REJECTED", updatedPo.getStatus());

        // Reversal (approving a rejected PO) must fail
        mockMvc.perform(post("/api/procurement/approval/approve/" + savedPo.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"));
    }

    // =========================================================================
    // 5. REPORT REST API TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/reports/vendor/{vendorId}: Employee access is Forbidden (403)")
    void testReportApiRbac() throws Exception {
        mockMvc.perform(get("/api/reports/vendor/1")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/reports/vendor/{vendorId}: Non-existent vendor returns 404 Resource Not Found")
    void testReportNonExistentVendorReturns404() throws Exception {
        mockMvc.perform(get("/api/reports/vendor/999999")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }
}
