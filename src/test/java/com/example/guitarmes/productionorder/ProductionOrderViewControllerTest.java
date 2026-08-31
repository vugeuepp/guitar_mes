package com.example.guitarmes.productionorder;

import static org.hamcrest.Matchers.hasSize;
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
import java.util.List;

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
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductionOrderViewControllerTest {

    @Mock
    private ProductionOrderService productionOrderService;

    @Mock
    private ProductService productService;

    @Mock
    private GuitarService guitarService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductionOrderViewController controller =
                new ProductionOrderViewController(
                        productionOrderService,
                        productService,
                        guitarService);

        DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setConversionService(conversionService)
                .build();
    }

    @Test
    @DisplayName("生産計画一覧画面を表示できる")
    void showList_succeeds() throws Exception {
        when(productionOrderService.getProductionOrders())
                .thenReturn(List.of(createOrder()));

        mockMvc.perform(get("/production-orders/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-list"))
                .andExpect(model().attribute("orders", hasSize(1)));
    }

    @Test
    @DisplayName("生産計画登録画面を表示できる")
    void showCreateForm_succeeds() throws Exception {
        when(productService.getProducts())
                .thenReturn(List.of(createProduct()));

        mockMvc.perform(get("/production-orders/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-form"))
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    @DisplayName("生産計画を登録して一覧へリダイレクトできる")
    void create_succeeds() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate dueDate = LocalDate.of(2026, 9, 30);

        mockMvc.perform(post("/production-orders/create")
                        .param("productId", "10")
                        .param("plannedQuantity", "20")
                        .param("planMonth", "2026-09")
                        .param("plannedStartDate", "2026-09-01")
                        .param("dueDate", "2026-09-30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/production-orders/view"));

        verify(productionOrderService)
                .createProductionOrder(
                        10L,
                        20,
                        YearMonth.of(2026, 9),
                        startDate,
                        dueDate);
    }

    @Test
    @DisplayName("生産計画詳細画面を表示できる")
    void showDetail_succeeds() throws Exception {
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(createOrder());
        when(guitarService.getGuitarsByProductionOrderId(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/production-orders/1/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-detail"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("guitars", hasSize(0)));
    }

    @Test
    @DisplayName("生産計画編集画面へ日付を含む初期値を渡せる")
    void showEditForm_containsInitialDates() throws Exception {
        ProductionOrder order = createOrder();
        ProductionOrderUpdateRequest request = createUpdateRequest();

        when(productionOrderService
                .getProductionOrderUpdateRequest(1L))
                .thenReturn(request);
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(order);
        when(productService.getProducts())
                .thenReturn(List.of(createProduct()));

        mockMvc.perform(get("/production-orders/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-edit-form"))
                .andExpect(model().attribute("request", request))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(model().attribute(
                        "request",
                        org.hamcrest.Matchers.hasProperty(
                                "planMonth",
                                is(YearMonth.of(2026, 9)))))
                .andExpect(model().attribute(
                        "request",
                        org.hamcrest.Matchers.hasProperty(
                                "plannedStartDate",
                                is(LocalDate.of(2026, 9, 1)))))
                .andExpect(model().attribute(
                        "request",
                        org.hamcrest.Matchers.hasProperty(
                                "dueDate",
                                is(LocalDate.of(2026, 9, 30)))));
    }

    @Test
    @DisplayName("日付を含む生産計画更新を受け付けて詳細へリダイレクトできる")
    void update_withIsoDates_succeeds() throws Exception {
        mockMvc.perform(post("/production-orders/1/edit")
                        .param("productId", "10")
                        .param("plannedQuantity", "30")
                        .param("planMonth", "2026-10")
                        .param("plannedStartDate", "2026-10-01")
                        .param("dueDate", "2026-10-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/production-orders/1/view"));

        verify(productionOrderService)
                .updateProductionOrder(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.argThat(request ->
                                request.getProductId().equals(10L)
                                && request.getPlannedQuantity().equals(30)
                                && request.getPlanMonth().equals(
                                        YearMonth.of(2026, 10))
                                && request.getPlannedStartDate().equals(
                                        LocalDate.of(2026, 10, 1))
                                && request.getDueDate().equals(
                                        LocalDate.of(2026, 10, 31))));
    }

    @Test
    @DisplayName("更新時の業務エラーを編集画面へ表示できる")
    void update_businessError_returnsEditForm() throws Exception {
        ProductionOrder order = createOrder();
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(order);
        when(productService.getProducts())
                .thenReturn(List.of(createProduct()));
        when(productionOrderService.updateProductionOrder(
                org.mockito.ArgumentMatchers.eq(1L),
                any(ProductionOrderUpdateRequest.class)))
                .thenThrow(new BusinessException(
                        "計画数は1以上で入力してください。"));

        mockMvc.perform(post("/production-orders/1/edit")
                        .param("productId", "10")
                        .param("plannedQuantity", "0")
                        .param("planMonth", "2026-09")
                        .param("plannedStartDate", "2026-09-01")
                        .param("dueDate", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-edit-form"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "計画数は1以上で入力してください。"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("products", hasSize(1)));
    }

    @Test
    @DisplayName("生産計画を中止して詳細へリダイレクトできる")
    void cancel_succeeds() throws Exception {
        mockMvc.perform(post("/production-orders/1/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/production-orders/1/view"));

        verify(productionOrderService)
                .cancelProductionOrder(1L);
    }

    @Test
    @DisplayName("中止時の業務エラーを詳細画面へ表示できる")
    void cancel_businessError_returnsDetail() throws Exception {
        ProductionOrder order = createOrder();
        when(productionOrderService.cancelProductionOrder(1L))
                .thenThrow(new BusinessException(
                        "計画中の生産指示だけ編集・取消できます。"));
        when(productionOrderService.getProductionOrderById(1L))
                .thenReturn(order);
        when(guitarService.getGuitarsByProductionOrderId(1L))
                .thenReturn(List.of());

        mockMvc.perform(post("/production-orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(view().name("production-order-detail"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "計画中の生産指示だけ編集・取消できます。"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("guitars", hasSize(0)));
    }

    private ProductionOrder createOrder() {
        ProductionOrder order = new ProductionOrder(
                "PO260001",
                createProduct(),
                20,
                YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                ProductionOrderStatusConstants.PLANNED);
        order.setId(1L);
        return order;
    }

    private ProductionOrderUpdateRequest createUpdateRequest() {
        ProductionOrderUpdateRequest request =
                new ProductionOrderUpdateRequest();
        request.setProductId(10L);
        request.setPlannedQuantity(20);
        request.setPlanMonth(YearMonth.of(2026, 9));
        request.setPlannedStartDate(LocalDate.of(2026, 9, 1));
        request.setDueDate(LocalDate.of(2026, 9, 30));
        return request;
    }

    private Product createProduct() {
        Product product = new Product();
        product.setId(10L);
        product.setModelNo("TEST-0001");
        product.setProductName("Test Product");
        product.setColor("3-Color Sunburst");
        product.setFingerboardMaterial("Rosewood");
        return product;
    }
}
