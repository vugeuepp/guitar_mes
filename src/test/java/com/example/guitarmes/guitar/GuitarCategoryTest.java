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
        GuitarService spy = spy(service);
        ProcessService process = mock(ProcessService.class);
        AssemblyService assembly = mock(AssemblyService.class);
        doReturn(all).when(spy).getGuitarProgressList(process, assembly);
        when(process.getAvailableGuitarProcesses()).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new GuitarViewController(spy, process, assembly)).build();
        mvc.perform(get("/guitars/view"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("guitars", List.of(waiting, working, unknown)))
                .andExpect(model().attribute("activeCount", 3)).andExpect(model().attribute("completedCount", 1));
        mvc.perform(get("/guitars/view").param("category", "completed").param("serial", "C").param("status", "WAITING"))
                .andExpect(status().isOk()).andExpect(model().attribute("category", "completed"))
                .andExpect(model().attribute("guitars", List.of(completed)))
                .andExpect(model().attribute("selectedStatus", "")).andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("activeCount", 3));
        mvc.perform(get("/guitars/view").param("category", "completed").param("status", "WORKING"))
                .andExpect(model().attribute("filterApplied", false));
        mvc.perform(get("/guitars/view").param("category", "invalid").param("serial", "A"))
                .andExpect(model().attribute("category", "active")).andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("activeCount", 3));
    }
}
