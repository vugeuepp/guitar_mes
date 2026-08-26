package com.example.guitarmes.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.master.InstrumentType;
import com.example.guitarmes.master.ProductSeries;

@Service
public class InternalModelCodeService {

    public String generateInternalModelCode(
            ProductSeries productSeries,
            InstrumentType instrumentType) {

        if (productSeries == null) {
            throw new BusinessException(
                    "製品シリーズを選択してください。");
        }

        if (instrumentType == null) {
            throw new BusinessException(
                    "楽器タイプを選択してください。");
        }

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

        return productSeries.getCode()
                + "-"
                + instrumentType.getCode();
    }

    public ProductSeries resolveProductSeries(
            String productName) {

        String normalizedName =
                normalizeProductName(productName);

        if (normalizedName.contains(
                "MADE IN JAPAN HYBRID II")) {

            return ProductSeries
                    .MADE_IN_JAPAN_HYBRID_II;
        }

        if (normalizedName.contains(
                "MADE IN JAPAN HERITAGE 50S")) {

            return ProductSeries
                    .MADE_IN_JAPAN_HERITAGE_50S;
        }

        if (normalizedName.contains(
                "MADE IN JAPAN TRADITIONAL "
                + "ORIGINAL 50S")) {

            return ProductSeries
                    .MADE_IN_JAPAN_TRADITIONAL_ORIGINAL_50S;
        }

        if (containsTraditionalEra(
                normalizedName,
                "50S")) {

            return ProductSeries
                    .MADE_IN_JAPAN_TRADITIONAL_50S;
        }

        if (containsTraditionalEra(
                normalizedName,
                "60S")) {

            return ProductSeries
                    .MADE_IN_JAPAN_TRADITIONAL_60S;
        }

        if (containsTraditionalEra(
                normalizedName,
                "70S")) {

            return ProductSeries
                    .MADE_IN_JAPAN_TRADITIONAL_70S;
        }

        return ProductSeries.OTHER;
    }

    public InstrumentType resolveInstrumentType(
            String productName) {

        String normalizedName =
                normalizeProductName(productName);

        if (normalizedName.contains(
                "TELECASTER CUSTOM")) {

            return InstrumentType.TELECASTER_CUSTOM;
        }

        if (normalizedName.contains(
                "PRECISION BASS")) {

            return InstrumentType.PRECISION_BASS;
        }

        if (normalizedName.contains(
                "JAZZ BASS")) {

            return InstrumentType.JAZZ_BASS;
        }

        if (normalizedName.contains(
                "STRATOCASTER")) {

            return InstrumentType.STRATOCASTER;
        }

        if (normalizedName.contains(
                "TELECASTER")) {

            return InstrumentType.TELECASTER;
        }

        if (normalizedName.contains(
                "JAZZMASTER")) {

            return InstrumentType.JAZZMASTER;
        }

        if (normalizedName.contains(
                "JAGUAR")) {

            return InstrumentType.JAGUAR;
        }

        if (normalizedName.contains(
                "MUSTANG")) {

            return InstrumentType.MUSTANG;
        }

        return InstrumentType.OTHER;
    }

    public String generateFromProductName(
            String productName) {

        ProductSeries productSeries =
                resolveProductSeries(productName);

        InstrumentType instrumentType =
                resolveInstrumentType(productName);

        return generateInternalModelCode(
                productSeries,
                instrumentType);
    }

    public String resolveBodyType(
            String productName) {

        InstrumentType instrumentType =
                resolveInstrumentType(productName);

        if (instrumentType == InstrumentType.OTHER) {
            throw new BusinessException(
                    "製品名からボディタイプを"
                    + "判定できませんでした。");
        }

        return instrumentType.getBodyType();
    }

    public String resolveNeckType(
            String productName) {

        InstrumentType instrumentType =
                resolveInstrumentType(productName);

        if (instrumentType == InstrumentType.OTHER) {
            throw new BusinessException(
                    "製品名からネックタイプを"
                    + "判定できませんでした。");
        }

        return instrumentType.getNeckType();
    }

    private boolean containsTraditionalEra(
            String productName,
            String era) {

        return productName.contains(
                "MADE IN JAPAN TRADITIONAL")
                && productName.contains(era);
    }

    private String normalizeProductName(
            String productName) {

        if (productName == null
                || productName.isBlank()) {

            throw new BusinessException(
                    "製品名を入力してください。");
        }

        return productName
                .trim()
                .replace(
                        "'",
                        "")
                .replace(
                        "’",
                        "")
                .replaceAll(
                        "\\s+",
                        " ")
                .toUpperCase(
                        Locale.ROOT);
    }
}