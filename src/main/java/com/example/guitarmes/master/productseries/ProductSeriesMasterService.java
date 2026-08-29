package com.example.guitarmes.master.productseries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;

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

    public List<ProductSeriesMaster>
            getProductSeriesMastersForEdit(
                    String currentSeriesCode) {

        List<ProductSeriesMaster> result =
                new ArrayList<>(
                        getActiveProductSeriesMasters());

        if (currentSeriesCode == null
                || currentSeriesCode.isBlank()) {
            return result;
        }

        productSeriesMasterRepository
                .findBySeriesCodeIgnoreCase(
                        currentSeriesCode.trim())
                .filter(series ->
                        !Boolean.TRUE.equals(
                                series.getActive()))
                .filter(series ->
                        result.stream().noneMatch(active ->
                                active.getSeriesCode()
                                        .equalsIgnoreCase(
                                                series.getSeriesCode())))
                .ifPresent(result::add);

        result.sort(
                Comparator.comparing(
                        ProductSeriesMaster::getSeriesName,
                        String.CASE_INSENSITIVE_ORDER));

        return result;
    }

    public ProductSeriesMaster
            getRequiredProductSeriesMaster(
                    String seriesCode) {

        String normalizedSeriesCode =
                normalizeSeriesCode(
                        seriesCode);

        return productSeriesMasterRepository
                .findBySeriesCodeIgnoreCase(
                        normalizedSeriesCode)
                .orElseThrow(() ->
                        new BusinessException(
                                "製品シリーズ「"
                                + normalizedSeriesCode
                                + "」は登録されていません。"));
    }

    public ProductSeriesMaster
            getRequiredActiveProductSeriesMaster(
                    String seriesCode) {

        ProductSeriesMaster productSeriesMaster =
                getRequiredProductSeriesMaster(
                        seriesCode);

        validateActive(
                productSeriesMaster);

        return productSeriesMaster;
    }

    public ProductSeriesMaster
            getRequiredProductSeriesMasterForUpdate(
                    String requestedSeriesCode,
                    String currentSeriesCode) {

        ProductSeriesMaster requestedSeries =
                getRequiredProductSeriesMaster(
                        requestedSeriesCode);

        if (Boolean.TRUE.equals(
                requestedSeries.getActive())) {
            return requestedSeries;
        }

        String normalizedCurrentSeriesCode =
                normalizeSeriesCode(
                        currentSeriesCode);

        if (!requestedSeries.getSeriesCode()
                .equalsIgnoreCase(
                        normalizedCurrentSeriesCode)) {

            throw new BusinessException(
                    "製品シリーズ「"
                    + requestedSeries.getSeriesName()
                    + "」は現在無効です。"
                    + "別の有効な製品シリーズを"
                    + "選択してください。");
        }

        return requestedSeries;
    }

    public ProductSeriesMaster
            getProductSeriesMasterById(
                    Long id) {

        if (id == null) {
            throw new BusinessException(
                    "製品シリーズIDが指定されていません。");
        }

        return productSeriesMasterRepository
                .findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "指定された製品シリーズが存在しません。"));
    }

    @Transactional
    public ProductSeriesMaster
            updateProductSeriesMaster(
                    Long id,
                    ProductSeriesMaster request) {

        if (request == null) {
            throw new BusinessException(
                    "製品シリーズ情報が指定されていません。");
        }

        ProductSeriesMaster current =
                getProductSeriesMasterById(id);
        String seriesName =
                normalizeRequired(
                        request.getSeriesName(),
                        "シリーズ名");
        validateSeriesNameLength(seriesName);

        current.setSeriesName(seriesName);
        return productSeriesMasterRepository.save(current);
    }

    @Transactional
    public ProductSeriesMaster
            toggleProductSeriesMasterActive(
                    Long id) {

        ProductSeriesMaster current =
                getProductSeriesMasterById(id);
        current.setActive(
                !Boolean.TRUE.equals(current.getActive()));
        return productSeriesMasterRepository.save(current);
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
        validateSeriesNameLength(seriesName);

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

    private void validateActive(
            ProductSeriesMaster productSeriesMaster) {

        if (!Boolean.TRUE.equals(
                productSeriesMaster.getActive())) {

            throw new BusinessException(
                    "製品シリーズ「"
                    + productSeriesMaster.getSeriesName()
                    + "」は現在無効です。"
                    + "有効な製品シリーズを"
                    + "選択してください。");
        }
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

    private void validateSeriesNameLength(
            String seriesName) {

        if (seriesName.length() > 150) {
            throw new BusinessException(
                    "シリーズ名は150文字以内で"
                    + "入力してください。");
        }
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
