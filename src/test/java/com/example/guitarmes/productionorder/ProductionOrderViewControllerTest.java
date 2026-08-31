package com.example.guitarmes.productionorder;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
        when(productionScheduleService
                .getProductionSchedulesByOrderId(1L))
                .thenReturn(List.of());
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
                        hasSize(0)))
                .andExpect(model().attribute("allocatedQuantity", 30))
                .andExpect(model().attribute("unallocatedQuantity", 70));
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
}
