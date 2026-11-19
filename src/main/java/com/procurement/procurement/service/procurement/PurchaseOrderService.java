package com.procurement.procurement.service.procurement;

import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderItem;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.exception.ResourceNotFoundException;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import com.procurement.procurement.service.audit.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ApprovalRepository approvalRepository;
    private final RequisitionRepository requisitionRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                ApprovalRepository approvalRepository,
                                RequisitionRepository requisitionRepository,
                                VendorRepository vendorRepository,
                                UserRepository userRepository,
                                AuditService auditService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.approvalRepository = approvalRepository;
        this.requisitionRepository = requisitionRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ===================== CREATE =====================
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            throw new BusinessException("Purchase order payload cannot be null");
        }

        // Validate PO number
        if (purchaseOrder.getPoNumber() == null || purchaseOrder.getPoNumber().trim().isEmpty()) {
            throw new BusinessException("PO number cannot be empty");
        }
        String poNumber = purchaseOrder.getPoNumber().trim();
        if (purchaseOrderRepository.existsByPoNumber(poNumber)) {
            throw new BusinessException("Purchase Order with number '" + poNumber + "' already exists");
        }
        purchaseOrder.setPoNumber(poNumber);

        // Validate Vendor
        if (purchaseOrder.getVendor() == null) {
            throw new BusinessException("Vendor is required for creating a purchase order");
        }
        Vendor vendor = purchaseOrder.getVendor();
        if (vendor.getId() != null) {
            vendor = vendorRepository.findById(vendor.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + purchaseOrder.getVendor().getId()));
        } else if (vendor.getName() != null) {
            vendor = vendorRepository.findByName(vendor.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with name: " + purchaseOrder.getVendor().getName()));
        }
        if (!"ACTIVE".equalsIgnoreCase(vendor.getStatus())) {
            throw new BusinessException("Only ACTIVE vendors are eligible for new purchase orders. Current vendor status: " + vendor.getStatus());
        }
        purchaseOrder.setVendor(vendor);

        // Validate Requisition linkage if present
        if (purchaseOrder.getRequisition() != null) {
            Requisition req = purchaseOrder.getRequisition();
            if (req.getId() != null) {
                req = requisitionRepository.findById(req.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Linked requisition not found with id: " + purchaseOrder.getRequisition().getId()));
            } else if (req.getRequisitionNumber() != null) {
                req = requisitionRepository.findByRequisitionNumber(req.getRequisitionNumber())
                        .orElseThrow(() -> new ResourceNotFoundException("Linked requisition not found with number: " + purchaseOrder.getRequisition().getRequisitionNumber()));
            }

            // Must be in APPROVED status
            if (!"APPROVED".equalsIgnoreCase(req.getStatus())) {
                throw new BusinessException("Purchase Order can only be generated from an APPROVED requisition. Requisition status: " + req.getStatus());
            }

            // Prevent duplicate PO for the same requisition
            if (purchaseOrderRepository.existsByRequisition(req)) {
                throw new BusinessException("A Purchase Order has already been generated for requisition: " + req.getRequisitionNumber());
            }
            purchaseOrder.setRequisition(req);
        }

        // Validate Items
        if (purchaseOrder.getItems() == null || purchaseOrder.getItems().isEmpty()) {
            throw new BusinessException("Purchase order must contain at least one item");
        }
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            String name = item.getItemName() != null ? item.getItemName() : item.getProductName();
            if (name == null || name.trim().isEmpty()) {
                throw new BusinessException("Item name cannot be empty in purchase order");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("Item quantity must be greater than zero for item: " + name);
            }
            if (item.getUnitPrice() == null || item.getUnitPrice() < 0.0) {
                throw new BusinessException("Item unit price cannot be negative for item: " + name);
            }
            item.setPurchaseOrder(purchaseOrder);
        }

        purchaseOrder.setStatus("PENDING");
        purchaseOrder.setCreatedAt(LocalDateTime.now());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);

        // Auto-create Approval record for the new PO
        Approval approval = new Approval();
        approval.setPurchaseOrder(saved);
        approval.setStatus("PENDING");
        approvalRepository.save(approval);

        auditService.log(
                "PurchaseOrder",
                saved.getId(),
                "CREATE",
                "Created Purchase Order: " + saved.getPoNumber() + " with vendor: " + vendor.getName()
        );

        return saved;
    }

    // ===================== UPDATE PO DETAILS =====================
    public PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrder updatedPO) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with id: " + id));

        // Only PENDING POs can have their details modified
        if (!"PENDING".equalsIgnoreCase(po.getStatus())) {
            throw new BusinessException("Cannot modify purchase order details: status is already " + po.getStatus());
        }

        if (updatedPO.getVendor() != null) {
            Vendor vendor = updatedPO.getVendor();
            if (vendor.getId() != null) {
                vendor = vendorRepository.findById(vendor.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + updatedPO.getVendor().getId()));
            }
            if (!"ACTIVE".equalsIgnoreCase(vendor.getStatus())) {
                throw new BusinessException("Only ACTIVE vendors are eligible for purchase orders");
            }
            po.setVendor(vendor);
        }

        if (updatedPO.getItems() != null && !updatedPO.getItems().isEmpty()) {
            for (PurchaseOrderItem item : updatedPO.getItems()) {
                String name = item.getItemName() != null ? item.getItemName() : item.getProductName();
                if (name == null || name.trim().isEmpty()) {
                    throw new BusinessException("Item name cannot be empty");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new BusinessException("Item quantity must be greater than zero");
                }
                if (item.getUnitPrice() == null || item.getUnitPrice() < 0.0) {
                    throw new BusinessException("Item unit price cannot be negative");
                }
                item.setPurchaseOrder(po);
            }
            po.setItems(updatedPO.getItems());
        }

        if (updatedPO.getStatus() != null && !updatedPO.getStatus().equalsIgnoreCase(po.getStatus())) {
            validateStatusTransition(po.getStatus(), updatedPO.getStatus());
            po.setStatus(updatedPO.getStatus().trim().toUpperCase());
        }

        po.setUpdatedAt(LocalDateTime.now());
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        auditService.log(
                "PurchaseOrder",
                saved.getId(),
                "UPDATE",
                "Updated Purchase Order: " + saved.getPoNumber()
        );

        return saved;
    }

    // ===================== STATUS TRANSITION VALIDATION =====================
    public void validateStatusTransition(String currentStatus, String targetStatus) {
        if (targetStatus == null || targetStatus.trim().isEmpty()) {
            throw new BusinessException("Target status cannot be empty");
        }

        String curr = currentStatus != null ? currentStatus.trim().toUpperCase() : "PENDING";
        String target = targetStatus.trim().toUpperCase();

        if (curr.equals(target)) {
            return;
        }

        switch (curr) {
            case "PENDING":
                if (!target.equals("APPROVED") && !target.equals("REJECTED")) {
                    throw new BusinessException("Invalid transition: PENDING purchase order can only transition to APPROVED or REJECTED, not " + target);
                }
                break;
            case "APPROVED":
                if (!target.equals("COMPLETED")) {
                    throw new BusinessException("Invalid transition: APPROVED purchase order can only transition to COMPLETED, not " + target);
                }
                break;
            case "REJECTED":
                throw new BusinessException("Cannot transition REJECTED purchase order. Rejected purchase orders cannot be reopened or approved.");
            case "COMPLETED":
                throw new BusinessException("Cannot transition COMPLETED purchase order. Completed workflows cannot be reopened.");
            default:
                throw new BusinessException("Unknown current status: " + curr);
        }
    }

    // ===================== UPDATE PO STATUS =====================
    public PurchaseOrder updatePurchaseOrderStatus(Long id, String targetStatus) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with id: " + id));

        validateStatusTransition(po.getStatus(), targetStatus);

        String previousStatus = po.getStatus();
        String normalizedTarget = targetStatus.trim().toUpperCase();
        po.setStatus(normalizedTarget);
        po.setUpdatedAt(LocalDateTime.now());

        PurchaseOrder saved = purchaseOrderRepository.save(po);

        // Synchronize with linked Approval record if transitioning to APPROVED or REJECTED
        if ("APPROVED".equals(normalizedTarget) || "REJECTED".equals(normalizedTarget)) {
            List<Approval> approvals = approvalRepository.findByPurchaseOrderId(saved.getId());
            if (approvals != null && !approvals.isEmpty()) {
                Approval approval = approvals.get(approvals.size() - 1);
                approval.setStatus(normalizedTarget);
                approval.setApprovedAt(LocalDateTime.now());
                User approver = getAuthenticatedUser();
                if (approver != null) {
                    approval.setApprover(approver);
                }
                approvalRepository.save(approval);
            }
        }

        auditService.log(
                "PurchaseOrder",
                saved.getId(),
                "UPDATE_STATUS",
                "Status transitioned from " + previousStatus + " to " + saved.getStatus()
        );

        return saved;
    }

    // ===================== GET ALL =====================
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    // ===================== GET BY ID =====================
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with id: " + id));
    }

    // ===================== GET BY VENDOR =====================
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPurchaseOrdersByVendor(Vendor vendor) {
        return purchaseOrderRepository.findByVendor(vendor);
    }

    // ===================== GET BY STATUS =====================
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPurchaseOrdersByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    // ===================== GET BY PO NUMBER =====================
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrderByPoNumber(String poNumber) {
        return purchaseOrderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with PO number: " + poNumber));
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
