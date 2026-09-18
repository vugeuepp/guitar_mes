package com.example.guitarmes.process.work;

import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.product.parts.BridgeType;
import com.example.guitarmes.product.parts.TunerMountingType;
import com.example.guitarmes.product.parts.TunerLayout;
import com.example.guitarmes.product.parts.JackMountingType;

/** テスト専用の有効snapshot。製品仕様から作業を導出する実装ではない。 */
final class ProcessWorkTestData {

    private ProcessWorkTestData() {
    }

    static ProcessWork work(ProcessHistory history) {
        ProcessWork work = new ProcessWork();
        work.setProcessHistory(history);
        work.setBridgeType(BridgeType.TWO_POINT);
        work.setBridgeModel("Bridge fixture");
        work.setRequiresStudHoleExpansion(false);
        work.setTunerModel("Tuner fixture");
        work.setTunerMountingType(TunerMountingType.PRESS_BUSHING);
        work.setTunerBushRequired(true);
        work.setTunerLayout(TunerLayout.SIX_IN_LINE);
        work.setPickupLayout("SSS");
        work.setSelectorPositions(5);
        work.setControlLayout("1Vol. 2Tone");
        work.setJackMountingType(JackMountingType.BOAT_PLATE);
        work.setStringMaker("String maker");
        work.setStringModel("String fixture");
        work.setStringGauge("09-42");
        return work;
    }
}
