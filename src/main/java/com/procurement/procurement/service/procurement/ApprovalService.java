package com.procurement.procurement.service.procurement;

import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.exception.ResourceNotFoundException;
import com.procurement.procurement.exception.UnauthorizedException;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.service.audit.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final RequisitionRepository requisitionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ApprovalService(ApprovalRepository approvalRepository,
                           RequisitionRepository requisitionRepository,
                           PurchaseOrderRepository purchaseOrderRepository) {
        this(approvalRepository, requisitionRepository, purchaseOrderRepository, null, null);
    }

    @Autowired
    public ApprovalService(ApprovalRepository approvalRepository,
                           RequisitionRepository requisitionRepository,
                           PurchaseOrderRepository purchaseOrderRepository,
                           UserRepository userRepository,
                           AuditService auditService) {
        this.approvalRepository = approvalRepository;
        this.requisitionRepository = requisitionRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ===================== CREATE APPROVAL =====================
    public Approval createApproval(Approval approval) {
        if (approval == null) {
            throw new BusinessException("Approval payload cannot be null");
        }

        // Prevent duplicate pending approval for requisition
        if (approval.getRequisition() != null && approval.getRequisition().getId() != null) {
            List<Approval> existing = approvalRepository.findByRequisitionId(approval.getRequisition().getId());
            if (existing != null && existing.stream().anyMatch(a -> "PENDING".equalsIgnoreCase(a.getStatus()))) {
                throw new BusinessException("A pending approval already exists for requisition id: " + approval.getRequisition().getId());
            }
        }

        // Prevent duplicate pending approval for purchase order
        if (approval.getPurchaseOrder() != null && approval.getPurchaseOrder().getId() != null) {
            List<Approval> existing = approvalRepository.findByPurchaseOrderId(approval.getPurchaseOrder().getId());
            if (existing != null && existing.stream().anyMatch(a -> "PENDING".equalsIgnoreCase(a.getStatus()))) {
                throw new BusinessException("A pending approval already exists for purchase order id: " + approval.getPurchaseOrder().getId());
            }
        }

        approval.setStatus("PENDING");
        approval.setApprovedAt(null);
        return approvalRepository.save(approval);
    }

    // ===================== UPDATE APPROVAL STATUS =====================
    public Approval updateApprovalStatus(Long approvalId, String status, String comments, User approver) {
        if (status == null || status.trim().isEmpty()) {
            throw new BusinessException("Status cannot be empty");
        }

        String targetStatus = status.trim().toUpperCase();
        if (!"APPROVED".equals(targetStatus) && !"REJECTED".equals(targetStatus)) {
            throw new BusinessException("Invalid approval status: " + status + ". Expected APPROVED or REJECTED.");
        }

        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval not found with id: " + approvalId));

        // Prevent repeated approval/rejection actions
        if (!"PENDING".equalsIgnoreCase(approval.getStatus())) {
            throw new BusinessException("Cannot process approval: current status is already " + approval.getStatus() + ". Duplicate approvals or reversals are prohibited.");
        }

        // Resolve approver if null
        if (approver == null) {
            approver = getAuthenticatedUser();
        }

        // Verify approver role if approver is present
        if (approver != null && approver.getRoles() != null && !approver.getRoles().isEmpty()) {
            boolean hasPermission = approver.getRoles().stream()
                    .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_PROCUREMENT_MANAGER".equalsIgnoreCase(r.getName())
                            || "ADMIN".equalsIgnoreCase(r.getName()) || "PROCUREMENT_MANAGER".equalsIgnoreCase(r.getName()));
            if (!hasPermission) {
                throw new UnauthorizedException("Only Admin or Procurement Manager can approve or reject requests");
            }
        }

        approval.setStatus(targetStatus);
        approval.setApprovedAt(LocalDateTime.now());

        if (comments != null && !comments.trim().isEmpty()) {
            approval.setComments(comments.trim());
        }

        if (approver != null) {
            approval.setApprover(approver);
        }

        // Sync status with linked Requisition
        if (approval.getRequisition() != null) {
            Requisition req = approval.getRequisition();
            if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
                throw new BusinessException("Cannot update approval: Requisition is already " + req.getStatus());
            }
            req.setStatus(targetStatus);
            req.setUpdatedAt(LocalDateTime.now());
            requisitionRepository.save(req);
        }

        // Sync status with linked PurchaseOrder
        if (approval.getPurchaseOrder() != null) {
            PurchaseOrder po = approval.getPurchaseOrder();
            if (!"PENDING".equalsIgnoreCase(po.getStatus())) {
                throw new BusinessException("Cannot update approval: Purchase Order is already " + po.getStatus());
            }
            po.setStatus(targetStatus);
            po.setUpdatedAt(LocalDateTime.now());
            purchaseOrderRepository.save(po);
        }

        Approval saved = approvalRepository.save(approval);

        if (auditService != null) {
            String performedBy = approver != null ? approver.getUsername() : "SYSTEM";
            auditService.log(
                    "Approval",
                    saved.getId(),
                    targetStatus,
                    "Approval " + targetStatus + " by " + performedBy + (comments != null ? " Reason: " + comments : "")
            );
        }

        return saved;
    }

    public Approval updateApprovalStatus(Long approvalId, String status) {
        return updateApprovalStatus(approvalId, status, null, null);
    }

    @Transactional(readOnly = true)
    public List<Approval> getAllApprovals() {
        return approvalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Approval> getApprovalsByPurchaseOrderId(Long purchaseOrderId) {
        return approvalRepository.findByPurchaseOrderId(purchaseOrderId);
    }

    @Transactional(readOnly = true)
    public List<Approval> getApprovalsByRequisitionId(Long requisitionId) {
        return approvalRepository.findByRequisitionId(requisitionId);
    }

    @Transactional(readOnly = true)
    public Approval getApprovalById(Long id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval not found with id: " + id));
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        if (userRepository != null) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }
}
