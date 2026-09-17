package com.example.guitarmes.process.work;
/** 永続化する安定作業コード。穴あけは取付作業に含め、独立した作業コードを設けない。 */
public enum ProcessWorkItemKey {
    BRIDGE_SIX_POINT_INSTALL("Bridge", "6点支持ブリッジ取付"),
    BRIDGE_MOVEMENT_CHECK("Bridge", "ブリッジ可動確認"),
    STUD_HOLE_EXPANSION("Bridge", "スタッド穴拡張"),
    STUD_INSTALL("Bridge", "スタッド取付"),
    BRIDGE_TWO_POINT_INSTALL("Bridge", "2点支持ブリッジ取付"),
    SPRING_HANGER_INSTALL("Bridge", "スプリングハンガー取付"),
    PICKGUARD_INSTALL("Electronics", "ピックガード取付"),
    JACK_PLATE_INSTALL("Electronics", "ジャックプレート取付"),
    JACK_WIRING("Electronics", "ジャック配線"),
    GROUND_WIRING("Electronics", "アース配線"),
    ELECTRONICS_SOUND_CHECK("Electronics", "音出し確認"),
    ELECTRONICS_PARTS_CHECK("Electronics", "電装パーツ確認"),
    ELECTRONICS_FINAL_FASTENING("Electronics", "電装パーツ最終固定"),
    TUNER_BUSHING_INSTALL("Tuner", "ペグブッシュ取付"),
    TUNER_INSTALL("Tuner", "ペグ取付"),
    STRING_INSTALL("String", "弦張り");
    private final String groupLabel;
    private final String label;
    ProcessWorkItemKey(String groupLabel, String label) { this.groupLabel = groupLabel; this.label = label; }
    public String getGroupLabel() { return groupLabel; }
    public String getLabel() { return label; }
}
