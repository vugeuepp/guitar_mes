package com.example.guitarmes.process.partsinstallation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.example.guitarmes.process.work.ProcessWorkItemKey.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.product.*;
import com.example.guitarmes.master.instrumenttype.*;
import com.example.guitarmes.master.productseries.*;
import com.example.guitarmes.product.parts.*;
import com.example.guitarmes.process.work.ProcessWorkItemKey;

class PartsInstallationWorkPlanGeneratorTest {
    ProductClassificationService classifier;
    ProductPartsSpecRepository repository;
    PartsInstallationWorkPlanGenerator generator;
    Product product;
    ProductPartsSpec spec;

    @BeforeEach void setUp() {
        classifier = mock(ProductClassificationService.class);
        repository = mock(ProductPartsSpecRepository.class);
        generator = new PartsInstallationWorkPlanGenerator(classifier, repository, new ProductPartsSpecValidator());
        product = new Product();
        product.setId(10L);
        product.setInternalModelCode("SERIES-ST");
        product.setPickupLayout("SSS");
        when(classifier.classify("SERIES-ST")).thenReturn(Optional.of(
                new ProductClassificationService.Classification("SERIES", "ST")));
        spec = new ProductPartsSpec();
        spec.setBridgeType(BridgeType.SIX_POINT);
        spec.setBridgeModel(" bridge ");
        spec.setRequiresStudHoleExpansion(false);
        spec.setTunerModel(" tuner ");
        spec.setTunerMountingType(TunerMountingType.PRESS_BUSHING);
        spec.setTunerBushRequired(true);
        spec.setTunerLayout(TunerLayout.SIX_IN_LINE);
        spec.setSelectorPositions(5);
        spec.setControlLayout("1Vol. 2Tone");
        spec.setJackMountingType(JackMountingType.BOAT_PLATE);
        spec.setStringMaker("maker");
        spec.setStringModel("strings");
        spec.setStringGauge("09-42");
        when(repository.findByProductId(10L)).thenReturn(Optional.of(spec));
    }

    PartsInstallationWorkPlan plan() { return generator.generate(product).plan().orElseThrow(); }

