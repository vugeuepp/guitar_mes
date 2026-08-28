package com.example.guitarmes.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.entity.InstrumentTypeMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.repository.InstrumentTypeMasterRepository;

@Service
public class InstrumentTypeMasterService {

    private final InstrumentTypeMasterRepository
            instrumentTypeMasterRepository;

    public InstrumentTypeMasterService(
            InstrumentTypeMasterRepository
                    instrumentTypeMasterRepository) {

        this.instrumentTypeMasterRepository =
                instrumentTypeMasterRepository;
    }

    public List<InstrumentTypeMaster>
            getInstrumentTypeMasters() {

        return instrumentTypeMasterRepository
                .findAllByOrderByInstrumentNameAsc();
    }

    public List<InstrumentTypeMaster>
            getActiveInstrumentTypeMasters() {

        return instrumentTypeMasterRepository
                .findByActiveTrueOrderByInstrumentNameAsc();
    }

    public InstrumentTypeMaster
            getRequiredActiveInstrumentTypeMaster(
                    String instrumentCode) {

        String normalizedInstrumentCode =
                normalizeInstrumentCode(
                        instrumentCode);

        InstrumentTypeMaster instrumentTypeMaster =
                instrumentTypeMasterRepository
                        .findByInstrumentCodeIgnoreCase(
                                normalizedInstrumentCode)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "楽器タイプ「"
                                        + normalizedInstrumentCode
                                        + "」は登録されていません。"));

        if (!Boolean.TRUE.equals(
                instrumentTypeMaster.getActive())) {

            throw new BusinessException(
                    "楽器タイプ「"
                    + instrumentTypeMaster.getInstrumentName()
                    + "」は現在無効です。"
                    + "有効な楽器タイプを"
                    + "選択してください。");
        }

        return instrumentTypeMaster;
    }

    public List<InstrumentTypeMaster>
            getInstrumentTypeMastersForEdit(
                    String currentInstrumentCode) {

        String normalizedCurrentCode =
                normalizeInstrumentCode(
                        currentInstrumentCode);

        List<InstrumentTypeMaster> activeTypes =
                new java.util.ArrayList<>(
                        getActiveInstrumentTypeMasters());

        InstrumentTypeMaster currentType =
                getRequiredInstrumentTypeMaster(
                        normalizedCurrentCode);

        boolean alreadyIncluded =
                activeTypes.stream()
                        .anyMatch(type ->
                                type.getInstrumentCode()
                                        .equalsIgnoreCase(
                                                normalizedCurrentCode));

        if (!alreadyIncluded) {
            activeTypes.add(currentType);
            activeTypes.sort(
                    java.util.Comparator.comparing(
                            InstrumentTypeMaster::getInstrumentName,
                            String.CASE_INSENSITIVE_ORDER));
        }

        return activeTypes;
    }

    public InstrumentTypeMaster
            getRequiredInstrumentTypeMaster(
                    String instrumentCode) {

        String normalizedInstrumentCode =
                normalizeInstrumentCode(
                        instrumentCode);

        return instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase(
                        normalizedInstrumentCode)
                .orElseThrow(() ->
                        new BusinessException(
                                "楽器タイプ「"
                                + normalizedInstrumentCode
                                + "」は登録されていません。"));
    }

    public InstrumentTypeMaster
            getRequiredInstrumentTypeMasterForUpdate(
                    String requestedInstrumentCode,
                    String currentInstrumentCode) {

        String normalizedRequestedCode =
                normalizeInstrumentCode(
                        requestedInstrumentCode);
        String normalizedCurrentCode =
                normalizeInstrumentCode(
                        currentInstrumentCode);

        InstrumentTypeMaster requestedType =
                getRequiredInstrumentTypeMaster(
                        normalizedRequestedCode);

        if (!Boolean.TRUE.equals(requestedType.getActive())
                && !normalizedRequestedCode.equalsIgnoreCase(
                        normalizedCurrentCode)) {
            throw new BusinessException(
                    "楽器タイプ「"
                    + requestedType.getInstrumentName()
                    + "」は現在無効です。"
                    + "別の有効な楽器タイプを"
                    + "選択してください。");
        }

        return requestedType;
    }

    @Transactional
    public InstrumentTypeMaster
            createInstrumentTypeMaster(
                    InstrumentTypeMaster request) {

        if (request == null) {
            throw new BusinessException(
                    "楽器タイプ情報が"
                    + "指定されていません。");
        }

        String instrumentCode =
                normalizeInstrumentCode(
                        request.getInstrumentCode());

        String instrumentName =
                normalizeRequired(
                        request.getInstrumentName(),
                        "楽器タイプ名");

        String bodyType =
                normalizeRequired(
                        request.getBodyType(),
                        "ボディタイプ");

        String neckType =
                normalizeRequired(
                        request.getNeckType(),
                        "ネックタイプ");

        validateLength(
                instrumentName,
                "楽器タイプ名",
                100);
        validateLength(
                bodyType,
                "ボディタイプ",
                100);
        validateLength(
                neckType,
                "ネックタイプ",
                100);

        if (instrumentTypeMasterRepository
                .existsByInstrumentCodeIgnoreCase(
                        instrumentCode)) {

            throw new BusinessException(
                    "楽器タイプコード「"
                    + instrumentCode
                    + "」は既に登録されています。");
        }

        InstrumentTypeMaster instrumentTypeMaster =
                new InstrumentTypeMaster();

        instrumentTypeMaster.setInstrumentCode(
                instrumentCode);
        instrumentTypeMaster.setInstrumentName(
                instrumentName);
        instrumentTypeMaster.setBodyType(
                bodyType);
        instrumentTypeMaster.setNeckType(
                neckType);
        instrumentTypeMaster.setActive(
                true);

        return instrumentTypeMasterRepository.save(
                instrumentTypeMaster);
    }

    private String normalizeInstrumentCode(
            String value) {

        String normalized =
                normalizeRequired(
                        value,
                        "楽器タイプコード")
                .toUpperCase(
                        Locale.ROOT)
                .replaceAll(
                        "\\s+",
                        "-");

        if (!normalized.matches(
                "[A-Z0-9-]+")) {

            throw new BusinessException(
                    "楽器タイプコードは"
                    + "半角英数字とハイフンで"
                    + "入力してください。");
        }

        if (normalized.startsWith("-")
                || normalized.endsWith("-")
                || normalized.contains("--")) {

            throw new BusinessException(
                    "楽器タイプコードの"
                    + "ハイフンの位置が不正です。");
        }

        if (normalized.length() > 20) {
            throw new BusinessException(
                    "楽器タイプコードは20文字以内で"
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

    private void validateLength(
            String value,
            String fieldName,
            int maximumLength) {

        if (value.length() > maximumLength) {
            throw new BusinessException(
                    fieldName
                    + "は"
                    + maximumLength
                    + "文字以内で入力してください。");
        }
    }
}
