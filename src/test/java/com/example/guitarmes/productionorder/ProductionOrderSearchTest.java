
package com.example.guitarmes.productionorder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.guitarmes.exception.BusinessException;

class ProductionOrderSearchTest {
    private final ProductionOrderRepository repository = mock(ProductionOrderRepository.class);
    private final ProductionOrderService service = new ProductionOrderService(repository, null, null);

    @Test
    void parsesNormalizesAndDelegatesIdenticalConditionsForListAndCount() {
        var rows = List.of(new ProductionOrder());
        when(repository.search(any())).thenReturn(rows);
        when(repository.countMatching(any())).thenReturn(7L);
        var result = service.searchProductionOrders(" ACTIVE ", " PO-I ", " GUITAR ",
                " IN_PROGRESS ", " 2026-09 ", "2026-09-10", "2026-09-10");
        var expected = new ProductionOrderSearchCriteria("active", List.of("PLANNED", "IN_PROGRESS"),
                "po-i", "guitar", "in_progress", YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10));
        verify(repository).search(expected);
        verify(repository).countMatching(expected);
        assertEquals(rows, result.orders());
        assertEquals(7L, result.resultCount()); // 件数はList.sizeではなくDBのcount。
        verify(repository, never()).findAllByOrderByIdDesc();
    }

    @Test
    void invalidDatesAndReversedRangeAreRejectedBeforeQueries() {
        assertEquals("計画月はyyyy-MM、納期はyyyy-MM-dd形式で正しく入力してください。",
                assertThrows(BusinessException.class, () -> search("2026-13", "", "")).getMessage());
        assertThrows(BusinessException.class, () -> search("", "2026-02-30", ""));
        assertThrows(BusinessException.class, () -> search("", "", "bad"));
        assertEquals("納期の開始日は終了日以前にしてください。",
                assertThrows(BusinessException.class, () -> search("", "2026-10-01", "2026-09-01")).getMessage());
        verifyNoInteractions(repository);
    }

    private void search(String month, String from, String to) {
        service.searchProductionOrders("active", "", "", "", month, from, to);
    }

    @Test
    void emptyConditionsAndUnknownCategoriesAreNormalized() {
        for (String value : java.util.Arrays.asList(null, "", " ", "invalid", "undefined")) {
            service.searchProductionOrders(value, null, " ", "", null, null, null);
        }
        var empty = new ProductionOrderSearchCriteria("active", List.of("PLANNED", "IN_PROGRESS"),
                "", "", "", null, null, null);
        verify(repository, times(5)).search(empty);
        assertFalse(service.hasSearchCondition(null, " ", ""));
        assertTrue(service.hasSearchCondition("2026-09"));
        assertEquals(List.of("COMPLETED"), service.getCategoryStatuses(" COMPLETED "));
        assertEquals(List.of("CANCELLED"), service.getCategoryStatuses("CANCELLED"));
    }

    @Test
    void normalizationDoesNotDependOnDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            service.searchProductionOrders("ACTIVE", "I", "I", "IN_PROGRESS", "", "", "");
            var captor = ArgumentCaptor.forClass(ProductionOrderSearchCriteria.class);
            verify(repository).search(captor.capture());
            assertEquals("i", captor.getValue().orderNo());
            assertEquals("i", captor.getValue().product());
            assertEquals("in_progress", captor.getValue().status());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void pagedSearchUsesFixedSizeAndNormalizesNegativePage() {
        var row = new ProductionOrder();
        var returned = new PageImpl<>(List.of(row), PageRequest.of(0, 20), 53);
        when(repository.search(any(), any())).thenReturn(returned);

        var result = service.searchProductionOrdersPaged(
                "active", "PO", "Guitar", "PLANNED",
                "2026-09", "2026-09-01", "2026-09-30", -5);

        var criteriaCaptor = ArgumentCaptor.forClass(
                ProductionOrderSearchCriteria.class);
        var pageableCaptor = ArgumentCaptor.forClass(
                org.springframework.data.domain.Pageable.class);
        verify(repository).search(
                criteriaCaptor.capture(), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(ProductionOrderService.PAGE_SIZE,
                pageableCaptor.getValue().getPageSize());
        assertTrue(pageableCaptor.getValue().getSort().isUnsorted());
        assertEquals(53, result.getTotalElements());
        assertEquals(1, result.getNumberOfElements());
        verify(repository, never()).countMatching(criteriaCaptor.getValue());
    }

    @Test
    void legacySearchRemainsAvailableForCurrentController() {
        when(repository.search(any())).thenReturn(List.of());
        when(repository.countMatching(any())).thenReturn(7L);
        var result = service.searchProductionOrders(
                "active", "", "", "", "", "", "");
        assertEquals(7L, result.resultCount());
        verify(repository).search(any(ProductionOrderSearchCriteria.class));
    }

    @Test
    void controllerUsesPagedSearchAndKeepsInputsCountsAndPageMetadata() throws Exception {
        ProductionOrder row = new ProductionOrder();
        when(repository.search(any(), any())).thenAnswer(invocation -> {
            org.springframework.data.domain.Pageable pageable = invocation.getArgument(1);
            int requested = pageable.getPageNumber();
            int corrected = requested >= 999 ? 2 : requested;
            if (requested < 0) corrected = 0;
            return new PageImpl<>(List.of(row),
                    PageRequest.of(corrected, ProductionOrderService.PAGE_SIZE), 53);
        });
        when(repository.countMatching(any())).thenAnswer(invocation -> {
            ProductionOrderSearchCriteria criteria = invocation.getArgument(0);
            return switch (criteria.category()) {
                case "completed" -> 3L;
                case "cancelled" -> 4L;
                default -> 2L;
            };
        });
        var mvc = MockMvcBuilders.standaloneSetup(
                new ProductionOrderViewController(service, null, null, null)).build();

        mvc.perform(get("/production-orders/view"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("orders", List.of(row)))
                .andExpect(model().attribute("resultCount", 53L))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 3))
                .andExpect(model().attribute("pageSize", 20))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", true));

        mvc.perform(get("/production-orders/view")
                        .param("category", "completed")
                        .param("page", "1")
                        .param("orderNo", "PO")
                        .param("product", "ST")
                        .param("status", "COMPLETED")
                        .param("planMonth", "2026-09")
                        .param("dueFrom", "2026-09-10")
                        .param("dueTo", "2026-09-10"))
                .andExpect(model().attribute("category", "completed"))
                .andExpect(model().attribute("activeCount", 2L))
                .andExpect(model().attribute("completedCount", 3L))
                .andExpect(model().attribute("cancelledCount", 4L))
                .andExpect(model().attribute("orders", List.of(row)))
                .andExpect(model().attribute("resultCount", 53L))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 3))
                .andExpect(model().attribute("pageSize", 20))
                .andExpect(model().attribute("hasPrevious", true))
                .andExpect(model().attribute("hasNext", true))
                .andExpect(model().attribute("orderNo", "PO"))
                .andExpect(model().attribute("product", "ST"))
                .andExpect(model().attribute("selectedStatus", "COMPLETED"))
                .andExpect(model().attribute("planMonth", "2026-09"))
                .andExpect(model().attribute("dueFrom", "2026-09-10"))
                .andExpect(model().attribute("dueTo", "2026-09-10"));

        mvc.perform(get("/production-orders/view").param("page", "999"))
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("hasPrevious", true))
                .andExpect(model().attribute("hasNext", false));

        mvc.perform(get("/production-orders/view").param("page", "-5"))
                .andExpect(model().attribute("currentPage", 0));

        var pageableCaptor = ArgumentCaptor.forClass(
                org.springframework.data.domain.Pageable.class);
        verify(repository, atLeastOnce()).search(any(), pageableCaptor.capture());
        assertTrue(pageableCaptor.getAllValues().stream()
                .anyMatch(value -> value.getPageNumber() == 1));
        verify(repository, never()).search(any(ProductionOrderSearchCriteria.class));
        verify(repository, never()).findAllByOrderByIdDesc();
    }

    @Test
    void controllerUsesSafePagingModelForZeroResultsAndBusinessErrors() throws Exception {
        when(repository.search(any(), any())).thenReturn(
                new PageImpl<>(List.of(),
                        PageRequest.of(0, ProductionOrderService.PAGE_SIZE), 0));
        when(repository.countMatching(any())).thenReturn(0L);
        var mvc = MockMvcBuilders.standaloneSetup(
                new ProductionOrderViewController(service, null, null, null)).build();

        mvc.perform(get("/production-orders/view"))
                .andExpect(model().attribute("orders", List.of()))
                .andExpect(model().attribute("resultCount", 0L))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0))
                .andExpect(model().attribute("pageSize", 20))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", false));

        mvc.perform(get("/production-orders/view")
                        .param("category", "cancelled")
                        .param("page", "2")
                        .param("dueFrom", "bad"))
                .andExpect(model().attribute("category", "cancelled"))
                .andExpect(model().attribute("orders", List.of()))
                .andExpect(model().attribute("resultCount", 0L))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0))
                .andExpect(model().attribute("pageSize", 20))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(model().attribute("dueFrom", "bad"))
                .andExpect(model().attributeExists("searchError"));
    }
}
