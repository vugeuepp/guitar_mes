package com.example.guitarmes.assembly;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.guitarmes.body.BodyService;
import com.example.guitarmes.neck.NeckService;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderService;
import com.example.guitarmes.productionschedule.ProductionSchedule;
import com.example.guitarmes.productionschedule.ProductionScheduleService;

@ExtendWith(MockitoExtension.class)
class AssemblyViewControllerTest {
    @Mock AssemblyService assemblyService;
    @Mock NeckService neckService;
    @Mock BodyService bodyService;
    @Mock ProductionOrderService productionOrderService;
    @Mock ProductionScheduleService productionScheduleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AssemblyViewController(
                        assemblyService,
                        neckService,
                        bodyService,
                        productionOrderService,
                        productionScheduleService))
                .build();
    }

    @Test
    @DisplayName("日産計画別のBodyとNeckをネック取付画面へ渡せる")
    void newAssemblyForm_filtersBySchedule() throws Exception {
        ProductionOrder order = new ProductionOrder(); order.setId(1L);
        ProductionSchedule schedule = new ProductionSchedule();
        schedule.setId(10L); schedule.setProductionOrder(order);
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(order);
        when(productionScheduleService.getProductionScheduleById(10L))
                .thenReturn(schedule);
        when(neckService.getAvailableNecksByProductionSchedule(order, schedule))
                .thenReturn(List.of());
        when(bodyService.getAvailableBodiesByProductionSchedule(order, schedule))
                .thenReturn(List.of());

        mockMvc.perform(get("/assemblies/new")
                        .param("productionOrderId", "1")
                        .param("productionScheduleId", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("assembly-form"))
                .andExpect(model().attribute("productionOrder", order))
                .andExpect(model().attribute("productionSchedule", schedule));
    }

    @Test
    @DisplayName("日産計画IDを指定してネック取付を登録できる")
    void createAssembly_forwardsScheduleId() throws Exception {
        Assembly assembly = new Assembly();
        com.example.guitarmes.guitar.Guitar guitar =
                org.mockito.Mockito.mock(
                        com.example.guitarmes.guitar.Guitar.class);
        when(guitar.getId()).thenReturn(99L);
        assembly.setGuitar(guitar);
        when(assemblyService.createAssembly(
                1L, 10L, 30L, 20L, "Worker"))
                .thenReturn(assembly);

        mockMvc.perform(post("/assemblies/create")
                        .param("productionOrderId", "1")
                        .param("productionScheduleId", "10")
                        .param("neckId", "30")
                        .param("bodyId", "20")
                        .param("workerName", "Worker"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/guitars/99/view"));
        verify(assemblyService).createAssembly(
                1L, 10L, 30L, 20L, "Worker");
    }

    @Test
    @DisplayName("複数のネック取付を一括登録できる")
    void createAssemblies_bulk_success() throws Exception {
        when(assemblyService.createAssemblies(
                1L, 10L, List.of(30L, 31L),
                List.of(20L, 21L), "Worker"))
                .thenReturn(List.of(new Assembly(), new Assembly()));

        mockMvc.perform(post("/assemblies/bulk/create")
                        .param("productionOrderId", "1")
                        .param("productionScheduleId", "10")
                        .param("neckIds", "30", "31")
                        .param("bodyIds", "20", "21")
                        .param("workerName", "Worker"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/production-orders/1/view"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "2件のネック取付を一括登録しました。"));
    }
}
