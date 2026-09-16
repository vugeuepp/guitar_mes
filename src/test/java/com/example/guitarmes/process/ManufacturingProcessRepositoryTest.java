package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.guitarmes.process.common.ProcessCodeConstants;
import com.example.guitarmes.process.common.ProcessTargetConstants;

/** SQL適用済みの専用PostgreSQLでユーザーが実行。自前fixtureは各テストでrollback。 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("e2e")
class ManufacturingProcessRepositoryTest {

    @Autowired private ManufacturingProcessRepository repository;
    @Autowired private EntityManager em;

    @BeforeEach
    void guardDatabase() {
        assertEquals("guitar_mes_e2e",
                em.createNativeQuery("select current_database()", String.class).getSingleResult());
    }

    private ManufacturingProcess fixture(String target, String code) {
        ManufacturingProcess process = new ManufacturingProcess(target, "CODE-" + UUID.randomUUID(), 999);
        process.setProcessCode(code);
        return repository.saveAndFlush(process);
    }

    @Test
    void multipleUncodedProcessesRemainValid() {
        ManufacturingProcess first = fixture(ProcessTargetConstants.GUITAR, null);
        ManufacturingProcess second = fixture(ProcessTargetConstants.BODY, null);
        em.clear();
        assertNull(repository.findById(first.getId()).orElseThrow().getProcessCode());
        assertNull(repository.findById(second.getId()).orElseThrow().getProcessCode());
    }

    @Test
    void findsOwnCodeRegardlessOfDisplayNameAndReturnsEmptyForUnknownCode() {
        String code = "TEST_" + UUID.randomUUID();
        ManufacturingProcess process = fixture(ProcessTargetConstants.GUITAR, code);
        process.setProcessName("renamed-" + UUID.randomUUID());
        repository.flush();
        em.clear();
        ManufacturingProcess stored = repository.findByProcessCode(code).orElseThrow();
        assertEquals(process.getId(), stored.getId());
        assertEquals(process.getProcessName(), stored.getProcessName());
        assertEquals(code, stored.getProcessCode());
        assertTrue(repository.findByProcessCode("MISSING_" + UUID.randomUUID()).isEmpty());
    }

    @Test
    void uniqueCodeAppliesAcrossTargetTypes() {
        String code = "TEST_" + UUID.randomUUID();
        fixture(ProcessTargetConstants.GUITAR, code);
        assertThrows(DataIntegrityViolationException.class,
                () -> fixture(ProcessTargetConstants.BODY, code));
    }

    @Test
    void findsInitialCodeProvidedByMigration() {
        // migrationの対象行を読み取るだけ。既存行の書換え・削除はしない。
        ManufacturingProcess process = repository.findByProcessCode(
                ProcessCodeConstants.GUITAR_PARTS_INSTALLATION).orElseThrow();
        assertEquals(ProcessTargetConstants.GUITAR, process.getTargetType());
        assertEquals("ギターパーツ取付", process.getProcessName());
    }
}
