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
    void eachConditionAndCombinedSearchExcludeNonMatchingBodies() {
        Body target = body("DB-ABC", "Strat", "塗装後検品", "WAITING_INSPECTION");
        Body otherModel = body("DB-ABD", "Tele", "塗装後検品", "WAITING_INSPECTION");
        Body otherProcess = body("DB-ABE", "Strat", "バフがけ", "WAITING");
        Body otherStatus = body("DB-ABF", "Strat", "塗装後検品", "WORKING");
        List<Body> all = List.of(target, otherModel, otherProcess, otherStatus);
        assertEquals(List.of(target), service.filterBodies(all, " abc ", null, null, null));
        assertEquals(List.of(target, otherProcess, otherStatus), service.filterBodies(all, null, "STR", null, null));
        assertEquals(List.of(otherProcess), service.filterBodies(all, null, null, "バフがけ", null));
        assertEquals(List.of(otherStatus), service.filterBodies(all, null, null, null, "working"));
        assertEquals(List.of(target), service.filterBodies(all, "db-ab", "str", "塗装後検品", "WAITING_INSPECTION"));
        assertTrue(service.filterBodies(all, null, null, "検品", null).isEmpty());
    }

    @Test
    void blankConditionsKeepAllAndNullFieldsAreSafe() {
        List<Body> all = List.of(new Body());
        assertEquals(all, service.filterBodies(all, null, " ", "", null));
        assertFalse(service.hasSearchCondition(null, " ", "", null));
        assertTrue(service.hasSearchCondition(null, null, null, "REWORK"));
        assertTrue(service.filterBodies(all, "missing", null, null, null).isEmpty());
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
