package com.example.guitarmes.product.parts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductRepository;
import com.example.guitarmes.product.ProductService;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;

@ExtendWith(MockitoExtension.class)
class ProductPartsSpecServiceTest {
    @Mock ProductRepository products;
    @Mock ProductPartsSpecRepository specs;
    @Mock GuitarRepository guitars;
    @Mock ProductionOrderRepository orders;

    ProductPartsSpecService service;
    Product product;

    @BeforeEach
    void setUp() {
        ProductService productService = new ProductService(
                products, null, null, guitars, orders, null, null, null);
        service = new ProductPartsSpecService(productService, specs);
        product = new Product();
        product.setId(10L);
        lenient().when(products.findById(10L)).thenReturn(Optional.of(product));
        lenient().when(specs.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }

    private ProductPartsSpecRequest valid() {
        ProductPartsSpecRequest request = new ProductPartsSpecRequest();
        request.setBridgeType(BridgeType.SIX_POINT);
        request.setRequiresStudHoleExpansion(false);
        request.setTunerModel("Tuner");
        request.setTunerMountingType(TunerMountingType.PRESS_BUSHING);
        request.setTunerBushRequired(true);
        request.setTunerLayout(TunerLayout.SIX_IN_LINE);
        request.setSelectorPositions(5);
        request.setControlLayout("1Vol / 2Tone");
        request.setJackMountingType(JackMountingType.BOAT_PLATE);
        request.setStringModel("Strings");
        request.setStringGauge(".009-.042");
        return request;
    }

    private ProductPartsSpec existing() {
        ProductPartsSpec spec = new ProductPartsSpec();
        spec.setId(20L);
        spec.setProduct(product);
        spec.setStringModel("Original");
        when(specs.findByProductId(10L)).thenReturn(Optional.of(spec));
        return spec;
    }

    @Test
    void createsSpecForProductWithoutSpec() {
        ProductPartsSpec saved = service.create(10L, valid());
        assertSame(product, saved.getProduct());
        assertEquals("Strings", saved.getStringModel());
        assertEquals(BridgeType.SIX_POINT, saved.getBridgeType());
        assertEquals(false, saved.getRequiresStudHoleExpansion());
        assertEquals(TunerMountingType.PRESS_BUSHING, saved.getTunerMountingType());
        assertEquals(true, saved.getTunerBushRequired());
        assertEquals(TunerLayout.SIX_IN_LINE, saved.getTunerLayout());
        assertEquals(5, saved.getSelectorPositions());
        assertEquals(JackMountingType.BOAT_PLATE, saved.getJackMountingType());
        verify(specs).existsByProductId(10L);
        verify(specs).saveAndFlush(saved);
    }

    @Test
    void firstSupplementDoesNotRestrictManufacturedProduct() {
        lenient().when(guitars.existsByProductId(10L)).thenReturn(true);
        ProductionOrder order = new ProductionOrder();
        order.setStartedQuantity(5);
        order.setCompletedQuantity(2);
        lenient().when(orders.findByProductId(10L)).thenReturn(List.of(order));
        assertNotNull(service.create(10L, valid()));
        verifyNoInteractions(guitars, orders);
    }

    @Test
    void missingProductFailsBeforeSpecAccess() {
        assertThrows(NotFoundException.class, () -> service.create(99L, valid()));
        assertThrows(NotFoundException.class, () -> service.update(99L, valid()));
        assertThrows(NotFoundException.class, () -> service.findByProductId(99L));
        verifyNoInteractions(specs);
    }

    @Test
    void duplicateRegistrationIsRejected() {
        when(specs.existsByProductId(10L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(10L, valid()));
        verify(specs, never()).saveAndFlush(any());
    }

    @Test
    void concurrentUniqueFailureIsPropagated() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("unique product_id");
        when(specs.saveAndFlush(any())).thenThrow(failure);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                () -> service.create(10L, valid())));
    }

    @Test
    void readsProductAndOptionalSpec() {
        assertSame(product, service.getProductById(10L));
        assertTrue(service.findByProductId(10L).isEmpty());
        ProductPartsSpec spec = existing();
        assertSame(spec, service.findByProductId(10L).orElseThrow());
        verify(specs, never()).saveAndFlush(any());
    }

    @Test
    void updateRequiresExistingSpecAndNullInputIsRejected() {
        assertThrows(NotFoundException.class, () -> service.update(10L, valid()));
        assertThrows(BusinessException.class, () -> service.create(null, valid()));
        assertThrows(BusinessException.class, () -> service.create(10L, null));
        verify(specs, never()).saveAndFlush(any());
    }

    @Test
    void updatesSameSpecBeforeProductionWithoutChangingIdentity() {
        ProductPartsSpec spec = existing();
        ProductPartsSpec updated = service.update(10L, valid());
        assertSame(spec, updated);
        assertEquals(20L, updated.getId());
        assertSame(product, updated.getProduct());
        assertEquals("Strings", updated.getStringModel());
        verify(guitars).existsByProductId(10L);
        verify(orders).findByProductId(10L);
    }

    @Test
    void rejectsUpdateAfterGuitarIssued() {
        ProductPartsSpec spec = existing();
        when(guitars.existsByProductId(10L)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.update(10L, valid()));
        assertEquals("Original", spec.getStringModel());
        verify(specs, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"1,0", "0,1"})
    void rejectsUpdateAfterProductionStartedOrCompleted(int started, int completed) {
        ProductPartsSpec spec = existing();
        ProductionOrder order = new ProductionOrder();
        order.setStartedQuantity(started);
        order.setCompletedQuantity(completed);
        when(orders.findByProductId(10L)).thenReturn(List.of(order));
        assertThrows(BusinessException.class, () -> service.update(10L, valid()));
        assertEquals("Original", spec.getStringModel());
        verify(specs, never()).saveAndFlush(any());
    }

    @Test
    void allowsUpdateWhenProductionQuantitiesAreNullOrZero() {
        existing();
        ProductionOrder order = new ProductionOrder();
        order.setStartedQuantity(null);
        order.setCompletedQuantity(0);
        when(orders.findByProductId(10L)).thenReturn(List.of(order));
        assertNotNull(service.update(10L, valid()));
    }

    static Stream<Consumer<ProductPartsSpecRequest>> missingRequired() {
        return Stream.of(r -> r.setBridgeType(null), r -> r.setRequiresStudHoleExpansion(null),
                r -> r.setTunerModel(null), r -> r.setTunerMountingType(null),
                r -> r.setTunerBushRequired(null), r -> r.setTunerLayout(null),
                r -> r.setSelectorPositions(null), r -> r.setControlLayout(null),
                r -> r.setJackMountingType(null), r -> r.setStringModel(null), r -> r.setStringGauge(null));
    }

    @ParameterizedTest
    @MethodSource("missingRequired")
    void rejectsMissingRequiredOnCreateAndUpdate(Consumer<ProductPartsSpecRequest> change) {
        ProductPartsSpecRequest request = valid();
        change.accept(request);
        rejectsOnBothPaths(request);
    }

    private void rejectsOnBothPaths(ProductPartsSpecRequest request) {
        assertThrows(BusinessException.class, () -> service.create(10L, request));
        ProductPartsSpec original = existing();
        assertThrows(BusinessException.class, () -> service.update(10L, request));
        assertEquals("Original", original.getStringModel());
        verify(specs, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"SIX_POINT,false,true", "SIX_POINT,true,false", "SIX_POINT,,false",
            "TWO_POINT,true,true", "TWO_POINT,false,true", "TWO_POINT,,false",
            "FLOYD_ROSE,true,true", "FLOYD_ROSE,false,true", "FLOYD_ROSE,,false"})
    void validatesBridge(BridgeType type, Boolean expansion, boolean accepted) {
        ProductPartsSpecRequest request = valid();
        request.setBridgeType(type);
        request.setRequiresStudHoleExpansion(expansion);
        if (accepted) {
            assertEquals(expansion, service.create(10L, request).getRequiresStudHoleExpansion());
        } else {
            rejectsOnBothPaths(request);
        }
    }

    @ParameterizedTest
    @CsvSource({"PRESS_BUSHING,true,true", "PRESS_BUSHING,false,false", "PRESS_BUSHING,,false",
            "NUT_FASTENING,true,true", "NUT_FASTENING,false,true", "NUT_FASTENING,,false"})
    void validatesTuner(TunerMountingType type, Boolean bush, boolean accepted) {
        ProductPartsSpecRequest request = valid();
        request.setTunerMountingType(type);
        request.setTunerBushRequired(bush);
        if (accepted) {
            assertEquals(bush, service.create(10L, request).getTunerBushRequired());
        } else {
            rejectsOnBothPaths(request);
        }
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,false", "1,true", "2147483647,true"})
    void validatesSelectorWithoutGuessing(int positions, boolean accepted) {
        product.setPickupLayout("SSS");
        ProductPartsSpecRequest request = valid();
        request.setSelectorPositions(positions);
        if (accepted) {
            assertEquals(positions, service.create(10L, request).getSelectorPositions());
        } else {
            rejectsOnBothPaths(request);
        }
    }

    static Stream<Arguments> stringFields() {
        return Stream.of(
                Arguments.of((BiConsumer<ProductPartsSpecRequest, String>) ProductPartsSpecRequest::setBridgeModel, 255, false),
                Arguments.of((BiConsumer<ProductPartsSpecRequest, String>) ProductPartsSpecRequest::setTunerModel, 255, true),
                Arguments.of((BiConsumer<ProductPartsSpecRequest, String>) ProductPartsSpecRequest::setControlLayout, 255, true),
                Arguments.of((BiConsumer<ProductPartsSpecRequest, String>) ProductPartsSpecRequest::setStringMaker, 150, false),
                Arguments.of((BiConsumer<ProductPartsSpecRequest, String>) ProductPartsSpecRequest::setStringModel, 255, true),
                Arguments.of((BiConsumer<ProductPartsSpecRequest, String>) ProductPartsSpecRequest::setStringGauge, 100, true));
    }

    @ParameterizedTest
    @MethodSource("stringFields")
    void enforcesStringBoundaries(BiConsumer<ProductPartsSpecRequest, String> setter, int limit, boolean mandatory) {
        ProductPartsSpecRequest request = valid();
        setter.accept(request, "x".repeat(limit));
        assertNotNull(service.create(10L, request));
        clearInvocations(specs);
        setter.accept(request, "x".repeat(limit + 1));
        rejectsOnBothPaths(request);
    }

    @ParameterizedTest
    @MethodSource("stringFields")
    void handlesBlankStrings(BiConsumer<ProductPartsSpecRequest, String> setter, int limit, boolean mandatory) {
        ProductPartsSpecRequest request = valid();
        setter.accept(request, " \t\n ");
        if (mandatory) {
            rejectsOnBothPaths(request);
        } else {
            ProductPartsSpec saved = service.create(10L, request);
            assertNull(saved.getBridgeModel());
            assertNull(saved.getStringMaker());
        }
    }

    @Test
    void trimsAllStringsWithoutChangingRequestAndKeepsArbitraryGaugeFormat() {
        ProductPartsSpecRequest request = valid();
        request.setBridgeModel(" Bridge ");
        request.setTunerModel(" Tuner ");
        request.setControlLayout(" 1Vol / 2Tone ");
        request.setStringMaker(" Maker ");
        request.setStringModel(" Model ");
        request.setStringGauge(" custom light set / special ");
        ProductPartsSpec spec = service.create(10L, request);
        assertEquals("Bridge", spec.getBridgeModel());
        assertEquals("Tuner", spec.getTunerModel());
        assertEquals("1Vol / 2Tone", spec.getControlLayout());
        assertEquals("Maker", spec.getStringMaker());
        assertEquals("Model", spec.getStringModel());
        assertEquals("custom light set / special", spec.getStringGauge());
        assertEquals(" Bridge ", request.getBridgeModel());
    }

    @Test
    void maximumLengthCountsUnicodeCharactersLikePostgres() {
        ProductPartsSpecRequest request = valid();
        request.setStringGauge("🎸".repeat(100));
        assertEquals(request.getStringGauge(), service.create(10L, request).getStringGauge());
    }
}
