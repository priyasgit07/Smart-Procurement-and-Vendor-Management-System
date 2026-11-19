package com.procurement.procurement.util;

import com.procurement.procurement.entity.procurement.PurchaseOrder;
import com.procurement.procurement.entity.procurement.PurchaseOrderItem;
import com.procurement.procurement.entity.procurement.Requisition;
import com.procurement.procurement.entity.procurement.RequisitionItem;
import com.procurement.procurement.entity.user.User;
import com.procurement.procurement.repository.procurement.PurchaseOrderRepository;
import com.procurement.procurement.repository.procurement.RequisitionRepository;
import com.procurement.procurement.repository.user.UserRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CurrencyUtilTest {

    @Autowired
    private CurrencyUtil currencyUtil;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Test
    @DisplayName("CurrencyUtil formats numbers according to Indian numbering system (INR - ₹)")
    void testIndianNumberFormatting() {
        assertEquals("₹0.00", currencyUtil.formatINR(null));
        assertEquals("₹0.00", currencyUtil.formatINR(0));
        assertEquals("₹0.00", currencyUtil.formatINR(0.0));
        assertEquals("₹99.99", currencyUtil.formatINR(99.99));
        assertEquals("₹150.50", currencyUtil.formatINR(150.5));
        assertEquals("₹1,500.00", currencyUtil.formatINR(1500));
        assertEquals("₹25,000.00", currencyUtil.formatINR(25000));
        assertEquals("₹1,10,000.00", currencyUtil.formatINR(110000.00));
        assertEquals("₹10,00,000.00", currencyUtil.formatINR(1000000));
        assertEquals("₹1,00,00,000.00", currencyUtil.formatINR(10000000.00));
        assertEquals("₹1,10,000.00", CurrencyUtil.format(new BigDecimal("110000.00")));
    }

    @Test
    @DisplayName("Requisition web page renders Indian Rupee labels and ₹ values correctly")
    void testRequisitionPageDisplaysRupeeSymbolAndIndianFormatting() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "employee")
                        .param("password", "employeeTestPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        User employee = userRepository.findByUsername("employee").orElseThrow();
        Requisition req = new Requisition();
        req.setRequisitionNumber("REQ-CURRENCY-001");
        req.setStatus("PENDING");
        req.setRequestedBy(employee);

        RequisitionItem item = new RequisitionItem();
        item.setItemName("Industrial Generator");
        item.setDescription("Heavy backup unit");
        item.setQuantity(1);
        item.setUnitPrice(110000.00);
        item.setRequisition(req);

        List<RequisitionItem> items = new ArrayList<>();
        items.add(item);
        req.setItems(items);
        requisitionRepository.save(req);

        MvcResult result = mockMvc.perform(get("/requisition").session(session))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Unit Price (₹)"), "Requisition page must contain 'Unit Price (₹)' label");
        assertTrue(html.contains("Total Value (₹)"), "Requisition page must contain 'Total Value (₹)' header");
        assertTrue(html.contains("₹1,10,000.00"), "Requisition page must display amount formatted as ₹1,10,000.00");
    }

    @Test
    @DisplayName("Purchase Order web page renders Indian Rupee labels and ₹ values correctly")
    void testPurchaseOrderPageDisplaysRupeeSymbolAndIndianFormatting() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "admin")
                        .param("password", "adminTestPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-CURRENCY-001");
        po.setStatus("PENDING");

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setItemName("Server Rack Cluster");
        item.setQuantity(2);
        item.setUnitPrice(55000.00);
        item.setPurchaseOrder(po);

        List<PurchaseOrderItem> items = new ArrayList<>();
        items.add(item);
        po.setItems(items);
        purchaseOrderRepository.save(po);

        MvcResult result = mockMvc.perform(get("/purchase-order").session(session))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Unit Price (₹)"), "PO page must contain 'Unit Price (₹)' label");
        assertTrue(html.contains("Total Value (₹)"), "PO page must contain 'Total Value (₹)' header");
        assertTrue(html.contains("₹55,000.00"), "PO page must display unit price as ₹55,000.00");
        assertTrue(html.contains("₹1,10,000.00"), "PO page must display total value (2 * 55,000.00) as ₹1,10,000.00");
    }
}
