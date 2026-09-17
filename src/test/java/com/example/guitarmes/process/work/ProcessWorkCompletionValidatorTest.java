package com.example.guitarmes.process.work;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.ProcessHistory;

class ProcessWorkCompletionValidatorTest {
    ProcessWorkRepository works;
    ProcessWorkItemRepository items;
    ProcessWorkCompletionValidator validator;
    ProcessHistory history;
    @BeforeEach void setup() {
        works = mock(ProcessWorkRepository.class); items = mock(ProcessWorkItemRepository.class);
        validator = new ProcessWorkCompletionValidator(works, items);
        history = new ProcessHistory(); history.setId(3L);
    }
    @Test void historyWithoutWorkIsAllowed() {
        when(works.findByProcessHistoryId(3L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> validator.validateCompletable(history));
        verifyNoInteractions(items);
    }
    void workWith(List<ProcessWorkItem> list) {
        var work = ProcessWorkTestData.work(history); work.setId(4L);
        when(works.findByProcessHistoryId(3L)).thenReturn(Optional.of(work));
        when(items.findByProcessWorkIdOrderByItemOrderAsc(4L)).thenReturn(list);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void checksAllItemsWithoutMutating(int remaining) {
        var list = new ArrayList<ProcessWorkItem>();
        for (int i = 0; i < 3; i++) {
            var item = new ProcessWorkItem();
            if (i >= remaining) { item.setStatus(ProcessWorkItemStatus.COMPLETED); item.setCompletedAt(LocalDateTime.of(2020,1,1,0,0)); }
            list.add(item);
        }
        workWith(list);
        var states = list.stream().map(ProcessWorkItem::getStatus).toList();
        var times = list.stream().map(ProcessWorkItem::getCompletedAt).toList();
        if (remaining == 0) assertDoesNotThrow(() -> validator.validateCompletable(history));
        else assertTrue(assertThrows(BusinessException.class, () -> validator.validateCompletable(history))
                .getMessage().contains(remaining + "件"));
        assertEquals(states, list.stream().map(ProcessWorkItem::getStatus).toList());
        assertEquals(times, list.stream().map(ProcessWorkItem::getCompletedAt).toList());
        assertNull(history.getEndTime());
        verify(works).findByProcessHistoryId(3L);
        verify(items).findByProcessWorkIdOrderByItemOrderAsc(4L);
        verifyNoMoreInteractions(works, items);
    }
    @Test void emptyWorkIsInvalid() {
        workWith(List.of());
        assertTrue(assertThrows(BusinessException.class, () -> validator.validateCompletable(history))
                .getMessage().contains("作業項目が存在しない"));
    }
    @Test void nullStatusIsNotCompleted() {
        var item = new ProcessWorkItem(); item.setStatus(null); workWith(List.of(item));
        assertThrows(BusinessException.class, () -> validator.validateCompletable(history));
    }
}
