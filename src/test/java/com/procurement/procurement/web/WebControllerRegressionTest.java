package com.procurement.procurement.web;

import com.procurement.procurement.entity.procurement.Approval;
import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderItem;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.procurement.RequisitionItem;
import com.procurement.procurement.entity.vendor.Vendor;
import com.procurement.procurement.repository.procurement.ApprovalRepository;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.vendor.VendorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebControllerRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    // ==========================================
    // 1. LOGIN & LOGOUT WEB WORKFLOW
    // ==========================================

    @Test
    @DisplayName("GET /login should display login view")
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Admin, Manager, and Employee can successfully log in via Web form")
    void testWebLoginsForAllRoles() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "adminTestPassword123!");
        assertNotNull(adminSession);

        MockHttpSession managerSession = loginAs("manager", "managerTestPassword123!");
        assertNotNull(managerSession);

        MockHttpSession employeeSession = loginAs("employee", "employeeTestPassword123!");
        assertNotNull(employeeSession);
    }

    @Test
    @DisplayName("Bad credentials redirect to /login?error")
    void testBadCredentialsRedirects() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "badPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("POST /logout terminates session and redirects to /login")
    void testLogout() throws Exception {
        MockHttpSession session = loginAs("admin", "adminTestPassword123!");

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ==========================================
    // 2. DASHBOARD WEB WORKFLOW
    // ==========================================

    @Test
    @DisplayName("Unauthenticated access to /dashboard redirects to /login")
    void testDashboardUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Authenticated dashboard access renders counts correctly")
    void testDashboardMetricsRendered() throws Exception {
        MockHttpSession session = loginAs("admin", "adminTestPassword123!");

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("vendorCount"))
                .andExpect(model().attributeExists("requisitionCount"))
                .andExpect(model().attributeExists("poCount"))
                .andExpect(model().attributeExists("pendingApprovalCount"));
    }

    // ==========================================
    // 3. VENDOR WEB WORKFLOW
    // ==========================================

    @Test
    @DisplayName("Manager can view vendor management page")
    void testVendorPageAccessForManager() throws Exception {
        MockHttpSession session = loginAs("manager", "managerTestPassword123!");

        mockMvc.perform(get("/vendor").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor"))
                .andExpect(model().attributeExists("vendors"));
    }

    @Test
    @DisplayName("Employee is denied access to /vendor and redirected to /dashboard")
    void testVendorPageAccessDeniedForEmployee() throws Exception {
        MockHttpSession session = loginAs("employee", "employeeTestPassword123!");

        mockMvc.perform(get("/vendor").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Manager creates vendor via Web form and is redirected to /vendor")
    void testVendorCreateWebForm() throws Exception {
        MockHttpSession session = loginAs("manager", "managerTestPassword123!");

        mockMvc.perform(post("/vendor")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Web Test Supplies Inc")
                        .param("email", "web_test@supplies.com")
                        .param("contactNumber", "555-1234")
                        .param("address", "100 Web St")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor"));

        assertTrue(vendorRepository.existsByName("Web Test Supplies Inc"));
    }

    @Test
    @DisplayName("Creating duplicate vendor via Web form sets flash error message")
    void testVendorCreateDuplicateShowsFlashError() throws Exception {
        Vendor v = new Vendor("Duplicate Corp", "dup@corp.com", "123", "Addr", "ACTIVE");
        vendorRepository.save(v);

        MockHttpSession session = loginAs("manager", "managerTestPassword123!");

        mockMvc.perform(post("/vendor")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Duplicate Corp")
                        .param("email", "dup2@corp.com")
                        .param("contactNumber", "123")
                        .param("address", "Addr")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Admin can delete vendor via Web; Manager cannot")
    void testVendorDeletePermissions() throws Exception {
        Vendor v = new Vendor("Deletable Vendor", "del@vendor.com", "123", "Addr", "ACTIVE");
        Vendor saved = vendorRepository.save(v);

        // Manager cannot delete (only ADMIN has /vendor/delete/**)
        MockHttpSession mgrSession = loginAs("manager", "managerTestPassword123!");
        mockMvc.perform(get("/vendor/delete/" + saved.getId()).session(mgrSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Admin can delete
        MockHttpSession adminSession = loginAs("admin", "adminTestPassword123!");
        mockMvc.perform(get("/vendor/delete/" + saved.getId()).session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor"));

        assertFalse(vendorRepository.existsById(saved.getId()));
    }

    // ==========================================
    // 4. REQUISITION WEB WORKFLOW
    // ==========================================

    @Test
    @DisplayName("Employee can view requisition page and create requisition via Web form")
    void testRequisitionWebWorkflow() throws Exception {
        MockHttpSession session = loginAs("employee", "employeeTestPassword123!");

        mockMvc.perform(get("/requisition").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("requisition"))
                .andExpect(model().attributeExists("requisitions"));

        mockMvc.perform(post("/requisition")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("requisitionNumber", "REQ-WEB-001")
                        .param("itemName", "Web Office Chair")
                        .param("description", "Ergonomic chair")
                        .param("quantity", "2")
                        .param("unitPrice", "199.99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisition"));

        assertTrue(requisitionRepository.existsByRequisitionNumber("REQ-WEB-001"));
    }

    // ==========================================
    // 5. PURCHASE ORDER WEB WORKFLOW
    // ==========================================

    @Test
    @DisplayName("Purchase Order web page is accessible to Manager but denied to Employee")
    void testPurchaseOrderWebAccess() throws Exception {
        MockHttpSession empSession = loginAs("employee", "employeeTestPassword123!");
        mockMvc.perform(get("/purchase-order").session(empSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        MockHttpSession mgrSession = loginAs("manager", "managerTestPassword123!");
        mockMvc.perform(get("/purchase-order").session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(view().name("purchase-order"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("vendors"));
    }

    @Test
    @DisplayName("Manager creates purchase order via Web form")
    void testPurchaseOrderCreateWeb() throws Exception {
        Vendor vendor = new Vendor("PO Web Vendor", "poweb@vendor.com", "123", "Addr", "ACTIVE");
        Vendor savedVendor = vendorRepository.save(vendor);

        MockHttpSession session = loginAs("manager", "managerTestPassword123!");

        mockMvc.perform(post("/purchase-order")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("poNumber", "PO-WEB-001")
                        .param("vendorId", String.valueOf(savedVendor.getId()))
                        .param("productName", "Standard Desk")
                        .param("quantity", "3")
                        .param("unitPrice", "350.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/purchase-order"));

        assertTrue(purchaseOrderRepository.existsByPoNumber("PO-WEB-001"));
    }

    @Test
    @DisplayName("PO creation with inactive vendor shows flash error message")
    void testPurchaseOrderInactiveVendorShowsFlashError() throws Exception {
        Vendor inactiveVendor = new Vendor("Inactive Web Vendor", "inactweb@vendor.com", "123", "Addr", "INACTIVE");
        Vendor savedVendor = vendorRepository.save(inactiveVendor);

        MockHttpSession session = loginAs("manager", "managerTestPassword123!");

        mockMvc.perform(post("/purchase-order")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("poNumber", "PO-WEB-INACT")
                        .param("vendorId", String.valueOf(savedVendor.getId()))
                        .param("productName", "Desk Lamp")
                        .param("quantity", "1")
                        .param("unitPrice", "25.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/purchase-order"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertFalse(purchaseOrderRepository.existsByPoNumber("PO-WEB-INACT"));
    }

    // ==========================================
    // 6. APPROVAL WEB WORKFLOW
    // ==========================================

    @Test
    @DisplayName("Approval web page is accessible to Manager but denied to Employee")
    void testApprovalWebAccess() throws Exception {
        MockHttpSession empSession = loginAs("employee", "employeeTestPassword123!");
        mockMvc.perform(get("/approval").session(empSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        MockHttpSession mgrSession = loginAs("manager", "managerTestPassword123!");
        mockMvc.perform(get("/approval").session(mgrSession))
                .andExpect(status().isOk())
                .andExpect(view().name("approval"))
                .andExpect(model().attributeExists("approvals"));
    }

    @Test
    @DisplayName("Manager approves pending approval via Web form")
    void testApprovalUpdateWeb() throws Exception {
        Vendor vendor = new Vendor("Approval Web Vendor", "apprweb@vendor.com", "123", "Addr", "ACTIVE");
        vendorRepository.save(vendor);

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-WEB-APPR-1");
        po.setVendor(vendor);
        po.setStatus("PENDING");
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        Approval approval = new Approval();
        approval.setPurchaseOrder(savedPo);
        approval.setStatus("PENDING");
        Approval savedApproval = approvalRepository.save(approval);

        MockHttpSession session = loginAs("manager", "managerTestPassword123!");

        mockMvc.perform(post("/approval/update")
                        .session(session)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", String.valueOf(savedApproval.getId()))
                        .param("status", "APPROVED")
                        .param("comments", "Approved from Web test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/approval"));

        Approval updated = approvalRepository.findById(savedApproval.getId()).orElseThrow();
        assertEquals("APPROVED", updated.getStatus());
        assertEquals("Approved from Web test", updated.getComments());
    }
}
