package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.example.guitarmes.process.common.GuitarProcessConstants;
import com.example.guitarmes.process.common.ProcessCodeConstants;
import com.example.guitarmes.process.common.ProcessTargetConstants;

class ManufacturingProcessTest {

    @Test
    void existingConstructorsLeaveProcessCodeUnset() {
        assertNull(new ManufacturingProcess().getProcessCode());
        ManufacturingProcess guitar = new ManufacturingProcess(
                GuitarProcessConstants.PARTS_INSTALLATION, 1);
        assertNull(guitar.getProcessCode());
        assertEquals(ProcessTargetConstants.GUITAR, guitar.getTargetType());
        assertEquals(GuitarProcessConstants.PARTS_INSTALLATION, guitar.getProcessName());
        assertEquals(1, guitar.getProcessOrder());
        ManufacturingProcess body = new ManufacturingProcess(ProcessTargetConstants.BODY, "Body fixture", 2);
        assertNull(body.getProcessCode());
        assertEquals(ProcessTargetConstants.BODY, body.getTargetType());
        assertEquals(2, body.getProcessOrder());
    }

    @Test
    void stableCodeIsIndependentOfDisplayNameAndCanRemainUnset() {
        ManufacturingProcess process = new ManufacturingProcess(
                GuitarProcessConstants.PARTS_INSTALLATION, 1);
        process.setProcessCode(ProcessCodeConstants.GUITAR_PARTS_INSTALLATION);
        process.setProcessName("表示名の変更");
        assertEquals("GUITAR_PARTS_INSTALLATION", process.getProcessCode());
        assertEquals("表示名の変更", process.getProcessName());
        assertEquals("ギターパーツ取付", GuitarProcessConstants.PARTS_INSTALLATION);
        process.setProcessCode(null);
        assertNull(process.getProcessCode());
    }
}
