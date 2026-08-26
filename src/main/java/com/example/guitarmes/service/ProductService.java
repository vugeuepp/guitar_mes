package com.example.guitarmes.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.dto.ProductVariationCreateRequest;
import com.example.guitarmes.dto.ProductVariationRequest;
import com.example.guitarmes.entity.BodyMaster;
import com.example.guitarmes.entity.NeckMaster;
import com.example.guitarmes.entity.Product;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.master.BodyMaterialType;
import com.example.guitarmes.master.FingerboardMaterialType;
import com.example.guitarmes.master.FretCountType;
import com.example.guitarmes.master.InstrumentType;
import com.example.guitarmes.master.NeckMaterialType;
import com.example.guitarmes.master.ProductSeries;
import com.example.guitarmes.master.ScaleLengthType;
import com.example.guitarmes.repository.BodyMasterRepository;
import com.example.guitarmes.repository.NeckMasterRepository;
import com.example.guitarmes.repository.ProductRepository;

@Service
public class ProductService {

    private static final int MASTER_CODE_SEQUENCE_LENGTH = 4;

    private final ProductRepository productRepository;

    private final BodyMasterRepository bodyMasterRepository;

    private final NeckMasterRepository neckMasterRepository;

    private final InternalModelCodeService internalModelCodeService;

    public ProductService(
            ProductRepository productRepository,
            BodyMasterRepository bodyMasterRepository,
            NeckMasterRepository neckMasterRepository,
            InternalModelCodeService internalModelCodeService) {

        this.productRepository = productRepository;
        this.bodyMasterRepository = bodyMasterRepository;
        this.neckMasterRepository = neckMasterRepository;
        this.internalModelCodeService =
                internalModelCodeService;
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

    /*
     * 従来の単一Product登録処理。
     *
     * Product登録画面をバリエーション登録方式へ
     * 移行するまで既存機能を維持する。
     */
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

    /*
     * Product、BodyMaster、NeckMasterを
     * バリエーション単位で一括登録する。
     */
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

    private NormalizedProductRequest
            normalizeAndValidateRequest(
                    ProductVariationCreateRequest request) {

        if (request == null) {
            throw new BusinessException(
                    "製品登録情報が指定されていません。");
        }

        ProductSeries productSeries =
                parseEnum(
                        request.getProductSeries(),
                        ProductSeries.class,
                        "製品シリーズ");

        InstrumentType instrumentType =
                parseEnum(
                        request.getInstrumentType(),
                        InstrumentType.class,
                        "楽器タイプ");

        validateAutomaticGenerationTarget(
                productSeries,
                instrumentType);

        String generatedInternalModelCode =
                internalModelCodeService
                        .generateInternalModelCode(
                                productSeries,
                                instrumentType);

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
                        instrumentType.getBodyType(),
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
                        instrumentType.getNeckType(),
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

        List<ProductVariationRequest>
                requestVariations =
                request.getVariations();

        if (requestVariations == null
                || requestVariations.isEmpty()) {

            throw new BusinessException(
                    "製品バリエーションを"
                    + "1件以上入力してください。");
        }

        List<NormalizedVariation> variations =
                normalizeVariations(
                        requestVariations);

        return new NormalizedProductRequest(
                productSeries,
                instrumentType,
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

    private void validateAutomaticGenerationTarget(
            ProductSeries productSeries,
            InstrumentType instrumentType) {

        if (productSeries == ProductSeries.OTHER) {
            throw new BusinessException(
                    "その他の製品シリーズでは"
                    + "MES内部モデルコードを"
                    + "自動生成できません。");
        }

        if (instrumentType == InstrumentType.OTHER) {
            throw new BusinessException(
                    "その他の楽器タイプでは"
                    + "MES内部モデルコードを"
                    + "自動生成できません。");
        }
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

        String modelName =
                request.productName()
                + " Body";

        return bodyMasterRepository
                .findFirstByModelNameIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                        modelName,
                        request.bodyType(),
                        request.bodyMaterial(),
                        variation.color())
                .orElseGet(() ->
                        createBodyMaster(
                                request,
                                variation,
                                modelName));
    }

    private BodyMaster createBodyMaster(
            NormalizedProductRequest request,
            NormalizedVariation variation,
            String modelName) {

        BodyMaster bodyMaster =
                new BodyMaster();

        bodyMaster.setModelCode(
                generateBodyMasterCode(
                        request.internalModelCode()));

        bodyMaster.setModelName(
                modelName);

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

        String modelName =
                request.productName()
                + " Neck / "
                + variation.fingerboardMaterial()
                + " Fingerboard";

        return neckMasterRepository
                .findFirstByModelNameIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                        modelName,
                        request.neckType(),
                        request.neckMaterial(),
                        variation.fingerboardMaterial(),
                        request.fretCount(),
                        request.scale())
                .orElseGet(() ->
                        createNeckMaster(
                                request,
                                variation,
                                modelName));
    }

    private NeckMaster createNeckMaster(
            NormalizedProductRequest request,
            NormalizedVariation variation,
            String modelName) {

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

        return neckMasterRepository.save(
                neckMaster);
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

        return scaleLengthType.getMillimeters()
                + "mm";
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

    private record NormalizedVariation(
            String modelNo,
            String color,
            String fingerboardMaterial) {
    }

    private record NormalizedProductRequest(
            ProductSeries productSeries,
            InstrumentType instrumentType,
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