package com.procurement.procurement.controller.procurement;

import com.procurement.procurement.dto.procurement.ApprovalResponseDTO;
import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.exception.ResourceNotFoundException;
import com.procurement.procurement.exception.UnauthorizedException;
import com.procurement.procurement.mapper.ApprovalMapper;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.service.audit.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/procurement/approval")
@PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_MANAGER')")
public class ApprovalController {

    private final ApprovalRepository approvalRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ApprovalController(ApprovalRepository approvalRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              UserRepository userRepository) {
        this(approvalRepository, purchaseOrderRepository, userRepository, null);
    }

    @Autowired
    public ApprovalController(ApprovalRepository approvalRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.approvalRepository = approvalRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ===================== Get all approvals =====================
    @GetMapping("/all")
    public ResponseEntity<List<ApprovalResponseDTO>> getAllApprovals() {
        List<ApprovalResponseDTO> list = approvalRepository.findAll().stream()
                .map(ApprovalMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ===================== Approve a Purchase Order =====================
    @PostMapping("/approve/{poId}")
    public ResponseEntity<String> approvePurchaseOrder(@PathVariable Long poId,
                                                       @RequestParam(required = false) Long approverId) {

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with id: " + poId));

        // Derive authenticated user identity securely from SecurityContext
        User approver = getAuthenticatedUser();

        if (po.getStatus() != null && !"PENDING".equalsIgnoreCase(po.getStatus())) {
            throw new BusinessException("Cannot approve Purchase Order: current status is " + po.getStatus() + ". Only PENDING purchase orders can be approved.");
        }

        List<Approval> existingApprovals = approvalRepository.findByPurchaseOrderId(poId);
        if (existingApprovals != null && existingApprovals.stream().anyMatch(a -> "APPROVED".equalsIgnoreCase(a.getStatus()) || "REJECTED".equalsIgnoreCase(a.getStatus()))) {
            throw new BusinessException("Purchase Order has already been processed by approval workflow");
        }

        // Update existing PENDING approval or create new
        Approval approval = (existingApprovals != null && !existingApprovals.isEmpty())
                ? existingApprovals.get(0)
                : new Approval();

        approval.setPurchaseOrder(po);
        approval.setApprover(approver);
        approval.setStatus("APPROVED");
        approval.setApprovedAt(LocalDateTime.now());

        approvalRepository.save(approval);

        // Update PO status
        po.setStatus("APPROVED");
        po.setUpdatedAt(LocalDateTime.now());
        purchaseOrderRepository.save(po);

        if (auditService != null) {
            auditService.log("PurchaseOrder", po.getId(), "APPROVE", "Approved by " + approver.getUsername());
        }

        return ResponseEntity.ok("Purchase Order approved successfully by " + approver.getUsername());
    }

    // ===================== Reject a Purchase Order =====================
    @PostMapping("/reject/{poId}")
    public ResponseEntity<String> rejectPurchaseOrder(@PathVariable Long poId,
                                                       @RequestParam(required = false) Long approverId,
                                                       @RequestParam(defaultValue = "No reason provided") String reason) {

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found with id: " + poId));

        // Derive authenticated user identity securely from SecurityContext
        User approver = getAuthenticatedUser();

        if (po.getStatus() != null && !"PENDING".equalsIgnoreCase(po.getStatus())) {
            throw new BusinessException("Cannot reject Purchase Order: current status is " + po.getStatus() + ". Only PENDING purchase orders can be rejected.");
        }

        List<Approval> existingApprovals = approvalRepository.findByPurchaseOrderId(poId);
        if (existingApprovals != null && existingApprovals.stream().anyMatch(a -> "APPROVED".equalsIgnoreCase(a.getStatus()) || "REJECTED".equalsIgnoreCase(a.getStatus()))) {
            throw new BusinessException("Purchase Order has already been processed by approval workflow");
        }

        // Update existing PENDING approval or create new
        Approval approval = (existingApprovals != null && !existingApprovals.isEmpty())
                ? existingApprovals.get(0)
                : new Approval();

        approval.setPurchaseOrder(po);
        approval.setApprover(approver);
        approval.setStatus("REJECTED");
        approval.setComments(reason != null ? reason.trim() : "Rejected");
        approval.setApprovedAt(LocalDateTime.now());

        approvalRepository.save(approval);

        // Update PO status
        po.setStatus("REJECTED");
        po.setUpdatedAt(LocalDateTime.now());
        purchaseOrderRepository.save(po);

        if (auditService != null) {
            auditService.log("PurchaseOrder", po.getId(), "REJECT", "Rejected by " + approver.getUsername() + ", reason: " + approval.getComments());
        }

        return ResponseEntity.ok("Purchase Order rejected successfully by " + approver.getUsername());
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("User must be authenticated to perform approvals");
        }

        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database: " + username));
    }
}
