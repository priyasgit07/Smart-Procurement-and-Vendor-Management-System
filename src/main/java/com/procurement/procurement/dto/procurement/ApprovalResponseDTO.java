package com.procurement.procurement.dto.procurement;

import java.time.LocalDateTime;

public class ApprovalResponseDTO {

    private Long id;
    private String status;
    private String comments;
    private Long approverId;
    private String approverUsername;
    private LocalDateTime approvedAt;
    private Long requisitionId;
    private String requisitionNumber;
    private Long purchaseOrderId;
    private String poNumber;

    public ApprovalResponseDTO() {
    }

    public ApprovalResponseDTO(Long id, String status, String comments, Long approverId, String approverUsername, LocalDateTime approvedAt, Long requisitionId, String requisitionNumber, Long purchaseOrderId, String poNumber) {
        this.id = id;
        this.status = status;
        this.comments = comments;
        this.approverId = approverId;
        this.approverUsername = approverUsername;
        this.approvedAt = approvedAt;
        this.requisitionId = requisitionId;
        this.requisitionNumber = requisitionNumber;
        this.purchaseOrderId = purchaseOrderId;
        this.poNumber = poNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getApproverUsername() {
        return approverUsername;
    }

    public void setApproverUsername(String approverUsername) {
        this.approverUsername = approverUsername;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Long getRequisitionId() {
        return requisitionId;
    }

    public void setRequisitionId(Long requisitionId) {
        this.requisitionId = requisitionId;
    }

    public String getRequisitionNumber() {
        return requisitionNumber;
    }

    public void setRequisitionNumber(String requisitionNumber) {
        this.requisitionNumber = requisitionNumber;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }
}
