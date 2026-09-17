package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.parts.*;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.process.common.ProcessCodeConstants;
import com.example.guitarmes.process.common.ProcessTargetConstants;
import com.example.guitarmes.process.partsinstallation.*;
import com.example.guitarmes.process.work.*;

class ProcessPartsInstallationStartTest {
    ProcessHistoryRepository histories;
    GuitarRepository guitars;
    ManufacturingProcessRepository processes;
    ProcessWorkRepository works;
    ProcessWorkItemRepository items;
    PartsInstallationWorkPlanGenerator generator;
    ProcessService service;
    ManufacturingProcess process;
    Guitar guitar;
    PartsInstallationWorkPlan plan;
    LocalDateTime previousTime = LocalDateTime.of(2020, 1, 1, 0, 0);

    @BeforeEach void setUp() {
        histories = mock(ProcessHistoryRepository.class);
        guitars = mock(GuitarRepository.class);
        processes = mock(ManufacturingProcessRepository.class);
        works = mock(ProcessWorkRepository.class);
        items = mock(ProcessWorkItemRepository.class);
        generator = mock(PartsInstallationWorkPlanGenerator.class);
        service = new ProcessService(histories, guitars, processes,
                mock(ProductionOrderRepository.class), generator, new PartsInstallationWorkWriter(works, items), null);
        process = new ManufacturingProcess(ProcessTargetConstants.GUITAR, "表示名変更後の工程", 1);
        process.setId(73L);
        process.setProcessCode(ProcessCodeConstants.GUITAR_PARTS_INSTALLATION);
        guitar = new Guitar();
        guitar.setId(10L);
        guitar.setProduct(new Product());
        guitar.setProductionOrder(new ProductionOrder());
        guitar.setCurrentProcess("開始前");
        guitar.setUpdatedAt(previousTime);
        when(guitars.findForUpdate(10L)).thenReturn(Optional.of(guitar));
        when(guitars.findById(10L)).thenReturn(Optional.of(guitar));
        when(processes.findById(73L)).thenReturn(Optional.of(process));
        when(processes.findByTargetTypeOrderByProcessOrderAsc(ProcessTargetConstants.GUITAR))
                .thenReturn(List.of(process));
        when(histories.findByGuitarId(10L)).thenReturn(List.of());
        when(histories.save(any())).thenAnswer(i -> {
            ProcessHistory history = i.getArgument(0);
            history.setId(101L);
            return history;
        });
        when(works.save(any())).thenAnswer(i -> {
            ProcessWork work = i.getArgument(0);
            work.setId(102L);
            return work;
        });
        plan = new PartsInstallationWorkPlan(new PartsInstallationWorkPlan.Snapshot(
                BridgeType.TWO_POINT, "bridge", true, "tuner", TunerMountingType.NUT_FASTENING,
                false, TunerLayout.SIX_IN_LINE, 5, "1Vol. 2Tone", JackMountingType.BOAT_PLATE,
                "maker", "strings", "09-42", "SSS"), List.of(
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.STUD_HOLE_EXPANSION, 1),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.STUD_INSTALL, 2),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.BRIDGE_TWO_POINT_INSTALL, 3),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.SPRING_HANGER_INSTALL, 4),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.PICKGUARD_INSTALL, 5),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.JACK_PLATE_INSTALL, 6),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.JACK_WIRING, 7),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.GROUND_WIRING, 8),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 9),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.ELECTRONICS_PARTS_CHECK, 10),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.ELECTRONICS_FINAL_FASTENING, 11),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.TUNER_INSTALL, 12),
                new PartsInstallationWorkPlan.Item(ProcessWorkItemKey.STRING_INSTALL, 13)));
        when(generator.generate(guitar.getProduct())).thenReturn(
                new PartsInstallationWorkPlanGenerator.Result(
                        PartsInstallationWorkPlanGenerator.Target.STRAT_TARGET, Optional.of(plan)));
    }

    ProcessHistory start() { return service.startProcess(10L, 73L, " Worker "); }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void targetCopiesPlanAndSavesInForeignKeyOrder(boolean optionalNulls) {
        if (optionalNulls) {
            var s = plan.snapshot();
            plan = new PartsInstallationWorkPlan(new PartsInstallationWorkPlan.Snapshot(
                    s.bridgeType(), null, s.requiresStudHoleExpansion(), s.tunerModel(), s.tunerMountingType(),
                    s.tunerBushRequired(), s.tunerLayout(), s.selectorPositions(), s.controlLayout(),
                    s.jackMountingType(), null, s.stringModel(), s.stringGauge(), s.pickupLayout()), plan.items());
            when(generator.generate(guitar.getProduct())).thenReturn(new PartsInstallationWorkPlanGenerator.Result(
                    PartsInstallationWorkPlanGenerator.Target.STRAT_TARGET, Optional.of(plan)));
        }
        ProcessHistory history = start();
        assertEquals("Worker", history.getWorkerName());
        assertEquals(10L, history.getGuitarId());
        assertEquals(73L, history.getProcessId());
        assertNotNull(history.getStartTime());
        assertEquals(history.getStartTime(), guitar.getUpdatedAt());
        assertEquals(process.getProcessName(), guitar.getCurrentProcess());
        var workCaptor = ArgumentCaptor.forClass(ProcessWork.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProcessWorkItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        var order = inOrder(guitars, generator, histories, works, items);
        order.verify(guitars).findForUpdate(10L);
        order.verify(generator).generate(guitar.getProduct());
        order.verify(histories).save(history);
        order.verify(works).save(workCaptor.capture());
        order.verify(items).saveAll(itemCaptor.capture());
        order.verify(guitars).save(guitar);
        var work = workCaptor.getValue();
        assertSame(history, work.getProcessHistory());
        assertEquals(plan.snapshot(), new PartsInstallationWorkPlan.Snapshot(
                work.getBridgeType(), work.getBridgeModel(), work.getRequiresStudHoleExpansion(),
                work.getTunerModel(), work.getTunerMountingType(), work.getTunerBushRequired(),
                work.getTunerLayout(), work.getSelectorPositions(), work.getControlLayout(),
                work.getJackMountingType(), work.getStringMaker(), work.getStringModel(),
                work.getStringGauge(), work.getPickupLayout()));
        assertNull(work.getCreatedAt()); // mockではcallbackを実行しない。Serviceは時刻を設定しない。
        var savedItems = itemCaptor.getValue();
        assertEquals(plan.items().size(), savedItems.size());
        for (int i = 0; i < savedItems.size(); i++) {
            var item = savedItems.get(i);
            assertSame(work, item.getProcessWork());
            assertEquals(plan.items().get(i).itemKey(), item.getItemKey());
            assertEquals(plan.items().get(i).itemOrder(), item.getItemOrder());
            assertEquals(ProcessWorkItemStatus.NOT_STARTED, item.getStatus());
            assertNull(item.getCompletedAt());
            assertNull(item.getCreatedAt());
        }
        verifyNoMoreInteractions(generator, works, items);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"OTHER_PROCESS"})
    void legacyCodeDoesNotInvokeWorkGenerationEvenWithPartsDisplayName(String code) {
        process.setProcessCode(code);
        process.setProcessName("ギターパーツ取付");
        start();
        verify(histories).save(any());
        verify(guitars).save(guitar);
        verifyNoInteractions(generator, works, items);
    }

    @Test void nonTargetStartsWithHistoryOnly() {
        when(generator.generate(guitar.getProduct())).thenReturn(new PartsInstallationWorkPlanGenerator.Result(
                PartsInstallationWorkPlanGenerator.Target.NON_TARGET, Optional.empty()));
        start();
        verify(histories).save(any());
        verify(guitars).save(guitar);
        verifyNoInteractions(works, items);
    }

    @Test void unclassifiableIsRejectedBeforeHistory() {
        when(generator.generate(guitar.getProduct())).thenReturn(new PartsInstallationWorkPlanGenerator.Result(
                PartsInstallationWorkPlanGenerator.Target.UNCLASSIFIABLE, Optional.empty()));
        BusinessException error = assertThrows(BusinessException.class, this::start);
        assertTrue(error.getMessage().contains("製品分類"));
        assertRejectedBeforeHistory();
    }

    @ParameterizedTest @ValueSource(strings = {"パーツ取付仕様未登録", "保存済み仕様が不正"})
    void generatorValidationFailureIsPropagated(String message) {
        var failure = new BusinessException(message);
        when(generator.generate(guitar.getProduct())).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class, this::start));
        assertRejectedBeforeHistory();
    }

    @Test void duplicateStartTakesPriorityOverPlanGeneration() {
        when(histories.findByGuitarId(10L)).thenReturn(List.of(
                new ProcessHistory(10L, 73L, "existing", LocalDateTime.now())));
        assertThrows(BusinessException.class, this::start);
        assertRejectedBeforeHistory();
        verifyNoInteractions(generator);
    }

    @Test void wrongNextProcessTakesPriorityOverPlanGeneration() {
        var first = new ManufacturingProcess(ProcessTargetConstants.GUITAR, "先行工程", 0);
        first.setId(72L);
        when(processes.findByTargetTypeOrderByProcessOrderAsc(ProcessTargetConstants.GUITAR))
                .thenReturn(List.of(first, process));
        assertThrows(BusinessException.class, this::start);
        assertRejectedBeforeHistory();
        verifyNoInteractions(generator);
    }

    void assertRejectedBeforeHistory() {
        verify(histories, never()).save(any());
        verifyNoInteractions(works, items);
        assertGuitarUnchanged();
    }

    void assertGuitarUnchanged() {
        assertEquals("開始前", guitar.getCurrentProcess());
        assertEquals(previousTime, guitar.getUpdatedAt());
        verify(guitars, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings = {"history", "work", "items"})
    void persistenceFailureEscapesAndRequestsTransactionRollback(String failingStage) {
        var failure = new DataIntegrityViolationException("test failure");
        switch (failingStage) {
            case "history" -> doThrow(failure).when(histories).save(any());
            case "work" -> doThrow(failure).when(works).save(any());
            default -> doThrow(failure).when(items).saveAll(any());
        }
        // DBを使わずSpringの@Transactional適用とrollback要求を確認する。
        // 実DBに保存済みの行が消えることは、このテストでは検証しない。
        var transactions = new RecordingTransactionManager();
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        service = (ProcessService) proxy.getProxy();
        assertSame(failure, assertThrows(DataIntegrityViolationException.class, this::start));
        assertEquals(1, transactions.begins);
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        assertGuitarUnchanged();
        if (failingStage.equals("history")) verifyNoInteractions(works, items);
        if (failingStage.equals("work")) verifyNoInteractions(items);
    }

    @Test void writerCannotStartAnIndependentTransaction() {
        var proxy = new ProxyFactory(new PartsInstallationWorkWriter(works, items));
        proxy.addAdvice(new TransactionInterceptor(new RecordingTransactionManager(),
                new AnnotationTransactionAttributeSource()));
        var writer = (PartsInstallationWorkWriter) proxy.getProxy();
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> writer.save(new ProcessHistory(), plan));
        verifyNoInteractions(works, items);
    }

    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        int begins;
        int commits;
        int rollbacks;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { begins++; }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
