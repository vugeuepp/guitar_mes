package com.example.guitarmes.productionschedule;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderService;
import com.example.guitarmes.productionorder.ProductionOrderStatusConstants;

@ExtendWith(MockitoExtension.class)
class ProductionScheduleViewControllerTest {

    @Mock
    private ProductionScheduleService productionScheduleService;

    @Mock
    private ProductionOrderService productionOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductionScheduleViewController controller =
                new ProductionScheduleViewController(
                        productionScheduleService,
                        productionOrderService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setConversionService(
                        new DefaultFormattingConversionService())
                .build();
    }

    @Test
    @DisplayName("日産計画登録画面を表示できる")
    void showCreateForm_succeeds() throws Exception {
        stubFormSummary();

        mockMvc.perform(get(
                "/production-orders/1/schedules/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-schedule-form"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("allocatedQuantity", 30))
                .andExpect(model().attribute("unallocatedQuantity", 70));
    }

    @Test
    @DisplayName("日産計画を登録して生産計画詳細へ戻れる")
    void create_succeeds() throws Exception {
        mockMvc.perform(post(
                "/production-orders/1/schedules/create")
                        .param("scheduleDate", "2026-09-01")
                        .param("plannedQuantity", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/production-orders/1/view"));

        verify(productionScheduleService)
                .createProductionSchedule(
                        1L,
                        LocalDate.of(2026, 9, 1),
                        20);
    }

    @Test
    @DisplayName("登録時の業務エラーを入力画面へ表示できる")
    void create_businessError_returnsForm() throws Exception {
        when(productionScheduleService.createProductionSchedule(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.eq(101)))
                .thenThrow(new BusinessException(
                        "日産計画の合計が月間計画数を超えています。"));
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(createOrder());
        when(productionScheduleService.getAllocatedQuantity(1L))
                .thenReturn(0);
        when(productionScheduleService.getUnallocatedQuantity(1L))
                .thenReturn(100);

        mockMvc.perform(post(
                "/production-orders/1/schedules/create")
                        .param("scheduleDate", "2026-09-01")
                        .param("plannedQuantity", "101"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-schedule-form"))
                .andExpect(model().attribute(
                        "errorMessage",
                        is("日産計画の合計が月間計画数を超えています。")))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    @DisplayName("日産計画編集画面を表示できる")
    void showEditForm_succeeds() throws Exception {
        ProductionSchedule schedule = createSchedule();
        when(productionScheduleService
                .getProductionScheduleById(10L))
                .thenReturn(schedule);
        stubFormSummary();

        mockMvc.perform(get("/production-schedules/10/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "production-schedule-edit-form"))
                .andExpect(model().attribute(
                        "productionSchedule",
                        schedule))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    @DisplayName("日産計画を更新して生産計画詳細へ戻れる")
    void update_succeeds() throws Exception {
        when(productionScheduleService
                .getProductionScheduleById(10L))
                .thenReturn(createSchedule());

        mockMvc.perform(post("/production-schedules/10/edit")
                        .param("scheduleDate", "2026-09-02")
                        .param("plannedQuantity", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/production-orders/1/view"));

        verify(productionScheduleService)
                .updateProductionSchedule(
                        10L,
                        LocalDate.of(2026, 9, 2),
                        30);
    }

    @Test
    @DisplayName("日産計画を確定して生産計画詳細へ戻れる")
    void confirm_succeeds() throws Exception {
        when(productionScheduleService
                .getProductionScheduleById(10L))
                .thenReturn(createSchedule());

        mockMvc.perform(post(
                "/production-schedules/10/confirm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/production-orders/1/view"));

        verify(productionScheduleService)
                .confirmProductionSchedule(10L);
    }

    @Test
    @DisplayName("確定済み日産計画から部品を発行して生産計画詳細へ戻れる")
    void issueComponents_succeeds() throws Exception {
        when(productionScheduleService
                .getProductionScheduleById(10L))
                .thenReturn(createSchedule());
        mockMvc.perform(post(
                "/production-schedules/10/issue-components"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/production-orders/1/view"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "BodyとNeckを一括発行しました。"));
        verify(productionScheduleService)
                .issueComponents(10L);
    }

    @Test
    @DisplayName("部品発行の業務エラーをFlashメッセージで表示できる")
    void issueComponents_businessError_redirectsWithFlash()
            throws Exception {
        when(productionScheduleService
                .getProductionScheduleById(10L))
                .thenReturn(createSchedule());
        org.mockito.Mockito.doThrow(
                new BusinessException("既に部品発行済みです。"))
                .when(productionScheduleService)
                .issueComponents(10L);

        mockMvc.perform(post(
                "/production-schedules/10/issue-components"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/production-orders/1/view"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "既に部品発行済みです。"));
    }

    @Test
    @DisplayName("日産計画を取消して生産計画詳細へ戻れる")
    void cancel_succeeds() throws Exception {
        when(productionScheduleService
                .getProductionScheduleById(10L))
                .thenReturn(createSchedule());

        mockMvc.perform(post(
                "/production-schedules/10/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/production-orders/1/view"));

        verify(productionScheduleService)
                .cancelProductionSchedule(10L);
    }

    private void stubFormSummary() {
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(createOrder());
        when(productionScheduleService.getAllocatedQuantity(1L))
                .thenReturn(30);
        when(productionScheduleService.getUnallocatedQuantity(1L))
                .thenReturn(70);
    }

    private ProductionScheduleUpdateRequest createUpdateRequest() {
        ProductionScheduleUpdateRequest request =
                new ProductionScheduleUpdateRequest();
        request.setScheduleDate(LocalDate.of(2026, 9, 1));
        request.setPlannedQuantity(20);
        return request;
    }

    private ProductionSchedule createSchedule() {
        ProductionSchedule schedule = new ProductionSchedule(
                createOrder(),
                LocalDate.of(2026, 9, 1),
                20,
                ProductionScheduleStatusConstants.PLANNED);
        schedule.setId(10L);
        return schedule;
    }

    private ProductionOrder createOrder() {
        Product product = new Product();
        product.setId(10L);
        product.setModelNo("TEST-0001");
        product.setProductName("Test Product");
        product.setColor("Black");

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
    @DisplayName("yyyy/MM/dd形式の日産計画日を変換できる")
    void create_bindsSlashDateFormat() throws Exception {
        mockMvc.perform(post("/production-orders/1/schedules/create")
                        .param("scheduleDate", "2026/09/03")
                        .param("plannedQuantity", "20"))
                .andExpect(status().is3xxRedirection());
        verify(productionScheduleService).createProductionSchedule(
                1L, LocalDate.of(2026, 9, 3), 20);
    }
}
