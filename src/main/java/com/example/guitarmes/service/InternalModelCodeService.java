package com.example.guitarmes.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.guitarmes.exception.BusinessException;

@Service
public class InternalModelCodeService {

    public String generateInternalModelCode(
            String seriesCode,
            String instrumentCode) {

        String normalizedSeriesCode =
                normalizeCode(
                        seriesCode,
                        "製品シリーズ");

        String normalizedInstrumentCode =
                normalizeCode(
                        instrumentCode,
                        "楽器タイプ");

        String internalModelCode =
                normalizedSeriesCode
                + "-"
                + normalizedInstrumentCode;

        if (internalModelCode.length() > 50) {
            throw new BusinessException(
                    "生成されるMES内部モデルコードが"
                    + "50文字を超えています。"
                    + "製品シリーズコードと"
                    + "楽器タイプコードを"
                    + "確認してください。");
        }

        return internalModelCode;
    }

    private String normalizeCode(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {
            throw new BusinessException(
                    fieldName
                    + "を選択してください。");
        }

        String normalized =
                value
                        .trim()
                        .toUpperCase(
                                Locale.ROOT)
                        .replaceAll(
                                "\\s+",
                                "-");

        if (!normalized.matches(
                "[A-Z0-9-]+")) {
            throw new BusinessException(
                    fieldName
                    + "コードは半角英数字とハイフンで"
                    + "指定してください。");
        }

        if (normalized.startsWith("-")
                || normalized.endsWith("-")
                || normalized.contains("--")) {
            throw new BusinessException(
                    fieldName
                    + "コードのハイフンの位置が不正です。");
        }

        return normalized;
    }
}
