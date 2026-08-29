package com.example.guitarmes.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
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
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;

@Service
public class ProductService {

    private static final int MASTER_CODE_SEQUENCE_LENGTH = 4;

    private final ProductRepository productRepository;

    private final BodyMasterRepository bodyMasterRepository;

    private final NeckMasterRepository neckMasterRepository;

    private final GuitarRepository guitarRepository;

    private final ProductionOrderRepository
            productionOrderRepository;

    private final InternalModelCodeService
            internalModelCodeService;

    private final ProductSeriesMasterService
            productSeriesMasterService;

    private final InstrumentTypeMasterService
            instrumentTypeMasterService;

    public ProductService(
            ProductRepository productRepository,
            BodyMasterRepository bodyMasterRepository,
            NeckMasterRepository neckMasterRepository,
            GuitarRepository guitarRepository,
            ProductionOrderRepository productionOrderRepository,
            InternalModelCodeService internalModelCodeService,
            ProductSeriesMasterService
                    productSeriesMasterService,
            InstrumentTypeMasterService
                    instrumentTypeMasterService) {

        this.productRepository =
                productRepository;

        this.bodyMasterRepository =
                bodyMasterRepository;

        this.neckMasterRepository =
                neckMasterRepository;

        this.guitarRepository =
                guitarRepository;

        this.productionOrderRepository =
                productionOrderRepository;

        this.internalModelCodeService =
                internalModelCodeService;

        this.productSeriesMasterService =
                productSeriesMasterService;

        this.instrumentTypeMasterService =
                instrumentTypeMasterService;
    }

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(
            Long id) {

        return productRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された製品が存在しません。"));
    }

    public ProductUpdateRequest getProductUpdateRequest(
            Long id) {

        Product product =
                getProductById(id);

        ResolvedProductClassification classification =
                resolveProductClassification(
                        product.getInternalModelCode());

        ProductUpdateRequest request =
                new ProductUpdateRequest();

        if (classification != null) {
            request.setProductSeries(
                    classification.seriesCode());

            request.setInstrumentType(
                    classification.instrumentCode());
        }

        request.setInternalModelCode(
                product.getInternalModelCode());

        request.setProductName(
                product.getProductName());

        request.setBodyType(
                resolveBodyType(product));

        request.setBodyMaterial(
                resolveBodyMaterialTypeName(
                        product.getBodyMaterial()));

        request.setNeckType(
                resolveNeckType(product));

        request.setNeckMaterial(
                resolveNeckMaterialTypeName(
                        product.getNeckMaterial()));

        request.setPickupLayout(
                product.getPickupLayout());

        request.setFretCount(
                product.getFretCount());

        request.setScale(
                resolveScaleLengthTypeName(
                        product.getScale()));

        request.setModelNo(
                product.getModelNo());

        request.setColor(
                product.getColor());

        request.setFingerboardMaterial(
                resolveFingerboardMaterialTypeName(
                        product.getFingerboardMaterial()));

        return request;
    }

    @Transactional
    public Product updateProduct(
            Long id,
            ProductUpdateRequest request) {

        Product product =
                getProductById(id);

        NormalizedProductUpdate normalized =
                normalizeAndValidateUpdateRequest(
                        product,
                        request);

        validateProductReferenceRestriction(
                product,
                normalized);

        validateProductUpdateDuplicates(
                id,
                normalized);

        BodyMaster bodyMaster =
                findOrCreateBodyMasterForUpdate(
                        normalized);

        NeckMaster neckMaster =
                findOrCreateNeckMasterForUpdate(
                        normalized);

        product.setModelNo(
                normalized.modelNo());

        product.setInternalModelCode(
                normalized.internalModelCode());

        product.setProductName(
                normalized.productName());

        product.setColor(
                normalized.color());

        product.setBodyMaterial(
                normalized.bodyMaterial());

        product.setNeckMaterial(
                normalized.neckMaterial());

        product.setFingerboardMaterial(
                normalized.fingerboardMaterial());

        product.setPickupLayout(
                normalized.pickupLayout());

        product.setFretCount(
                normalized.fretCount());

        product.setScale(
                normalized.scale());

        product.setBodyMaster(
                bodyMaster);

        product.setNeckMaster(
                neckMaster);

        return productRepository.save(
                product);
    }

