package com.example.guitarmes.assembly;

import static com.example.guitarmes.common.StatusConstants.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.example.guitarmes.body.Body;
import com.example.guitarmes.body.BodyRepository;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.neck.Neck;
import com.example.guitarmes.neck.NeckRepository;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.productionorder.ProductionOrderStatusConstants;
import com.example.guitarmes.productionschedule.ProductionSchedule;
import com.example.guitarmes.productionschedule.ProductionScheduleRepository;

@ExtendWith(MockitoExtension.class)
class AssemblyServiceTest {
    @Mock AssemblyRepository assemblyRepository;
    @Mock NeckRepository neckRepository;
    @Mock BodyRepository bodyRepository;
    @Mock ProductionOrderRepository productionOrderRepository;
    @Mock GuitarService guitarService;
    @Mock ProductionScheduleRepository productionScheduleRepository;
    private AssemblyService service;

    @BeforeEach
    void setUp() {
        service = new AssemblyService(
                assemblyRepository,
                neckRepository,
                bodyRepository,
                productionOrderRepository,
                guitarService,
                productionScheduleRepository);
    }

    @Test
    @DisplayName("異なる日産計画のBodyとNeckは組み立てられない")
    void createAssembly_differentSchedules_throws() {
        ProductionOrder order = order();
        ProductionSchedule selected = schedule(order, 10L);
        ProductionSchedule other = schedule(order, 11L);
        Body body = body(order, selected);
        Neck neck = neck(order, other);
        when(productionOrderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(productionScheduleRepository.findById(10L))
                .thenReturn(Optional.of(selected));
        when(bodyRepository.findById(20L)).thenReturn(Optional.of(body));
        when(neckRepository.findById(30L)).thenReturn(Optional.of(neck));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAssembly(
                        1L, 10L, 30L, 20L, "Worker"));
        assertTrue(exception.getMessage().contains("同じ日産計画"));
    }

    private ProductionOrder order() {
        Product product = org.mockito.Mockito.mock(Product.class);
        com.example.guitarmes.master.body.BodyMaster bm =
                org.mockito.Mockito.mock(com.example.guitarmes.master.body.BodyMaster.class);
        com.example.guitarmes.master.neck.NeckMaster nm =
                org.mockito.Mockito.mock(com.example.guitarmes.master.neck.NeckMaster.class);
        when(bm.getId()).thenReturn(100L);
        when(nm.getId()).thenReturn(200L);
        when(product.getBodyMaster()).thenReturn(bm);
        when(product.getNeckMaster()).thenReturn(nm);
        ProductionOrder order = new ProductionOrder(
                "PO260001", product, 1, YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                ProductionOrderStatusConstants.PLANNED);
        order.setId(1L);
        return order;
    }

    private ProductionSchedule schedule(ProductionOrder order, Long id) {
        ProductionSchedule schedule = new ProductionSchedule(
                order, LocalDate.of(2026, 9, 1), 1, "CONFIRMED");
        schedule.setId(id);
        return schedule;
    }

    private Body body(ProductionOrder order, ProductionSchedule schedule) {
        Body body = new Body();
        body.setId(20L); body.setStatus(AVAILABLE);
        body.setProductionOrder(order); body.setProductionSchedule(schedule);
        body.setBodyMaster(order.getProduct().getBodyMaster());
        return body;
    }

    private Neck neck(ProductionOrder order, ProductionSchedule schedule) {
        Neck neck = new Neck();
        neck.setId(30L); neck.setStatus(AVAILABLE);
        neck.setProductionOrder(order); neck.setProductionSchedule(schedule);
        neck.setNeckMaster(order.getProduct().getNeckMaster());
        return neck;
    }
}
