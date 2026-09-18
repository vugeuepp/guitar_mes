package com.example.guitarmes.process.work;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.product.parts.BridgeType;
import com.example.guitarmes.product.parts.TunerMountingType;
import com.example.guitarmes.product.parts.TunerLayout;
import com.example.guitarmes.product.parts.JackMountingType;

class ProcessWorkTest {

    @Test
    void holdsHistoryAndAllFourteenSnapshotValues() {
        ProcessHistory history = new ProcessHistory();
        ProcessWork work = ProcessWorkTestData.work(history);
        assertSame(history, work.getProcessHistory());
        assertEquals(BridgeType.TWO_POINT, work.getBridgeType());
        assertEquals("Bridge fixture", work.getBridgeModel());
        assertEquals(false, work.getRequiresStudHoleExpansion());
        assertEquals("Tuner fixture", work.getTunerModel());
        assertEquals(TunerMountingType.PRESS_BUSHING, work.getTunerMountingType());
        assertEquals(true, work.getTunerBushRequired());
        assertEquals(TunerLayout.SIX_IN_LINE, work.getTunerLayout());
        assertEquals("SSS", work.getPickupLayout());
        assertEquals(5, work.getSelectorPositions());
        assertEquals("1Vol. 2Tone", work.getControlLayout());
        assertEquals(JackMountingType.BOAT_PLATE, work.getJackMountingType());
        assertEquals("String maker", work.getStringMaker());
        assertEquals("String fixture", work.getStringModel());
        assertEquals("09-42", work.getStringGauge());
    }

    @Test
    void creationTimeIsInitializedButExplicitTimeIsPreserved() {
        ProcessWork work = new ProcessWork();
        LocalDateTime before = LocalDateTime.now();
        work.prePersist();
        assertNotNull(work.getCreatedAt());
        assertFalse(work.getCreatedAt().isBefore(before));
        assertFalse(work.getCreatedAt().isAfter(LocalDateTime.now()));
        LocalDateTime event = LocalDateTime.of(2026, 1, 2, 3, 4);
        work.setCreatedAt(event);
        work.prePersist();
        assertEquals(event, work.getCreatedAt());
    }

    @Test
    void optionalSpecificationsCanRemainNull() {
        ProcessWork work = ProcessWorkTestData.work(new ProcessHistory());
        work.setBridgeModel(null);
        work.setStringMaker(null);
        assertNull(work.getBridgeModel());
        assertNull(work.getStringMaker());
        assertEquals("SSS", work.getPickupLayout());
    }
}
