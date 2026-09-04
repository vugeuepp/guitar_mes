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

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new GuitarViewController(
                        guitarService,
                        processService,
                        assemblyService))
                .build();
    }

    @Test
    void listContainsSearchConditionsAndResults() throws Exception {
        List<GuitarProgressResponse> all = List.of();
        when(guitarService.getGuitarProgressList(
                processService,
                assemblyService)).thenReturn(all);
        when(guitarService.filterGuitarProgressList(
                all,
                "DY26",
                "Stratocaster",
                "調整・調音",
                "WAITING")).thenReturn(all);
        when(guitarService.getProductOptions(all))
                .thenReturn(List.of("Stratocaster"));
        when(guitarService.hasSearchCondition(
                "DY26",
                "Stratocaster",
                "調整・調音",
                "WAITING")).thenReturn(true);
        when(processService.getAvailableGuitarProcesses())
                .thenReturn(List.of());

        mvc.perform(get("/guitars/view")
                        .param("serial", "DY26")
                        .param("product", "Stratocaster")
                        .param("currentProcess", "調整・調音")
                        .param("status", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(view().name("guitar-list"))
                .andExpect(model().attribute("serial", "DY26"))
                .andExpect(model().attribute(
                        "selectedProduct",
                        "Stratocaster"))
                .andExpect(model().attribute(
                        "selectedCurrentProcess",
                        "調整・調音"))
                .andExpect(model().attribute(
                        "selectedStatus",
                        "WAITING"))
                .andExpect(model().attribute("filterApplied", true))
                .andExpect(model().attribute("resultCount", 0))
                .andExpect(model().attributeExists(
                        "guitars",
                        "processes",
                        "productOptions"));
    }
}
