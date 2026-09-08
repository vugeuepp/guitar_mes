package com.example.guitarmes.neck;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.guitarmes.master.neck.NeckMasterRepository;

class NeckSearchTest {
    private final NeckService service = new NeckService(
            mock(NeckRepository.class), mock(NeckMasterRepository.class));

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
    void necksAreClassifiedByStatus() {
        Neck inspection = neck("A", "Model", "検品", "WAITING_INSPECTION");
        Neck waiting = neck("B", "Model", "工程", "WAITING");
        Neck working = neck("C", "Model", "工程", "WORKING");
        Neck rework = neck("D", "Model", "バフ", "REWORK");
        Neck returned = neck("E", "Model", "戻り", "RETURNED");
        Neck available = neck("F", "Model", "組立待ち", "AVAILABLE");
        Neck assembled = neck("G", "Model", "組立済み", "ASSEMBLED");
        Neck rejected = neck("H", "Model", "製造終了", "REJECTED");
        Neck unknown = neck("I", "Model", "不明", "UNKNOWN");
        List<Neck> all = List.of(inspection, waiting, working, rework,
                returned, available, assembled, rejected, unknown, neck("NULL", "Model", "工程", null));

        assertEquals(List.of(waiting, working),
                service.filterByCategory(all, "active"));
        assertEquals(List.of(returned),
                service.filterByCategory(all, "attention"));
        assertEquals(List.of(available, assembled, rejected),
                service.filterByCategory(all, "passed"));
        assertEquals(List.of(waiting, working),
                service.filterByCategory(all, "invalid"));
        assertTrue(service.filterByCategory(List.of(), "passed").isEmpty());
    }

    @Test
    void categoryAndExistingSearchAreCombined() {
        Neck target = neck("DB-ABC", "Strat", "PLEK", "WAITING");
        Neck activeOther = neck("DB-ABD", "Tele", "PLEK", "WAITING");
        Neck attentionMatch = neck("DB-ABC-R", "Strat", "PLEK", "RETURNED");
        List<Neck> active = service.filterByCategory(
                List.of(target, activeOther, attentionMatch), "active");

        assertEquals(List.of(target), service.filterNecks(active,
                " abc ", "STR", "PLEK", "waiting"));
        assertTrue(service.filterNecks(active,
                null, null, null, "REJECTED").isEmpty());
    }

    @Test
    void existingSearchStillNormalizesAndUsesAndConditions() {
        Neck target = neck("DB-ABC", "Strat", "PLEK", "WAITING");
        Neck otherModel = neck("DB-ABD", "Tele", "PLEK", "WAITING");
        Neck otherProcess = neck("DB-ABE", "Strat", "ネックパーツ付け", "WAITING");
        Neck otherStatus = neck("DB-ABF", "Strat", "PLEK", "WORKING");
        List<Neck> all = List.of(target, otherModel, otherProcess, otherStatus);

        assertEquals(List.of(target), service.filterNecks(all,
                "db-ab", "str", "PLEK", "WAITING"));
        assertEquals(all, service.filterNecks(all, null, " ", "", null));
        assertFalse(service.hasSearchCondition(null, " ", "", null));
        assertTrue(service.hasSearchCondition(null, null, null, "RETURNED"));
    }

    private Neck neck(String serial, String model, String process, String status) {
        Neck neck = new Neck();
        neck.setSerialNo(serial);
        neck.setModelName(model);
        neck.setCurrentProcess(process);
        neck.setStatus(status);
        return neck;
    }
}
