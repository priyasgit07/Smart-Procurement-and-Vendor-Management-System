package com.procurement.procurement.service.procurement;

import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.procurement.RequisitionItem;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.exception.ResourceNotFoundException;
import com.procurement.procurement.exception.UnauthorizedException;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.UserRepository;
import com.procurement.procurement.service.audit.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@Transactional
public class RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ApprovalService approvalService;

    public RequisitionService(RequisitionRepository requisitionRepository,
                              UserRepository userRepository,
                              AuditService auditService,
                              ApprovalService approvalService) {
        this.requisitionRepository = requisitionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.approvalService = approvalService;
    }

    // ===================== CREATE REQUISITION =====================
    public Requisition createRequisition(Requisition requisition) {

        User currentUser = getAuthenticatedUser();

        // Validate requisition number
        if (requisition.getRequisitionNumber() == null || requisition.getRequisitionNumber().trim().isEmpty()) {
            throw new BusinessException("Requisition number is required");
        }
        String reqNumber = requisition.getRequisitionNumber().trim();
        if (requisitionRepository.existsByRequisitionNumber(reqNumber)) {
            throw new BusinessException("Requisition number already exists: " + reqNumber);
        }
        requisition.setRequisitionNumber(reqNumber);

        // Validate items
        if (requisition.getItems() == null || requisition.getItems().isEmpty()) {
            throw new BusinessException("Requisition must contain at least one item specification");
        }

        for (RequisitionItem item : requisition.getItems()) {
            if (item.getItemName() == null || item.getItemName().trim().isEmpty()) {
                throw new BusinessException("Item name cannot be empty");
            }
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Item quantity must be greater than zero for item: " + item.getItemName());
            }
            if (item.getUnitPrice() < 0.0) {
                throw new BusinessException("Item unit price cannot be negative for item: " + item.getItemName());
            }
            item.setRequisition(requisition);
        }

        // Always bind the authenticated user (never trust client user id)
        requisition.setRequestedBy(currentUser);
        requisition.setStatus("PENDING");
        requisition.setCreatedAt(LocalDateTime.now());
        requisition.setUpdatedAt(LocalDateTime.now());

        Requisition saved = requisitionRepository.save(requisition);

        // Auto-create approval record
        Approval approval = new Approval();
        approval.setRequisition(saved);
        approval.setStatus("PENDING");
        approvalService.createApproval(approval);

        auditService.log(
                "Requisition",
                saved.getId(),
                "CREATE",
                "Requisition " + saved.getRequisitionNumber() + " created by " + currentUser.getUsername()
        );

        return saved;
    }

    // ===================== STATUS TRANSITION VALIDATION =====================
    public void validateStatusTransition(String currentStatus, String targetStatus) {
        if (targetStatus == null || targetStatus.trim().isEmpty()) {
            throw new BusinessException("Target status cannot be empty");
        }

        String current = currentStatus != null ? currentStatus.trim().toUpperCase() : "PENDING";
        String target = targetStatus.trim().toUpperCase();

        if (current.equals(target)) {
            throw new BusinessException("Requisition is already in status: " + current);
        }

        switch (current) {
            case "PENDING":
                if (!target.equals("APPROVED") && !target.equals("REJECTED")) {
                    throw new BusinessException("A PENDING requisition can only transition to APPROVED or REJECTED, not " + target);
                }
                break;
            case "APPROVED":
                if (!target.equals("COMPLETED")) {
                    throw new BusinessException("An APPROVED requisition can only transition to COMPLETED, not " + target);
                }
                break;
            case "REJECTED":
                throw new BusinessException("Cannot modify status: REJECTED requisitions cannot be reopened or modified");
            case "COMPLETED":
                throw new BusinessException("Cannot modify status: COMPLETED requisitions cannot be reopened");
            default:
                throw new BusinessException("Unrecognized current requisition status: " + current);
        }
    }

    // ===================== UPDATE STATUS =====================
    public Requisition updateRequisitionStatus(Long id, String targetStatus) {
        Requisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + id));

        // Enforce role permission: only Admin or Manager can change status
        if (!isManagerOrAdmin()) {
            throw new UnauthorizedException("Only Admin or Procurement Manager can update requisition status");
        }

        validateStatusTransition(req.getStatus(), targetStatus);

        String previousStatus = req.getStatus();
        req.setStatus(targetStatus.trim().toUpperCase());
        req.setUpdatedAt(LocalDateTime.now());

        Requisition saved = requisitionRepository.save(req);

        auditService.log(
                "Requisition",
                saved.getId(),
                "UPDATE_STATUS",
                "Status transitioned from " + previousStatus + " to " + saved.getStatus()
        );

        return saved;
    }

    // ===================== UPDATE REQUISITION DETAILS =====================
    public Requisition updateRequisition(Long id, Requisition updatedReq) {
        Requisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + id));

        User currentUser = getAuthenticatedUser();

        // Employees can only update their own PENDING requisitions
        if (!isManagerOrAdmin()) {
            if (req.getRequestedBy() == null || !req.getRequestedBy().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("You are not authorized to modify another employee's requisition");
            }
            if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
                throw new BusinessException("Cannot modify requisition: status is already " + req.getStatus());
            }
        }

        if (updatedReq.getStatus() != null && !updatedReq.getStatus().equalsIgnoreCase(req.getStatus())) {
            if (!isManagerOrAdmin()) {
                throw new UnauthorizedException("Employees cannot modify requisition status directly");
            }
            validateStatusTransition(req.getStatus(), updatedReq.getStatus());
            req.setStatus(updatedReq.getStatus().trim().toUpperCase());
        }

        if (updatedReq.getItems() != null && !updatedReq.getItems().isEmpty()) {
            for (RequisitionItem item : updatedReq.getItems()) {
                item.setRequisition(req);
            }
            req.setItems(updatedReq.getItems());
        }

        req.setUpdatedAt(LocalDateTime.now());
        Requisition saved = requisitionRepository.save(req);

        auditService.log(
                "Requisition",
                saved.getId(),
                "UPDATE",
                "Requisition " + saved.getRequisitionNumber() + " updated by " + currentUser.getUsername()
        );

        return saved;
    }

    // ===================== DATA ISOLATION QUERY =====================
    /**
     * Resolves requisitions based on the authenticated user's role:
     * - EMPLOYEE: Only returns requisitions created by this user.
     * - ADMIN / PROCUREMENT_MANAGER: Returns all requisitions across the organization.
     */
    public List<Requisition> getRequisitionsForCurrentUser() {
        if (isManagerOrAdmin()) {
            return getAllRequisitions();
        }
        return getMyRequisitions();
    }

    public Requisition getRequisitionById(Long id) {
        Requisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + id));

        // Enforce employee ownership
        if (!isManagerOrAdmin()) {
            User currentUser = getAuthenticatedUser();
            if (req.getRequestedBy() == null || !req.getRequestedBy().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("Access denied: You can only view your own requisitions");
            }
        }

        syncRequisitionStatusWithApproval(req);
        return req;
    }

    public List<Requisition> getAllRequisitions() {
        List<Requisition> list = requisitionRepository.findAll();
        for (Requisition req : list) {
            syncRequisitionStatusWithApproval(req);
        }
        return list;
    }

    public List<Requisition> getMyRequisitions() {
        User currentUser = getAuthenticatedUser();
        List<Requisition> list = requisitionRepository.findByRequestedBy(currentUser);
        for (Requisition req : list) {
            syncRequisitionStatusWithApproval(req);
        }
        return list;
    }

    public List<Requisition> getRequisitionsByStatus(String status) {
        return requisitionRepository.findByStatus(status);
    }

    // ===================== HELPER METHODS =====================
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user in security context");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found: " + username));
    }

    private boolean isManagerOrAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_PROCUREMENT_MANAGER"));
    }

    private void syncRequisitionStatusWithApproval(Requisition req) {
        if (req != null && req.getId() != null) {
            List<Approval> approvals = approvalService.getApprovalsByRequisitionId(req.getId());
            if (approvals != null && !approvals.isEmpty()) {
                Approval latestApproval = approvals.get(approvals.size() - 1);
                if (latestApproval.getStatus() != null && !latestApproval.getStatus().equalsIgnoreCase(req.getStatus())) {
                    req.setStatus(latestApproval.getStatus());
                    req.setUpdatedAt(LocalDateTime.now());
                    requisitionRepository.save(req);
                }
            }
        }
    }
}
