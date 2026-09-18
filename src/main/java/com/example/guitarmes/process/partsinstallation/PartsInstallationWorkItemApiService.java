package com.example.guitarmes.process.partsinstallation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.process.work.*;
@Service
public class PartsInstallationWorkItemApiService {
    private final ProcessWorkItemService itemService; private final ProcessWorkItemRepository items;
    public PartsInstallationWorkItemApiService(ProcessWorkItemService itemService, ProcessWorkItemRepository items){this.itemService=itemService;this.items=items;}
    @Transactional public PartsInstallationWorkItemResponse complete(Long id){return response(itemService.completeItem(id));}
    @Transactional public PartsInstallationWorkItemResponse uncomplete(Long id){return response(itemService.uncompleteItem(id));}
    private PartsInstallationWorkItemResponse response(ProcessWorkItem changed){
        Long historyId=items.findHistoryIdByItemId(changed.getId()).orElseThrow(() -> new NotFoundException("作業項目または親工程履歴が存在しません。"));
        var work=changed.getProcessWork(); var all=items.findByProcessWorkIdOrderByItemOrderAsc(work.getId());
        int complete=(int)all.stream().filter(i->i.getStatus()==ProcessWorkItemStatus.COMPLETED).count();
        boolean readOnly=work.getProcessHistory().getEndTime()!=null;
        return new PartsInstallationWorkItemResponse(changed.getId(),changed.getStatus().name(),
                changed.getStatus()==ProcessWorkItemStatus.COMPLETED,
                com.example.guitarmes.common.DateTimeFormatterUtil.format(changed.getCompletedAt()),complete,all.size(),readOnly);
    }
}
