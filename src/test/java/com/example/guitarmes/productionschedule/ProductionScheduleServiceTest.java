package com.example.guitarmes.productionschedule;

import static com.example.guitarmes.productionschedule.ProductionScheduleStatusConstants.CANCELLED;
import static com.example.guitarmes.productionschedule.ProductionScheduleStatusConstants.COMPLETED;
import static com.example.guitarmes.productionschedule.ProductionScheduleStatusConstants.CONFIRMED;
import static com.example.guitarmes.productionschedule.ProductionScheduleStatusConstants.PLANNED;
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.productionorder.ProductionOrderStatusConstants;

@ExtendWith(MockitoExtension.class)
class ProductionScheduleServiceTest {

    @Mock
    private ProductionScheduleRepository
            productionScheduleRepository;

    @Mock
    private ProductionOrderRepository
            productionOrderRepository;

    private ProductionScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ProductionScheduleService(
                productionScheduleRepository,
                productionOrderRepository);
    }

    @Test
    @DisplayName("日産計画を計画中で登録できる")
    void create_validRequest_succeeds() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        LocalDate scheduleDate =
                LocalDate.of(2026, 9, 1);

        stubOrder(order);

        when(productionScheduleRepository
                .existsByProductionOrderIdAndScheduleDate(
                        1L,
                        scheduleDate))
                .thenReturn(false);

        when(productionScheduleRepository
                .sumAllocatedQuantity(1L, CANCELLED))
                .thenReturn(30L);

        when(productionScheduleRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductionSchedule result =
                service.createProductionSchedule(
                        1L,
                        scheduleDate,
                        20);

        assertSame(order, result.getProductionOrder());
        assertEquals(scheduleDate, result.getScheduleDate());
        assertEquals(20, result.getPlannedQuantity());
        assertEquals(PLANNED, result.getStatus());
    }

    @Test
    @DisplayName("存在しない生産計画への登録を拒否する")
    void create_unknownOrder_throws() {
        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.createProductionSchedule(
                        1L,
                        LocalDate.of(2026, 9, 1),
                        20));

        verify(productionScheduleRepository, never())
                .save(any());
    }

    @Test
    @DisplayName("日産計画日が未指定なら登録を拒否する")
    void create_nullScheduleDate_throws() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        stubOrder(order);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProductionSchedule(
                        1L,
                        null,
                        20));

        assertTrue(exception.getMessage().contains("日産計画日"));
    }

    @Test
    @DisplayName("日産計画数が0なら登録を拒否する")
    void create_zeroQuantity_throws() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        stubOrder(order);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProductionSchedule(
                        1L,
                        LocalDate.of(2026, 9, 1),
                        0));

        assertTrue(exception.getMessage().contains("1以上"));
    }

    @Test
    @DisplayName("対象月外の日産計画を拒否する")
    void create_outsidePlanMonth_throws() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        stubOrder(order);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProductionSchedule(
                        1L,
                        LocalDate.of(2026, 10, 1),
                        20));

        assertTrue(exception.getMessage().contains("対象月内"));
    }

    @Test
    @DisplayName("同じ日付の日産計画を拒否する")
    void create_duplicateDate_throws() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        LocalDate scheduleDate =
                LocalDate.of(2026, 9, 1);

        stubOrder(order);

        when(productionScheduleRepository
                .existsByProductionOrderIdAndScheduleDate(
                        1L,
                        scheduleDate))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProductionSchedule(
                        1L,
                        scheduleDate,
                        20));

        assertTrue(exception.getMessage().contains("すでに登録"));
    }

    @Test
    @DisplayName("月間計画数を超える日産計画を拒否する")
    void create_exceedsMonthlyQuantity_throws() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        LocalDate scheduleDate =
                LocalDate.of(2026, 9, 1);

        stubOrder(order);

        when(productionScheduleRepository
                .existsByProductionOrderIdAndScheduleDate(
                        1L,
                        scheduleDate))
                .thenReturn(false);

        when(productionScheduleRepository
                .sumAllocatedQuantity(1L, CANCELLED))
                .thenReturn(90L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProductionSchedule(
                        1L,
                        scheduleDate,
                        20));

        assertTrue(exception.getMessage().contains("月間計画数"));
    }

    @Test
    @DisplayName("中止済み生産計画への登録を拒否する")
    void create_cancelledOrder_throws() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.CANCELLED,
                100);

        stubOrder(order);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProductionSchedule(
                        1L,
                        LocalDate.of(2026, 9, 1),
                        20));

        assertTrue(exception.getMessage().contains("中止"));
    }

    @Test
    @DisplayName("完了済み生産計画への登録を拒否する")
    void create_completedOrder_throws() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.COMPLETED,
                100);

        stubOrder(order);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProductionSchedule(
                        1L,
                        LocalDate.of(2026, 9, 1),
                        20));

        assertTrue(exception.getMessage().contains("完了済み"));
    }

    @Test
    @DisplayName("計画中の日産計画を更新できる")
    void update_planned_succeeds() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        ProductionSchedule schedule = schedule(
                order,
                PLANNED,
                LocalDate.of(2026, 9, 1),
                20);

        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        when(productionScheduleRepository
                .existsByProductionOrderIdAndScheduleDateAndIdNot(
                        1L,
                        LocalDate.of(2026, 9, 2),
                        10L))
                .thenReturn(false);

        when(productionScheduleRepository
                .sumAllocatedQuantityExcludingId(
                        1L,
                        CANCELLED,
                        10L))
                .thenReturn(40L);

        when(productionScheduleRepository.save(schedule))
                .thenReturn(schedule);

        ProductionSchedule result =
                service.updateProductionSchedule(
                        10L,
                        LocalDate.of(2026, 9, 2),
                        30);

        assertEquals(
                LocalDate.of(2026, 9, 2),
                result.getScheduleDate());

        assertEquals(30, result.getPlannedQuantity());
    }

    @Test
    @DisplayName("確定済みの日産計画は編集できない")
    void update_confirmed_throws() {
        ProductionSchedule schedule = schedule(
                order(
                        ProductionOrderStatusConstants.PLANNED,
                        100),
                CONFIRMED,
                LocalDate.of(2026, 9, 1),
                20);

        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateProductionSchedule(
                        10L,
                        LocalDate.of(2026, 9, 2),
                        30));

        assertTrue(exception.getMessage().contains("計画中"));
    }

    @Test
    @DisplayName("日産計画を確定できる")
    void confirm_planned_succeeds() {
        ProductionSchedule schedule = schedule(
                order(
                        ProductionOrderStatusConstants.PLANNED,
                        100),
                PLANNED,
                LocalDate.of(2026, 9, 1),
                20);

        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        when(productionScheduleRepository.save(schedule))
                .thenReturn(schedule);

        ProductionSchedule result =
                service.confirmProductionSchedule(10L);

        assertEquals(CONFIRMED, result.getStatus());
    }

    @Test
    @DisplayName("確定済みの日産計画を取消できる")
    void cancel_confirmed_succeeds() {
        ProductionSchedule schedule = schedule(
                order(
                        ProductionOrderStatusConstants.PLANNED,
                        100),
                CONFIRMED,
                LocalDate.of(2026, 9, 1),
                20);

        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        when(productionScheduleRepository.save(schedule))
                .thenReturn(schedule);

        ProductionSchedule result =
                service.cancelProductionSchedule(10L);

        assertEquals(CANCELLED, result.getStatus());
    }

    @Test
    @DisplayName("完了済みの日産計画は取消できない")
    void cancel_completed_throws() {
        ProductionSchedule schedule = schedule(
                order(
                        ProductionOrderStatusConstants.PLANNED,
                        100),
                COMPLETED,
                LocalDate.of(2026, 9, 1),
                20);

        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                BusinessException.class,
                () -> service.cancelProductionSchedule(10L));
    }

    @Test
    @DisplayName("取消済みを除いた割当済数を取得できる")
    void getAllocatedQuantity_succeeds() {
        stubOrder(order(
                ProductionOrderStatusConstants.PLANNED,
                100));

        when(productionScheduleRepository
                .sumAllocatedQuantity(1L, CANCELLED))
                .thenReturn(60L);

        assertEquals(60, service.getAllocatedQuantity(1L));
    }

    @Test
    @DisplayName("未割当数を取得できる")
    void getUnallocatedQuantity_succeeds() {
        stubOrder(order(
                ProductionOrderStatusConstants.PLANNED,
                100));

        when(productionScheduleRepository
                .sumAllocatedQuantity(1L, CANCELLED))
                .thenReturn(60L);

        assertEquals(40, service.getUnallocatedQuantity(1L));
    }

    @Test
    @DisplayName("日産計画を日付順で取得できる")
    void getSchedulesByOrderId_succeeds() {
        ProductionOrder order = order(
                ProductionOrderStatusConstants.PLANNED,
                100);

        List<ProductionSchedule> schedules = List.of(
                schedule(
                        order,
                        PLANNED,
                        LocalDate.of(2026, 9, 1),
                        20),
                schedule(
                        order,
                        CONFIRMED,
                        LocalDate.of(2026, 9, 2),
                        30));

        stubOrder(order);

        when(productionScheduleRepository
                .findByProductionOrderIdOrderByScheduleDateAsc(1L))
                .thenReturn(schedules);

        assertSame(
                schedules,
                service.getProductionSchedulesByOrderId(1L));
    }

    private void stubOrder(
            ProductionOrder order) {

        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
    }

    private ProductionOrder order(
            String status,
            int plannedQuantity) {

        Product product = new Product();
        product.setId(10L);

        ProductionOrder order = new ProductionOrder(
                "PO260001",
                product,
                plannedQuantity,
                YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                status);

        order.setId(1L);

        return order;
    }

    private ProductionSchedule schedule(
            ProductionOrder order,
            String status,
            LocalDate scheduleDate,
            int plannedQuantity) {

        ProductionSchedule schedule =
                new ProductionSchedule(
                        order,
                        scheduleDate,
                        plannedQuantity,
                        status);

        schedule.setId(10L);

        return schedule;
    }
}
