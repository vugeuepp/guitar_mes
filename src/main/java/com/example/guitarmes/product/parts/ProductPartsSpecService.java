package com.example.guitarmes.product.parts;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductService;

@Service
public class ProductPartsSpecService {

    private final ProductService productService;
    private final ProductPartsSpecRepository repository;

    public ProductPartsSpecService(
            ProductService productService,
            ProductPartsSpecRepository repository) {

        this.productService = productService;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        if (productId == null) {
            throw new BusinessException("製品IDを指定してください。");
        }
        return productService.getProductById(productId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductPartsSpec> findByProductId(Long productId) {
        getProductById(productId);
        return repository.findByProductId(productId);
    }

    @Transactional
    public ProductPartsSpec create(Long productId, ProductPartsSpecRequest request) {
        Product product = getProductById(productId);
        if (repository.existsByProductId(productId)) {
            throw new BusinessException("パーツ取付仕様は既に登録されています。");
        }
        ProductPartsSpecRequest normalized = normalizeAndValidate(request);
        ProductPartsSpec spec = new ProductPartsSpec();
        spec.setProduct(product);
        apply(spec, normalized);
        // Specなしなら製造実績に関係なく初回補完を許可する。仕様推測は行わない。
        // 同時登録はDBのUNIQUEで拒否。整合性例外は既存方式に従い伝播・ロールバックする。
        return repository.saveAndFlush(spec);
    }

    @Transactional
    public ProductPartsSpec update(Long productId, ProductPartsSpecRequest request) {
        getProductById(productId);
        ProductPartsSpec spec = repository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("パーツ取付仕様が登録されていません。"));
        ProductPartsSpecRequest normalized = normalizeAndValidate(request);
        productService.validateManufacturingSpecificationChange(productId);
        apply(spec, normalized);
        return repository.saveAndFlush(spec);
    }

    private ProductPartsSpecRequest normalizeAndValidate(ProductPartsSpecRequest request) {
        if (request == null) {
            throw new BusinessException("パーツ取付仕様を入力してください。");
        }
        ProductPartsSpecRequest normalized = new ProductPartsSpecRequest();
        BridgeType bridgeType = required(request.getBridgeType(), "ブリッジ方式");
        Boolean expansion = required(request.getRequiresStudHoleExpansion(), "スタッド穴拡張要否");
        if (bridgeType == BridgeType.SIX_POINT && expansion) {
            throw new BusinessException("6点支持ではスタッド穴拡張要否を不要にしてください。");
        }
        normalized.setBridgeType(bridgeType);
        normalized.setRequiresStudHoleExpansion(expansion);
        normalized.setBridgeModel(text(request.getBridgeModel(), "ブリッジ型番", 255, false));
        normalized.setTunerModel(text(request.getTunerModel(), "ペグ型番", 255, true));
        TunerMountingType mounting = required(request.getTunerMountingType(), "ペグ取付方式");
        Boolean bush = required(request.getTunerBushRequired(), "ペグブッシュ要否");
        if (mounting == TunerMountingType.PRESS_BUSHING && !bush) {
            throw new BusinessException("圧入ブッシュ式ではペグブッシュが必要です。");
        }
        normalized.setTunerMountingType(mounting);
        normalized.setTunerBushRequired(bush);
        normalized.setTunerLayout(required(request.getTunerLayout(), "ペグ配列"));
        Integer positions = required(request.getSelectorPositions(), "セレクターポジション数");
        if (positions <= 0) {
            throw new BusinessException("セレクターポジション数は1以上で入力してください。");
        }
        normalized.setSelectorPositions(positions);
        normalized.setControlLayout(text(request.getControlLayout(), "コントロール構成", 255, true));
        normalized.setJackMountingType(required(request.getJackMountingType(), "ジャック取付形状"));
        normalized.setStringMaker(text(request.getStringMaker(), "弦メーカー", 150, false));
        normalized.setStringModel(text(request.getStringModel(), "指定弦", 255, true));
        normalized.setStringGauge(text(request.getStringGauge(), "弦ゲージ", 100, true));
        return normalized;
    }

    private <T> T required(T value, String label) {
        if (value == null) {
            throw new BusinessException(label + "を入力してください。");
        }
        return value;
    }

    private String text(String value, String label, int maxLength, boolean mandatory) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            if (mandatory) {
                throw new BusinessException(label + "を入力してください。");
            }
            return null;
        }
        // PostgreSQL varcharの文字数に合わせ、補助文字も1文字として数える。
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new BusinessException(label + "は" + maxLength + "文字以内で入力してください。");
        }
        return normalized;
    }

    private void apply(ProductPartsSpec spec, ProductPartsSpecRequest request) {
        spec.setBridgeType(request.getBridgeType());
        spec.setBridgeModel(request.getBridgeModel());
        spec.setRequiresStudHoleExpansion(request.getRequiresStudHoleExpansion());
        spec.setTunerModel(request.getTunerModel());
        spec.setTunerMountingType(request.getTunerMountingType());
        spec.setTunerBushRequired(request.getTunerBushRequired());
        spec.setTunerLayout(request.getTunerLayout());
        spec.setSelectorPositions(request.getSelectorPositions());
        spec.setControlLayout(request.getControlLayout());
        spec.setJackMountingType(request.getJackMountingType());
        spec.setStringMaker(request.getStringMaker());
        spec.setStringModel(request.getStringModel());
        spec.setStringGauge(request.getStringGauge());
    }
}
