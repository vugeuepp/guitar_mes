package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.process.work.ProcessWork;
import com.example.guitarmes.process.work.ProcessWorkCompletionValidator;
import com.example.guitarmes.process.work.ProcessWorkItem;
import com.example.guitarmes.process.work.ProcessWorkItemRepository;
import com.example.guitarmes.process.work.ProcessWorkItemStatus;
import com.example.guitarmes.process.work.ProcessWorkRepository;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;

/** 実ValidatorとServiceを組み合わせるDB不要テスト。DBのロック動作自体は検証しない。 */
class ProcessWorkEndIntegrationTest {
    private static final LocalDateTime OLD = LocalDateTime.of(2020, 1, 1, 0, 0);
    private final ProcessHistoryRepository histories = mock(ProcessHistoryRepository.class);
    private final GuitarRepository guitars = mock(GuitarRepository.class);
    private final ManufacturingProcessRepository processes = mock(ManufacturingProcessRepository.class);
    private final ProductionOrderRepository orders = mock(ProductionOrderRepository.class);
    private final ProcessWorkRepository works = mock(ProcessWorkRepository.class);
    private final ProcessWorkItemRepository items = mock(ProcessWorkItemRepository.class);
    private final List<ProcessHistory> targets = new ArrayList<>();
    private final List<Guitar> instruments = new ArrayList<>();
    private final ProductionOrder order = new ProductionOrder();
    private ProcessService service;
    private ManufacturingProcess process;

