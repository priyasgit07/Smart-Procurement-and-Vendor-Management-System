package com.procurement.procurement.service;

import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderItem;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.procurement.RequisitionItem;
import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.exception.UnauthorizedException;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.RoleRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import com.procurement.procurement.service.procurement.ApprovalService;
import com.procurement.procurement.service.procurement.PurchaseOrderService;
import com.procurement.procurement.service.procurement.RequisitionService;
import com.procurement.procurement.service.vendor.VendorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProcurementBusinessLogicTest {

    @Autowired
    private RequisitionService requisitionService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User employee1;
    private User employee2;
    private User manager;
    private User admin;
    private Vendor activeVendor;
    private Vendor inactiveVendor;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        Role roleEmployee = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> roleRepository.save(new Role("ROLE_EMPLOYEE", "Employee role")));
        Role roleManager = roleRepository.findByName("ROLE_PROCUREMENT_MANAGER").orElseGet(() -> roleRepository.save(new Role("ROLE_PROCUREMENT_MANAGER", "Manager role")));
        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Admin role")));

        employee1 = userRepository.findByUsername("emp1_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("emp1_test");
            u.setEmail("emp1_test@example.com");
            u.setPassword("hashed");
            u.setRoles(Set.of(roleEmployee));
            return userRepository.save(u);
        });

        employee2 = userRepository.findByUsername("emp2_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("emp2_test");
            u.setEmail("emp2_test@example.com");
            u.setPassword("hashed");
            u.setRoles(Set.of(roleEmployee));
            return userRepository.save(u);
        });

        manager = userRepository.findByUsername("mgr_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("mgr_test");
            u.setEmail("mgr_test@example.com");
            u.setPassword("hashed");
            u.setRoles(Set.of(roleManager));
            return userRepository.save(u);
        });

        admin = userRepository.findByUsername("admin_test").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin_test");
            u.setEmail("admin_test@example.com");
            u.setPassword("hashed");
            u.setRoles(Set.of(roleAdmin));
            return userRepository.save(u);
        });

        activeVendor = vendorRepository.findByName("Acme Supplies Active").orElseGet(() -> {
            Vendor v = new Vendor();
            v.setName("Acme Supplies Active");
            v.setEmail("acme_active@example.com");
            v.setStatus("ACTIVE");
            return vendorRepository.save(v);
        });

        inactiveVendor = vendorRepository.findByName("Dormant Vendor Inactive").orElseGet(() -> {
            Vendor v = new Vendor();
            v.setName("Dormant Vendor Inactive");
            v.setEmail("dormant_inactive@example.com");
            v.setStatus("INACTIVE");
            return vendorRepository.save(v);
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(role))
                )
        );
    }

    // =========================================================================
    // 1. REQUISITION DATA ISOLATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Employee sees only their own requisitions, not other employees'")
    void testRequisitionDataIsolationForEmployees() {
        authenticateAs(employee1, "ROLE_EMPLOYEE");
        Requisition req1 = new Requisition();
        req1.setRequisitionNumber("REQ-ISO-01");
        List<RequisitionItem> items1 = new ArrayList<>();
        items1.add(new RequisitionItem("Laptop", "MacBook Pro", 1, 1500.0, req1));
        req1.setItems(items1);
        requisitionService.createRequisition(req1);

        authenticateAs(employee2, "ROLE_EMPLOYEE");
        Requisition req2 = new Requisition();
        req2.setRequisitionNumber("REQ-ISO-02");
        List<RequisitionItem> items2 = new ArrayList<>();
        items2.add(new RequisitionItem("Monitor", "Dell 27-inch", 2, 350.0, req2));
        req2.setItems(items2);
        requisitionService.createRequisition(req2);

        // Employee 1 accesses list -> should only see REQ-ISO-01
        authenticateAs(employee1, "ROLE_EMPLOYEE");
        List<Requisition> emp1List = requisitionService.getRequisitionsForCurrentUser();
        assertTrue(emp1List.stream().anyMatch(r -> "REQ-ISO-01".equals(r.getRequisitionNumber())));
        assertFalse(emp1List.stream().anyMatch(r -> "REQ-ISO-02".equals(r.getRequisitionNumber())));

        // Employee 2 accesses list -> should only see REQ-ISO-02
        authenticateAs(employee2, "ROLE_EMPLOYEE");
        List<Requisition> emp2List = requisitionService.getRequisitionsForCurrentUser();
        assertFalse(emp2List.stream().anyMatch(r -> "REQ-ISO-01".equals(r.getRequisitionNumber())));
        assertTrue(emp2List.stream().anyMatch(r -> "REQ-ISO-02".equals(r.getRequisitionNumber())));

        // Manager accesses list -> should see both
        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");
        List<Requisition> mgrList = requisitionService.getRequisitionsForCurrentUser();
        assertTrue(mgrList.stream().anyMatch(r -> "REQ-ISO-01".equals(r.getRequisitionNumber())));
        assertTrue(mgrList.stream().anyMatch(r -> "REQ-ISO-02".equals(r.getRequisitionNumber())));
    }

    @Test
    @DisplayName("Employee cannot view another employee's requisition by ID")
    void testEmployeeCannotAccessOtherEmployeeRequisitionById() {
        authenticateAs(employee1, "ROLE_EMPLOYEE");
        Requisition req1 = new Requisition();
        req1.setRequisitionNumber("REQ-ISO-03");
        List<RequisitionItem> items = new ArrayList<>();
        items.add(new RequisitionItem("Keyboard", "Mechanical", 1, 120.0, req1));
        req1.setItems(items);
        Requisition saved = requisitionService.createRequisition(req1);

        authenticateAs(employee2, "ROLE_EMPLOYEE");
        assertThrows(UnauthorizedException.class, () ->
                requisitionService.getRequisitionById(saved.getId()));
    }

    // =========================================================================
    // 2. REQUISITION WORKFLOW & VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Requisition validation: empty items, zero quantity, negative price rejected")
    void testRequisitionItemValidation() {
        authenticateAs(employee1, "ROLE_EMPLOYEE");

        // Empty items list
        Requisition reqNoItems = new Requisition();
        reqNoItems.setRequisitionNumber("REQ-VAL-01");
        reqNoItems.setItems(Collections.emptyList());
        assertThrows(BusinessException.class, () -> requisitionService.createRequisition(reqNoItems));

        // Zero quantity
        Requisition reqZeroQty = new Requisition();
        reqZeroQty.setRequisitionNumber("REQ-VAL-02");
        List<RequisitionItem> zeroQtyItems = new ArrayList<>();
        zeroQtyItems.add(new RequisitionItem("Chairs", "Office Chairs", 0, 100.0, reqZeroQty));
        reqZeroQty.setItems(zeroQtyItems);
        assertThrows(BusinessException.class, () -> requisitionService.createRequisition(reqZeroQty));

        // Negative price
        Requisition reqNegPrice = new Requisition();
        reqNegPrice.setRequisitionNumber("REQ-VAL-03");
        List<RequisitionItem> negPriceItems = new ArrayList<>();
        negPriceItems.add(new RequisitionItem("Desks", "Standing Desks", 1, -50.0, reqNegPrice));
        reqNegPrice.setItems(negPriceItems);
        assertThrows(BusinessException.class, () -> requisitionService.createRequisition(reqNegPrice));
    }

    @Test
    @DisplayName("Requisition workflow: rejected requisitions cannot be approved or reopened")
    void testRequisitionStatusTransitionRules() {
        authenticateAs(employee1, "ROLE_EMPLOYEE");
        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-TRANS-01");
        List<RequisitionItem> items = new ArrayList<>();
        items.add(new RequisitionItem("Pen", "Ballpoint Pen", 10, 2.0, req));
        req.setItems(items);
        Requisition saved = requisitionService.createRequisition(req);

        // Manager rejects requisition
        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");
        requisitionService.updateRequisitionStatus(saved.getId(), "REJECTED");
        Requisition rejected = requisitionRepository.findById(saved.getId()).orElseThrow();
        assertEquals("REJECTED", rejected.getStatus());

        // Attempt to approve rejected requisition -> must throw BusinessException
        assertThrows(BusinessException.class, () ->
                requisitionService.updateRequisitionStatus(saved.getId(), "APPROVED"));

        // Attempt to transition to COMPLETED -> must throw BusinessException
        assertThrows(BusinessException.class, () ->
                requisitionService.updateRequisitionStatus(saved.getId(), "COMPLETED"));
    }

    // =========================================================================
    // 3. VENDOR VALIDATION & SAFE DELETION TESTS
    // =========================================================================

    @Test
    @DisplayName("Vendor validation: duplicate vendor name or email rejected")
    void testDuplicateVendorPrevention() {
        Vendor v1 = new Vendor();
        v1.setName("Unique Vendor Corp");
        v1.setEmail("unique@vendor.com");
        v1.setStatus("ACTIVE");
        vendorService.createVendor(v1);

        // Duplicate name
        Vendor v2 = new Vendor();
        v2.setName("Unique Vendor Corp");
        v2.setEmail("other@vendor.com");
        v2.setStatus("ACTIVE");
        assertThrows(BusinessException.class, () -> vendorService.createVendor(v2));

        // Duplicate email
        Vendor v3 = new Vendor();
        v3.setName("Different Vendor");
        v3.setEmail("unique@vendor.com");
        v3.setStatus("ACTIVE");
        assertThrows(BusinessException.class, () -> vendorService.createVendor(v3));
    }

    @Test
    @DisplayName("Safe deletion: vendor referenced by purchase order cannot be deleted")
    void testSafeVendorDeletionPreventsBreakingRelationships() {
        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");

        Vendor testVendor = new Vendor();
        testVendor.setName("PO Linked Vendor");
        testVendor.setEmail("po_linked@vendor.com");
        testVendor.setStatus("ACTIVE");
        Vendor savedVendor = vendorService.createVendor(testVendor);

        // Create PO referencing this vendor
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-VEND-SAFE-01");
        po.setVendor(savedVendor);
        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(new PurchaseOrderItem("Item A", 5, 20.0, po));
        po.setItems(items);
        purchaseOrderService.createPurchaseOrder(po);

        // Attempting to delete vendor must throw BusinessException
        assertThrows(BusinessException.class, () ->
                vendorService.deleteVendor(savedVendor.getId()));
    }

    // =========================================================================
    // 4. PURCHASE ORDER WORKFLOW & VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Purchase Order creation fails if vendor is INACTIVE")
    void testPurchaseOrderRejectsInactiveVendor() {
        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-INACT-01");
        po.setVendor(inactiveVendor);
        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(new PurchaseOrderItem("Widget", 10, 5.0, po));
        po.setItems(items);

        assertThrows(BusinessException.class, () ->
                purchaseOrderService.createPurchaseOrder(po));
    }

    @Test
    @DisplayName("Purchase Order can only be generated from APPROVED requisition")
    void testPurchaseOrderRejectsUnapprovedRequisition() {
        authenticateAs(employee1, "ROLE_EMPLOYEE");
        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-UNAPPR-01");
        List<RequisitionItem> reqItems = new ArrayList<>();
        reqItems.add(new RequisitionItem("Cable", "HDMI Cable", 2, 15.0, req));
        req.setItems(reqItems);
        Requisition savedReq = requisitionService.createRequisition(req);
        assertEquals("PENDING", savedReq.getStatus());

        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-UNAPPR-01");
        po.setVendor(activeVendor);
        po.setRequisition(savedReq);
        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(new PurchaseOrderItem("Cable", 2, 15.0, po));
        po.setItems(items);

        // Fails because requisition is PENDING, not APPROVED
        assertThrows(BusinessException.class, () ->
                purchaseOrderService.createPurchaseOrder(po));
    }

    @Test
    @DisplayName("Prevent duplicate Purchase Order for the same requisition")
    void testDuplicatePurchaseOrderForSameRequisitionPrevented() {
        authenticateAs(employee1, "ROLE_EMPLOYEE");
        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-DUP-PO-01");
        List<RequisitionItem> reqItems = new ArrayList<>();
        reqItems.add(new RequisitionItem("Router", "Cisco Router", 1, 500.0, req));
        req.setItems(reqItems);
        Requisition savedReq = requisitionService.createRequisition(req);

        // Approve requisition
        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");
        requisitionService.updateRequisitionStatus(savedReq.getId(), "APPROVED");

        // First PO succeeds
        PurchaseOrder po1 = new PurchaseOrder();
        po1.setPoNumber("PO-DUP-01");
        po1.setVendor(activeVendor);
        po1.setRequisition(savedReq);
        List<PurchaseOrderItem> items1 = new ArrayList<>();
        items1.add(new PurchaseOrderItem("Router", 1, 500.0, po1));
        po1.setItems(items1);
        purchaseOrderService.createPurchaseOrder(po1);

        // Second PO for same requisition must fail
        PurchaseOrder po2 = new PurchaseOrder();
        po2.setPoNumber("PO-DUP-02");
        po2.setVendor(activeVendor);
        po2.setRequisition(savedReq);
        List<PurchaseOrderItem> items2 = new ArrayList<>();
        items2.add(new PurchaseOrderItem("Router", 1, 500.0, po2));
        po2.setItems(items2);

        assertThrows(BusinessException.class, () ->
                purchaseOrderService.createPurchaseOrder(po2));
    }

    @Test
    @DisplayName("Purchase Order status transitions: invalid transitions rejected")
    void testPurchaseOrderStatusTransitions() {
        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-TRANS-01");
        po.setVendor(activeVendor);
        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(new PurchaseOrderItem("Paper", 20, 4.0, po));
        po.setItems(items);
        PurchaseOrder saved = purchaseOrderService.createPurchaseOrder(po);

        // PENDING cannot jump directly to COMPLETED
        assertThrows(BusinessException.class, () ->
                purchaseOrderService.updatePurchaseOrderStatus(saved.getId(), "COMPLETED"));

        // Reject PO
        purchaseOrderService.updatePurchaseOrderStatus(saved.getId(), "REJECTED");

        // REJECTED PO cannot transition to APPROVED or COMPLETED
        assertThrows(BusinessException.class, () ->
                purchaseOrderService.updatePurchaseOrderStatus(saved.getId(), "APPROVED"));
        assertThrows(BusinessException.class, () ->
                purchaseOrderService.updatePurchaseOrderStatus(saved.getId(), "COMPLETED"));
    }

    // =========================================================================
    // 5. APPROVAL WORKFLOW TESTS
    // =========================================================================

    @Test
    @DisplayName("Approval workflow: duplicate approvals and status reversals prohibited")
    void testDuplicateApprovalPrevention() {
        authenticateAs(manager, "ROLE_PROCUREMENT_MANAGER");

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-APPR-DUP-01");
        po.setVendor(activeVendor);
        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(new PurchaseOrderItem("Stapler", 2, 8.0, po));
        po.setItems(items);
        PurchaseOrder savedPo = purchaseOrderService.createPurchaseOrder(po);

        List<Approval> approvals = approvalRepository.findByPurchaseOrderId(savedPo.getId());
        assertFalse(approvals.isEmpty());
        Approval approval = approvals.get(0);
        assertEquals("PENDING", approval.getStatus());

        // Approve approval
        approvalService.updateApprovalStatus(approval.getId(), "APPROVED", "Approved for purchase", manager);
        Approval approved = approvalRepository.findById(approval.getId()).orElseThrow();
        assertEquals("APPROVED", approved.getStatus());

        // Purchase order status must be synced to APPROVED
        PurchaseOrder updatedPo = purchaseOrderRepository.findById(savedPo.getId()).orElseThrow();
        assertEquals("APPROVED", updatedPo.getStatus());

        // Attempting to approve again or reject already-approved approval must throw BusinessException
        assertThrows(BusinessException.class, () ->
                approvalService.updateApprovalStatus(approval.getId(), "APPROVED", "Repeat approve", manager));
        assertThrows(BusinessException.class, () ->
                approvalService.updateApprovalStatus(approval.getId(), "REJECTED", "Reversal attempt", manager));
    }
}
