package com.example.guitarmes.process.partsinstallation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.process.work.ProcessWork;
import com.example.guitarmes.process.work.ProcessWorkItem;
import com.example.guitarmes.process.work.ProcessWorkRepository;
import com.example.guitarmes.process.work.ProcessWorkItemRepository;

/** 検証済みplanを開始処理のtransaction内で保存する。仕様の再取得・再導出はしない。 */
@Service
public class PartsInstallationWorkWriter {
    private final ProcessWorkRepository works;
    private final ProcessWorkItemRepository items;

    public PartsInstallationWorkWriter(ProcessWorkRepository works, ProcessWorkItemRepository items) {
        this.works = works;
        this.items = items;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void save(ProcessHistory history, PartsInstallationWorkPlan plan) {
        ProcessWork work = new ProcessWork();
        work.setProcessHistory(history);
        var snapshot = plan.snapshot();
        work.setBridgeType(snapshot.bridgeType());
        work.setBridgeModel(snapshot.bridgeModel());
        work.setRequiresStudHoleExpansion(snapshot.requiresStudHoleExpansion());
        work.setTunerModel(snapshot.tunerModel());
        work.setTunerMountingType(snapshot.tunerMountingType());
        work.setTunerBushRequired(snapshot.tunerBushRequired());
        work.setTunerLayout(snapshot.tunerLayout());
        work.setSelectorPositions(snapshot.selectorPositions());
        work.setControlLayout(snapshot.controlLayout());
        work.setJackMountingType(snapshot.jackMountingType());
        work.setStringMaker(snapshot.stringMaker());
        work.setStringModel(snapshot.stringModel());
        work.setStringGauge(snapshot.stringGauge());
        work.setPickupLayout(snapshot.pickupLayout());
        ProcessWork savedWork = works.save(work);
        items.saveAll(plan.items().stream().map(plannedItem -> {
            ProcessWorkItem item = new ProcessWorkItem();
            item.setProcessWork(savedWork);
            item.setItemKey(plannedItem.itemKey());
            item.setItemOrder(plannedItem.itemOrder());
            // NOT_STARTED / completedAt=null、作成日時は既存Domainの初期化に任せる。
            return item;
        }).toList());
    }
}
