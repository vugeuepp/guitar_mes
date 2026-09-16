package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.*;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.parts.*;
import com.example.guitarmes.productionorder.*;
import com.example.guitarmes.process.common.*;
import com.example.guitarmes.process.partsinstallation.*;
import com.example.guitarmes.process.work.*;

class ProcessPartsInstallationBulkStartTest {
    ProcessHistoryRepository histories;
    GuitarRepository guitars;
    ManufacturingProcessRepository processes;
    ProcessWorkRepository works;
    ProcessWorkItemRepository items;
    PartsInstallationWorkPlanGenerator generator;
    ProcessService service;
    ManufacturingProcess process;
    List<Guitar> targets;
    Map<Long, PartsInstallationWorkPlan> plans;
    LocalDateTime before = LocalDateTime.of(2020, 1, 1, 0, 0);

    @BeforeEach void setup() {
        histories = mock(ProcessHistoryRepository.class);
        guitars = mock(GuitarRepository.class);
        processes = mock(ManufacturingProcessRepository.class);
        works = mock(ProcessWorkRepository.class);
        items = mock(ProcessWorkItemRepository.class);
        generator = mock(PartsInstallationWorkPlanGenerator.class);
        service = new ProcessService(histories, guitars, processes, mock(ProductionOrderRepository.class),
                generator, new PartsInstallationWorkWriter(works, items));
        process = new ManufacturingProcess("GUITAR", "変更後の表示名", 1);
        process.setId(73L);
        process.setProcessCode(ProcessCodeConstants.GUITAR_PARTS_INSTALLATION);
        when(processes.findById(73L)).thenReturn(Optional.of(process));
        when(processes.findByTargetTypeOrderByProcessOrderAsc("GUITAR")).thenReturn(List.of(process));
        targets = new ArrayList<>();
        plans = new HashMap<>();
        for (long id = 1; id <= 3; id++) {
            var guitar = new Guitar();
            guitar.setId(id);
            guitar.setProduct(new Product());
            guitar.setProductionOrder(new ProductionOrder());
            guitar.setCurrentProcess("開始前");
            guitar.setUpdatedAt(before);
            targets.add(guitar);
            when(guitars.findForUpdate(id)).thenReturn(Optional.of(guitar));
            when(guitars.findById(id)).thenReturn(Optional.of(guitar));
            when(histories.findByGuitarId(id)).thenReturn(List.of());
            var keys = List.of(ProcessWorkItemKey.BRIDGE_SIX_POINT_INSTALL, ProcessWorkItemKey.BRIDGE_MOVEMENT_CHECK,
                    ProcessWorkItemKey.SPRING_HANGER_INSTALL, ProcessWorkItemKey.PICKGUARD_INSTALL,
                    ProcessWorkItemKey.JACK_PLATE_INSTALL, ProcessWorkItemKey.JACK_WIRING,
                    ProcessWorkItemKey.GROUND_WIRING, ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK,
                    ProcessWorkItemKey.ELECTRONICS_PARTS_CHECK, ProcessWorkItemKey.ELECTRONICS_FINAL_FASTENING,
                    ProcessWorkItemKey.TUNER_BUSHING_INSTALL, ProcessWorkItemKey.TUNER_INSTALL, ProcessWorkItemKey.STRING_INSTALL);
            var plannedItems = new ArrayList<PartsInstallationWorkPlan.Item>();
            for (int i = 0; i < keys.size(); i++) plannedItems.add(new PartsInstallationWorkPlan.Item(keys.get(i), i + 1));
            var plan = new PartsInstallationWorkPlan(new PartsInstallationWorkPlan.Snapshot(
                    BridgeType.SIX_POINT, null, false, "tuner-" + id, TunerMountingType.PRESS_BUSHING,
                    true, TunerLayout.SIX_IN_LINE, 5, "1Vol. 2Tone", JackMountingType.BOAT_PLATE,
                    null, "strings-" + id, "09-42", "SSS"), plannedItems);
            plans.put(id, plan);
            when(generator.generate(guitar.getProduct())).thenReturn(new PartsInstallationWorkPlanGenerator.Result(
                    PartsInstallationWorkPlanGenerator.Target.STRAT_TARGET, Optional.of(plan)));
        }
        when(histories.saveAll(any())).thenAnswer(call -> {
            List<ProcessHistory> result = new ArrayList<>(call.getArgument(0));
            result.forEach(h -> h.setId(100L + h.getGuitarId()));
            // 保存戻り値の順序に頼らずGuitar IDで対応できることを検証する。
            Collections.reverse(result);
            return result;
        });
        when(works.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    List<ProcessHistory> start() { return service.startProcesses(List.of(3L, 1L, 2L, 1L), 73L, " Worker "); }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void allStratAndMixedSuccessPreserveIdentityAndTwoPhaseOrder(boolean mixed) {
        if (mixed) when(generator.generate(targets.get(1).getProduct())).thenReturn(
                new PartsInstallationWorkPlanGenerator.Result(PartsInstallationWorkPlanGenerator.Target.NON_TARGET, Optional.empty()));
        var result = start();
        assertEquals(3, result.size());
        assertEquals(1, result.stream().map(ProcessHistory::getStartTime).distinct().count());
        result.forEach(h -> {
            assertEquals("Worker", h.getWorkerName());
            assertEquals(73L, h.getProcessId());
        });
        var sequence = inOrder(guitars, generator, histories, works, items);
        for (long id = 1; id <= 3; id++) sequence.verify(guitars).findForUpdate(id);
        for (Guitar guitar : targets) sequence.verify(generator).generate(guitar.getProduct());
        sequence.verify(histories).saveAll(any());
        var workCaptor = ArgumentCaptor.forClass(ProcessWork.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProcessWorkItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        for (ProcessHistory h : result) {
            if (mixed && h.getGuitarId() == 2L) continue;
            sequence.verify(works).save(workCaptor.capture());
            sequence.verify(items).saveAll(itemCaptor.capture());
            var work = workCaptor.getValue();
            assertSame(h, work.getProcessHistory());
            var plan = plans.get(h.getGuitarId());
            assertEquals(plan.snapshot(), new PartsInstallationWorkPlan.Snapshot(
                    work.getBridgeType(), work.getBridgeModel(), work.getRequiresStudHoleExpansion(),
                    work.getTunerModel(), work.getTunerMountingType(), work.getTunerBushRequired(), work.getTunerLayout(),
                    work.getSelectorPositions(), work.getControlLayout(), work.getJackMountingType(),
                    work.getStringMaker(), work.getStringModel(), work.getStringGauge(), work.getPickupLayout()));
            assertEquals(plan.items().size(), itemCaptor.getValue().size());
            for (int i = 0; i < plan.items().size(); i++) {
                var item = itemCaptor.getValue().get(i);
                assertSame(work, item.getProcessWork());
                assertEquals(plan.items().get(i).itemKey(), item.getItemKey());
                assertEquals(plan.items().get(i).itemOrder(), item.getItemOrder());
                assertEquals(ProcessWorkItemStatus.NOT_STARTED, item.getStatus());
                assertNull(item.getCompletedAt());
            }
        }
        sequence.verify(guitars).saveAll(targets);
        verifyNoMoreInteractions(generator, works, items);
        targets.forEach(g -> {
            assertEquals(process.getProcessName(), g.getCurrentProcess());
            assertEquals(result.get(0).getStartTime(), g.getUpdatedAt());
        });
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"OTHER"})
    void legacyBulkNeverGeneratesWork(String code) {
        process.setProcessCode(code);
        process.setProcessName("ギターパーツ取付");
        assertEquals(3, start().size());
        verifyNoInteractions(generator, works, items);
        verify(guitars).saveAll(targets);
    }

    @ParameterizedTest @ValueSource(strings = {"unknown", "missing", "invalid"})
    void lastPlanFailureLeavesAllGuitarsUnchangedAndSavesNothing(String kind) {
        var product = targets.get(2).getProduct();
        if (kind.equals("unknown")) when(generator.generate(product)).thenReturn(
                new PartsInstallationWorkPlanGenerator.Result(PartsInstallationWorkPlanGenerator.Target.UNCLASSIFIABLE, Optional.empty()));
        else when(generator.generate(product)).thenThrow(new BusinessException(kind));
        assertThrows(BusinessException.class, this::start);
        for (Guitar guitar : targets) verify(generator).generate(guitar.getProduct());
        verify(histories, never()).saveAll(any());
        verify(histories, never()).save(any());
        verifyNoInteractions(works, items);
        assertNoGuitarUpdates();
    }

    @ParameterizedTest @ValueSource(strings = {"active", "completed"})
    void lastExistingValidationFailurePrecedesAllPlanGeneration(String kind) {
        if (kind.equals("active")) when(histories.findByGuitarId(3L)).thenReturn(List.of(
                new ProcessHistory(3L, 73L, "existing", before)));
        else targets.get(2).setCurrentProcess(GuitarProcessConstants.COMPLETED);
        assertThrows(BusinessException.class, this::start);
        verifyNoInteractions(generator, works, items);
        verify(histories, never()).saveAll(any());
        verify(guitars, never()).saveAll(any());
        assertEquals("開始前", targets.get(0).getCurrentProcess());
        assertEquals("開始前", targets.get(1).getCurrentProcess());
    }

    @ParameterizedTest @ValueSource(strings = {"history", "work", "items"})
    void persistenceFailuresRequestRollbackWithoutGuitarUpdate(String stage) {
        var failure = new DataIntegrityViolationException("failure");
        if (stage.equals("history")) doThrow(failure).when(histories).saveAll(any());
        // 先行Work保存後の失敗も確認する。DB上のrollbackそのものは検証しない。
        if (stage.equals("work")) doAnswer(i -> i.getArgument(0)).doThrow(failure).when(works).save(any());
        if (stage.equals("items")) doReturn(List.of()).doThrow(failure).when(items).saveAll(any());
        var manager = new ProcessPartsInstallationStartTest.RecordingTransactionManager();
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        service = (ProcessService) proxy.getProxy();
        assertSame(failure, assertThrows(DataIntegrityViolationException.class, this::start));
        assertEquals(1, manager.rollbacks);
        assertEquals(0, manager.commits);
        assertNoGuitarUpdates();
    }

    void assertNoGuitarUpdates() {
        verify(guitars, never()).saveAll(any());
        verify(guitars, never()).save(any());
        targets.forEach(g -> {
            assertEquals("開始前", g.getCurrentProcess());
            assertEquals(before, g.getUpdatedAt());
        });
    }
}
