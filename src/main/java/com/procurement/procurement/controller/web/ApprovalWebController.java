package com.procurement.procurement.controller.web;

import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.exception.UnauthorizedException;
import com.procurement.procurement.service.procurement.ApprovalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/approval")
public class ApprovalWebController {

    private final ApprovalService approvalService;

    public ApprovalWebController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // ================= VIEW PAGE =================
    @GetMapping
    public String viewApprovals(Model model) {
        model.addAttribute("approvals", approvalService.getAllApprovals());
        return "approval";
    }

    // ================= UPDATE STATUS =================
    @PostMapping("/update")
    public String updateApprovalStatus(
            @RequestParam Long id,
            @RequestParam String status,
            @RequestParam(required = false) String comments,
            RedirectAttributes redirectAttributes) {

        try {
            approvalService.updateApprovalStatus(id, status, comments, null);
        } catch (BusinessException | UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/approval";
    }
}
