package com.example.guitarmes.process.partsinstallation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.process.work.*;

class PartsInstallationWorkItemApiServiceTest {
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void delegatesMutationAndReturnsPersistedProgress(boolean complete) {
        var operations = mock(ProcessWorkItemService.class);
        var items = mock(ProcessWorkItemRepository.class);
        var service = new PartsInstallationWorkItemApiService(operations, items);
        var history = new ProcessHistory(); history.setId(9L);
        var work = new ProcessWork(); work.setId(10L); work.setProcessHistory(history);
        var item = new ProcessWorkItem(); item.setId(11L); item.setProcessWork(work);
        item.setStatus(complete ? ProcessWorkItemStatus.COMPLETED : ProcessWorkItemStatus.NOT_STARTED);
        if (complete) item.setCompletedAt(LocalDateTime.of(2026, 9, 17, 12, 0));
        if (complete) when(operations.completeItem(11L)).thenReturn(item);
        else when(operations.uncompleteItem(11L)).thenReturn(item);
        when(items.findHistoryIdByItemId(11L)).thenReturn(Optional.of(9L));
        when(items.findByProcessWorkIdOrderByItemOrderAsc(10L)).thenReturn(List.of(item, new ProcessWorkItem()));
        var response = complete ? service.complete(11L) : service.uncomplete(11L);
        assertEquals(complete, response.checked());
        assertEquals(complete ? 1 : 0, response.completedCount());
        assertEquals(2, response.totalCount());
        assertFalse(response.readOnly());
        assertEquals(complete ? item.getStatus().name() : "NOT_STARTED", response.status());
        if (complete) verify(operations).completeItem(11L);
        else verify(operations).uncompleteItem(11L);
        verify(items, never()).save(any());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void endedHistoryRejectionPropagatesWithoutReadingProgress(boolean complete) {
        var operations = mock(ProcessWorkItemService.class);
        var items = mock(ProcessWorkItemRepository.class);
        var service = new PartsInstallationWorkItemApiService(operations, items);
        var error = new BusinessException("終了済み");
        if (complete) when(operations.completeItem(11L)).thenThrow(error);
        else when(operations.uncompleteItem(11L)).thenThrow(error);
        assertSame(error, assertThrows(BusinessException.class, () -> {
            if (complete) service.complete(11L); else service.uncomplete(11L);
        }));
        verifyNoInteractions(items);
    }
}
