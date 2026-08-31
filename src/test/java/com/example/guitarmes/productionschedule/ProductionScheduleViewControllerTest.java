package com.example.guitarmes.productionschedule;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(createOrder());
        when(productionScheduleService.getAllocatedQuantity(1L))
                .thenReturn(30);
        when(productionScheduleService.getUnallocatedQuantity(1L))
                .thenReturn(70);

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
}
