package com.example.guitarmes.process.work;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.process.ManufacturingProcess;
import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.product.Product;

/** SQL適用済み専用DB用。関連する親も自前作成し、各テストのtransactionでrollback。 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("e2e")
class ProcessWorkRepositoryTest {

    @Autowired private ProcessWorkRepository works;
    @Autowired private ProcessWorkItemRepository items;
    @Autowired private EntityManager em;

    @BeforeEach
    void guardDatabase() {
        assertEquals("guitar_mes_e2e",
                em.createNativeQuery("select current_database()", String.class).getSingleResult());
    }

    private ProcessHistory history() {
        String token = "WORK-" + UUID.randomUUID();
        Product product = new Product();
        product.setModelNo(token);
        product.setProductName(token);
        em.persist(product);
        ManufacturingProcess process = new ManufacturingProcess(token, 999);
        em.persist(process);
        Guitar guitar = new Guitar(token, token);
        guitar.setProduct(product);
        em.persist(guitar);
        ProcessHistory history = new ProcessHistory(guitar.getId(), process.getId(), token, LocalDateTime.now());
        em.persist(history);
        return history;
    }

    private ProcessWork work() {
        return works.saveAndFlush(ProcessWorkTestData.work(history()));
    }

    private ProcessWorkItem item(ProcessWork work, ProcessWorkItemKey key, int order) {
        ProcessWorkItem item = new ProcessWorkItem();
        item.setProcessWork(work);
        item.setItemKey(key);
        item.setItemOrder(order);
        return item;
    }

    private void assertConstraint(String expected, Runnable operation) {
        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class, operation::run);
        Throwable cause = exception;
        while (!(cause instanceof ConstraintViolationException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertInstanceOf(ConstraintViolationException.class, cause);
        assertEquals(expected, ((ConstraintViolationException) cause).getConstraintName());
    }

    @Test
    void reloadsAllSnapshotValuesByHistoryWithoutMasterReference() {
        ProcessWork original = work();
        Long historyId = original.getProcessHistory().getId();
        em.clear();
        ProcessWork stored = works.findByProcessHistoryId(historyId).orElseThrow();
        assertEquals(original.getId(), stored.getId());
        assertEquals(historyId, stored.getProcessHistory().getId());
        assertEquals(original.getBridgeType(), stored.getBridgeType());
        assertEquals(original.getBridgeModel(), stored.getBridgeModel());
        assertEquals(original.getRequiresStudHoleExpansion(), stored.getRequiresStudHoleExpansion());
        assertEquals(original.getTunerModel(), stored.getTunerModel());
        assertEquals(original.getTunerMountingType(), stored.getTunerMountingType());
        assertEquals(original.getTunerBushRequired(), stored.getTunerBushRequired());
        assertEquals(original.getTunerLayout(), stored.getTunerLayout());
        assertEquals(original.getPickupLayout(), stored.getPickupLayout());
        assertEquals(original.getSelectorPositions(), stored.getSelectorPositions());
        assertEquals(original.getControlLayout(), stored.getControlLayout());
        assertEquals(original.getJackMountingType(), stored.getJackMountingType());
        assertEquals(original.getStringMaker(), stored.getStringMaker());
        assertEquals(original.getStringModel(), stored.getStringModel());
        assertEquals(original.getStringGauge(), stored.getStringGauge());
        assertNotNull(stored.getCreatedAt());
        assertTrue(works.findByProcessHistoryId(history().getId()).isEmpty());
    }

    @Test
    void rejectsSecondWorkForSameHistory() {
        ProcessWork first = work();
        assertConstraint("uk_process_work_process_history", () ->
                works.saveAndFlush(ProcessWorkTestData.work(first.getProcessHistory())));
    }

    @Test
    void optionalSnapshotValuesRemainNull() {
        ProcessWork work = ProcessWorkTestData.work(history());
        work.setBridgeModel(null);
        work.setStringMaker(null);
        works.saveAndFlush(work);
        em.clear();
        ProcessWork stored = works.findById(work.getId()).orElseThrow();
        assertNull(stored.getBridgeModel());
        assertNull(stored.getStringMaker());
    }

    @Test
    void fetchesOnlyOwnItemsInOrderAndPersistsBothStates() {
        ProcessWork work = work();
        ProcessWorkItem second = item(work, ProcessWorkItemKey.STRING_INSTALL, 2);
        LocalDateTime event = LocalDateTime.of(2026, 9, 16, 12, 0);
        second.setStatus(ProcessWorkItemStatus.COMPLETED);
        second.setCompletedAt(event);
        second.setUpdatedAt(event);
        items.saveAndFlush(second);
        ProcessWorkItem first = items.saveAndFlush(item(work, ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 1));
        items.saveAndFlush(item(work(), ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 1));
        em.clear();
        List<ProcessWorkItem> stored = items.findByProcessWorkIdOrderByItemOrderAsc(work.getId());
        assertEquals(List.of(first.getId(), second.getId()), stored.stream().map(ProcessWorkItem::getId).toList());
        assertEquals(ProcessWorkItemStatus.NOT_STARTED, stored.get(0).getStatus());
        assertNull(stored.get(0).getCompletedAt());
        assertNotNull(stored.get(0).getCreatedAt());
        assertEquals(stored.get(0).getCreatedAt(), stored.get(0).getUpdatedAt());
        assertEquals(ProcessWorkItemStatus.COMPLETED, stored.get(1).getStatus());
        assertEquals(event, stored.get(1).getCompletedAt());
        assertEquals(event, stored.get(1).getUpdatedAt());
    }

    @Test
    void rejectsDuplicateKeyWithinWork() {
        ProcessWork work = work();
        items.saveAndFlush(item(work, ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 1));
        assertConstraint("uk_process_work_item_work_key", () ->
                items.saveAndFlush(item(work, ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 2)));
    }

    @Test
    void rejectsDuplicateOrderWithinWork() {
        ProcessWork work = work();
        items.saveAndFlush(item(work, ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 1));
        assertConstraint("uk_process_work_item_work_order", () ->
                items.saveAndFlush(item(work, ProcessWorkItemKey.STRING_INSTALL, 1)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsStatusAndCompletedAtContradiction(boolean completed) {
        ProcessWorkItem item = item(work(), ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 1);
        item.setStatus(completed ? ProcessWorkItemStatus.COMPLETED : ProcessWorkItemStatus.NOT_STARTED);
        item.setCompletedAt(completed ? null : LocalDateTime.now());
        assertConstraint("ck_process_work_item_status_completed_at", () -> items.saveAndFlush(item));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveOrder(int order) {
        ProcessWorkItem item = item(work(), ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, order);
        assertConstraint("ck_process_work_item_order", () -> items.saveAndFlush(item));
    }

    @Test
    void parentDeletesDoNotCascadeIntoWorkOrItems() {
        ProcessWork work = work();
        items.saveAndFlush(item(work, ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 1));
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () ->
                em.createNativeQuery("delete from t_process_work where id=:id")
                        .setParameter("id", work.getId()).executeUpdate());
        assertEquals("fk_process_work_item_process_work", exception.getConstraintName());
    }

    @Test
    void historyDeleteDoesNotCascadeIntoWork() {
        ProcessWork work = work();
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () ->
                em.createNativeQuery("delete from t_process_history where id=:id")
                        .setParameter("id", work.getProcessHistory().getId()).executeUpdate());
        assertEquals("fk_process_work_process_history", exception.getConstraintName());
    }
}
