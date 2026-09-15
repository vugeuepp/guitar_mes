package com.example.guitarmes.product.parts;

import java.util.List;

/** 詳細画面専用の表示値。保存値や業務上の必須条件は変更しない。 */
public record ProductPartsSpecView(List<Group> groups) {

    public record Group(String title, List<Item> items) {
    }

    public record Item(String label, String value) {
    }

    public static ProductPartsSpecView from(ProductPartsSpec spec, String pickupLayout) {
        return new ProductPartsSpecView(List.of(
                new Group("Bridge", List.of(
                        item("ブリッジ方式", spec.getBridgeType() == null
                                ? null : spec.getBridgeType().getLabel()),
                        item("ブリッジ型番", spec.getBridgeModel()),
                        item("スタッド穴拡張", required(spec.getRequiresStudHoleExpansion())))),
                new Group("Tuner", List.of(
                        item("ペグ型番", spec.getTunerModel()),
                        item("ペグ取付方式", spec.getTunerMountingType() == null
                                ? null : spec.getTunerMountingType().getLabel()),
                        item("ブッシュ要否", required(spec.getTunerBushRequired())),
                        item("ペグ配列", spec.getTunerLayout() == null
                                ? null : spec.getTunerLayout().getLabel()))),
                new Group("Electronics", List.of(
                        item("PU構成", pickupLayout),
                        item("セレクターポジション数", spec.getSelectorPositions() == null
                                ? null : spec.getSelectorPositions().toString()),
                        item("コントロール構成", spec.getControlLayout()),
                        item("ジャック取付形状", spec.getJackMountingType() == null
                                ? null : spec.getJackMountingType().getLabel()))),
                new Group("String", List.of(
                        item("弦メーカー", spec.getStringMaker()),
                        item("指定弦", spec.getStringModel()),
                        item("弦ゲージ", spec.getStringGauge())))));
    }

    private static Item item(String label, String value) {
        return new Item(label, value == null || value.isBlank() ? "-" : value);
    }

    private static String required(Boolean value) {
        return value == null ? "-" : value ? "必要" : "不要";
    }
}
