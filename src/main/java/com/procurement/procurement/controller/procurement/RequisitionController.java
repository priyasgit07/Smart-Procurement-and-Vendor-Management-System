package com.procurement.procurement.controller.procurement;

import com.procurement.procurement.dto.procurement.RequisitionRequestDTO;
import com.procurement.procurement.dto.procurement.RequisitionResponseDTO;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.mapper.RequisitionMapper;
import com.procurement.procurement.service.procurement.RequisitionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/procurement/requisition")
public class RequisitionController {

    private final RequisitionService requisitionService;

    public RequisitionController(RequisitionService requisitionService) {
        this.requisitionService = requisitionService;
    }

    @PostMapping
    public ResponseEntity<RequisitionResponseDTO> create(@Valid @RequestBody RequisitionRequestDTO request) {
        Requisition entity = RequisitionMapper.toEntity(request, null);
        Requisition saved = requisitionService.createRequisition(entity);
        return ResponseEntity.ok(RequisitionMapper.toResponseDTO(saved));
    }

    @GetMapping
    public ResponseEntity<List<RequisitionResponseDTO>> getAll() {
        List<RequisitionResponseDTO> list = requisitionService.getRequisitionsForCurrentUser().stream()
                .map(RequisitionMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequisitionResponseDTO> getById(@PathVariable Long id) {
        Requisition req = requisitionService.getRequisitionById(id);
        return ResponseEntity.ok(RequisitionMapper.toResponseDTO(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_MANAGER')")
    public ResponseEntity<String> updateStatus(@PathVariable Long id,
                                               @RequestParam String status) {
        Requisition updated = requisitionService.updateRequisitionStatus(id, status);
        return ResponseEntity.ok("Requisition status updated to " + updated.getStatus());
    }
}
