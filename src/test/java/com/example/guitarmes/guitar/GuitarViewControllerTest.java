package com.example.guitarmes.guitar;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.guitarmes.assembly.AssemblyService;
import com.example.guitarmes.process.ProcessService;
@ExtendWith(MockitoExtension.class)
class GuitarViewControllerTest {
    @Mock GuitarService guitarService;
    @Mock ProcessService processService;
    @Mock AssemblyService assemblyService;
    MockMvc mvc;
    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new GuitarViewController(guitarService, processService, assemblyService)).build();
    }
    @Test void listUsesPagedServiceAndKeepsModel() throws Exception {
        var row = new GuitarProgressResponse(1L, "DY26", "Stratocaster", "Red", "調整・調音", 50, false, true);
        when(guitarService.normalizeCategory("active")).thenReturn("active");
        when(guitarService.searchGuitarsPaged(processService, "active", "DY26", "Stratocaster", "調整・調音", "WAITING", 1))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(1, 20), 53));
        when(guitarService.countCategory("active")).thenReturn(60L);
        when(guitarService.countCategory("completed")).thenReturn(12L);
        when(guitarService.getProductOptions()).thenReturn(List.of("Stratocaster"));
        when(guitarService.hasSearchCondition("DY26", "Stratocaster", "調整・調音", "WAITING")).thenReturn(true);
        when(processService.getAvailableGuitarProcesses()).thenReturn(List.of());
        mvc.perform(get("/guitars/view").param("page", "1").param("serial", "DY26")
                .param("product", "Stratocaster").param("currentProcess", "調整・調音").param("status", "WAITING"))
                .andExpect(status().isOk()).andExpect(view().name("guitar-list"))
                .andExpect(model().attribute("guitars", List.of(row)))
                .andExpect(model().attribute("resultCount", 53L))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 3))
                .andExpect(model().attribute("pageSize", 20))
                .andExpect(model().attribute("hasPrevious", true))
                .andExpect(model().attribute("hasNext", true))
                .andExpect(model().attribute("activeCount", 60L))
                .andExpect(model().attribute("completedCount", 12L))
                .andExpect(model().attribute("filterApplied", true));
        verify(guitarService, never()).getGuitarProgressList(any(), any());
        verify(guitarService, never()).filterByCategory(anyList(), anyString());
        verify(guitarService, never()).filterGuitarProgressList(anyList(), any(), any(), any(), any());
        verify(guitarService, never()).getProductOptions(anyList());
    }
}
