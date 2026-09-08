package com.example.guitarmes.productionorder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.product.Product;

class ProductionOrderSearchTest {
    private final ProductionOrderRepository repository = mock(ProductionOrderRepository.class);
    private final ProductionOrderService service = new ProductionOrderService(repository, null, null);
    private ProductionOrder order(String number, String state, String month, String due) {
        Product product = new Product();
        product.setProductName("Test Guitar");
        product.setModelNo("ST-001");
        return new ProductionOrder(number, product, 1, YearMonth.parse(month), null,
                due == null ? null : LocalDate.parse(due), state);
    }

    @Test
    void filtersAreCombinedAndDateEndpointsAreInclusive() {
        var a = order("PO-A", "PLANNED", "2026-09", "2026-09-10");
        var b = order("PO-B", "COMPLETED", "2026-10", "2026-10-10");
        var all = List.of(a, b);
        assertEquals(List.of(a), service.filterProductionOrders(all, " po-a ", "gUiTaR", "planned", "2026-09", "2026-09-10", "2026-09-10"));
        assertEquals(all, service.filterProductionOrders(all, "", "st-", "", "", "", ""));
        assertEquals(List.of(b), service.filterProductionOrders(all, "", "", "COMPLETED", "", "", ""));
        assertEquals(List.of(a), service.filterProductionOrders(all, "", "", "", "2026-09", "", ""));
        assertEquals(List.of(b), service.filterProductionOrders(all, "", "", "", "", "2026-10-01", ""));
        assertEquals(List.of(a), service.filterProductionOrders(all, "", "", "", "", "", "2026-09-30"));
        assertTrue(service.filterProductionOrders(all, "PO-A", "missing", "", "", "", "").isEmpty());
        assertTrue(service.filterProductionOrders(all, "PO-A", "", "COMPLETED", "", "", "").isEmpty());
    }

    @Test
    void invalidDatesAndReversedRangeAreRejected() {
        assertThrows(BusinessException.class, () -> service.filterProductionOrders(List.of(), "", "", "", "2026-13", "", ""));
        assertThrows(BusinessException.class, () -> service.filterProductionOrders(List.of(), "", "", "", "", "2026-02-30", ""));
        assertThrows(BusinessException.class, () -> service.filterProductionOrders(List.of(), "", "", "", "", "2026-10-01", "2026-09-01"));
    }

    @Test
    void emptyConditionsAndMissingDatesAreHandled() {
        var a = order("PO-A", "PLANNED", "2026-09", null);
        assertEquals(List.of(a), service.filterProductionOrders(List.of(a), null, " ", "", null, null, null));
        assertTrue(service.filterProductionOrders(List.of(a), "", "", "", "", "2026-09-01", "").isEmpty());
        assertFalse(service.hasSearchCondition(null, " ", ""));
        assertTrue(service.hasSearchCondition("2026-09"));
    }

    @Test
    void controllerKeepsInputsAndDistinguishesErrorFromNoResults() throws Exception {
        var a = order("PO-A", "PLANNED", "2026-09", "2026-09-10");
        when(repository.findAllByOrderByIdDesc()).thenReturn(List.of(a));
        var mvc = MockMvcBuilders.standaloneSetup(new ProductionOrderViewController(service, null, null, null)).build();
        mvc.perform(get("/production-orders/view").param("orderNo", "PO-A").param("product", "ST")
                .param("status", "PLANNED").param("planMonth", "2026-09").param("dueFrom", "2026-09-10").param("dueTo", "2026-09-10"))
                .andExpect(status().isOk()).andExpect(view().name("production-order-list"))
                .andExpect(model().attribute("orders", List.of(a))).andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("filterApplied", true)).andExpect(model().attribute("orderNo", "PO-A"))
                .andExpect(model().attribute("product", "ST")).andExpect(model().attribute("selectedStatus", "PLANNED"))
                .andExpect(model().attribute("planMonth", "2026-09")).andExpect(model().attribute("dueFrom", "2026-09-10"))
                .andExpect(model().attribute("dueTo", "2026-09-10"));
        mvc.perform(get("/production-orders/view").param("dueFrom", "bad"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("searchError"))
                .andExpect(model().attribute("dueFrom", "bad"));
        mvc.perform(get("/production-orders/view").param("orderNo", "missing"))
                .andExpect(model().attribute("resultCount", 0)).andExpect(model().attributeDoesNotExist("searchError"));
        mvc.perform(get("/production-orders/view"))
                .andExpect(model().attribute("resultCount", 1)).andExpect(model().attribute("filterApplied", false));
    }
}
