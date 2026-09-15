package com.example.guitarmes.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.product.parts.ProductPartsSpecRequest;
import com.example.guitarmes.product.parts.ProductPartsSpecService;

/** フォーム操作全体のtransaction。個々の業務validationは既存Serviceを利用する。 */
@Service
public class ProductFormService {
    private final ProductService products;
    private final ProductPartsSpecService parts;

    public ProductFormService(ProductService products, ProductPartsSpecService parts) {
        this.products = products;
        this.parts = parts;
    }

    @Transactional
    public List<Product> createProductVariations(ProductVariationCreateRequest request) {
        List<Product> created = products.createProductVariations(request);
        if (!request.getPartsSpec().isEmpty()) {
            for (Product product : created) {
                parts.create(product.getId(), request.getPartsSpec());
            }
        }
        return created;
    }

    @Transactional(readOnly = true)
    public ProductUpdateRequest getProductUpdateRequest(Long productId) {
        ProductUpdateRequest request = products.getProductUpdateRequest(productId);
        parts.findByProductId(productId).ifPresent(spec ->
                request.setPartsSpec(ProductPartsSpecRequest.from(spec)));
        return request;
    }

    @Transactional(readOnly = true)
    public boolean isPartsLocked(Long productId) {
        if (parts.findByProductId(productId).isEmpty()) {
            return false;
        }
        try {
            products.validateManufacturingSpecificationChange(productId);
            return false;
        } catch (BusinessException exception) {
            return true;
        }
    }

    @Transactional
    public Product updateProduct(Long productId, ProductUpdateRequest request) {
        Product updated = products.updateProduct(productId, request);
        ProductPartsSpecRequest input = request.getPartsSpec();
        if (parts.findByProductId(productId).isEmpty()) {
            if (!input.isEmpty()) {
                parts.create(productId, input);
            }
        } else if (!input.isEmpty() || !isPartsLocked(productId)) {
            // disabled欄の未送信は既存値を維持する。値が送られた場合は必ず更新制限を通す。
            parts.update(productId, input);
        }
        return updated;
    }
}
