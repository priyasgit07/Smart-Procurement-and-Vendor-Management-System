package com.procurement.procurement.mapper;

import com.procurement.procurement.dto.procurement.ApprovalResponseDTO;
import com.procurement.procurement.entity.procurement.Approval;

public class ApprovalMapper {

    public static ApprovalResponseDTO toResponseDTO(Approval approval) {
        if (approval == null) return null;

        ApprovalResponseDTO dto = new ApprovalResponseDTO();
        dto.setId(approval.getId());
        dto.setStatus(approval.getStatus());
        dto.setComments(approval.getComments());
        dto.setApprovedAt(approval.getApprovedAt());

        if (approval.getApprover() != null) {
            dto.setApproverId(approval.getApprover().getId());
            dto.setApproverUsername(approval.getApprover().getUsername());
        }

        if (approval.getRequisition() != null) {
            dto.setRequisitionId(approval.getRequisition().getId());
            dto.setRequisitionNumber(approval.getRequisition().getRequisitionNumber());
        }

        if (approval.getPurchaseOrder() != null) {
            dto.setPurchaseOrderId(approval.getPurchaseOrder().getId());
            dto.setPoNumber(approval.getPurchaseOrder().getPoNumber());
        }

        return dto;
    }
}
