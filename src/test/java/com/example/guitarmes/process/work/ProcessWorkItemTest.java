package com.example.guitarmes.process.work;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ProcessWorkItemTest {

    @Test
    void holdsParentKeyOrderAndStartsUnchecked() {
        ProcessWork work = new ProcessWork();
        ProcessWorkItem item = new ProcessWorkItem();
        item.setProcessWork(work);
        item.setItemKey(ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK);
        item.setItemOrder(1);
        assertSame(work, item.getProcessWork());
        assertEquals(ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, item.getItemKey());
        assertEquals(1, item.getItemOrder());
        assertEquals(ProcessWorkItemStatus.NOT_STARTED, item.getStatus());
        assertNull(item.getCompletedAt());
    }

    @Test
    void initializesBothTimestampsToSameTime() {
        ProcessWorkItem item = new ProcessWorkItem();
        LocalDateTime before = LocalDateTime.now();
        item.prePersist();
        assertFalse(item.getCreatedAt().isBefore(before));
        assertFalse(item.getCreatedAt().isAfter(LocalDateTime.now()));
        assertEquals(item.getCreatedAt(), item.getUpdatedAt());
        assertNull(item.getCompletedAt());
    }

    @Test
    void preservesExplicitCreationAndEventTimes() {
        ProcessWorkItem item = new ProcessWorkItem();
        LocalDateTime creation = LocalDateTime.of(2026, 1, 2, 3, 4);
        LocalDateTime event = creation.plusMinutes(1);
        item.setCreatedAt(creation);
        item.setUpdatedAt(event);
        item.prePersist();
        assertEquals(creation, item.getCreatedAt());
        assertEquals(event, item.getUpdatedAt());
    }

    @Test
    void ordinaryUpdatesAdvanceTimeWithoutChangingCreation() {
        ProcessWorkItem item = new ProcessWorkItem();
        LocalDateTime old = LocalDateTime.of(2020, 1, 1, 0, 0);
        item.setCreatedAt(old);
        item.setUpdatedAt(old);
        item.rememberUpdatedAt();
        item.preUpdate();
        assertTrue(item.getUpdatedAt().isAfter(old));
        assertEquals(old, item.getCreatedAt());
    }

    @Test
    void completionEventTimeSurvivesCallbackAndCanBeCleared() {
        ProcessWorkItem item = new ProcessWorkItem();
        item.prePersist();
        item.rememberUpdatedAt();
        LocalDateTime event = LocalDateTime.of(2026, 1, 2, 3, 4);
        item.setStatus(ProcessWorkItemStatus.COMPLETED);
        item.setCompletedAt(event);
        item.setUpdatedAt(event);
        item.preUpdate();
        assertEquals(event, item.getCompletedAt());
        assertEquals(event, item.getUpdatedAt());
        item.rememberUpdatedAt();
        item.setStatus(ProcessWorkItemStatus.NOT_STARTED);
        item.setCompletedAt(null);
        item.preUpdate();
        assertNull(item.getCompletedAt());
        assertTrue(item.getUpdatedAt().isAfter(event));
    }

    @Test
    void itemKeysAreExactlyTheApprovedPersistentCodes() {
        Set<String> expected = Set.of(
                "BRIDGE_SIX_POINT_INSTALL",
                "BRIDGE_MOVEMENT_CHECK",
                "STUD_HOLE_EXPANSION",
                "STUD_INSTALL",
                "BRIDGE_TWO_POINT_INSTALL",
                "SPRING_HANGER_INSTALL",
                "PICKGUARD_INSTALL",
                "JACK_PLATE_INSTALL",
                "JACK_WIRING",
                "GROUND_WIRING",
                "ELECTRONICS_SOUND_CHECK",
                "ELECTRONICS_PARTS_CHECK",
                "ELECTRONICS_FINAL_FASTENING",
                "TUNER_BUSHING_INSTALL",
                "TUNER_INSTALL",
                "STRING_INSTALL");
        assertEquals(expected, Arrays.stream(ProcessWorkItemKey.values())
                .map(Enum::name).collect(Collectors.toSet()));
        assertTrue(expected.stream().allMatch(code -> code.length() <= 64));
    }

    @Test
    void onlyTwoStatusesAreDefined() {
        assertArrayEquals(new ProcessWorkItemStatus[]{ProcessWorkItemStatus.NOT_STARTED,
                ProcessWorkItemStatus.COMPLETED}, ProcessWorkItemStatus.values());
    }
}
