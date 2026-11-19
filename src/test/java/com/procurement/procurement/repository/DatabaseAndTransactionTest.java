package com.procurement.procurement.repository;

import com.procurement.procurement.entity.audit.AuditLog;
import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderItem;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.procurement.RequisitionItem;
import com.procurement.procurement.entity.user.Role;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.repository.audit.AuditLogRepository;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.RoleRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import com.procurement.procurement.service.audit.AuditService;
import com.procurement.procurement.service.procurement.ApprovalService;
import com.procurement.procurement.service.procurement.PurchaseOrderService;
import com.procurement.procurement.service.procurement.RequisitionService;
import com.procurement.procurement.service.vendor.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseAndTransactionTest {

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

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private RequisitionService requisitionService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User managerUser;

    @BeforeEach
    void setUp() {
        Role roleManager = roleRepository.findByName("ROLE_PROCUREMENT_MANAGER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_PROCUREMENT_MANAGER", "Manager")));

        managerUser = userRepository.findByUsername("db_mgr").orElseGet(() -> {
            User u = new User("db_mgr", "db_mgr@procurement.com", "pass", "DB Manager");
            u.setRoles(Set.of(roleManager));
            return userRepository.save(u);
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "db_mgr",
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROCUREMENT_MANAGER"))
                )
        );
    }

    // =========================================================================
    // 1. UNIQUE CONSTRAINT & INTEGRITY TESTS
    // =========================================================================

    @Test
    @DisplayName("Database enforces unique constraint on User username")
    void testUserDuplicateUsernameConstraint() {
        User u1 = new User("unique_user_1", "unique1@procurement.com", "pass", "User One");
        userRepository.save(u1);

        User u2 = new User("unique_user_1", "different_email@procurement.com", "pass", "User Two");
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(u2));
    }

    @Test
    @DisplayName("Database enforces unique constraint on User email")
    void testUserDuplicateEmailConstraint() {
        User u1 = new User("user_email_test_1", "same_email@procurement.com", "pass", "User One");
        userRepository.save(u1);

        User u2 = new User("user_email_test_2", "same_email@procurement.com", "pass", "User Two");
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(u2));
    }

    // =========================================================================
    // 2. MULTI-STEP TRANSACTION ROLLBACK TESTS
    // =========================================================================

    @Test
    @DisplayName("Transaction rollback: when an exception occurs in a transaction, changes roll back completely")
    void testTransactionRollbackOnFailure() {
        String testPoNumber = "PO-ROLLBACK-TEST-01";
        assertFalse(purchaseOrderRepository.existsByPoNumber(testPoNumber));

        Vendor vendor = vendorService.createVendor(new Vendor("Rollback Supplies", "rollback@supplies.com", "123", "Addr", "ACTIVE"));

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            PurchaseOrder po = new PurchaseOrder();
            po.setPoNumber(testPoNumber);
            po.setVendor(vendor);
            po.setStatus("PENDING");

            List<PurchaseOrderItem> items = new ArrayList<>();
            items.add(new PurchaseOrderItem("Item 1", 5, 100.0, po));
            po.setItems(items);

            purchaseOrderRepository.save(po);

            // Simulate mid-transaction business failure
            throw new RuntimeException("Simulated unexpected failure during multi-step operation");
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
        }

        // PO should NOT exist after rollback
        assertFalse(purchaseOrderRepository.existsByPoNumber(testPoNumber));
    }

    // =========================================================================
    // 3. REQUISITION - PO - APPROVAL CONSISTENCY TESTS
    // =========================================================================

    @Test
    @DisplayName("Requisition, PurchaseOrder, and Approval status synchronization stays consistent")
    void testStatusSynchronizationConsistency() {
        Vendor vendor = vendorService.createVendor(new Vendor("Sync Test Vendor", "sync@vendor.com", "123", "Addr", "ACTIVE"));

        // Create requisition
        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-SYNC-001");
        List<RequisitionItem> reqItems = new ArrayList<>();
        reqItems.add(new RequisitionItem("Monitors", "4K", 2, 400.0, req));
        req.setItems(reqItems);
        Requisition savedReq = requisitionService.createRequisition(req);

        // Approve requisition
        requisitionService.updateRequisitionStatus(savedReq.getId(), "APPROVED");
        Requisition approvedReq = requisitionRepository.findById(savedReq.getId()).orElseThrow();
        assertEquals("APPROVED", approvedReq.getStatus());

        // Create PO linked to approved requisition
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-SYNC-001");
        po.setVendor(vendor);
        po.setRequisition(approvedReq);
        List<PurchaseOrderItem> poItems = new ArrayList<>();
        poItems.add(new PurchaseOrderItem("Monitors", 2, 400.0, po));
        po.setItems(poItems);
        PurchaseOrder savedPo = purchaseOrderService.createPurchaseOrder(po);

        // Auto-created Approval record should exist and be PENDING
        List<Approval> approvals = approvalRepository.findByPurchaseOrderId(savedPo.getId());
        assertFalse(approvals.isEmpty());
        Approval approval = approvals.get(0);
        assertEquals("PENDING", approval.getStatus());

        // Approving via ApprovalService syncs both Approval and PurchaseOrder
        approvalService.updateApprovalStatus(approval.getId(), "APPROVED", "Approved for procurement", managerUser);

        Approval updatedApproval = approvalRepository.findById(approval.getId()).orElseThrow();
        assertEquals("APPROVED", updatedApproval.getStatus());

        PurchaseOrder updatedPo = purchaseOrderRepository.findById(savedPo.getId()).orElseThrow();
        assertEquals("APPROVED", updatedPo.getStatus());
    }

    // =========================================================================
    // 4. AUDIT LOGGING PERSISTENCE TESTS
    // =========================================================================

    @Test
    @DisplayName("AuditService records audit trail with entity name, ID, action, and user")
    void testAuditLogPersistence() {
        long countBefore = auditLogRepository.count();

        auditService.log("TestEntity", 999L, "CUSTOM_ACTION", "Testing audit trail persistence");

        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityId("TestEntity", 999L);
        assertFalse(logs.isEmpty());

        AuditLog log = logs.get(0);
        assertEquals("CUSTOM_ACTION", log.getAction());
        assertEquals("db_mgr", log.getPerformedBy());
        assertEquals("Testing audit trail persistence", log.getDetails());
        assertNotNull(log.getTimestamp());

        assertTrue(auditLogRepository.count() > countBefore);
    }

    // =========================================================================
    // 5. CASCADE & RELATIONSHIP INTEGRITY
    // =========================================================================

    @Test
    @DisplayName("Cascade persistence saves child items when parent requisition is saved")
    void testCascadePersistenceForRequisitionItems() {
        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-CASCADE-01");
        List<RequisitionItem> items = new ArrayList<>();
        items.add(new RequisitionItem("Cascade Item 1", "Desc 1", 10, 5.0, req));
        items.add(new RequisitionItem("Cascade Item 2", "Desc 2", 20, 15.0, req));
        req.setItems(items);

        Requisition saved = requisitionService.createRequisition(req);
        assertNotNull(saved.getId());

        Requisition fetched = requisitionRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(fetched.getItems());
        assertEquals(2, fetched.getItems().size());
        assertTrue(fetched.getItems().stream().allMatch(i -> i.getId() != null));
    }
}
