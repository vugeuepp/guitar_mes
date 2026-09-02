package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.process.common.GuitarProcessConstants;
import com.example.guitarmes.process.common.ProcessTargetConstants;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

    @Mock
    private ProcessHistoryRepository historyRepository;

    @Mock
    private GuitarRepository guitarRepository;

    @Mock
    private ManufacturingProcessRepository processRepository;

    @Mock
    private ProductionOrderRepository orderRepository;

    private ProcessService service;

    @BeforeEach
    void setUp() {
        service = new ProcessService(
                historyRepository,
                guitarRepository,
                processRepository,
                orderRepository);
    }

    @Test
    void bulkStart_empty_rejected() {
        assertThrows(
                BusinessException.class,
                () -> service.startProcesses(List.of(), 1L, "Worker"));

        verifyNoInteractions(guitarRepository);
        verifyNoInteractions(historyRepository);
    }

    @Test
    void bulkStart_blankWorker_rejected() {
        assertThrows(
                BusinessException.class,
                () -> service.startProcesses(List.of(10L), 1L, "  "));

        verifyNoInteractions(guitarRepository);
        verifyNoInteractions(historyRepository);
        verifyNoInteractions(processRepository);
    }

    @Test
    void bulkStart_duplicateIds_areRemovedAndSavedOnce() {
        ManufacturingProcess process = guitarProcess(1L, "ギターパーツ取付", 1);
        Guitar guitar = currentFlowGuitar(10L, "ギターパーツ取付");

        prepareStartableGuitar(guitar, process);
        when(guitarRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProcessHistory> result = service.startProcesses(
                List.of(10L, 10L),
                1L,
                " Worker ");

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getGuitarId());
        assertEquals(1L, result.get(0).getProcessId());
        assertEquals("Worker", result.get(0).getWorkerName());

        ArgumentCaptor<List<Guitar>> guitarCaptor = ArgumentCaptor.forClass(List.class);
        verify(guitarRepository).saveAll(guitarCaptor.capture());
        assertEquals(1, guitarCaptor.getValue().size());

        ArgumentCaptor<List<ProcessHistory>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(historyRepository).saveAll(historyCaptor.capture());
        assertEquals(1, historyCaptor.getValue().size());
    }

    @Test
    void bulkStart_multipleGuitars_success() {
        ManufacturingProcess process = guitarProcess(1L, "ギターパーツ取付", 1);
        Guitar first = currentFlowGuitar(10L, "ギターパーツ取付");
        Guitar second = currentFlowGuitar(11L, "ギターパーツ取付");

        prepareStartableGuitar(first, process);
        prepareStartableGuitar(second, process);
        when(guitarRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProcessHistory> result = service.startProcesses(
                List.of(10L, 11L),
                1L,
                "Bulk Worker");

        assertEquals(2, result.size());
        assertEquals("ギターパーツ取付", first.getCurrentProcess());
        assertEquals("ギターパーツ取付", second.getCurrentProcess());
        verify(guitarRepository).saveAll(List.of(first, second));
        verify(historyRepository).saveAll(any());
    }

    @Test
    void bulkStart_invalidMember_savesNothing() {
        ManufacturingProcess process = guitarProcess(1L, "ギターパーツ取付", 1);
        Guitar valid = currentFlowGuitar(10L, "ギターパーツ取付");
        Guitar invalid = new Guitar();
        invalid.setId(11L);

        when(processRepository.findById(1L)).thenReturn(Optional.of(process));
        when(guitarRepository.findById(10L)).thenReturn(Optional.of(valid));
        when(guitarRepository.findById(11L)).thenReturn(Optional.of(invalid));
        when(processRepository.findByTargetTypeOrderByProcessOrderAsc(
                ProcessTargetConstants.GUITAR)).thenReturn(List.of(process));
        when(historyRepository.findByEndTimeIsNull()).thenReturn(List.of());
        when(historyRepository.findByGuitarId(10L)).thenReturn(List.of());

        assertThrows(
                BusinessException.class,
                () -> service.startProcesses(List.of(10L, 11L), 1L, "Worker"));

        verify(guitarRepository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void bulkStart_completedGuitar_savesNothing() {
        ManufacturingProcess process = guitarProcess(1L, "ギターパーツ取付", 1);
        Guitar guitar = currentFlowGuitar(10L, GuitarProcessConstants.COMPLETED);

        when(processRepository.findById(1L)).thenReturn(Optional.of(process));
        when(guitarRepository.findById(10L)).thenReturn(Optional.of(guitar));

        assertThrows(
                BusinessException.class,
                () -> service.startProcesses(List.of(10L), 1L, "Worker"));

        verify(guitarRepository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void bulkStart_runningGuitar_savesNothing() {
        ManufacturingProcess process = guitarProcess(1L, "ギターパーツ取付", 1);
        Guitar guitar = currentFlowGuitar(10L, "ギターパーツ取付");
        ProcessHistory running = history(100L, 10L, 1L, null);

        when(processRepository.findById(1L)).thenReturn(Optional.of(process));
        when(guitarRepository.findById(10L)).thenReturn(Optional.of(guitar));
        when(processRepository.findByTargetTypeOrderByProcessOrderAsc(
                ProcessTargetConstants.GUITAR)).thenReturn(List.of(process));
        when(historyRepository.findByEndTimeIsNull()).thenReturn(List.of(running));

        assertThrows(
                BusinessException.class,
                () -> service.startProcesses(List.of(10L), 1L, "Worker"));

        verify(guitarRepository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void bulkStart_nextProcessMismatch_savesNothing() {
        ManufacturingProcess first = guitarProcess(1L, "ギターパーツ取付", 1);
        ManufacturingProcess second = guitarProcess(2L, "調整・調音", 2);
        Guitar guitar = currentFlowGuitar(10L, "ギターパーツ取付");

        when(processRepository.findById(2L)).thenReturn(Optional.of(second));
        when(guitarRepository.findById(10L)).thenReturn(Optional.of(guitar));
        when(processRepository.findByTargetTypeOrderByProcessOrderAsc(
                ProcessTargetConstants.GUITAR)).thenReturn(List.of(first, second));
        when(historyRepository.findByEndTimeIsNull()).thenReturn(List.of());
        when(historyRepository.findByGuitarId(10L)).thenReturn(List.of());

        assertThrows(
                BusinessException.class,
                () -> service.startProcesses(List.of(10L), 2L, "Worker"));

        verify(guitarRepository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void bulkEnd_empty_rejected() {
        assertThrows(
                BusinessException.class,
                () -> service.endProcesses(List.of()));

        verifyNoInteractions(historyRepository);
    }

    @Test
    void bulkEnd_multipleHistories_success() {
        ManufacturingProcess first = guitarProcess(1L, "ギターパーツ取付", 1);
        ManufacturingProcess second = guitarProcess(2L, "調整・調音", 2);
        Guitar guitar1 = currentFlowGuitar(10L, "ギターパーツ取付");
        Guitar guitar2 = currentFlowGuitar(11L, "ギターパーツ取付");
        ProcessHistory history1 = history(100L, 10L, 1L, null);
        ProcessHistory history2 = history(101L, 11L, 1L, null);

        when(historyRepository.findById(100L)).thenReturn(Optional.of(history1));
        when(historyRepository.findById(101L)).thenReturn(Optional.of(history2));
        when(processRepository.findById(1L)).thenReturn(Optional.of(first));
        when(guitarRepository.findById(10L)).thenReturn(Optional.of(guitar1));
        when(guitarRepository.findById(11L)).thenReturn(Optional.of(guitar2));
        when(processRepository.findByTargetTypeOrderByProcessOrderAsc(
                ProcessTargetConstants.GUITAR)).thenReturn(List.of(first, second));
        when(guitarRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProcessHistory> result = service.endProcesses(List.of(100L, 101L));

        assertEquals(2, result.size());
        assertEquals("調整・調音", guitar1.getCurrentProcess());
        assertEquals("調整・調音", guitar2.getCurrentProcess());
        assertEquals(true, history1.getEndTime() != null);
        assertEquals(true, history2.getEndTime() != null);
        verify(guitarRepository).saveAll(List.of(guitar1, guitar2));
        verify(historyRepository).saveAll(List.of(history1, history2));
    }

    @Test
    void bulkEnd_mixedProcesses_savesNothing() {
        ManufacturingProcess first = guitarProcess(1L, "ギターパーツ取付", 1);
        ManufacturingProcess second = guitarProcess(2L, "調整・調音", 2);
        ProcessHistory history1 = history(100L, 10L, 1L, null);
        ProcessHistory history2 = history(101L, 11L, 2L, null);

        when(historyRepository.findById(100L)).thenReturn(Optional.of(history1));
        when(historyRepository.findById(101L)).thenReturn(Optional.of(history2));
        when(processRepository.findById(1L)).thenReturn(Optional.of(first));
        when(processRepository.findById(2L)).thenReturn(Optional.of(second));
        when(guitarRepository.findById(10L))
                .thenReturn(Optional.of(currentFlowGuitar(10L, first.getProcessName())));
        when(guitarRepository.findById(11L))
                .thenReturn(Optional.of(currentFlowGuitar(11L, second.getProcessName())));

        assertThrows(
                BusinessException.class,
                () -> service.endProcesses(List.of(100L, 101L)));

        verify(guitarRepository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    @Test
    void bulkEnd_completedHistory_savesNothing() {
        ProcessHistory completed = history(
                100L,
                10L,
                1L,
                LocalDateTime.now());

        when(historyRepository.findById(100L)).thenReturn(Optional.of(completed));

        assertThrows(
                BusinessException.class,
                () -> service.endProcesses(List.of(100L)));

        verify(guitarRepository, never()).saveAll(any());
        verify(historyRepository, never()).saveAll(any());
    }

    private void prepareStartableGuitar(
            Guitar guitar,
            ManufacturingProcess process) {
        when(processRepository.findById(process.getId()))
                .thenReturn(Optional.of(process));
        when(guitarRepository.findById(guitar.getId()))
                .thenReturn(Optional.of(guitar));
        when(processRepository.findByTargetTypeOrderByProcessOrderAsc(
                ProcessTargetConstants.GUITAR)).thenReturn(List.of(process));
        when(historyRepository.findByEndTimeIsNull()).thenReturn(List.of());
        when(historyRepository.findByGuitarId(guitar.getId())).thenReturn(List.of());
    }

    private ManufacturingProcess guitarProcess(
            Long id,
            String name,
            int order) {
        ManufacturingProcess process = new ManufacturingProcess(
                ProcessTargetConstants.GUITAR,
                name,
                order);
        process.setId(id);
        return process;
    }

    private Guitar currentFlowGuitar(
            Long id,
            String currentProcess) {
        ProductionOrder order = new ProductionOrder();
        order.setId(1000L + id);
        order.setPlannedQuantity(1);
        order.setStartedQuantity(1);
        order.setCompletedQuantity(0);

        Guitar guitar = new Guitar();
        guitar.setId(id);
        guitar.setProductionOrder(order);
        guitar.setCurrentProcess(currentProcess);
        return guitar;
    }

    private ProcessHistory history(
            Long id,
            Long guitarId,
            Long processId,
            LocalDateTime endTime) {
        ProcessHistory history = new ProcessHistory(
                guitarId,
                processId,
                "Worker",
                LocalDateTime.now().minusMinutes(5));
        history.setId(id);
        history.setEndTime(endTime);
        return history;
    }
}
