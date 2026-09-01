package com.example.guitarmes.productionschedule;

import static com.example.guitarmes.productionschedule.ProductionScheduleStatusConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.guitarmes.body.BodyRepository;
import com.example.guitarmes.body.BodyService;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.master.body.BodyMaster;
import com.example.guitarmes.master.neck.NeckMaster;
import com.example.guitarmes.neck.NeckRepository;
import com.example.guitarmes.neck.NeckService;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.productionorder.ProductionOrderStatusConstants;

@ExtendWith(MockitoExtension.class)
class ProductionScheduleIssueServiceTest {
    @Mock
    private ProductionScheduleRepository productionScheduleRepository;
    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private BodyRepository bodyRepository;
    @Mock
    private BodyService bodyService;
    @Mock
    private NeckRepository neckRepository;
    @Mock
    private NeckService neckService;

    private ProductionScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ProductionScheduleService(
                productionScheduleRepository,
                productionOrderRepository,
                bodyRepository,
                bodyService,
                neckRepository,
                neckService);
    }

    @Test
    @DisplayName("確定済み日産計画から計画数分のBodyとNeckを発行できる")
    void issueComponents_confirmed_succeeds() {
        ProductionSchedule schedule = schedule(CONFIRMED, 3, true, true);
        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));
        when(bodyRepository.countByProductionSchedule_Id(10L))
                .thenReturn(0L);

        service.issueComponents(10L);

        Product product = schedule.getProductionOrder().getProduct();
        verify(bodyService, times(3)).createBody(
                product.getBodyMaster().getId(),
                schedule.getProductionOrder(),
                schedule);
        verify(neckService, times(3)).createNeck(
                product.getNeckMaster().getId(),
                product,
                schedule.getProductionOrder(),
                schedule);
    }

    @Test
    @DisplayName("計画中の日産計画から部品を発行できない")
    void issueComponents_planned_throws() {
        ProductionSchedule schedule = schedule(PLANNED, 3, true, true);
        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.issueComponents(10L));

        assertTrue(exception.getMessage().contains("確定済み"));
        verify(bodyService, never()).createBody(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("ボディマスタ未設定なら部品を発行できない")
    void issueComponents_missingBodyMaster_throws() {
        ProductionSchedule schedule = schedule(CONFIRMED, 3, false, true);
        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.issueComponents(10L));

        assertTrue(exception.getMessage().contains("ボディマスタ"));
    }

    @Test
    @DisplayName("ネックマスタ未設定なら部品を発行できない")
    void issueComponents_missingNeckMaster_throws() {
        ProductionSchedule schedule = schedule(CONFIRMED, 3, true, false);
        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.issueComponents(10L));

        assertTrue(exception.getMessage().contains("ネックマスタ"));
    }

    @Test
    @DisplayName("BodyまたはNeckが発行済みなら二重発行を拒否する")
    void issueComponents_alreadyIssued_throws() {
        ProductionSchedule schedule = schedule(CONFIRMED, 3, true, true);
        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));
        when(bodyRepository.countByProductionSchedule_Id(10L))
                .thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.issueComponents(10L));

        assertTrue(exception.getMessage().contains("発行済み"));
        verify(bodyService, never()).createBody(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("部品発行済みの日産計画は取消できない")
    void cancel_issued_throws() {
        ProductionSchedule schedule = schedule(CONFIRMED, 3, true, true);
        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(schedule));
        when(bodyRepository.countByProductionSchedule_Id(10L))
                .thenReturn(3L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.cancelProductionSchedule(10L));

        assertTrue(exception.getMessage().contains("発行済み"));
    }

    private ProductionSchedule schedule(
            String status,
            int quantity,
            boolean withBodyMaster,
            boolean withNeckMaster) {
        Product product = new Product();
        product.setId(20L);
        if (withBodyMaster) {
            BodyMaster bodyMaster = new BodyMaster();
            bodyMaster.setId(30L);
            product.setBodyMaster(bodyMaster);
        }
        if (withNeckMaster) {
            NeckMaster neckMaster =
                    new NeckMaster(
                            "NM-TEST",
                            "Test Neck",
                            "Stratocaster",
                            "Maple",
                            "Rosewood",
                            21,
                            "25.5");
            product.setNeckMaster(neckMaster);
        }
        ProductionOrder order = new ProductionOrder(
                "PO260001",
                product,
                quantity,
                YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                ProductionOrderStatusConstants.PLANNED);
        order.setId(1L);
        ProductionSchedule schedule = new ProductionSchedule(
                order,
                LocalDate.of(2026, 9, 1),
                quantity,
                status);
        schedule.setId(10L);
        return schedule;
    }
}
