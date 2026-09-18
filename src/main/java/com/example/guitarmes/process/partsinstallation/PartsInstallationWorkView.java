package com.example.guitarmes.process.partsinstallation;
import java.util.List;
public record PartsInstallationWorkView(
        Long historyId, Long guitarId, String guitarSerialNo, String processName,
        String workerName, String startTime, boolean readOnly, boolean inconsistent,
        int completedCount, int totalCount, List<Group> groups, Snapshot snapshot) {
    public record Group(String name, List<Item> items) {}
    public record Item(Long id, String key, String label, int order, boolean checked,
            String completedAt, String instruction) {}
    public record Snapshot(String bridgeType, String bridgeModel, String requiresStudHoleExpansion,
            String tunerModel, String tunerMountingType, String tunerBushRequired, String tunerLayout,
            String pickupLayout, String selectorPositions, String controlLayout, String jackMountingType,
            String stringMaker, String stringModel, String stringGauge) {}
}
