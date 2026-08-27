package com.example.guitarmes.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.entity.ProductSeriesMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.repository.ProductSeriesMasterRepository;

@Service
public class ProductSeriesMasterService {

    private final ProductSeriesMasterRepository
            productSeriesMasterRepository;

    public ProductSeriesMasterService(
            ProductSeriesMasterRepository
                    productSeriesMasterRepository) {

        this.productSeriesMasterRepository =
                productSeriesMasterRepository;
    }

    public List<ProductSeriesMaster>
            getProductSeriesMasters() {

        return productSeriesMasterRepository
                .findAllByOrderBySeriesNameAsc();
    }

    public List<ProductSeriesMaster>
            getActiveProductSeriesMasters() {

        return productSeriesMasterRepository
                .findByActiveTrueOrderBySeriesNameAsc();
    }

    @Transactional
    public ProductSeriesMaster
            createProductSeriesMaster(
                    ProductSeriesMaster request) {

        if (request == null) {
            throw new BusinessException(
                    "製品シリーズ情報が"
                    + "指定されていません。");
        }

        String seriesCode =
                normalizeSeriesCode(
                        request.getSeriesCode());

        String seriesName =
                normalizeRequired(
                        request.getSeriesName(),
                        "シリーズ名");

        if (productSeriesMasterRepository
                .existsBySeriesCodeIgnoreCase(
                        seriesCode)) {

            throw new BusinessException(
                    "シリーズコード「"
                    + seriesCode
                    + "」は既に登録されています。");
        }

        ProductSeriesMaster productSeriesMaster =
                new ProductSeriesMaster();

        productSeriesMaster.setSeriesCode(
                seriesCode);

        productSeriesMaster.setSeriesName(
                seriesName);

        productSeriesMaster.setActive(
                true);

        return productSeriesMasterRepository.save(
                productSeriesMaster);
    }

    private String normalizeSeriesCode(
            String value) {

        String normalized =
                normalizeRequired(
                        value,
                        "シリーズコード")
                .toUpperCase(
                        Locale.ROOT)
                .replaceAll(
                        "\\s+",
                        "-");

        if (!normalized.matches(
                "[A-Z0-9-]+")) {

            throw new BusinessException(
                    "シリーズコードは"
                    + "半角英数字とハイフンで"
                    + "入力してください。");
        }

        if (normalized.startsWith("-")
                || normalized.endsWith("-")
                || normalized.contains("--")) {

            throw new BusinessException(
                    "シリーズコードの"
                    + "ハイフンの位置が不正です。");
        }

        if (normalized.length() > 50) {
            throw new BusinessException(
                    "シリーズコードは50文字以内で"
                    + "入力してください。");
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
}