    @ParameterizedTest
    @CsvSource({"SIX_POINT,false", "TWO_POINT,false", "TWO_POINT,true", "FLOYD_ROSE,false", "FLOYD_ROSE,true"})
    void exactOrderedItemsForEveryBridge(BridgeType bridge, boolean expansion) {
        spec.setBridgeType(bridge);
        spec.setRequiresStudHoleExpansion(expansion);
        List<ProcessWorkItemKey> expected = new ArrayList<>();
        if (bridge == BridgeType.SIX_POINT) expected.addAll(List.of(BRIDGE_SIX_POINT_INSTALL, BRIDGE_MOVEMENT_CHECK));
        else {
            if (expansion) expected.add(STUD_HOLE_EXPANSION);
            expected.add(STUD_INSTALL);
            if (bridge == BridgeType.TWO_POINT) expected.add(BRIDGE_TWO_POINT_INSTALL);
        }
        expected.addAll(List.of(SPRING_HANGER_INSTALL, PICKGUARD_INSTALL, JACK_PLATE_INSTALL,
                JACK_WIRING, GROUND_WIRING, ELECTRONICS_SOUND_CHECK, ELECTRONICS_PARTS_CHECK,
                ELECTRONICS_FINAL_FASTENING, TUNER_BUSHING_INSTALL, TUNER_INSTALL, STRING_INSTALL));
        var plan = plan();
        assertEquals(expected, plan.items().stream().map(PartsInstallationWorkPlan.Item::itemKey).toList());
        for (int i = 0; i < expected.size(); i++) assertEquals(i + 1, plan.items().get(i).itemOrder());
        verify(repository).findByProductId(10L);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest @CsvSource({"true", "false"})
    void nutFasteningUsesBushSpecification(boolean bush) {
        spec.setTunerMountingType(TunerMountingType.NUT_FASTENING);
        spec.setTunerBushRequired(bush);
        assertEquals(bush, plan().items().stream().anyMatch(i -> i.itemKey() == TUNER_BUSHING_INSTALL));
    }

    @Test void snapshotCopiesAllFourteenValuesWithoutNormalizationOrEntityReferences() {
        var plan = plan();
        var expected = new PartsInstallationWorkPlan.Snapshot(BridgeType.SIX_POINT, " bridge ", false,
                " tuner ", TunerMountingType.PRESS_BUSHING, true, TunerLayout.SIX_IN_LINE, 5,
                "1Vol. 2Tone", JackMountingType.BOAT_PLATE, "maker", "strings", "09-42", "SSS");
        assertEquals(expected, plan.snapshot());
        assertEquals(14, plan.snapshot().getClass().getRecordComponents().length);
        assertEquals(" tuner ", spec.getTunerModel());
        spec.setTunerModel("changed");
        product.setPickupLayout("HH");
        assertEquals(expected, plan.snapshot());
        assertThrows(UnsupportedOperationException.class, () -> plan.items().clear());
        var source = new ArrayList<>(plan.items());
        var copy = new PartsInstallationWorkPlan(plan.snapshot(), source);
        source.clear();
        assertEquals(plan.items(), copy.items());
    }

    @Test void optionalValuesMayBeNullAndSelectorCountDoesNotSplitSoundCheck() {
        spec.setBridgeModel(null);
        spec.setStringMaker(null);
        spec.setSelectorPositions(7);
        var plan = plan();
        assertNull(plan.snapshot().bridgeModel());
        assertNull(plan.snapshot().stringMaker());
        assertEquals(7, plan.snapshot().selectorPositions());
        assertEquals(1, plan.items().stream().filter(i -> i.itemKey() == ELECTRONICS_SOUND_CHECK).count());
        assertEquals(1, plan.items().stream().filter(i -> i.itemKey() == STRING_INSTALL).count());
    }

    @Test void nonTargetAndUnclassifiableAreDistinctAndDoNotReadSpec() {
        when(classifier.classify("SERIES-ST")).thenReturn(Optional.of(
                new ProductClassificationService.Classification("SERIES", "TL")));
        var nonTarget = generator.generate(product);
        assertEquals(PartsInstallationWorkPlanGenerator.Target.NON_TARGET, nonTarget.target());
        assertTrue(nonTarget.plan().isEmpty());
        when(classifier.classify("SERIES-ST")).thenReturn(Optional.empty());
        var unknown = generator.generate(product);
        assertEquals(PartsInstallationWorkPlanGenerator.Target.UNCLASSIFIABLE, unknown.target());
        assertTrue(unknown.plan().isEmpty());
        verifyNoInteractions(repository);
    }

    @Test void formalMasterClassificationControlsTargetDespiteMisleadingDisplayValues() {
        var types = mock(InstrumentTypeMasterService.class);
        var series = mock(ProductSeriesMasterService.class);
        when(types.getInstrumentTypeMasters()).thenReturn(List.of(
                new InstrumentTypeMaster("ST", "Stratocaster", "body", "neck", true),
                new InstrumentTypeMaster("TL", "Telecaster", "body", "neck", true)));
        when(series.getRequiredProductSeriesMaster("SERIES")).thenReturn(
                new ProductSeriesMaster("SERIES", "series", true));
        var actual = new PartsInstallationWorkPlanGenerator(
                new ProductClassificationService(series, types), repository, new ProductPartsSpecValidator());
        assertEquals(PartsInstallationWorkPlanGenerator.Target.STRAT_TARGET, actual.generate(product).target());
        clearInvocations(repository);
        product.setInternalModelCode("SERIES-TL");
        product.setProductName("Stratocaster SSS");
        product.setPickupLayout("SSS");
        assertEquals(PartsInstallationWorkPlanGenerator.Target.NON_TARGET, actual.generate(product).target());
        product.setInternalModelCode("Stratocaster");
        assertEquals(PartsInstallationWorkPlanGenerator.Target.UNCLASSIFIABLE, actual.generate(product).target());
        verifyNoInteractions(repository);
    }

    @Test void missingSpecFailsBeforeAnyPlanIsReturned() {
        when(repository.findByProductId(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> plan());
        verify(repository).findByProductId(10L);
        verifyNoMoreInteractions(repository);
    }

    static Stream<Consumer<ProductPartsSpec>> invalidSpecs() {
        return Stream.of(s -> s.setBridgeType(null), s -> s.setRequiresStudHoleExpansion(null),
                s -> s.setRequiresStudHoleExpansion(true), s -> s.setTunerModel(null),
                s -> s.setTunerModel(" "), s -> s.setTunerModel("x".repeat(256)),
                s -> s.setTunerMountingType(null), s -> s.setTunerBushRequired(null),
                s -> s.setTunerBushRequired(false), s -> s.setTunerLayout(null),
                s -> s.setSelectorPositions(null), s -> s.setSelectorPositions(0),
                s -> s.setControlLayout(null), s -> s.setJackMountingType(null),
                s -> s.setStringModel(null), s -> s.setStringGauge(null),
                s -> s.setStringGauge("x".repeat(101)));
    }

    @ParameterizedTest @MethodSource("invalidSpecs")
    void invalidStoredSpecFailsWithoutSaving(Consumer<ProductPartsSpec> change) {
        change.accept(spec);
        assertThrows(BusinessException.class, () -> plan());
        verify(repository).findByProductId(10L);
        verifyNoMoreInteractions(repository);
    }

    @Test void missingBlankAndOverlongPickupLayoutAreRejected() {
        for (String value : Arrays.asList(null, " ", "x".repeat(256))) {
            product.setPickupLayout(value);
            assertThrows(BusinessException.class, () -> plan());
        }
    }
}