    @BeforeEach
    void setup() {
        service = new ProcessService(histories, guitars, processes, orders, null, null,
                new ProcessWorkCompletionValidator(works, items));
        ReflectionTestUtils.setField(service, "entityManager", mock(jakarta.persistence.EntityManager.class));
        process = new ManufacturingProcess("GUITAR", "対象工程", 1);
        process.setId(73L);
        var next = new ManufacturingProcess("GUITAR", "次工程", 2);
        next.setId(74L);
        when(processes.findById(73L)).thenReturn(Optional.of(process));
        when(processes.findByTargetTypeOrderByProcessOrderAsc("GUITAR")).thenReturn(List.of(process, next));
        order.setId(50L);
        order.setPlannedQuantity(3);
        order.setStartedQuantity(3);
        order.setCompletedQuantity(0);
        order.setStatus("IN_PROGRESS");
        order.setUpdatedAt(OLD);
        for (long id = 1; id <= 3; id++) {
            var history = new ProcessHistory(id, 73L, "Worker", OLD);
            history.setId(100 + id);
            targets.add(history);
            when(histories.findForUpdate(history.getId())).thenReturn(Optional.of(history));
            var guitar = new Guitar();
            guitar.setId(id);
            guitar.setCurrentProcess("対象工程");
            guitar.setUpdatedAt(OLD);
            guitar.setProductionOrder(order);
            instruments.add(guitar);
            when(guitars.findForUpdate(id)).thenReturn(Optional.of(guitar));
        }
        when(histories.save(any())).thenAnswer(i -> i.getArgument(0));
        when(histories.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void work(int index, String state) {
        var work = new ProcessWork();
        work.setId(200L + index);
        work.setProcessHistory(targets.get(index));
        when(works.findByProcessHistoryId(targets.get(index).getId())).thenReturn(Optional.of(work));
        var complete = new ProcessWorkItem();
        complete.setStatus(ProcessWorkItemStatus.COMPLETED);
        complete.setCompletedAt(OLD);
        var other = new ProcessWorkItem();
        other.setStatus("null".equals(state) ? null : ProcessWorkItemStatus.NOT_STARTED);
        var list = switch (state) {
            case "empty" -> List.<ProcessWorkItem>of();
            case "complete" -> List.of(complete);
            default -> List.of(complete, other);
        };
        when(items.findByProcessWorkIdOrderByItemOrderAsc(work.getId())).thenReturn(list);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void individualLegacyOrCompletedWorkAdvances(boolean hasWork) {
        if (hasWork) work(0, "complete");
        assertSame(targets.get(0), service.endProcess(101L));
        assertNotNull(targets.get(0).getEndTime());
        assertEquals("次工程", instruments.get(0).getCurrentProcess());
        assertEquals(targets.get(0).getEndTime(), instruments.get(0).getUpdatedAt());
        var sequence = inOrder(histories, works, guitars);
        sequence.verify(histories).findForUpdate(101L);
        sequence.verify(works).findByProcessHistoryId(101L);
        sequence.verify(guitars).findForUpdate(1L);
        sequence.verify(histories).save(targets.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unfinished", "empty", "null"})
    void individualInvalidWorkDoesNotChangeAnyState(String state) {
        work(0, state);
        assertThrows(BusinessException.class, () -> service.endProcess(101L));
        assertUnchanged();
        verifyNoInteractions(guitars, orders);
    }

    @Test
    void alreadyEndedHistoryTakesPriorityOverWorkValidation() {
        targets.get(0).setEndTime(OLD);
        assertTrue(assertThrows(BusinessException.class, () -> service.endProcess(101L))
                .getMessage().contains("すでに終了"));
        verifyNoInteractions(works, items, guitars, orders);
        assertEquals(OLD, targets.get(0).getEndTime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"legacy", "complete", "mixed"})
    void bulkSuccessValidatesAllBeforeUpdatesAndKeepsSortedUniqueLocks(String mode) {
        if (!"legacy".equals(mode)) {
            work(0, "complete");
            work(2, "complete");
        }
        if ("complete".equals(mode)) work(1, "complete");
        var result = service.endProcesses(List.of(103L, 101L, 102L, 101L));
        assertEquals(targets, result);
        var time = targets.get(0).getEndTime();
        assertNotNull(time);
        for (int i = 0; i < 3; i++) {
            assertEquals(time, targets.get(i).getEndTime());
            assertEquals(time, instruments.get(i).getUpdatedAt());
            assertEquals("次工程", instruments.get(i).getCurrentProcess());
        }
        var sequence = inOrder(histories, guitars, works);
        for (long id = 101; id <= 103; id++) sequence.verify(histories).findForUpdate(id);
        for (long id = 1; id <= 3; id++) sequence.verify(guitars).findForUpdate(id);
        for (long id = 101; id <= 103; id++) sequence.verify(works).findByProcessHistoryId(id);
        sequence.verify(guitars).saveAll(instruments);
        sequence.verify(histories).saveAll(targets);
        verify(histories, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unfinished", "empty", "null"})
    void lastInvalidWorkRejectsWholeBulkBeforeAnyMutation(String state) {
        work(0, "complete");
        // 中央はWorkなし。最後の検証失敗時も、先行する正常Historyを変更しない。
        work(2, state);
        assertThrows(BusinessException.class, () -> service.endProcesses(List.of(103L, 101L, 102L)));
        assertUnchanged();
        var sequence = inOrder(histories, works);
        for (long id = 101; id <= 103; id++) sequence.verify(histories).findForUpdate(id);
        for (long id = 101; id <= 103; id++) sequence.verify(works).findByProcessHistoryId(id);
        verifyNoInteractions(orders);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void completedWorkRetainsFinalCompletionAndOrderQuantities(boolean bulk) {
        when(processes.findByTargetTypeOrderByProcessOrderAsc("GUITAR")).thenReturn(List.of(process));
        int count = bulk ? 3 : 1;
        order.setPlannedQuantity(count);
        order.setStartedQuantity(count);
        for (int i = 0; i < count; i++) work(i, "complete");
        if (bulk) service.endProcesses(List.of(103L, 101L, 102L));
        else service.endProcess(101L);
        var time = targets.get(0).getEndTime();
        assertNotNull(time);
        assertEquals(count, order.getCompletedQuantity());
        assertEquals("COMPLETED", order.getStatus());
        assertEquals(time, order.getCompletedAt());
        assertEquals(time, order.getUpdatedAt());
        for (int i = 0; i < count; i++) {
            assertEquals("完成", instruments.get(i).getCurrentProcess());
            assertEquals(time, instruments.get(i).getCompletedAt());
            assertEquals(time, instruments.get(i).getUpdatedAt());
            assertEquals(time, targets.get(i).getEndTime());
        }
    }

    private void assertUnchanged() {
        for (var history : targets) assertNull(history.getEndTime());
        for (var guitar : instruments) {
            assertEquals("対象工程", guitar.getCurrentProcess());
            assertEquals(OLD, guitar.getUpdatedAt());
            assertNull(guitar.getCompletedAt());
        }
        assertEquals(0, order.getCompletedQuantity());
        assertEquals("IN_PROGRESS", order.getStatus());
        assertEquals(OLD, order.getUpdatedAt());
        assertNull(order.getCompletedAt());
        verify(histories, never()).save(any());
        verify(histories, never()).saveAll(any());
        verify(guitars, never()).save(any());
        verify(guitars, never()).saveAll(any());
        verify(orders, never()).save(any());
    }
}