    @Transactional
    public Product createProduct(
            String modelNo,
            String productName,
            String color,
            String bodyMaterial,
            String neckMaterial,
            String fingerboardMaterial,
            String pickupLayout,
            Integer fretCount,
            String scale) {

        Product product = new Product(
                normalizeRequired(
                        modelNo,
                        "モデル番号"),
                normalizeRequired(
                        productName,
                        "製品名"),
                normalizeRequired(
                        color,
                        "カラー"),
                normalizeRequired(
                        bodyMaterial,
                        "ボディ材"),
                normalizeRequired(
                        neckMaterial,
                        "ネック材"),
                normalizeRequired(
                        fingerboardMaterial,
                        "指板材"),
                normalizeRequired(
                        pickupLayout,
                        "PU構成"),
                validateFretCount(fretCount),
                normalizeRequired(
                        scale,
                        "スケール"));

        return productRepository.save(product);
    }

    @Transactional
    public List<Product> createProductVariations(
            ProductVariationCreateRequest request) {

        NormalizedProductRequest normalized =
                normalizeAndValidateRequest(request);

        validateProductDuplicates(normalized);

        List<Product> products =
                new ArrayList<>();

        for (NormalizedVariation variation
                : normalized.variations()) {

            BodyMaster bodyMaster =
                    findOrCreateBodyMaster(
                            normalized,
                            variation);

            NeckMaster neckMaster =
                    findOrCreateNeckMaster(
                            normalized,
                            variation);

            Product product = new Product(
                    variation.modelNo(),
                    normalized.internalModelCode(),
                    normalized.productName(),
                    variation.color(),
                    normalized.bodyMaterial(),
                    normalized.neckMaterial(),
                    variation.fingerboardMaterial(),
                    normalized.pickupLayout(),
                    normalized.fretCount(),
                    normalized.scale(),
                    bodyMaster,
                    neckMaster);

            products.add(product);
        }

        return productRepository.saveAll(products);
    }

    public List<Product> searchProducts(
            String keyword) {

        if (keyword == null
                || keyword.isBlank()) {

            return productRepository.findAll();
        }

        return productRepository
                .findByProductNameContaining(
                        keyword.trim());
    }

    private void validateProductReferenceRestriction(
            Product product,
            NormalizedProductUpdate request) {

        if (!hasRestrictedProductChanges(
                product,
                request)) {

            return;
        }

        if (guitarRepository.existsByProductId(
                product.getId())) {

            throw new BusinessException(
                    "製造個体が発行済みのため、"
                    + "公式モデル番号や製造仕様を"
                    + "変更できません。"
                    + "製品名のみ変更できます。");
        }

        List<ProductionOrder> productionOrders =
                productionOrderRepository
                        .findByProductId(
                                product.getId());

        boolean productionStarted =
                productionOrders
                        .stream()
                        .anyMatch(
                                this::isProductionStarted);

        if (productionStarted) {
            throw new BusinessException(
                    "製造開始済みの生産計画が存在するため、"
                    + "公式モデル番号や製造仕様を"
                    + "変更できません。"
                    + "製品名のみ変更できます。");
        }
    }

    private boolean isProductionStarted(
            ProductionOrder productionOrder) {

        if (productionOrder == null) {
            return false;
        }

        Integer startedQuantity =
                productionOrder
                        .getStartedQuantity();

        Integer completedQuantity =
                productionOrder
                        .getCompletedQuantity();

        return hasPositiveQuantity(
                startedQuantity)
                || hasPositiveQuantity(
                        completedQuantity);
    }

    private boolean hasPositiveQuantity(
            Integer quantity) {

        return quantity != null
                && quantity > 0;
    }

    private boolean hasRestrictedProductChanges(
            Product product,
            NormalizedProductUpdate request) {

        return !equalsIgnoreCase(
                    product.getModelNo(),
                    request.modelNo())
                || !equalsIgnoreCase(
                    product.getInternalModelCode(),
                    request.internalModelCode())
                || !equalsIgnoreCase(
                    product.getColor(),
                    request.color())
                || !equalsIgnoreCase(
                    product.getBodyMaterial(),
                    request.bodyMaterial())
                || !equalsIgnoreCase(
                    product.getNeckMaterial(),
                    request.neckMaterial())
                || !equalsIgnoreCase(
                    product.getFingerboardMaterial(),
                    request.fingerboardMaterial())
                || !equalsIgnoreCase(
                    product.getPickupLayout(),
                    request.pickupLayout())
                || !Objects.equals(
                    product.getFretCount(),
                    request.fretCount())
                || !equalsIgnoreCase(
                    product.getScale(),
                    request.scale())
                || !equalsIgnoreCase(
                    resolveBodyType(product),
                    request.bodyType())
                || !equalsIgnoreCase(
                    resolveNeckType(product),
                    request.neckType());
    }

