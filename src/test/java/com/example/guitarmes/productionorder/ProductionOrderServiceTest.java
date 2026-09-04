package com.example.guitarmes.productionorder;

import static com.example.guitarmes.productionorder.ProductionOrderStatusConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductRepository;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.productionorder.ProductionOrderService;
import com.example.guitarmes.productionorder.ProductionOrderUpdateRequest;

@ExtendWith(MockitoExtension.class)
class ProductionOrderServiceTest {

    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private GuitarRepository guitarRepository;

    private ProductionOrderService service;

    @BeforeEach
    void setUp() {
        service = new ProductionOrderService(
                productionOrderRepository,
                productRepository,
                guitarRepository);
    }

    @Test
    @DisplayName("未着手の計画から編集DTOを作成できる")
    void getUpdateRequest_planned_succeeds() {
        ProductionOrder order = order(PLANNED, 0, 0);
        stubOrder(order);
        ProductionOrderUpdateRequest request =
                service.getProductionOrderUpdateRequest(1L);
        assertEquals(10L, request.getProductId());
        assertEquals(20, request.getPlannedQuantity());
        assertEquals(YearMonth.of(2026, 9), request.getPlanMonth());
        assertEquals(LocalDate.of(2026, 9, 1), request.getPlannedStartDate());
        assertEquals(LocalDate.of(2026, 9, 30), request.getDueDate());
    }

    @Test
    @DisplayName("未着手の計画を更新できる")
    void update_planned_succeeds() {
        ProductionOrder order = order(PLANNED, 0, 0);
        Product changedProduct = product(20L);
        ProductionOrderUpdateRequest request = request();
        request.setProductId(20L);
        request.setPlannedQuantity(30);
        request.setPlanMonth(YearMonth.of(2026, 10));
        request.setPlannedStartDate(LocalDate.of(2026, 10, 1));
        request.setDueDate(LocalDate.of(2026, 10, 31));
        stubOrder(order);
        when(productRepository.findById(20L))
                .thenReturn(Optional.of(changedProduct));
        when(productionOrderRepository.save(order)).thenReturn(order);
        ProductionOrder result = service.updateProductionOrder(1L, request);
        assertSame(changedProduct, result.getProduct());
        assertEquals(30, result.getPlannedQuantity());
        assertEquals(YearMonth.of(2026, 10), result.getPlanMonth());
        assertEquals(LocalDate.of(2026, 10, 1), result.getPlannedStartDate());
        assertEquals(LocalDate.of(2026, 10, 31), result.getDueDate());
        assertEquals("PO260001", result.getOrderNo());
        assertEquals(PLANNED, result.getStatus());
    }

    @Test
    @DisplayName("未着手の計画を中止できる")
    void cancel_planned_succeeds() {
        ProductionOrder order = order(PLANNED, 0, 0);
        stubOrder(order);
        when(productionOrderRepository.save(order)).thenReturn(order);
        ProductionOrder result = service.cancelProductionOrder(1L);
        assertEquals(CANCELLED, result.getStatus());
    }

    @Test
    @DisplayName("製造中の計画は編集できない")
    void update_inProgress_throws() {
        ProductionOrder order = order(IN_PROGRESS, 1, 0);
        stubOrderWithoutGuitarCheck(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request()));
        assertTrue(ex.getMessage().contains("計画中"));
        verify(productionOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("着手実績がある計画は編集できない")
    void update_started_throws() {
        ProductionOrder order = order(PLANNED, 1, 0);
        stubOrderWithoutGuitarCheck(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request()));
        assertTrue(ex.getMessage().contains("着手済み"));
    }

    @Test
    @DisplayName("完成実績がある計画は中止できない")
    void cancel_completedQuantity_throws() {
        ProductionOrder order = order(PLANNED, 0, 1);
        stubOrderWithoutGuitarCheck(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.cancelProductionOrder(1L));
        assertTrue(ex.getMessage().contains("完成実績"));
    }

