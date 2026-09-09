package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.example.guitarmes.guitar.*;
import com.example.guitarmes.productionorder.ProductionOrderRepository;

class ProcessPageProgressTest {
    @Test void endedHistoryDoesNotCountAsRunning() {
        var histories = mock(ProcessHistoryRepository.class);
        var processes = mock(ManufacturingProcessRepository.class);
        var service = new ProcessService(histories, mock(GuitarRepository.class), processes, mock(ProductionOrderRepository.class));
        var process = new ManufacturingProcess("GUITAR", "first", 1); process.setId(1L);
        when(processes.findByTargetTypeOrderByProcessOrderAsc("GUITAR")).thenReturn(List.of(process));
        var done = new ProcessHistory(10L, 1L, "worker", LocalDateTime.now()); done.setEndTime(LocalDateTime.now());
        when(histories.findByGuitarId(10L)).thenReturn(List.of(done));
        assertFalse(service.hasRunningProcess(10L));
        verify(histories, never()).findByEndTimeIsNull();
    }
    @Test void pageAggregationPreservesProgressAndIgnoresLegacyHistory() {
        var histories = mock(ProcessHistoryRepository.class);
        var guitars = mock(GuitarRepository.class);
        var processes = mock(ManufacturingProcessRepository.class);
        var service = new ProcessService(histories, guitars, processes, mock(ProductionOrderRepository.class));
        var first = new ManufacturingProcess("GUITAR", "first", 1); first.setId(1L);
        var second = new ManufacturingProcess("GUITAR", "second", 2); second.setId(2L);
        when(processes.findByTargetTypeOrderByProcessOrderAsc("GUITAR")).thenReturn(List.of(first, second));
        var waiting = new Guitar("waiting", "first"); waiting.setId(10L);
        var working = new Guitar("working", "second"); working.setId(11L);
        var completed = new Guitar("done", "完成"); completed.setId(12L);
        var done = new ProcessHistory(11L, 1L, "worker", LocalDateTime.now()); done.setEndTime(LocalDateTime.now());
        var running = new ProcessHistory(11L, 2L, "worker", LocalDateTime.now());
        var legacy = new ProcessHistory(10L, 99L, "worker", LocalDateTime.now());
        when(histories.findByGuitarIdInOrderByIdAsc(List.of(10L, 11L, 12L))).thenReturn(List.of(done, running, legacy));
        var result = service.getPageProgress(List.of(waiting, working, completed));
        assertEquals(new ProcessService.PageProgress(0, false, true), result.get(10L));
        assertEquals(new ProcessService.PageProgress(75, true, false), result.get(11L));
        assertEquals(new ProcessService.PageProgress(0, false, false), result.get(12L));
        verify(histories, never()).findByEndTimeIsNull(); verifyNoInteractions(guitars);
        assertTrue(service.getPageProgress(List.of()).isEmpty());
        verify(histories, times(1)).findByGuitarIdInOrderByIdAsc(any());
    }
}
