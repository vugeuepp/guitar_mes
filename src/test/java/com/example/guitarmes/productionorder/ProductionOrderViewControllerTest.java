package com.example.guitarmes.productionorder;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductService;
import com.example.guitarmes.productionschedule.ProductionSchedule;
import com.example.guitarmes.productionschedule.ProductionScheduleService;

@ExtendWith(MockitoExtension.class)
class ProductionOrderViewControllerTest {

    @Mock
    private ProductionOrderService productionOrderService;
    @Mock
    private ProductService productService;
    @Mock
    private GuitarService guitarService;
    @Mock
    private ProductionScheduleService productionScheduleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductionOrderViewController controller =
                new ProductionOrderViewController(
                        productionOrderService,
                        productService,
                        guitarService,
                        productionScheduleService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("生産計画詳細へ日産計画情報を渡せる")
    void showDetail_containsScheduleSummary() throws Exception {
        ProductionOrder order = createOrder();

        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(order);
        when(guitarService.getGuitarsByProductionOrderId(1L))
                .thenReturn(List.of());
        ProductionSchedule schedule = createSchedule(order);
        when(productionScheduleService
                .getProductionSchedulesByOrderId(1L))
                .thenReturn(List.of(schedule));
        when(productionScheduleService.getIssuedBodyCount(10L))
                .thenReturn(30L);
        when(productionScheduleService.getIssuedNeckCount(10L))
                .thenReturn(30L);
        when(productionScheduleService.isComponentsIssued(schedule))
                .thenReturn(true);
        when(productionScheduleService.getAllocatedQuantity(1L))
                .thenReturn(30);
        when(productionScheduleService.getUnallocatedQuantity(1L))
                .thenReturn(70);

        mockMvc.perform(get("/production-orders/1/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-detail"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("guitars", hasSize(0)))
                .andExpect(model().attribute(
                        "productionSchedules",
                        hasSize(1)))
                .andExpect(model().attribute(
                        "issuedBodyCounts",
                        hasEntry(10L, 30L)))
                .andExpect(model().attribute(
                        "issuedNeckCounts",
                        hasEntry(10L, 30L)))
                .andExpect(model().attribute(
                        "componentsIssued",
                        hasEntry(10L, true)))
                .andExpect(model().attribute("allocatedQuantity", 30))
                .andExpect(model().attribute("unallocatedQuantity", 70))
                .andExpect(model().attribute(
                        "neckInstallAvailable",
                        hasEntry(10L, true)));
    }

    private com.example.guitarmes.productionschedule.ProductionSchedule
            createSchedule(ProductionOrder order) {
        com.example.guitarmes.productionschedule.ProductionSchedule schedule =
                new com.example.guitarmes.productionschedule.ProductionSchedule(
                        order,
                        LocalDate.of(2026, 9, 1),
                        30,
                        com.example.guitarmes.productionschedule
                                .ProductionScheduleStatusConstants.CONFIRMED);
        schedule.setId(10L);
        return schedule;
    }

    private ProductionOrder createOrder() {
        Product product = new Product();
        product.setId(10L);

        ProductionOrder order = new ProductionOrder(
                "PO260001",
                product,
                100,
                YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                ProductionOrderStatusConstants.PLANNED);
        order.setId(1L);
        return order;
    }

    @Test
    @DisplayName("新規登録画面の日付初期値は当日と7日後")
    void showCreateForm_setsDateDefaults() throws Exception {
        when(productService.getProducts()).thenReturn(List.of(new Product()));
        mockMvc.perform(get("/production-orders/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-form"))
                .andExpect(model().attribute(
                        "request",
                        allOf(
                                hasProperty("plannedStartDate", is(LocalDate.now())),
                                hasProperty("dueDate", is(LocalDate.now().plusDays(7))))))
                .andExpect(model().attribute("minimumPlanDate", LocalDate.now()))
                .andExpect(model().attribute(
                        "maximumPlanDate",
                        LocalDate.now().plusYears(5)));
    }

    @Test
    @DisplayName("yyyyスラッシュMMスラッシュdd形式を日付へ変換できる")
    void create_bindsSlashDateFormat() throws Exception {
        mockMvc.perform(post("/production-orders/create")
                        .param("productId", "10")
                        .param("plannedQuantity", "5")
                        .param("planMonth", "2026-09")
                        .param("plannedStartDate", "2026/09/03")
                        .param("dueDate", "2026/09/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/production-orders/view"));
        verify(productionOrderService).createProductionOrder(
                10L,
                5,
                YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 10));
    }

    @Test
    @DisplayName("yyyy-MM-dd形式を日付へ変換できる")
    void create_bindsIsoDateFormat() throws Exception {
        performCreateWithDates("2026-09-03", "2026-09-10");
    }

    @Test
    @DisplayName("yyyyMMdd形式を日付へ変換できる")
    void create_bindsBasicDateFormat() throws Exception {
        performCreateWithDates("20260903", "20260910");
    }

    private void performCreateWithDates(
            String plannedStartDate,
            String dueDate) throws Exception {
        mockMvc.perform(post("/production-orders/create")
                        .param("productId", "10")
                        .param("plannedQuantity", "5")
                        .param("planMonth", "2026-09")
                        .param("plannedStartDate", plannedStartDate)
                        .param("dueDate", dueDate))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/production-orders/view"));
        verify(productionOrderService).createProductionOrder(
                10L,
                5,
                YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 10));
    }
}
