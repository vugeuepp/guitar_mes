package com.example.guitarmes.product.parts;

import org.springframework.stereotype.Component;

import com.example.guitarmes.exception.BusinessException;

/** 保存時は正規化したコピーを返し、読取時は元Entityを変更せず同じ規則を検証する。 */
@Component
public class ProductPartsSpecValidator {

    public ProductPartsSpecRequest normalizeAndValidate(ProductPartsSpecRequest request) {
        return validate(request, true);
    }

    public void validateStored(ProductPartsSpec spec) {
        if (spec == null) throw new BusinessException("パーツ取付仕様が登録されていません。");
        ProductPartsSpecRequest request = new ProductPartsSpecRequest();
        request.setBridgeType(spec.getBridgeType());
        request.setBridgeModel(spec.getBridgeModel());
        request.setRequiresStudHoleExpansion(spec.getRequiresStudHoleExpansion());
        request.setTunerModel(spec.getTunerModel());
        request.setTunerMountingType(spec.getTunerMountingType());
        request.setTunerBushRequired(spec.getTunerBushRequired());
        request.setTunerLayout(spec.getTunerLayout());
        request.setSelectorPositions(spec.getSelectorPositions());
        request.setControlLayout(spec.getControlLayout());
        request.setJackMountingType(spec.getJackMountingType());
        request.setStringMaker(spec.getStringMaker());
        request.setStringModel(spec.getStringModel());
        request.setStringGauge(spec.getStringGauge());
        validate(request, false);
    }

    private ProductPartsSpecRequest validate(ProductPartsSpecRequest request, boolean normalize) {
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
        normalized.setBridgeModel(text(request.getBridgeModel(), "ブリッジ型番", 255, false, normalize));
        normalized.setTunerModel(text(request.getTunerModel(), "ペグ型番", 255, true, normalize));
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
        normalized.setControlLayout(text(request.getControlLayout(), "コントロール構成", 255, true, normalize));
        normalized.setJackMountingType(required(request.getJackMountingType(), "ジャック取付形状"));
        normalized.setStringMaker(text(request.getStringMaker(), "弦メーカー", 150, false, normalize));
        normalized.setStringModel(text(request.getStringModel(), "指定弦", 255, true, normalize));
        normalized.setStringGauge(text(request.getStringGauge(), "弦ゲージ", 100, true, normalize));
        return normalized;
    }

    private <T> T required(T value, String label) {
        if (value == null) {
            throw new BusinessException(label + "を入力してください。");
        }
        return value;
    }

    private String text(String value, String label, int maxLength, boolean mandatory, boolean normalize) {
        String normalized = value == null ? null : normalize ? value.trim() : value;
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

}
