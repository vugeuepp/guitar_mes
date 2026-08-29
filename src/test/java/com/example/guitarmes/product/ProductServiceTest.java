package com.example.guitarmes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.master.BodyMaterialType;
import com.example.guitarmes.master.FingerboardMaterialType;
import com.example.guitarmes.master.FretCountType;
import com.example.guitarmes.master.NeckMaterialType;
import com.example.guitarmes.master.ScaleLengthType;
import com.example.guitarmes.master.body.BodyMaster;
import com.example.guitarmes.master.body.BodyMasterRepository;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMaster;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMasterService;
import com.example.guitarmes.master.neck.NeckMaster;
import com.example.guitarmes.master.neck.NeckMasterRepository;
import com.example.guitarmes.master.productseries.ProductSeriesMaster;
import com.example.guitarmes.master.productseries.ProductSeriesMasterService;
import com.example.guitarmes.product.InternalModelCodeService;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductRepository;
import com.example.guitarmes.product.ProductService;
import com.example.guitarmes.product.ProductUpdateRequest;
import com.example.guitarmes.product.ProductVariationCreateRequest;
import com.example.guitarmes.product.ProductVariationRequest;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String SERIES_CODE =
            "MIJ-HER50";

    private static final String INTERNAL_MODEL_CODE =
            "MIJ-HER50-ST";

    private static final String MODEL_NO =
            "TEST-0001";

    private static final String COLOR =
            "3-Color Sunburst";

    private static final String INSTRUMENT_CODE =
            "ST";

    private static final String INSTRUMENT_NAME =
            "Stratocaster";

    private static final String BODY_TYPE =
            "Stratocaster";

    private static final String NECK_TYPE =
            "Stratocaster";

    private static final String OTHER_BODY_TYPE =
            "Telecaster";

    private static final String OTHER_NECK_TYPE =
            "Telecaster";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BodyMasterRepository bodyMasterRepository;

    @Mock
    private NeckMasterRepository neckMasterRepository;

    @Mock
    private GuitarRepository guitarRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private ProductSeriesMasterService
            productSeriesMasterService;

    @Mock
    private InstrumentTypeMasterService
            instrumentTypeMasterService;

    private ProductService productService;

    @BeforeEach
    void setUp() {

        productService =
                new ProductService(
                        productRepository,
                        bodyMasterRepository,
                        neckMasterRepository,
                        guitarRepository,
                        productionOrderRepository,
                        new InternalModelCodeService(),
                        productSeriesMasterService,
                        instrumentTypeMasterService);
    }

    @Test
    @DisplayName("有効なDBシリーズでProductを登録できる")
    void createProductVariations_withActiveSeries_succeeds() {

        ProductVariationCreateRequest request =
                createValidRequest();

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");
        stubNoDuplicates();
        stubNoExistingMasters();
        stubMasterCodeGeneration();
        stubRepositorySaves();

        List<Product> result =
                productService.createProductVariations(
                        request);

        assertEquals(
                1,
                result.size());

        Product createdProduct =
                result.get(0);

        assertEquals(
                MODEL_NO,
                createdProduct.getModelNo());
        assertEquals(
                INTERNAL_MODEL_CODE,
                createdProduct.getInternalModelCode());
        assertEquals(
                "Made in Japan Heritage 50s Stratocaster",
                createdProduct.getProductName());
        assertEquals(
                BodyMaterialType.ALDER.getLabel(),
                createdProduct.getBodyMaterial());
        assertEquals(
                NeckMaterialType.MAPLE.getLabel(),
                createdProduct.getNeckMaterial());
        assertEquals(
                FingerboardMaterialType.ROSEWOOD.getLabel(),
                createdProduct.getFingerboardMaterial());
        assertEquals(
                FretCountType.TWENTY_ONE.getCount(),
                createdProduct.getFretCount());
        assertEquals(
                ScaleLengthType.GUITAR_LONG.getValue(),
                createdProduct.getScale());
        assertEquals(
                INTERNAL_MODEL_CODE,
                createdProduct
                        .getBodyMaster()
                        .getProductFamilyCode());
        assertEquals(
                INTERNAL_MODEL_CODE,
                createdProduct
                        .getNeckMaster()
                        .getProductFamilyCode());

        verify(productRepository)
                .saveAll(any());
    }

    @Test
    @DisplayName("存在しない製品シリーズを拒否する")
    void createProductVariations_withUnknownSeries_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();

        when(productSeriesMasterService
                .getRequiredActiveProductSeriesMaster(
                        SERIES_CODE))
                .thenThrow(
                        new BusinessException(
                                "製品シリーズは登録されていません。"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(
                exception.getMessage()
                        .contains("登録されていません"));

        verify(productRepository, never())
                .saveAll(any());
        verify(bodyMasterRepository, never())
                .save(any(BodyMaster.class));
        verify(neckMasterRepository, never())
                .save(any(NeckMaster.class));
    }

    @Test
    @DisplayName("無効な製品シリーズを拒否する")
    void createProductVariations_withInactiveSeries_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();

        when(productSeriesMasterService
                .getRequiredActiveProductSeriesMaster(
                        SERIES_CODE))
                .thenThrow(
                        new BusinessException(
                                "製品シリーズは現在無効です。"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(
                exception.getMessage()
                        .contains("無効"));

        verify(productRepository, never())
                .saveAll(any());
    }

    @Test
    @DisplayName("改ざんされたMES内部モデルコードを拒否する")
    void createProductVariations_withTamperedInternalModelCode_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();
        request.setInternalModelCode(
                "MIJ-TR70-ST");

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(
                exception.getMessage()
                        .contains("一致しません"));
        assertTrue(
                exception.getMessage()
                        .contains(INTERNAL_MODEL_CODE));

        verify(productRepository, never())
                .saveAll(any());
    }

    @Test
    @DisplayName("楽器タイプと一致しないBodyTypeを拒否する")
    void createProductVariations_withTamperedBodyType_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();
        request.setBodyType(
                OTHER_BODY_TYPE);

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(
                exception.getMessage()
                        .contains("ボディタイプ"));
        assertTrue(
                exception.getMessage()
                        .contains("一致しません"));

        verify(productRepository, never())
                .saveAll(any());
    }

    @Test
    @DisplayName("楽器タイプと一致しないNeckTypeを拒否する")
    void createProductVariations_withTamperedNeckType_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();
        request.setNeckType(
                OTHER_NECK_TYPE);

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(
                exception.getMessage()
                        .contains("ネックタイプ"));
        assertTrue(
                exception.getMessage()
                        .contains("一致しません"));

        verify(productRepository, never())
                .saveAll(any());
    }

    @Test
    @DisplayName("登録済みの公式モデル番号を拒否する")
    void createProductVariations_withDuplicateModelNo_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");

        when(productRepository
                .existsByModelNoIgnoreCase(
                        MODEL_NO))
                .thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(
                exception.getMessage()
                        .contains(MODEL_NO));
        assertTrue(
                exception.getMessage()
                        .contains("既に登録"));

        verify(productRepository, never())
                .saveAll(any());
        verify(bodyMasterRepository, never())
                .save(any(BodyMaster.class));
        verify(neckMasterRepository, never())
                .save(any(NeckMaster.class));
    }

    @Test
    @DisplayName("同一ファミリーかつ同一仕様のMasterを再利用する")
    void createProductVariations_withMatchingMasters_reusesMasters() {

        ProductVariationCreateRequest request =
                createValidRequest();

        BodyMaster existingBodyMaster =
                createExistingBodyMaster(
                        INTERNAL_MODEL_CODE);
        NeckMaster existingNeckMaster =
                createExistingNeckMaster(
                        INTERNAL_MODEL_CODE);

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");
        stubNoDuplicates();

        when(bodyMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                        INTERNAL_MODEL_CODE,
                        BODY_TYPE,
                        BodyMaterialType.ALDER.getLabel(),
                        COLOR))
                .thenReturn(
                        Optional.of(
                                existingBodyMaster));

        when(neckMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                        INTERNAL_MODEL_CODE,
                        NECK_TYPE,
                        NeckMaterialType.MAPLE.getLabel(),
                        FingerboardMaterialType.ROSEWOOD.getLabel(),
                        FretCountType.TWENTY_ONE.getCount(),
                        ScaleLengthType.GUITAR_LONG.getValue()))
                .thenReturn(
                        Optional.of(
                                existingNeckMaster));

        stubProductSaveAll();

        List<Product> result =
                productService.createProductVariations(
                        request);

        assertSame(
                existingBodyMaster,
                result.get(0).getBodyMaster());
        assertSame(
                existingNeckMaster,
                result.get(0).getNeckMaster());

        verify(bodyMasterRepository, never())
                .save(any(BodyMaster.class));
        verify(neckMasterRepository, never())
                .save(any(NeckMaster.class));
    }

    @Test
    @DisplayName("Masterが存在しない場合は製品ファミリーコード付きで新規生成する")
    void createProductVariations_withoutMasters_createsMasters() {

        ProductVariationCreateRequest request =
                createValidRequest();

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");
        stubNoDuplicates();
        stubNoExistingMasters();
        stubMasterCodeGeneration();
        stubRepositorySaves();

        productService.createProductVariations(
                request);

        ArgumentCaptor<BodyMaster> bodyCaptor =
                ArgumentCaptor.forClass(
                        BodyMaster.class);
        ArgumentCaptor<NeckMaster> neckCaptor =
                ArgumentCaptor.forClass(
                        NeckMaster.class);

        verify(bodyMasterRepository)
                .save(bodyCaptor.capture());
        verify(neckMasterRepository)
                .save(neckCaptor.capture());

        BodyMaster createdBody =
                bodyCaptor.getValue();
        NeckMaster createdNeck =
                neckCaptor.getValue();

        assertEquals(
                INTERNAL_MODEL_CODE,
                createdBody.getProductFamilyCode());
        assertEquals(
                "BM-MIJ-HER50-ST-0001",
                createdBody.getModelCode());
        assertEquals(
                COLOR,
                createdBody.getColor());

        assertEquals(
                INTERNAL_MODEL_CODE,
                createdNeck.getProductFamilyCode());
        assertEquals(
                "NM-MIJ-HER50-ST-0001",
                createdNeck.getModelCode());
        assertEquals(
                ScaleLengthType.GUITAR_LONG.getValue(),
                createdNeck.getScale());
    }

    @Test
    @DisplayName("異なる製品ファミリーのMasterを検索条件から除外する")
    void createProductVariations_usesProductFamilyInMasterLookup() {

        ProductVariationCreateRequest request =
                createValidRequest();

        stubActiveSeries(
                SERIES_CODE,
                "Made in Japan Heritage 50s");
        stubNoDuplicates();
        stubNoExistingMasters();
        stubMasterCodeGeneration();
        stubRepositorySaves();

        productService.createProductVariations(
                request);

        verify(bodyMasterRepository)
                .findFirstByProductFamilyCodeIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                        eq(INTERNAL_MODEL_CODE),
                        eq(BODY_TYPE),
                        eq(BodyMaterialType.ALDER.getLabel()),
                        eq(COLOR));

        verify(neckMasterRepository)
                .findFirstByProductFamilyCodeIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                        eq(INTERNAL_MODEL_CODE),
                        eq(NECK_TYPE),
                        eq(NeckMaterialType.MAPLE.getLabel()),
                        eq(FingerboardMaterialType.ROSEWOOD.getLabel()),
                        eq(FretCountType.TWENTY_ONE.getCount()),
                        eq(ScaleLengthType.GUITAR_LONG.getValue()));
    }

    @Test
    @DisplayName("変更なしでProductを保存できる")
    void updateProduct_withoutChanges_succeeds() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        stubNoUpdateDuplicates();
        stubExistingMastersForUpdate(
                product.getBodyMaster(),
                product.getNeckMaster());
        stubProductSave();

        Product result =
                productService.updateProduct(
                        product.getId(),
                        request);

        assertSame(product, result);
        assertEquals(
                "Made in Japan Heritage 50s Stratocaster",
                result.getProductName());
        verify(guitarRepository, never())
                .existsByProductId(any());
        verify(productionOrderRepository, never())
                .findByProductId(any());
    }

    @Test
    @DisplayName("製品名だけを変更できる")
    void updateProduct_withProductNameOnly_succeeds() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setProductName(
                "Updated Product Name");

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        stubNoUpdateDuplicates();
        stubExistingMastersForUpdate(
                product.getBodyMaster(),
                product.getNeckMaster());
        stubProductSave();

        Product result =
                productService.updateProduct(
                        product.getId(),
                        request);

        assertEquals(
                "Updated Product Name",
                result.getProductName());
        verify(guitarRepository, never())
                .existsByProductId(any());
        verify(productionOrderRepository, never())
                .findByProductId(any());
    }

    @Test
    @DisplayName("ProductionOrder未開始なら仕様変更できる")
    void updateProduct_withUnstartedProductionOrder_allowsSpecificationChange() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setColor(
                "Olympic White");

        ProductionOrder order =
                createProductionOrder(
                        product,
                        0,
                        0);

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        stubNoUpdateDuplicates();
        when(guitarRepository
                .existsByProductId(product.getId()))
                .thenReturn(false);
        when(productionOrderRepository
                .findByProductId(product.getId()))
                .thenReturn(List.of(order));
        stubNewMastersForUpdate();
        stubProductSave();

        Product result =
                productService.updateProduct(
                        product.getId(),
                        request);

        assertEquals(
                "Olympic White",
                result.getColor());
        verify(productRepository)
                .save(product);
    }

    @Test
    @DisplayName("ProductionOrder開始済みなら仕様変更を拒否する")
    void updateProduct_withStartedProductionOrder_rejectsSpecificationChange() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setColor(
                "Olympic White");

        ProductionOrder order =
                createProductionOrder(
                        product,
                        1,
                        0);

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        when(guitarRepository
                .existsByProductId(product.getId()))
                .thenReturn(false);
        when(productionOrderRepository
                .findByProductId(product.getId()))
                .thenReturn(List.of(order));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService.updateProduct(
                                product.getId(),
                                request));

        assertTrue(
                exception.getMessage()
                        .contains("製造開始済み"));
        verify(productRepository, never())
                .save(any(Product.class));
        verify(bodyMasterRepository, never())
                .save(any(BodyMaster.class));
        verify(neckMasterRepository, never())
                .save(any(NeckMaster.class));
    }

    @Test
    @DisplayName("ProductionOrder完了数がある場合は仕様変更を拒否する")
    void updateProduct_withCompletedProductionOrder_rejectsSpecificationChange() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setPickupLayout("HSS");

        ProductionOrder order =
                createProductionOrder(
                        product,
                        0,
                        1);

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        when(guitarRepository
                .existsByProductId(product.getId()))
                .thenReturn(false);
        when(productionOrderRepository
                .findByProductId(product.getId()))
                .thenReturn(List.of(order));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService.updateProduct(
                                product.getId(),
                                request));

        assertTrue(
                exception.getMessage()
                        .contains("製造開始済み"));
        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    @DisplayName("ProductionOrder開始済みでも製品名だけ変更できる")
    void updateProduct_withStartedProductionOrder_allowsProductNameOnly() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setProductName(
                "Updated Product Name");

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        stubNoUpdateDuplicates();
        stubExistingMastersForUpdate(
                product.getBodyMaster(),
                product.getNeckMaster());
        stubProductSave();

        Product result =
                productService.updateProduct(
                        product.getId(),
                        request);

        assertEquals(
                "Updated Product Name",
                result.getProductName());
        verify(guitarRepository, never())
                .existsByProductId(any());
        verify(productionOrderRepository, never())
                .findByProductId(any());
    }

    @Test
    @DisplayName("Guitar発行済みなら仕様変更を拒否する")
    void updateProduct_withIssuedGuitar_rejectsSpecificationChange() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setFretCount(
                FretCountType.TWENTY_TWO.getCount());

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        when(guitarRepository
                .existsByProductId(product.getId()))
                .thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService.updateProduct(
                                product.getId(),
                                request));

        assertTrue(
                exception.getMessage()
                        .contains("製造個体が発行済み"));
        verify(productionOrderRepository, never())
                .findByProductId(any());
        verify(productRepository, never())
                .save(any(Product.class));
        verify(bodyMasterRepository, never())
                .save(any(BodyMaster.class));
        verify(neckMasterRepository, never())
                .save(any(NeckMaster.class));
    }

    @Test
    @DisplayName("Guitar発行済みでも製品名だけ変更できる")
    void updateProduct_withIssuedGuitar_allowsProductNameOnly() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setProductName(
                "Updated Product Name");

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        stubNoUpdateDuplicates();
        stubExistingMastersForUpdate(
                product.getBodyMaster(),
                product.getNeckMaster());
        stubProductSave();

        Product result =
                productService.updateProduct(
                        product.getId(),
                        request);

        assertEquals(
                "Updated Product Name",
                result.getProductName());
        verify(guitarRepository, never())
                .existsByProductId(any());
    }

    @Test
    @DisplayName("編集時に改ざんされた内部モデルコードを拒否する")
    void updateProduct_withTamperedInternalModelCode_rejects() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setInternalModelCode(
                "MIJ-TR70-ST");

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService.updateProduct(
                                product.getId(),
                                request));

        assertTrue(
                exception.getMessage()
                        .contains("一致しません"));
        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    @DisplayName("別Productの公式モデル番号との重複を拒否する")
    void updateProduct_withDuplicateModelNo_rejects() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();

        stubProductForUpdate(product);
        stubCurrentSeriesForUpdate();
        when(productRepository
                .existsByModelNoIgnoreCaseAndIdNot(
                        MODEL_NO,
                        product.getId()))
                .thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService.updateProduct(
                                product.getId(),
                                request));

        assertTrue(
                exception.getMessage()
                        .contains(MODEL_NO));
        assertTrue(
                exception.getMessage()
                        .contains("別の製品"));
        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    @DisplayName("現在使用中の無効シリーズを維持して更新できる")
    void updateProduct_withCurrentInactiveSeries_succeeds() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setProductName(
                "Inactive Series Product");

        stubProductForUpdate(product);
        when(productSeriesMasterService
                .getRequiredProductSeriesMaster(
                        SERIES_CODE))
                .thenReturn(createSeries(
                        SERIES_CODE,
                        false));
        when(productSeriesMasterService
                .getRequiredProductSeriesMasterForUpdate(
                        SERIES_CODE,
                        SERIES_CODE))
                .thenReturn(createSeries(
                        SERIES_CODE,
                        false));
        when(instrumentTypeMasterService
                .getRequiredInstrumentTypeMasterForUpdate(
                        INSTRUMENT_CODE,
                        INSTRUMENT_CODE))
                .thenReturn(createInstrumentTypeMaster(true));
        stubNoUpdateDuplicates();
        stubExistingMastersForUpdate(
                product.getBodyMaster(),
                product.getNeckMaster());
        stubProductSave();

        Product result =
                productService.updateProduct(
                        product.getId(),
                        request);

        assertEquals(
                "Inactive Series Product",
                result.getProductName());
    }

    @Test
    @DisplayName("別の無効シリーズへの変更を拒否する")
    void updateProduct_toDifferentInactiveSeries_rejects() {

        Product product =
                createExistingProduct();
        ProductUpdateRequest request =
                createValidUpdateRequest();
        request.setProductSeries(
                "MIJ-INACTIVE");
        request.setInternalModelCode(
                "MIJ-INACTIVE-ST");

        stubProductForUpdate(product);
        when(productSeriesMasterService
                .getRequiredProductSeriesMaster(
                        SERIES_CODE))
                .thenReturn(createSeries(
                        SERIES_CODE,
                        true));
        when(productSeriesMasterService
                .getRequiredProductSeriesMasterForUpdate(
                        "MIJ-INACTIVE",
                        SERIES_CODE))
                .thenThrow(
                        new BusinessException(
                                "製品シリーズは現在無効です。"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService.updateProduct(
                                product.getId(),
                                request));

        assertTrue(
                exception.getMessage()
                        .contains("無効"));
        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    @DisplayName("存在しないDB楽器タイプを拒否する")
    void createProductVariations_withUnknownInstrumentType_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();

        ProductSeriesMaster series =
                new ProductSeriesMaster(
                        SERIES_CODE,
                        "Made in Japan Heritage 50s",
                        true);

        when(productSeriesMasterService
                .getRequiredActiveProductSeriesMaster(
                        SERIES_CODE))
                .thenReturn(series);

        when(instrumentTypeMasterService
                .getRequiredActiveInstrumentTypeMaster(
                        INSTRUMENT_CODE))
                .thenThrow(
                        new BusinessException(
                                "楽器タイプは登録されていません。"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(exception.getMessage()
                .contains("登録されていません"));
        verify(productRepository, never())
                .saveAll(any());
    }

    @Test
    @DisplayName("無効なDB楽器タイプを拒否する")
    void createProductVariations_withInactiveInstrumentType_throws() {

        ProductVariationCreateRequest request =
                createValidRequest();

        ProductSeriesMaster series =
                new ProductSeriesMaster(
                        SERIES_CODE,
                        "Made in Japan Heritage 50s",
                        true);

        when(productSeriesMasterService
                .getRequiredActiveProductSeriesMaster(
                        SERIES_CODE))
                .thenReturn(series);

        when(instrumentTypeMasterService
                .getRequiredActiveInstrumentTypeMaster(
                        INSTRUMENT_CODE))
                .thenThrow(
                        new BusinessException(
                                "楽器タイプは現在無効です。"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> productService
                                .createProductVariations(
                                        request));

        assertTrue(exception.getMessage()
                .contains("無効"));
        verify(productRepository, never())
                .saveAll(any());
    }

    @Test
    @DisplayName("編集時に存在しないDB楽器タイプを拒否する")
    void updateProduct_withUnknownInstrumentType_rejects() {
        Product product = createExistingProduct();
        ProductUpdateRequest request = createValidUpdateRequest();
        stubProductForUpdate(product);
        when(productSeriesMasterService
                .getRequiredProductSeriesMaster(SERIES_CODE))
                .thenReturn(createSeries(SERIES_CODE, true));
        when(productSeriesMasterService
                .getRequiredProductSeriesMasterForUpdate(
                        SERIES_CODE, SERIES_CODE))
                .thenReturn(createSeries(SERIES_CODE, true));
        when(instrumentTypeMasterService
                .getRequiredInstrumentTypeMasterForUpdate(
                        INSTRUMENT_CODE, INSTRUMENT_CODE))
                .thenThrow(new BusinessException(
                        "楽器タイプは登録されていません。"));
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.updateProduct(
                        product.getId(), request));
        assertTrue(exception.getMessage()
                .contains("登録されていません"));
    }

    @Test
    @DisplayName("編集時に別の無効なDB楽器タイプを拒否する")
    void updateProduct_toDifferentInactiveInstrumentType_rejects() {
        Product product = createExistingProduct();
        ProductUpdateRequest request = createValidUpdateRequest();
        request.setInstrumentType("INACTIVE");
        request.setInternalModelCode("MIJ-HER50-INACTIVE");
        stubProductForUpdate(product);
        when(productSeriesMasterService
                .getRequiredProductSeriesMaster(SERIES_CODE))
                .thenReturn(createSeries(SERIES_CODE, true));
        when(productSeriesMasterService
                .getRequiredProductSeriesMasterForUpdate(
                        SERIES_CODE, SERIES_CODE))
                .thenReturn(createSeries(SERIES_CODE, true));
        when(instrumentTypeMasterService
                .getRequiredInstrumentTypeMasterForUpdate(
                        "INACTIVE", INSTRUMENT_CODE))
                .thenThrow(new BusinessException(
                        "楽器タイプは現在無効です。"));
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.updateProduct(
                        product.getId(), request));
        assertTrue(exception.getMessage().contains("無効"));
    }

    private ProductVariationCreateRequest
            createValidRequest() {

        ProductVariationCreateRequest request =
                new ProductVariationCreateRequest();

        request.setProductSeries(
                SERIES_CODE);
        request.setInstrumentType(
                INSTRUMENT_CODE);
        request.setInternalModelCode(
                INTERNAL_MODEL_CODE);
        request.setProductName(
                "Made in Japan Heritage 50s Stratocaster");
        request.setBodyType(
                BODY_TYPE);
        request.setBodyMaterial(
                BodyMaterialType.ALDER.name());
        request.setNeckType(
                NECK_TYPE);
        request.setNeckMaterial(
                NeckMaterialType.MAPLE.name());
        request.setPickupLayout(
                "SSS");
        request.setFretCount(
                FretCountType.TWENTY_ONE.getCount());
        request.setScale(
                ScaleLengthType.GUITAR_LONG.name());
        request.setVariations(
                List.of(
                        new ProductVariationRequest(
                                MODEL_NO,
                                COLOR,
                                FingerboardMaterialType
                                        .ROSEWOOD
                                        .name())));

        return request;
    }

    private void stubActiveSeries(
            String seriesCode,
            String seriesName) {

        ProductSeriesMaster series =
                new ProductSeriesMaster(
                        seriesCode,
                        seriesName,
                        true);

        when(productSeriesMasterService
                .getRequiredActiveProductSeriesMaster(
                        seriesCode))
                .thenReturn(series);

        InstrumentTypeMaster instrumentType =
                new InstrumentTypeMaster(
                        INSTRUMENT_CODE,
                        INSTRUMENT_NAME,
                        BODY_TYPE,
                        NECK_TYPE,
                        true);

        when(instrumentTypeMasterService
                .getRequiredActiveInstrumentTypeMaster(
                        INSTRUMENT_CODE))
                .thenReturn(instrumentType);
    }

    private void stubNoDuplicates() {

        when(productRepository
                .existsByModelNoIgnoreCase(
                        anyString()))
                .thenReturn(false);

        when(productRepository
                .existsByInternalModelCodeIgnoreCaseAndColorIgnoreCaseAndFingerboardMaterialIgnoreCase(
                        anyString(),
                        anyString(),
                        anyString()))
                .thenReturn(false);
    }

    private void stubNoExistingMasters() {

        when(bodyMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()))
                .thenReturn(Optional.empty());

        when(neckMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(Integer.class),
                        anyString()))
                .thenReturn(Optional.empty());
    }

    private void stubMasterCodeGeneration() {

        when(bodyMasterRepository
                .findTopByModelCodeStartingWithOrderByIdDesc(
                        anyString()))
                .thenReturn(Optional.empty());
        when(bodyMasterRepository
                .existsByModelCodeIgnoreCase(
                        anyString()))
                .thenReturn(false);

        when(neckMasterRepository
                .findTopByModelCodeStartingWithOrderByIdDesc(
                        anyString()))
                .thenReturn(Optional.empty());
        when(neckMasterRepository
                .existsByModelCodeIgnoreCase(
                        anyString()))
                .thenReturn(false);
    }

    private void stubRepositorySaves() {

        when(bodyMasterRepository
                .save(any(BodyMaster.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(neckMasterRepository
                .save(any(NeckMaster.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        stubProductSaveAll();
    }

    private void stubProductSaveAll() {

        when(productRepository
                .saveAll(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
    }

    private ProductUpdateRequest
            createValidUpdateRequest() {

        ProductUpdateRequest request =
                new ProductUpdateRequest();

        request.setProductSeries(SERIES_CODE);
        request.setInstrumentType(
                INSTRUMENT_CODE);
        request.setInternalModelCode(
                INTERNAL_MODEL_CODE);
        request.setProductName(
                "Made in Japan Heritage 50s Stratocaster");
        request.setBodyType(
                BODY_TYPE);
        request.setBodyMaterial(
                BodyMaterialType.ALDER.name());
        request.setNeckType(
                NECK_TYPE);
        request.setNeckMaterial(
                NeckMaterialType.MAPLE.name());
        request.setPickupLayout("SSS");
        request.setFretCount(
                FretCountType.TWENTY_ONE.getCount());
        request.setScale(
                ScaleLengthType.GUITAR_LONG.name());
        request.setModelNo(MODEL_NO);
        request.setColor(COLOR);
        request.setFingerboardMaterial(
                FingerboardMaterialType.ROSEWOOD.name());

        return request;
    }

    private Product createExistingProduct() {

        BodyMaster bodyMaster =
                createExistingBodyMaster(
                        INTERNAL_MODEL_CODE);
        NeckMaster neckMaster =
                createExistingNeckMaster(
                        INTERNAL_MODEL_CODE);

        Product product =
                new Product(
                        MODEL_NO,
                        INTERNAL_MODEL_CODE,
                        "Made in Japan Heritage 50s Stratocaster",
                        COLOR,
                        BodyMaterialType.ALDER.getLabel(),
                        NeckMaterialType.MAPLE.getLabel(),
                        FingerboardMaterialType.ROSEWOOD.getLabel(),
                        "SSS",
                        FretCountType.TWENTY_ONE.getCount(),
                        ScaleLengthType.GUITAR_LONG.getValue(),
                        bodyMaster,
                        neckMaster);

        product.setId(1L);
        return product;
    }

    private InstrumentTypeMaster
            createInstrumentTypeMaster(
                    boolean active) {

        return new InstrumentTypeMaster(
                INSTRUMENT_CODE,
                INSTRUMENT_NAME,
                BODY_TYPE,
                NECK_TYPE,
                active);
    }

    private ProductSeriesMaster createSeries(
            String seriesCode,
            boolean active) {

        return new ProductSeriesMaster(
                seriesCode,
                "Test Series",
                active);
    }

    private void stubProductForUpdate(
            Product product) {

        when(productRepository.findById(
                product.getId()))
                .thenReturn(Optional.of(product));

        when(instrumentTypeMasterService
                .getInstrumentTypeMasters())
                .thenReturn(List.of(
                        createInstrumentTypeMaster(true)));
    }

    private void stubCurrentSeriesForUpdate() {

        when(instrumentTypeMasterService
                .getRequiredInstrumentTypeMasterForUpdate(
                        INSTRUMENT_CODE,
                        INSTRUMENT_CODE))
                .thenReturn(createInstrumentTypeMaster(true));

        when(productSeriesMasterService
                .getRequiredProductSeriesMaster(
                        SERIES_CODE))
                .thenReturn(createSeries(
                        SERIES_CODE,
                        true));

        when(productSeriesMasterService
                .getRequiredProductSeriesMasterForUpdate(
                        SERIES_CODE,
                        SERIES_CODE))
                .thenReturn(createSeries(
                        SERIES_CODE,
                        true));
    }

    private void stubNoUpdateDuplicates() {

        when(productRepository
                .existsByModelNoIgnoreCaseAndIdNot(
                        anyString(),
                        any()))
                .thenReturn(false);

        when(productRepository
                .existsByInternalModelCodeIgnoreCaseAndColorIgnoreCaseAndFingerboardMaterialIgnoreCaseAndIdNot(
                        anyString(),
                        anyString(),
                        anyString(),
                        any()))
                .thenReturn(false);
    }

    private void stubExistingMastersForUpdate(
            BodyMaster bodyMaster,
            NeckMaster neckMaster) {

        when(bodyMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()))
                .thenReturn(Optional.of(bodyMaster));

        when(neckMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(Integer.class),
                        anyString()))
                .thenReturn(Optional.of(neckMaster));
    }

    private void stubNewMastersForUpdate() {

        stubNoExistingMasters();
        stubMasterCodeGeneration();

        when(bodyMasterRepository
                .save(any(BodyMaster.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
        when(neckMasterRepository
                .save(any(NeckMaster.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
    }

    private void stubProductSave() {

        when(productRepository
                .save(any(Product.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
    }

    private ProductionOrder createProductionOrder(
            Product product,
            int startedQuantity,
            int completedQuantity) {

        ProductionOrder order =
                new ProductionOrder();

        order.setProduct(product);
        order.setStartedQuantity(startedQuantity);
        order.setCompletedQuantity(completedQuantity);

        return order;
    }

    private BodyMaster createExistingBodyMaster(
            String productFamilyCode) {

        BodyMaster bodyMaster =
                new BodyMaster();

        bodyMaster.setId(100L);
        bodyMaster.setModelCode(
                "BM-EXISTING-0001");
        bodyMaster.setModelName(
                "Existing Body");
        bodyMaster.setProductFamilyCode(
                productFamilyCode);
        bodyMaster.setBodyType(
                BODY_TYPE);
        bodyMaster.setMaterial(
                BodyMaterialType.ALDER.getLabel());
        bodyMaster.setColor(
                COLOR);

        return bodyMaster;
    }

    private NeckMaster createExistingNeckMaster(
            String productFamilyCode) {

        NeckMaster neckMaster =
                new NeckMaster(
                        "NM-EXISTING-0001",
                        "Existing Neck",
                        NECK_TYPE,
                        NeckMaterialType.MAPLE.getLabel(),
                        FingerboardMaterialType.ROSEWOOD.getLabel(),
                        FretCountType.TWENTY_ONE.getCount(),
                        ScaleLengthType.GUITAR_LONG.getValue());

        neckMaster.setProductFamilyCode(
                productFamilyCode);

        return neckMaster;
    }
}