    @Test
    @DisplayName("Guitar発行済みの計画は編集できない")
    void update_issuedGuitar_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(guitarRepository.existsByProductionOrderId(1L))
                .thenReturn(true);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request()));
        assertTrue(ex.getMessage().contains("ギター個体"));
    }

    @Test
    @DisplayName("中止済み計画は再度中止できない")
    void cancel_cancelled_throws() {
        ProductionOrder order = order(CANCELLED, 0, 0);
        stubOrderWithoutGuitarCheck(order);
        assertThrows(
                BusinessException.class,
                () -> service.cancelProductionOrder(1L));
    }

    @Test
    @DisplayName("計画数が0なら更新を拒否する")
    void update_zeroQuantity_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setPlannedQuantity(0);
        stubOrder(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request));
        assertTrue(ex.getMessage().contains("1以上"));
    }

    @Test
    @DisplayName("納期が開始予定日より前なら更新を拒否する")
    void update_invalidDateRange_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setPlannedStartDate(LocalDate.of(2026, 10, 10));
        request.setDueDate(LocalDate.of(2026, 10, 9));
        stubOrder(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request));
        assertTrue(ex.getMessage().contains("開始予定日以降"));
    }

    @Test
    @DisplayName("存在しない製品への変更を拒否する")
    void update_unknownProduct_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        stubOrder(order);
        when(productRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> service.updateProductionOrder(1L, request()));
    }

    @Test
    @DisplayName("nullの更新内容を拒否する")
    void update_nullRequest_throws() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, null));
        assertTrue(ex.getMessage().contains("指定されていません"));
        verify(productionOrderRepository, never()).findById(any());
    }

    private void stubOrder(ProductionOrder order) {
        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(guitarRepository.existsByProductionOrderId(1L))
                .thenReturn(false);
    }

    private void stubOrderWithoutGuitarCheck(ProductionOrder order) {
        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
    }

    private ProductionOrder order(
            String status,
            int started,
            int completed) {
        ProductionOrder order = new ProductionOrder(
                "PO260001",
                product(10L),
                20,
                YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                status);
        order.setId(1L);
        order.setStartedQuantity(started);
        order.setCompletedQuantity(completed);
        return order;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private ProductionOrderUpdateRequest request() {
        ProductionOrderUpdateRequest request =
                new ProductionOrderUpdateRequest();
        request.setProductId(10L);
        request.setPlannedQuantity(20);
        request.setPlanMonth(YearMonth.of(2026, 9));
        request.setPlannedStartDate(LocalDate.now().plusDays(1));
        request.setDueDate(LocalDate.now().plusDays(30));
        return request;
    }

    @Test
    @DisplayName("過去の開始予定日は更新を拒否する")
    void update_pastStartDate_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setPlannedStartDate(LocalDate.now().minusDays(1));
        request.setDueDate(LocalDate.now().plusDays(1));
        stubOrder(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request));
        assertEquals(
                "生産開始予定日は当日以降にしてください。",
                ex.getMessage());
    }

    @Test
    @DisplayName("5年を超える開始予定日は更新を拒否する")
    void update_tooFarFuture_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setPlannedStartDate(LocalDate.now().plusYears(5).plusDays(1));
        request.setDueDate(LocalDate.now().plusYears(5).plusDays(1));
        stubOrder(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request));
        assertEquals(
                "日付は当日から5年以内で入力してください。",
                ex.getMessage());
    }

    @Test
    @DisplayName("開始予定日が未入力なら更新を拒否する")
    void update_nullStartDate_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setPlannedStartDate(null);
        stubOrder(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request));
        assertEquals("生産開始予定日を入力してください。", ex.getMessage());
    }

    @Test
    @DisplayName("納期が未入力なら更新を拒否する")
    void update_nullDueDate_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setDueDate(null);
        stubOrder(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request));
        assertEquals("納期を入力してください。", ex.getMessage());
    }

    @Test
    @DisplayName("当日の開始予定日は更新できる")
    void update_todayStartDate_succeeds() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setPlannedStartDate(LocalDate.now());
        request.setDueDate(LocalDate.now());
        stubOrder(order);
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(order.getProduct()));
        when(productionOrderRepository.save(order)).thenReturn(order);
        ProductionOrder result = service.updateProductionOrder(1L, request);
        assertEquals(LocalDate.now(), result.getPlannedStartDate());
        assertEquals(LocalDate.now(), result.getDueDate());
    }

    @Test
    @DisplayName("5年後の境界日は更新できる")
    void update_maximumDate_succeeds() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        LocalDate maximum = LocalDate.now().plusYears(5);
        request.setPlannedStartDate(maximum);
        request.setDueDate(maximum);
        stubOrder(order);
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(order.getProduct()));
        when(productionOrderRepository.save(order)).thenReturn(order);
        ProductionOrder result = service.updateProductionOrder(1L, request);
        assertEquals(maximum, result.getPlannedStartDate());
        assertEquals(maximum, result.getDueDate());
    }

    @Test
    @DisplayName("5年を超える納期は更新を拒否する")
    void update_dueDateTooFarFuture_throws() {
        ProductionOrder order = order(PLANNED, 0, 0);
        ProductionOrderUpdateRequest request = request();
        request.setPlannedStartDate(LocalDate.now().plusDays(1));
        request.setDueDate(LocalDate.now().plusYears(5).plusDays(1));
        stubOrder(order);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateProductionOrder(1L, request));
        assertEquals("日付は当日から5年以内で入力してください。", ex.getMessage());
    }
}
