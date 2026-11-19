package com.procurement.procurement.repository.procurement;

import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequisitionRepository extends JpaRepository<Requisition, Long> {

    @Override
    @EntityGraph(attributePaths = {"items", "requestedBy"})
    List<Requisition> findAll();

    @Override
    @EntityGraph(attributePaths = {"items", "requestedBy"})
    Optional<Requisition> findById(Long id);

    // Find all requisitions by requested user
    @EntityGraph(attributePaths = {"items", "requestedBy"})
    List<Requisition> findByRequestedBy(User requestedBy);

    // Find all requisitions by status
    List<Requisition> findByStatus(String status);

    java.util.Optional<Requisition> findByRequisitionNumber(String requisitionNumber);

    boolean existsByRequisitionNumber(String requisitionNumber);
}
