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

    private final ProductPartsSpecValidator validator;

    public ProductPartsSpecService(
            ProductService productService,
            ProductPartsSpecRepository repository,
            ProductPartsSpecValidator validator) {

        this.validator = validator;
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
        ProductPartsSpecRequest normalized = validator.normalizeAndValidate(request);
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
        ProductPartsSpecRequest normalized = validator.normalizeAndValidate(request);
        productService.validateManufacturingSpecificationChange(productId);
        apply(spec, normalized);
        return repository.saveAndFlush(spec);
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