    private boolean equalsIgnoreCase(
            String currentValue,
            String requestedValue) {

        if (currentValue == null
                && requestedValue == null) {

            return true;
        }

        if (currentValue == null
                || requestedValue == null) {

            return false;
        }

        return currentValue
                .trim()
                .equalsIgnoreCase(
                        requestedValue.trim());
    }

    private NormalizedProductUpdate
            normalizeAndValidateUpdateRequest(
                    Product product,
                    ProductUpdateRequest request) {

        if (request == null) {
            throw new BusinessException(
                    "製品更新情報が指定されていません。");
        }

        ResolvedProductClassification currentClassification =
                resolveProductClassification(
                        product.getInternalModelCode());

        if (currentClassification == null) {
            throw new BusinessException(
                    "現在のMES内部モデルコードから"
                    + "製品シリーズと楽器タイプを"
                    + "判定できません。");
        }

        ProductSeriesMaster productSeriesMaster =
                productSeriesMasterService
                        .getRequiredProductSeriesMasterForUpdate(
                                request.getProductSeries(),
                                currentClassification.seriesCode());

        InstrumentTypeMaster instrumentTypeMaster =
                instrumentTypeMasterService
                        .getRequiredInstrumentTypeMasterForUpdate(
                                request.getInstrumentType(),
                                currentClassification.instrumentCode());

        String seriesCode =
                productSeriesMaster.getSeriesCode();
        String instrumentCode =
                instrumentTypeMaster.getInstrumentCode();

        String generatedInternalModelCode =
                internalModelCodeService
                        .generateInternalModelCode(
                                seriesCode,
                                instrumentCode);

        String submittedInternalModelCode =
                normalizeInternalModelCode(
                        request.getInternalModelCode());

        if (!generatedInternalModelCode.equalsIgnoreCase(
                submittedInternalModelCode)) {
            throw new BusinessException(
                    "MES内部モデルコードが"
                    + "選択された製品シリーズと"
                    + "楽器タイプから生成される値と"
                    + "一致しません。"
                    + "生成されるコードは「"
                    + generatedInternalModelCode
                    + "」です。");
        }

        String productName =
                normalizeRequired(
                        request.getProductName(),
                        "製品名");
        String expectedBodyType =
                normalizeRequired(
                        instrumentTypeMaster.getBodyType(),
                        "楽器タイプのボディタイプ");
        String submittedBodyType =
                normalizeRequired(
                        request.getBodyType(),
                        "ボディタイプ");

        if (!expectedBodyType.equalsIgnoreCase(
                submittedBodyType)) {
            throw new BusinessException(
                    "ボディタイプが選択された楽器タイプと"
                    + "一致しません。正しいボディタイプは「"
                    + expectedBodyType
                    + "」です。");
        }

        String expectedNeckType =
                normalizeRequired(
                        instrumentTypeMaster.getNeckType(),
                        "楽器タイプのネックタイプ");
        String submittedNeckType =
                normalizeRequired(
                        request.getNeckType(),
                        "ネックタイプ");

        if (!expectedNeckType.equalsIgnoreCase(
                submittedNeckType)) {
            throw new BusinessException(
                    "ネックタイプが選択された楽器タイプと"
                    + "一致しません。正しいネックタイプは「"
                    + expectedNeckType
                    + "」です。");
        }

        BodyMaterialType bodyMaterialType =
                parseEnum(request.getBodyMaterial(),
                        BodyMaterialType.class, "ボディ材");
        NeckMaterialType neckMaterialType =
                parseEnum(request.getNeckMaterial(),
                        NeckMaterialType.class, "ネック材");
        FingerboardMaterialType fingerboardMaterialType =
                parseEnum(request.getFingerboardMaterial(),
                        FingerboardMaterialType.class, "指板材");
        FretCountType fretCountType =
                parseEnum(request.getFretCount(),
                        FretCountType.class, "フレット数");
        ScaleLengthType scaleLengthType =
                parseEnum(request.getScale(),
                        ScaleLengthType.class, "スケール");

        return new NormalizedProductUpdate(
                seriesCode,
                instrumentCode,
                generatedInternalModelCode,
                productName,
                expectedBodyType,
                bodyMaterialType.getLabel(),
                expectedNeckType,
                neckMaterialType.getLabel(),
                normalizeRequired(request.getPickupLayout(), "PU構成"),
                fretCountType.getCount(),
                toScaleStorageValue(scaleLengthType),
                normalizeRequired(request.getModelNo(), "公式モデル番号"),
                normalizeRequired(request.getColor(), "カラー"),
                fingerboardMaterialType.getLabel());
    }

