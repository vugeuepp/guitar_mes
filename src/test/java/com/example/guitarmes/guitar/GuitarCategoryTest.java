
package com.example.guitarmes.guitar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.guitarmes.process.ProcessService;
import com.example.guitarmes.assembly.AssemblyService;

class GuitarCategoryTest {
    private final GuitarService service = new GuitarService(null);
    private final GuitarProgressResponse waiting = new GuitarProgressResponse(1L, "A", "Model", "Red", "調整・調音", 0, false, true);
    private final GuitarProgressResponse working = new GuitarProgressResponse(2L, "B", "Model", "Red", "調整・調音", 0, true, true);
    private final GuitarProgressResponse completed = new GuitarProgressResponse(3L, "C", "Model", "Red", "完成", 0, true, true);
    private final GuitarProgressResponse unknown = new GuitarProgressResponse(4L, "D", "Other", "Red", null, 100, false, false);
    private final List<GuitarProgressResponse> all = List.of(waiting, working, completed, unknown);

    @Test void categoryUsesOnlyExactCurrentProcess() {
        assertEquals(List.of(waiting, working, unknown), service.filterByCategory(all, "active"));
        assertEquals(List.of(completed), service.filterByCategory(all, "completed"));
        assertEquals("active", service.normalizeCategory(null));
        assertEquals("active", service.normalizeCategory("invalid"));
        assertEquals("active", service.normalizeCategory(""));
        assertTrue(service.filterByCategory(List.of(), "completed").isEmpty());
    }
    @Test void categoryAndSearchAreCombined() {
        var active = service.filterByCategory(all, "active");
        assertEquals(List.of(waiting), service.filterGuitarProgressList(active, "A", "Model", "調整・調音", "WAITING"));
        assertEquals(List.of(working), service.filterGuitarProgressList(active, "", "Model", "", "WORKING"));
        assertTrue(service.filterGuitarProgressList(active, "C", "", "", "").isEmpty());
    }
    @Test void controllerDefaultsCountsAndCompletedStatusHandling() throws Exception {
        GuitarRepository repository = mock(GuitarRepository.class);
        GuitarService pagedService = spy(new GuitarService(repository));
        ProcessService process = mock(ProcessService.class);
        AssemblyService assembly = mock(AssemblyService.class);
        when(repository.search(any(), any())).thenAnswer(invocation -> {
            var criteria = (GuitarSearchCriteria) invocation.getArgument(0);
            var pageable = (org.springframework.data.domain.Pageable) invocation.getArgument(1);
            List<Guitar> rows = new java.util.ArrayList<>();
            if ("completed".equals(criteria.category())) {
                Guitar guitar = new Guitar("C", "完成"); guitar.setId(3L); rows.add(guitar);
            }
            return new org.springframework.data.domain.PageImpl<>(rows, pageable, rows.size());
        });
        when(repository.countMatching(any())).thenAnswer(invocation ->
                "completed".equals(((GuitarSearchCriteria) invocation.getArgument(0)).category()) ? 1L : 3L);
        when(repository.findProductOptions()).thenReturn(List.of("Model"));
        when(process.getAvailableGuitarProcesses()).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new GuitarViewController(pagedService, process, assembly)).build();
        mvc.perform(get("/guitars/view"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("activeCount", 3L))
                .andExpect(model().attribute("completedCount", 1L));
        mvc.perform(get("/guitars/view").param("category", "completed").param("status", "WAITING"))
                .andExpect(model().attribute("selectedStatus", ""))
                .andExpect(model().attribute("filterApplied", false));
        mvc.perform(get("/guitars/view").param("category", "invalid").param("page", "-2"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("currentPage", 0));
        var captor = org.mockito.ArgumentCaptor.forClass(GuitarSearchCriteria.class);
        verify(repository, atLeastOnce()).search(captor.capture(), any());
        assertTrue(captor.getAllValues().stream().anyMatch(c -> "completed".equals(c.category()) && c.status().isEmpty()));
        verify(repository, never()).findAll();
    }

}
