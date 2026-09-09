package com.example.guitarmes.body.process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.example.guitarmes.body.Body;
import com.example.guitarmes.body.BodyRepository;
import com.example.guitarmes.process.ManufacturingProcess;
import com.example.guitarmes.process.ManufacturingProcessRepository;

class BodyProcessTimestampTest {
    private final BodyRepository entities = mock(BodyRepository.class);
    private final BodyProcessHistoryRepository histories = mock(BodyProcessHistoryRepository.class);
    private final ManufacturingProcessRepository processes = mock(ManufacturingProcessRepository.class);
    private final BodyProcessService service = new BodyProcessService(histories, entities, processes);
    private final LocalDateTime old = LocalDateTime.of(2020, 1, 1, 0, 0);

    private Body entity(long id) {
        Body item = new Body(); item.setId(id); item.setStatus("WAITING");
        item.setCurrentProcess("パーツ付け"); item.setUpdatedAt(old); item.rememberUpdatedAt();
        when(entities.findById(id)).thenReturn(Optional.of(item));
        return item;
    }
    private void process() {
        ManufacturingProcess p = new ManufacturingProcess("BODY", "パーツ付け", 3); p.setId(10L);
        when(processes.findById(10L)).thenReturn(Optional.of(p));
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void startUsesHistoryEventTime(boolean bulk) {
        process(); Body a = entity(1L);
        if (bulk) {
            Body b = entity(2L);
            when(histories.saveAll(any())).thenAnswer(i -> i.getArgument(0));
            var result = service.startProcesses(List.of(1L, 2L), 10L, "Worker");
            assertEquals(result.get(0).getStartTime(), b.getUpdatedAt());
            assertEquals(result.get(0).getStartTime(), result.get(1).getStartTime());
            assertEquals(result.get(0).getStartTime(), a.getUpdatedAt());
        } else {
            when(histories.save(any())).thenAnswer(i -> i.getArgument(0));
            var result = service.startProcess(1L, 10L, "Worker");
            assertEquals(result.getStartTime(), a.getUpdatedAt());
        }
        assertTrue(a.getUpdatedAt().isAfter(old));
        LocalDateTime event = a.getUpdatedAt(); a.preUpdate();
        assertEquals(event, a.getUpdatedAt());
        assertNull(a.getAvailableAt());
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void finalEndUsesSameTimeForHistoryAndAvailability(boolean bulk) {
        process(); Body a = entity(1L); a.setStatus("WORKING");
        BodyProcessHistory h = new BodyProcessHistory(1L, 10L, "Worker", old);
        when(histories.findById(100L)).thenReturn(Optional.of(h));
        if (bulk) {
            Body b = entity(2L); b.setStatus("WORKING");
            BodyProcessHistory h2 = new BodyProcessHistory(2L, 10L, "Worker", old);
            when(histories.findById(101L)).thenReturn(Optional.of(h2));
            service.endProcesses(List.of(100L, 101L), "COMPLETED", "");
            assertEquals(h.getEndTime(), h2.getEndTime());
            assertEquals(h.getEndTime(), b.getAvailableAt());
            assertEquals(h.getEndTime(), b.getUpdatedAt());
        } else {
            service.endProcess(100L, "COMPLETED", "");
        }
        assertEquals("AVAILABLE", a.getStatus());
        assertNotNull(h.getEndTime());
        assertEquals(h.getEndTime(), a.getAvailableAt());
        assertEquals(h.getEndTime(), a.getUpdatedAt());
        a.preUpdate(); assertEquals(h.getEndTime(), a.getUpdatedAt());
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void intermediateEndUpdatesActivityWithoutAvailability(boolean bulk) {
        Body a = entity(1L); a.setStatus("WORKING"); a.setCurrentProcess("バフがけ");
        ManufacturingProcess p = new ManufacturingProcess("BODY", "バフがけ", 1); p.setId(10L);
        when(processes.findById(10L)).thenReturn(Optional.of(p));
        BodyProcessHistory h = new BodyProcessHistory(1L, 10L, "Worker", old);
        when(histories.findById(100L)).thenReturn(Optional.of(h));
        if (bulk) service.endProcesses(List.of(100L), "COMPLETED", "");
        else service.endProcess(100L, "COMPLETED", "");
        assertNotNull(h.getEndTime());
        assertEquals(h.getEndTime(), a.getUpdatedAt());
        assertNull(a.getAvailableAt());
        assertNotEquals("AVAILABLE", a.getStatus());
    }

}