    private void validateProductUpdateDuplicates(
            Long productId,
            NormalizedProductUpdate request) {

        if (productRepository
                .existsByModelNoIgnoreCaseAndIdNot(
                        request.modelNo(),
                        productId)) {

            throw new BusinessException(
                    "公式モデル番号「"
                    + request.modelNo()
                    + "」は既に別の製品で"
                    + "使用されています。");
        }

        boolean specificationExists =
                productRepository
                        .existsByInternalModelCodeIgnoreCaseAndColorIgnoreCaseAndFingerboardMaterialIgnoreCaseAndIdNot(
                                request.internalModelCode(),
                                request.color(),
                                request.fingerboardMaterial(),
                                productId);

        if (specificationExists) {
            throw new BusinessException(
                    "MES内部モデルコード「"
                    + request.internalModelCode()
                    + "」、カラー「"
                    + request.color()
                    + "」、指板材「"
                    + request.fingerboardMaterial()
                    + "」の製品は"
                    + "既に別のProductとして"
                    + "登録されています。");
        }
    }

    private BodyMaster findOrCreateBodyMasterForUpdate(
            NormalizedProductUpdate request) {

        return bodyMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                        request.internalModelCode(),
                        request.bodyType(),
                        request.bodyMaterial(),
                        request.color())
                .orElseGet(() ->
                        createBodyMasterForUpdate(
                                request));
    }

    private BodyMaster createBodyMasterForUpdate(
            NormalizedProductUpdate request) {

        BodyMaster bodyMaster =
                new BodyMaster();

        bodyMaster.setModelCode(
                generateBodyMasterCode(
                        request.internalModelCode()));

        bodyMaster.setModelName(
                request.productName()
                + " Body");

        bodyMaster.setProductFamilyCode(
                request.internalModelCode());

        bodyMaster.setBodyType(
                request.bodyType());

        bodyMaster.setMaterial(
                request.bodyMaterial());

        bodyMaster.setColor(
                request.color());

        return bodyMasterRepository.save(
                bodyMaster);
    }

    private NeckMaster findOrCreateNeckMasterForUpdate(
            NormalizedProductUpdate request) {

        return neckMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                        request.internalModelCode(),
                        request.neckType(),
                        request.neckMaterial(),
                        request.fingerboardMaterial(),
                        request.fretCount(),
                        request.scale())
                .orElseGet(() ->
                        createNeckMasterForUpdate(
                                request));
    }

    private NeckMaster createNeckMasterForUpdate(
            NormalizedProductUpdate request) {

        String modelName =
                request.productName()
                + " Neck / "
                + request.fingerboardMaterial()
                + " Fingerboard";

        NeckMaster neckMaster =
                new NeckMaster(
                        generateNeckMasterCode(
                                request.internalModelCode()),
                        modelName,
                        request.neckType(),
                        request.neckMaterial(),
                        request.fingerboardMaterial(),
                        request.fretCount(),
                        request.scale());

        neckMaster.setProductFamilyCode(
                request.internalModelCode());

        return neckMasterRepository.save(
                neckMaster);
    }

    private NormalizedProductRequest
            normalizeAndValidateRequest(
                    ProductVariationCreateRequest request) {

        if (request == null) {
            throw new BusinessException(
                    "製品登録情報が指定されていません。");
        }

        ProductSeriesMaster productSeriesMaster =
                productSeriesMasterService
                        .getRequiredActiveProductSeriesMaster(
                                request.getProductSeries());

        InstrumentTypeMaster instrumentTypeMaster =
                instrumentTypeMasterService
                        .getRequiredActiveInstrumentTypeMaster(
                                request.getInstrumentType());

        String seriesCode =
                productSeriesMaster.getSeriesCode();

        String instrumentCode =
                instrumentTypeMaster.getInstrumentCode();

        String generatedInternalModelCode =
                internalModelCodeService
                        .generateInternalModelCode(
                                seriesCode,
                                instrumentCode);

        String submittedInternalModelCode =
                normalizeInternalModelCode(
                        request.getInternalModelCode());

        if (!generatedInternalModelCode.equalsIgnoreCase(
                submittedInternalModelCode)) {
            throw new BusinessException(
                    "MES内部モデルコードが"
                    + "選択された製品シリーズと"
                    + "楽器タイプから生成される値と"
                    + "一致しません。"
                    + "生成されるコードは「"
                    + generatedInternalModelCode
                    + "」です。");
        }

        String productName =
                normalizeRequired(
                        request.getProductName(),
                        "製品名");

        String expectedBodyType =
                normalizeRequired(
                        instrumentTypeMaster.getBodyType(),
                        "楽器タイプのボディタイプ");

        String submittedBodyType =
                normalizeRequired(
                        request.getBodyType(),
                        "ボディタイプ");

        if (!expectedBodyType.equalsIgnoreCase(
                submittedBodyType)) {
            throw new BusinessException(
                    "ボディタイプが"
                    + "選択された楽器タイプと"
                    + "一致しません。"
                    + "正しいボディタイプは「"
                    + expectedBodyType
                    + "」です。");
        }

        String expectedNeckType =
                normalizeRequired(
                        instrumentTypeMaster.getNeckType(),
                        "楽器タイプのネックタイプ");

        String submittedNeckType =
                normalizeRequired(
                        request.getNeckType(),
                        "ネックタイプ");

        if (!expectedNeckType.equalsIgnoreCase(
                submittedNeckType)) {
            throw new BusinessException(
                    "ネックタイプが"
                    + "選択された楽器タイプと"
                    + "一致しません。"
                    + "正しいネックタイプは「"
                    + expectedNeckType
                    + "」です。");
        }

        BodyMaterialType bodyMaterialType =
                parseEnum(
                        request.getBodyMaterial(),
                        BodyMaterialType.class,
                        "ボディ材");

        NeckMaterialType neckMaterialType =
                parseEnum(
                        request.getNeckMaterial(),
                        NeckMaterialType.class,
                        "ネック材");

        FretCountType fretCountType =
                parseEnum(
                        request.getFretCount(),
                        FretCountType.class,
                        "フレット数");

        ScaleLengthType scaleLengthType =
                parseEnum(
                        request.getScale(),
                        ScaleLengthType.class,
                        "スケール");

        String pickupLayout =
                normalizeRequired(
                        request.getPickupLayout(),
                        "PU構成");

        List<ProductVariationRequest> requestVariations =
                request.getVariations();

        if (requestVariations == null
                || requestVariations.isEmpty()) {
            throw new BusinessException(
                    "製品バリエーションを1件以上入力してください。");
        }

        List<NormalizedVariation> variations =
                normalizeVariations(
                        requestVariations);

        return new NormalizedProductRequest(
                seriesCode,
                instrumentCode,
                generatedInternalModelCode,
                productName,
                expectedBodyType,
                bodyMaterialType.getLabel(),
                expectedNeckType,
                neckMaterialType.getLabel(),
                pickupLayout,
                fretCountType.getCount(),
                toScaleStorageValue(
                        scaleLengthType),
                variations);
    }

    private List<NormalizedVariation>
            normalizeVariations(
                    List<ProductVariationRequest>
                            requestVariations) {

        List<NormalizedVariation> variations =
                new ArrayList<>();

        Set<String> modelNumbers =
                new HashSet<>();

        Set<String> specificationKeys =
                new HashSet<>();

        for (int index = 0;
                index < requestVariations.size();
                index++) {

            ProductVariationRequest variation =
                    requestVariations.get(index);

            int displayNumber = index + 1;

            if (variation == null) {
                throw new BusinessException(
                        "バリエーション"
                        + displayNumber
                        + "の情報がありません。");
            }

            String modelNo =
                    normalizeRequired(
                            variation.getModelNo(),
                            "バリエーション"
                            + displayNumber
                            + "の公式モデル番号");

            String color =
                    normalizeRequired(
                            variation.getColor(),
                            "バリエーション"
                            + displayNumber
                            + "のカラー");

            FingerboardMaterialType
                    fingerboardMaterialType =
                    parseEnum(
                            variation
                                    .getFingerboardMaterial(),
                            FingerboardMaterialType.class,
                            "バリエーション"
                            + displayNumber
                            + "の指板材");

            String fingerboardMaterial =
                    fingerboardMaterialType.getLabel();

            String normalizedModelNo =
                    modelNo.toUpperCase(
                            Locale.ROOT);

            if (!modelNumbers.add(
                    normalizedModelNo)) {

                throw new BusinessException(
                        "公式モデル番号「"
                        + modelNo
                        + "」が入力内で重複しています。");
            }

            String specificationKey =
                    buildSpecificationKey(
                            color,
                            fingerboardMaterial);

            if (!specificationKeys.add(
                    specificationKey)) {

                throw new BusinessException(
                        "カラー「"
                        + color
                        + "」、指板材「"
                        + fingerboardMaterial
                        + "」の組み合わせが"
                        + "入力内で重複しています。");
            }

            variations.add(
                    new NormalizedVariation(
                            modelNo,
                            color,
                            fingerboardMaterial));
        }

        return variations;
    }

    private void validateProductDuplicates(
            NormalizedProductRequest request) {

        for (NormalizedVariation variation
                : request.variations()) {

            if (productRepository
                    .existsByModelNoIgnoreCase(
                            variation.modelNo())) {

                throw new BusinessException(
                        "公式モデル番号「"
                        + variation.modelNo()
                        + "」は既に登録されています。");
            }

            boolean specificationExists =
                    productRepository
                            .existsByInternalModelCodeIgnoreCaseAndColorIgnoreCaseAndFingerboardMaterialIgnoreCase(
                                    request.internalModelCode(),
                                    variation.color(),
                                    variation
                                            .fingerboardMaterial());

            if (specificationExists) {
                throw new BusinessException(
                        "MES内部モデルコード「"
                        + request.internalModelCode()
                        + "」、カラー「"
                        + variation.color()
                        + "」、指板材「"
                        + variation
                                .fingerboardMaterial()
                        + "」の製品は"
                        + "既に登録されています。");
            }
        }
    }

    private BodyMaster findOrCreateBodyMaster(
            NormalizedProductRequest request,
            NormalizedVariation variation) {

        return bodyMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                        request.internalModelCode(),
                        request.bodyType(),
                        request.bodyMaterial(),
                        variation.color())
                .orElseGet(() ->
                        createBodyMaster(
                                request,
                                variation));
    }

    private BodyMaster createBodyMaster(
            NormalizedProductRequest request,
            NormalizedVariation variation) {

        BodyMaster bodyMaster =
                new BodyMaster();

        bodyMaster.setModelCode(
                generateBodyMasterCode(
                        request.internalModelCode()));

        bodyMaster.setModelName(
                request.productName()
                + " Body");

        bodyMaster.setProductFamilyCode(
                request.internalModelCode());

        bodyMaster.setBodyType(
                request.bodyType());

        bodyMaster.setMaterial(
                request.bodyMaterial());

        bodyMaster.setColor(
                variation.color());

        return bodyMasterRepository.save(
                bodyMaster);
    }

    private NeckMaster findOrCreateNeckMaster(
            NormalizedProductRequest request,
            NormalizedVariation variation) {

        return neckMasterRepository
                .findFirstByProductFamilyCodeIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                        request.internalModelCode(),
                        request.neckType(),
                        request.neckMaterial(),
                        variation.fingerboardMaterial(),
                        request.fretCount(),
                        request.scale())
                .orElseGet(() ->
                        createNeckMaster(
                                request,
                                variation));
    }

    private NeckMaster createNeckMaster(
            NormalizedProductRequest request,
            NormalizedVariation variation) {

        String modelName =
                request.productName()
                + " Neck / "
                + variation.fingerboardMaterial()
                + " Fingerboard";

        NeckMaster neckMaster =
                new NeckMaster(
                        generateNeckMasterCode(
                                request.internalModelCode()),
                        modelName,
                        request.neckType(),
                        request.neckMaterial(),
                        variation.fingerboardMaterial(),
                        request.fretCount(),
                        request.scale());

        neckMaster.setProductFamilyCode(
                request.internalModelCode());

        return neckMasterRepository.save(
                neckMaster);
    }

    private ResolvedProductClassification
            resolveProductClassification(
                    String internalModelCode) {

        if (internalModelCode == null
                || internalModelCode.isBlank()) {
            return null;
        }

        String normalizedCode =
                internalModelCode.trim()
                        .toUpperCase(Locale.ROOT);

        for (InstrumentTypeMaster type
                : instrumentTypeMasterService
                        .getInstrumentTypeMasters()) {

            String suffix =
                    "-" + type.getInstrumentCode()
                            .trim()
                            .toUpperCase(Locale.ROOT);

            if (!normalizedCode.endsWith(suffix)) {
                continue;
            }

            String seriesCode =
                    normalizedCode.substring(
                            0,
                            normalizedCode.length()
                                    - suffix.length());

            if (seriesCode.isBlank()) {
                continue;
            }

            try {
                ProductSeriesMaster series =
                        productSeriesMasterService
                                .getRequiredProductSeriesMaster(
                                        seriesCode);
                return new ResolvedProductClassification(
                        series.getSeriesCode(),
                        type.getInstrumentCode());
            } catch (BusinessException exception) {
                continue;
            }
        }

        return null;
    }

    private String resolveBodyType(
            Product product) {

        if (product.getBodyMaster() == null) {
            return null;
        }

        return product
                .getBodyMaster()
                .getBodyType();
    }

    private String resolveNeckType(
            Product product) {

        if (product.getNeckMaster() == null) {
            return null;
        }

        return product
                .getNeckMaster()
                .getNeckType();
    }

    private String resolveBodyMaterialTypeName(
            String storedValue) {

        if (storedValue == null
                || storedValue.isBlank()) {

            return null;
        }

        String normalizedValue =
                storedValue.trim();

        for (BodyMaterialType type
                : BodyMaterialType.values()) {

            if (type.getLabel().equalsIgnoreCase(
                    normalizedValue)) {

                return type.name();
            }
        }

        return null;
    }

    private String resolveNeckMaterialTypeName(
            String storedValue) {

        if (storedValue == null
                || storedValue.isBlank()) {

            return null;
        }

        String normalizedValue =
                storedValue.trim();

        for (NeckMaterialType type
                : NeckMaterialType.values()) {

            if (type.getLabel().equalsIgnoreCase(
                    normalizedValue)) {

                return type.name();
            }
        }

        return null;
    }

    private String resolveFingerboardMaterialTypeName(
            String storedValue) {

        if (storedValue == null
                || storedValue.isBlank()) {

            return null;
        }

        String normalizedValue =
                storedValue.trim();

        for (FingerboardMaterialType type
                : FingerboardMaterialType.values()) {

            if (type.getLabel().equalsIgnoreCase(
                    normalizedValue)) {

                return type.name();
            }
        }

        return null;
    }

    private String resolveScaleLengthTypeName(
            String storedValue) {

        if (storedValue == null
                || storedValue.isBlank()) {

            return null;
        }

        String normalizedValue =
                storedValue.trim();

        for (ScaleLengthType type
                : ScaleLengthType.values()) {

            if (type.getValue().equalsIgnoreCase(
                    normalizedValue)) {

                return type.name();
            }
        }

        return null;
    }

    private String generateBodyMasterCode(
            String internalModelCode) {

        String prefix =
                "BM-"
                + internalModelCode
                + "-";

        return generateMasterCode(
                prefix,
                bodyMasterRepository
                        .findTopByModelCodeStartingWithOrderByIdDesc(
                                prefix)
                        .map(
                                BodyMaster::getModelCode)
                        .orElse(null),
                bodyMasterRepository
                        ::existsByModelCodeIgnoreCase);
    }

    private String generateNeckMasterCode(
            String internalModelCode) {

        String prefix =
                "NM-"
                + internalModelCode
                + "-";

        return generateMasterCode(
                prefix,
                neckMasterRepository
                        .findTopByModelCodeStartingWithOrderByIdDesc(
                                prefix)
                        .map(
                                NeckMaster::getModelCode)
                        .orElse(null),
                neckMasterRepository
                        ::existsByModelCodeIgnoreCase);
    }

    private String generateMasterCode(
            String prefix,
            String lastCode,
            CodeExistenceChecker checker) {

        int nextNumber =
                extractNextSequence(
                        prefix,
                        lastCode);

        String candidate;

        do {
            candidate =
                    prefix
                    + String.format(
                            "%0"
                            + MASTER_CODE_SEQUENCE_LENGTH
                            + "d",
                            nextNumber);

            nextNumber++;
        } while (checker.exists(candidate));

        return candidate;
    }

    private int extractNextSequence(
            String prefix,
            String lastCode) {

        if (lastCode == null
                || !lastCode.startsWith(prefix)) {

            return 1;
        }

        String sequenceText =
                lastCode.substring(
                        prefix.length());

        try {
            return Integer.parseInt(
                    sequenceText)
                    + 1;
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private String normalizeInternalModelCode(
            String value) {

        String normalized =
                normalizeRequired(
                        value,
                        "MES内部モデルコード")
                .toUpperCase(
                        Locale.ROOT)
                .replaceAll(
                        "\\s+",
                        "-");

        if (!normalized.matches(
                "[A-Z0-9-]+")) {

            throw new BusinessException(
                    "MES内部モデルコードは"
                    + "半角英数字とハイフンで"
                    + "入力してください。");
        }

        if (normalized.startsWith("-")
                || normalized.endsWith("-")
                || normalized.contains("--")) {

            throw new BusinessException(
                    "MES内部モデルコードの"
                    + "ハイフンの位置が不正です。");
        }

        return normalized;
    }

    private String normalizeRequired(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {

            throw new BusinessException(
                    fieldName
                    + "を入力してください。");
        }

        return value.trim();
    }

    private Integer validateFretCount(
            Integer fretCount) {

        if (fretCount == null
                || fretCount <= 0) {

            throw new BusinessException(
                    "フレット数は1以上で"
                    + "入力してください。");
        }

        return fretCount;
    }

    private String toScaleStorageValue(
            ScaleLengthType scaleLengthType) {

        return scaleLengthType.getValue();
    }

    private <E extends Enum<E>> E parseEnum(
            String value,
            Class<E> enumType,
            String fieldName) {

        String normalized =
                normalizeRequired(
                        value,
                        fieldName);

        try {
            return Enum.valueOf(
                    enumType,
                    normalized.toUpperCase(
                            Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    fieldName
                    + "に不正な値が指定されています。");
        }
    }

    private FretCountType parseEnum(
            Integer value,
            Class<FretCountType> enumType,
            String fieldName) {

        if (value == null) {
            throw new BusinessException(
                    fieldName
                    + "を選択してください。");
        }

        for (FretCountType fretCountType
                : enumType.getEnumConstants()) {

            if (fretCountType.getCount()
                    == value) {

                return fretCountType;
            }
        }

        throw new BusinessException(
                fieldName
                + "に不正な値が指定されています。");
    }

    private String buildSpecificationKey(
            String color,
            String fingerboardMaterial) {

        return color
                .trim()
                .toUpperCase(
                        Locale.ROOT)
                + "\u0000"
                + fingerboardMaterial
                        .trim()
                        .toUpperCase(
                                Locale.ROOT);
    }

    @FunctionalInterface
    private interface CodeExistenceChecker {

        boolean exists(String modelCode);
    }

    private record ResolvedProductClassification(
            String seriesCode,
            String instrumentCode) {
    }

    private record NormalizedProductUpdate(
            String seriesCode,
            String instrumentCode,
            String internalModelCode,
            String productName,
            String bodyType,
            String bodyMaterial,
            String neckType,
            String neckMaterial,
            String pickupLayout,
            Integer fretCount,
            String scale,
            String modelNo,
            String color,
            String fingerboardMaterial) {
    }

    private record NormalizedVariation(
            String modelNo,
            String color,
            String fingerboardMaterial) {
    }

    private record NormalizedProductRequest(
            String seriesCode,
            String instrumentCode,
            String internalModelCode,
            String productName,
            String bodyType,
            String bodyMaterial,
            String neckType,
            String neckMaterial,
            String pickupLayout,
            Integer fretCount,
            String scale,
            List<NormalizedVariation> variations) {
    }
}