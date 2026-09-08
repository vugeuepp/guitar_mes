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
    @Test
    void categoriesUseStatusOnlyAndNormalizeUnknownValues() {
        for (String value : java.util.Arrays.asList(null, "", " ", "invalid", "undefined"))
            assertEquals("active", service.normalizeCategory(value));
        assertEquals("completed", service.normalizeCategory(" COMPLETED "));
        assertEquals("cancelled", service.normalizeCategory("CANCELLED"));
        var planned = order("P", "PLANNED", "2020-01", "2020-01-01");
        var working = order("W", "IN_PROGRESS", "2026-09", "2026-09-10");
        var completed = order("C", "COMPLETED", "2026-09", "2026-09-10");
        var cancelled = order("X", "CANCELLED", "2026-09", "2026-09-10");
        var all = List.of(planned, working, completed, cancelled,
                order("U", "UNKNOWN", "2026-09", null), order("N", null, "2026-09", null));
        assertEquals(List.of(planned, working), service.filterByCategory(all, "active"));
        assertEquals(List.of(completed), service.filterByCategory(all, "completed"));
        assertEquals(List.of(cancelled), service.filterByCategory(all, "cancelled"));
        assertEquals(List.of(planned, working), service.filterByCategory(all, "invalid"));
        assertEquals(List.of("PLANNED", "IN_PROGRESS"), service.getCategoryStatuses("active"));
        assertEquals(List.of("COMPLETED"), service.getCategoryStatuses("completed"));
        assertEquals(List.of("CANCELLED"), service.getCategoryStatuses("cancelled"));
        assertTrue(service.filterProductionOrders(service.filterByCategory(all, "active"),
                "", "", "COMPLETED", "", "", "").isEmpty());
    }

    @Test
    void controllerKeepsCategoryCountsEvenOnSearchErrors() throws Exception {
        var planned = order("PO-P", "PLANNED", "2026-09", "2026-09-10");
        var working = order("PO-W", "IN_PROGRESS", "2026-09", "2026-09-10");
        var completed = order("PO-C", "COMPLETED", "2026-09", "2026-09-10");
        var cancelled = order("PO-X", "CANCELLED", "2026-09", "2026-09-10");
        when(repository.findAllByOrderByIdDesc()).thenReturn(List.of(planned, working, completed, cancelled));
        var mvc = MockMvcBuilders.standaloneSetup(new ProductionOrderViewController(service, null, null, null)).build();
        mvc.perform(get("/production-orders/view"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("orders", List.of(planned, working)));
        for (String invalid : List.of("", " ", "invalid", "undefined")) {
            mvc.perform(get("/production-orders/view").param("category", invalid))
                    .andExpect(model().attribute("category", "active"))
                    .andExpect(model().attribute("orders", List.of(planned, working)));
        }
        for (String category : List.of("active", "completed", "cancelled")) {
            var target = "active".equals(category) ? planned : "completed".equals(category) ? completed : cancelled;
            mvc.perform(get("/production-orders/view").param("category", category)
                    .param("orderNo", target.getOrderNo()).param("product", "ST-001")
                    .param("status", target.getStatus()).param("planMonth", "2026-09")
                    .param("dueFrom", "2026-09-10").param("dueTo", "2026-09-10"))
                    .andExpect(model().attribute("category", category))
                    .andExpect(model().attribute("orders", List.of(target)))
                    .andExpect(model().attribute("resultCount", 1))
                    .andExpect(model().attribute("activeCount", 2))
                    .andExpect(model().attribute("completedCount", 1))
                    .andExpect(model().attribute("cancelledCount", 1));
            for (String[] dates : List.of(new String[]{"bad", ""}, new String[]{"2026-10-01", "2026-09-01"})) {
                mvc.perform(get("/production-orders/view").param("category", category)
                        .param("dueFrom", dates[0]).param("dueTo", dates[1]))
                        .andExpect(model().attribute("category", category))
                        .andExpect(model().attribute("activeCount", 2))
                        .andExpect(model().attribute("completedCount", 1))
                        .andExpect(model().attribute("cancelledCount", 1))
                        .andExpect(model().attribute("resultCount", 0))
                        .andExpect(model().attributeExists("searchError"));
            }
        }
        mvc.perform(get("/production-orders/view").param("category", "completed").param("status", "PLANNED"))
                .andExpect(model().attribute("selectedStatus", "PLANNED"))
                .andExpect(model().attribute("orders", List.of()));
    }

}
