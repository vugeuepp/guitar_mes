package com.example.guitarmes.process.partsinstallation;

import java.util.List;
import java.util.Objects;

import com.example.guitarmes.process.work.ProcessWorkItemKey;
import com.example.guitarmes.product.parts.BridgeType;
import com.example.guitarmes.product.parts.TunerMountingType;
import com.example.guitarmes.product.parts.TunerLayout;
import com.example.guitarmes.product.parts.JackMountingType;

/** 保存前の生成計画。Entity参照を持たず、値と作業順序だけを保持する。 */
public record PartsInstallationWorkPlan(Snapshot snapshot, List<Item> items) {

    public PartsInstallationWorkPlan {
        Objects.requireNonNull(snapshot);
        items = List.copyOf(items);
    }

    public record Snapshot(
            BridgeType bridgeType, String bridgeModel, Boolean requiresStudHoleExpansion,
            String tunerModel, TunerMountingType tunerMountingType, Boolean tunerBushRequired,
            TunerLayout tunerLayout, Integer selectorPositions, String controlLayout,
            JackMountingType jackMountingType, String stringMaker, String stringModel,
            String stringGauge, String pickupLayout) {
    }

    public record Item(ProcessWorkItemKey itemKey, int itemOrder) {
    }
}
