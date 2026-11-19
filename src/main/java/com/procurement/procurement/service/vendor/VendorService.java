package com.procurement.procurement.service.vendor;

import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.exception.BusinessException;
import com.procurement.procurement.exception.ResourceNotFoundException;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import com.procurement.procurement.service.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VendorService {

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AuditService auditService;

    public VendorService(VendorRepository vendorRepository,
                         PurchaseOrderRepository purchaseOrderRepository,
                         AuditService auditService) {
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.auditService = auditService;
    }

    // ===================== CREATE =====================
    public Vendor createVendor(Vendor vendor) {
        if (vendor == null) {
            throw new BusinessException("Vendor payload cannot be null");
        }
        if (vendor.getName() == null || vendor.getName().trim().isEmpty()) {
            throw new BusinessException("Vendor name cannot be empty");
        }

        String trimmedName = vendor.getName().trim();
        if (vendorRepository.existsByName(trimmedName)) {
            throw new BusinessException("Vendor with name '" + trimmedName + "' already exists");
        }
        vendor.setName(trimmedName);

        if (vendor.getEmail() != null && !vendor.getEmail().trim().isEmpty()) {
            String trimmedEmail = vendor.getEmail().trim();
            if (vendorRepository.existsByEmail(trimmedEmail)) {
                throw new BusinessException("Vendor with email '" + trimmedEmail + "' already exists");
            }
            vendor.setEmail(trimmedEmail);
        }

        if (vendor.getStatus() == null || vendor.getStatus().trim().isEmpty()) {
            vendor.setStatus("ACTIVE");
        } else {
            vendor.setStatus(vendor.getStatus().trim().toUpperCase());
        }

        Vendor saved = vendorRepository.save(vendor);

        auditService.log(
                "Vendor",
                saved.getId(),
                "CREATE",
                "Created vendor: " + saved.getName() + " with status: " + saved.getStatus()
        );

        return saved;
    }

    // ===================== UPDATE =====================
    public Vendor updateVendor(Long id, Vendor updatedVendor) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + id));

        if (updatedVendor.getName() != null && !updatedVendor.getName().trim().isEmpty()) {
            String newName = updatedVendor.getName().trim();
            if (!newName.equalsIgnoreCase(vendor.getName()) && vendorRepository.existsByName(newName)) {
                throw new BusinessException("Vendor with name '" + newName + "' already exists");
            }
            vendor.setName(newName);
        }

        if (updatedVendor.getEmail() != null && !updatedVendor.getEmail().trim().isEmpty()) {
            String newEmail = updatedVendor.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(vendor.getEmail()) && vendorRepository.existsByEmail(newEmail)) {
                throw new BusinessException("Vendor with email '" + newEmail + "' already exists");
            }
            vendor.setEmail(newEmail);
        }

        if (updatedVendor.getContactNumber() != null) {
            vendor.setContactNumber(updatedVendor.getContactNumber().trim());
        }

        if (updatedVendor.getAddress() != null) {
            vendor.setAddress(updatedVendor.getAddress().trim());
        }

        if (updatedVendor.getStatus() != null && !updatedVendor.getStatus().trim().isEmpty()) {
            vendor.setStatus(updatedVendor.getStatus().trim().toUpperCase());
        }

        Vendor saved = vendorRepository.save(vendor);

        auditService.log(
                "Vendor",
                saved.getId(),
                "UPDATE",
                "Updated vendor: " + saved.getName()
        );

        return saved;
    }

    // ===================== GET ALL =====================
    @Transactional(readOnly = true)
    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    // ===================== GET ACTIVE VENDORS =====================
    @Transactional(readOnly = true)
    public List<Vendor> getActiveVendors() {
        return vendorRepository.findByStatus("ACTIVE");
    }

    // ===================== GET BY ID =====================
    @Transactional(readOnly = true)
    public Vendor getVendorById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + id));
    }

    // ===================== SEARCH =====================
    @Transactional(readOnly = true)
    public List<Vendor> getVendorsByName(String name) {
        return vendorRepository.findByNameContainingIgnoreCase(name);
    }

    // ===================== DELETE (SAFE DELETION) =====================
    public void deleteVendor(Long id) {
        Vendor vendor = getVendorById(id);

        // Check if vendor is referenced by any purchase orders
        if (purchaseOrderRepository.existsByVendor(vendor)) {
            throw new BusinessException("Cannot delete vendor '" + vendor.getName() + "' because it is referenced by existing purchase orders. Consider setting its status to INACTIVE instead.");
        }

        vendorRepository.delete(vendor);

        auditService.log(
                "Vendor",
                id,
                "DELETE",
                "Deleted vendor: " + vendor.getName()
        );
    }
}
