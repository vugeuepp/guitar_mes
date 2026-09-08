package com.example.guitarmes.body;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.guitarmes.master.body.BodyMasterRepository;

class BodySearchTest {
    private final BodyService service = new BodyService(
            mock(BodyRepository.class), mock(BodyMasterRepository.class));

    @Test
    void categoryNormalizationDefaultsToActive() {
        assertEquals("active", service.normalizeCategory(null));
        assertEquals("active", service.normalizeCategory(""));
        assertEquals("active", service.normalizeCategory("  "));
        assertEquals("active", service.normalizeCategory("invalid"));
        assertEquals("attention", service.normalizeCategory(" ATTENTION "));
        assertEquals("passed", service.normalizeCategory("PASSED"));
    }

    @Test
    void bodiesAreClassifiedByStatus() {
        Body inspection = body("A", "Model", "検品", "WAITING_INSPECTION");
        Body waiting = body("B", "Model", "工程", "WAITING");
        Body working = body("C", "Model", "工程", "WORKING");
        Body rework = body("D", "Model", "バフ", "REWORK");
        Body returned = body("E", "Model", "戻り", "RETURNED");
        Body available = body("F", "Model", "組立待ち", "AVAILABLE");
        Body assembled = body("G", "Model", "組立済み", "ASSEMBLED");
        Body rejected = body("H", "Model", "製造終了", "REJECTED");
        Body unknown = body("I", "Model", "不明", "UNKNOWN");
        List<Body> all = List.of(inspection, waiting, working, rework,
                returned, available, assembled, rejected, unknown);

        assertEquals(List.of(inspection, waiting, working),
                service.filterByCategory(all, "active"));
        assertEquals(List.of(rework, returned),
                service.filterByCategory(all, "attention"));
        assertEquals(List.of(available, assembled, rejected),
                service.filterByCategory(all, "passed"));
        assertEquals(List.of(inspection, waiting, working),
                service.filterByCategory(all, "invalid"));
        assertTrue(service.filterByCategory(List.of(), "passed").isEmpty());
    }

    @Test
    void categoryAndExistingSearchAreCombined() {
        Body target = body("DB-ABC", "Strat", "塗装後検品", "WAITING_INSPECTION");
        Body activeOther = body("DB-ABD", "Tele", "塗装後検品", "WAITING_INSPECTION");
        Body attentionMatch = body("DB-ABC-R", "Strat", "塗装後検品", "REWORK");
        List<Body> active = service.filterByCategory(
                List.of(target, activeOther, attentionMatch), "active");

        assertEquals(List.of(target), service.filterBodies(active,
                " abc ", "STR", "塗装後検品", "waiting_inspection"));
        assertTrue(service.filterBodies(active,
                null, null, null, "REJECTED").isEmpty());
    }

    @Test
    void existingSearchStillNormalizesAndUsesAndConditions() {
        Body target = body("DB-ABC", "Strat", "塗装後検品", "WAITING_INSPECTION");
        Body otherModel = body("DB-ABD", "Tele", "塗装後検品", "WAITING_INSPECTION");
        Body otherProcess = body("DB-ABE", "Strat", "バフがけ", "WAITING");
        Body otherStatus = body("DB-ABF", "Strat", "塗装後検品", "WORKING");
        List<Body> all = List.of(target, otherModel, otherProcess, otherStatus);

        assertEquals(List.of(target), service.filterBodies(all,
                "db-ab", "str", "塗装後検品", "WAITING_INSPECTION"));
        assertEquals(all, service.filterBodies(all, null, " ", "", null));
        assertFalse(service.hasSearchCondition(null, " ", "", null));
        assertTrue(service.hasSearchCondition(null, null, null, "REWORK"));
    }

    private Body body(String serial, String model, String process, String status) {
        Body body = new Body();
        body.setSerialNo(serial);
        body.setModelName(model);
        body.setCurrentProcess(process);
        body.setStatus(status);
        return body;
    }
}
