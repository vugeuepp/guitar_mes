package com.example.guitarmes.process.work;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.process.ProcessHistoryRepository;

class ProcessWorkItemServiceTest {
    ProcessWorkItemRepository items;
    ProcessHistoryRepository histories;
    EntityManager em;
    ProcessWorkItemService service;
    ProcessHistory history;
    ProcessWorkItem item;
    LocalDateTime old = LocalDateTime.of(2020, 1, 1, 0, 0);

    @BeforeEach void setup() {
        items = mock(ProcessWorkItemRepository.class);
        histories = mock(ProcessHistoryRepository.class);
        em = mock(EntityManager.class);
        service = new ProcessWorkItemService(items, histories);
        ReflectionTestUtils.setField(service, "entityManager", em);
        history = new ProcessHistory(); history.setId(3L);
        item = new ProcessWorkItem(); item.setId(1L);
        item.setProcessWork(ProcessWorkTestData.work(history));
        item.setUpdatedAt(old); item.rememberUpdatedAt();
        when(items.findHistoryIdByItemId(1L)).thenReturn(Optional.of(3L));
        when(histories.findForUpdate(3L)).thenReturn(Optional.of(history));
        when(items.findById(1L)).thenReturn(Optional.of(item));
        when(items.save(item)).thenReturn(item);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void changesStateWithOneEventTimeAfterHistoryLock(boolean complete) {
        if (!complete) { item.setStatus(ProcessWorkItemStatus.COMPLETED); item.setCompletedAt(old); }
        var before = LocalDateTime.now();
        assertSame(item, complete ? service.completeItem(1L) : service.uncompleteItem(1L));
        var after = LocalDateTime.now();
        assertFalse(item.getUpdatedAt().isBefore(before));
        assertFalse(item.getUpdatedAt().isAfter(after));
        assertEquals(complete ? ProcessWorkItemStatus.COMPLETED : ProcessWorkItemStatus.NOT_STARTED, item.getStatus());
        assertEquals(complete ? item.getUpdatedAt() : null, item.getCompletedAt());
        var event = item.getUpdatedAt(); item.preUpdate(); assertEquals(event, item.getUpdatedAt());
        var sequence = inOrder(items, histories, em);
        sequence.verify(items).findHistoryIdByItemId(1L);
        sequence.verify(histories).findForUpdate(3L);
        sequence.verify(em).refresh(history);
        sequence.verify(items).findById(1L);
        sequence.verify(em).refresh(item);
        sequence.verify(items).save(item);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void repeatedStateDoesNotSaveOrChangeTime(boolean complete) {
        if (complete) { item.setStatus(ProcessWorkItemStatus.COMPLETED); item.setCompletedAt(old); }
        if (complete) service.completeItem(1L); else service.uncompleteItem(1L);
        assertEquals(old, item.getUpdatedAt());
        assertEquals(complete ? old : null, item.getCompletedAt());
        verify(items, never()).save(any());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void historyEndedWhileWaitingRejectsBothOperations(boolean complete) {
        doAnswer(i -> { history.setEndTime(old); return null; }).when(em).refresh(history);
        var error = assertThrows(BusinessException.class,
                () -> { if (complete) service.completeItem(1L); else service.uncompleteItem(1L); });
        assertTrue(error.getMessage().contains("終了済み"));
        verify(items, never()).findById(any());
        verify(items, never()).save(any());
        assertEquals(ProcessWorkItemStatus.NOT_STARTED, item.getStatus());
        assertEquals(old, item.getUpdatedAt());
    }

    @Test void refreshesItemAfterLockBeforeIdempotencyCheck() {
        doAnswer(i -> {
            item.setStatus(ProcessWorkItemStatus.COMPLETED); item.setCompletedAt(old); return null;
        }).when(em).refresh(item);
        service.completeItem(1L);
        verify(items, never()).save(any());
        assertEquals(old, item.getCompletedAt());
    }

    @ParameterizedTest @ValueSource(strings = {"id", "history", "item"})
    void missingRelationsFailExplicitly(String missing) {
        if (missing.equals("id")) when(items.findHistoryIdByItemId(1L)).thenReturn(Optional.empty());
        if (missing.equals("history")) when(histories.findForUpdate(3L)).thenReturn(Optional.empty());
        if (missing.equals("item")) when(items.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.completeItem(1L));
        verify(items, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings = {"work", "history", "wrongParent", "nullStatus", "completedWithoutTime", "waitingWithTime"})
    void malformedDataIsNotSilentlyChanged(String kind) {
        switch (kind) {
            case "work" -> item.setProcessWork(null);
            case "history" -> item.getProcessWork().setProcessHistory(null);
            case "wrongParent" -> { var other = new ProcessHistory(); other.setId(99L); item.getProcessWork().setProcessHistory(other); }
            case "nullStatus" -> item.setStatus(null);
            case "completedWithoutTime" -> item.setStatus(ProcessWorkItemStatus.COMPLETED);
            default -> item.setCompletedAt(old);
        }
        assertThrows(BusinessException.class, () -> service.completeItem(1L));
        verify(items, never()).save(any());
    }
}
