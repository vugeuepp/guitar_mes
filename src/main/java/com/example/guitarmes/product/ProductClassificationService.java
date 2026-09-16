package com.example.guitarmes.product;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMaster;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMasterService;
import com.example.guitarmes.master.productseries.ProductSeriesMaster;
import com.example.guitarmes.master.productseries.ProductSeriesMasterService;

/** 既存の内部モデルコードを登録済みシリーズ・楽器コードへ解決する。表示名は使用しない。 */
@Service
@Transactional(readOnly = true)
public class ProductClassificationService {

    private final ProductSeriesMasterService productSeriesMasterService;
    private final InstrumentTypeMasterService instrumentTypeMasterService;

    public ProductClassificationService(ProductSeriesMasterService productSeriesMasterService,
            InstrumentTypeMasterService instrumentTypeMasterService) {
        this.productSeriesMasterService = productSeriesMasterService;
        this.instrumentTypeMasterService = instrumentTypeMasterService;
    }

    public record Classification(String seriesCode, String instrumentCode) {
    }

    public Optional<Classification> classify(String internalModelCode) {

        if (internalModelCode == null
                || internalModelCode.isBlank()) {
            return Optional.empty();
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
                return Optional.of(new Classification(
                        series.getSeriesCode(),
                        type.getInstrumentCode()));
            } catch (BusinessException exception) {
                continue;
            }
        }

        return Optional.empty();
    }

}
