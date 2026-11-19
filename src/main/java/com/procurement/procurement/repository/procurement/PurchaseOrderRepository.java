// Purchase order repository
package com.procurement.procurement.repository.procurement;

import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.vendor.Vendor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"items", "vendor", "requisition"})
    List<PurchaseOrder> findAll();

    @Override
    @EntityGraph(attributePaths = {"items", "vendor", "requisition"})
    Optional<PurchaseOrder> findById(Long id);

    List<PurchaseOrder> findByVendor(Vendor vendor);

    boolean existsByVendor(Vendor vendor);

    List<PurchaseOrder> findByStatus(String status);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    List<PurchaseOrder> findByRequisition(Requisition requisition);

    boolean existsByRequisition(Requisition requisition);
}

