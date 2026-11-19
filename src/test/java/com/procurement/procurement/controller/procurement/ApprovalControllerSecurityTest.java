package com.procurement.procurement.controller.procurement;

import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.exception.ResourceNotFoundException;
import com.procurement.procurement.exception.UnauthorizedException;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalControllerSecurityTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private UserRepository userRepository;

    private ApprovalController approvalController;

    @BeforeEach
    void setUp() {
        approvalController = new ApprovalController(approvalRepository, purchaseOrderRepository, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Approve PO should use authenticated user from SecurityContext, preventing approverId spoofing")
    void testApproveUsesAuthenticatedUserNotSpoofedId() {
        // Given an authenticated manager "actual_manager"
        String authenticatedUsername = "actual_manager";
        User authenticatedUser = new User();
        authenticatedUser.setId(99L);
        authenticatedUser.setUsername(authenticatedUsername);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        authenticatedUsername,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROCUREMENT_MANAGER"))
                )
        );

        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(authenticatedUser));

        PurchaseOrder po = new PurchaseOrder();
        po.setId(10L);
        po.setStatus("PENDING");
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(po));

        // When client passes spoofed approverId=1 (e.g. attempting to spoof admin)
        Long spoofedApproverId = 1L;
        ResponseEntity<String> response = approvalController.approvePurchaseOrder(10L, spoofedApproverId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains(authenticatedUsername));
        assertEquals("APPROVED", po.getStatus());

        // Verify that approval was saved with the actual authenticated user, NOT the spoofed ID
        ArgumentCaptor<Approval> approvalCaptor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository).save(approvalCaptor.capture());
        Approval savedApproval = approvalCaptor.getValue();

        assertEquals(authenticatedUser, savedApproval.getApprover());
        assertEquals("APPROVED", savedApproval.getStatus());
        assertNotNull(savedApproval.getApprovedAt());
    }

    @Test
    @DisplayName("Reject PO should record rejection reason and actual authenticated user")
    void testRejectUsesAuthenticatedUser() {
        String authenticatedUsername = "manager";
        User authenticatedUser = new User();
        authenticatedUser.setId(5L);
        authenticatedUser.setUsername(authenticatedUsername);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        authenticatedUsername,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROCUREMENT_MANAGER"))
                )
        );

        when(userRepository.findByUsername(authenticatedUsername)).thenReturn(Optional.of(authenticatedUser));

        PurchaseOrder po = new PurchaseOrder();
        po.setId(20L);
        po.setStatus("PENDING");
        when(purchaseOrderRepository.findById(20L)).thenReturn(Optional.of(po));

        ResponseEntity<String> response = approvalController.rejectPurchaseOrder(20L, 999L, "Budget exceeded");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("REJECTED", po.getStatus());

        ArgumentCaptor<Approval> approvalCaptor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalRepository).save(approvalCaptor.capture());
        Approval savedApproval = approvalCaptor.getValue();

        assertEquals(authenticatedUser, savedApproval.getApprover());
        assertEquals("REJECTED", savedApproval.getStatus());
        assertEquals("Budget exceeded", savedApproval.getComments());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when Purchase Order does not exist")
    void testApproveNonExistentPO() {
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                approvalController.approvePurchaseOrder(999L, 1L));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when no user is in SecurityContext")
    void testApproveWithoutAuthenticationThrowsUnauthorized() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId(10L);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(po));

        assertThrows(UnauthorizedException.class, () ->
                approvalController.approvePurchaseOrder(10L, 1L));
    }
}
