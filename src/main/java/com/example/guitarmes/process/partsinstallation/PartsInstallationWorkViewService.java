package com.example.guitarmes.process.partsinstallation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.guitarmes.common.DateTimeFormatterUtil;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.process.ManufacturingProcessRepository;
import com.example.guitarmes.process.ProcessHistoryRepository;
import com.example.guitarmes.process.work.*;
@Service
@Transactional(readOnly = true)
public class PartsInstallationWorkViewService {
    private final ProcessHistoryRepository histories; private final ProcessWorkRepository works;
    private final ProcessWorkItemRepository items; private final ManufacturingProcessRepository processes;
    private final GuitarService guitars;
    public PartsInstallationWorkViewService(ProcessHistoryRepository histories, ProcessWorkRepository works,
            ProcessWorkItemRepository items, ManufacturingProcessRepository processes, GuitarService guitars) {
        this.histories=histories; this.works=works; this.items=items; this.processes=processes; this.guitars=guitars;
    }
    public PartsInstallationWorkView get(Long historyId) {
        var h=histories.findById(historyId).orElseThrow(() -> new NotFoundException("指定された工程履歴が存在しません。"));
        var w=works.findByProcessHistoryId(historyId).orElseThrow(() -> new NotFoundException("指定された工程履歴に作業票が存在しません。"));
        var list=items.findByProcessWorkIdOrderByItemOrderAsc(w.getId());
        var grouped=new LinkedHashMap<String,List<PartsInstallationWorkView.Item>>();
        for(String g:List.of("Bridge","Electronics","Tuner","String")) grouped.put(g,new ArrayList<>());
        for(var i:list) {
            if(i.getItemKey()==null || i.getItemOrder()==null) throw new BusinessException("作業項目データが不正です。");
            String instruction=i.getItemKey()==ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK
                    ? "全"+w.getSelectorPositions()+"ポジションで音出し確認" : "";
            grouped.computeIfAbsent(i.getItemKey().getGroupLabel(), k->new ArrayList<>()).add(
                    new PartsInstallationWorkView.Item(i.getId(),i.getItemKey().name(),i.getItemKey().getLabel(),
                            i.getItemOrder(),i.getStatus()==ProcessWorkItemStatus.COMPLETED,
                            DateTimeFormatterUtil.format(i.getCompletedAt()),instruction));
        }
        var gs=grouped.entrySet().stream().filter(e->!e.getValue().isEmpty())
                .map(e->new PartsInstallationWorkView.Group(e.getKey(),List.copyOf(e.getValue()))).toList();
        int completed=(int)list.stream().filter(i->i.getStatus()==ProcessWorkItemStatus.COMPLETED).count();
        var g=guitars.getGuitarById(h.getGuitarId());
        var p=processes.findById(h.getProcessId()).orElseThrow(() -> new NotFoundException("指定された工程が存在しません。"));
        return new PartsInstallationWorkView(historyId,h.getGuitarId(),g.getSerialNo(),p.getProcessName(),h.getWorkerName(),
                DateTimeFormatterUtil.format(h.getStartTime()),h.getEndTime()!=null,list.isEmpty(),completed,list.size(),gs,
                new PartsInstallationWorkView.Snapshot(label(w.getBridgeType()),text(w.getBridgeModel()),yesNo(w.getRequiresStudHoleExpansion()),
                        text(w.getTunerModel()),label(w.getTunerMountingType()),yesNo(w.getTunerBushRequired()),label(w.getTunerLayout()),
                        text(w.getPickupLayout()),w.getSelectorPositions()==null?"-":String.valueOf(w.getSelectorPositions()),
                        text(w.getControlLayout()),label(w.getJackMountingType()),text(w.getStringMaker()),text(w.getStringModel()),text(w.getStringGauge())));
    }
    private String text(String v){return v==null||v.isBlank()?"-":v;}
    private String yesNo(Boolean v){return v==null?"-":v?"必要":"不要";}
    private String label(Object v){
        if(v==null)return "-";
        try{return String.valueOf(v.getClass().getMethod("getLabel").invoke(v));}catch(Exception e){return String.valueOf(v);}
    }
}
