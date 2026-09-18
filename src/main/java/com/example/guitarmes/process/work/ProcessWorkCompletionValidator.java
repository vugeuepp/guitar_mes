package com.example.guitarmes.process.work;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.ProcessHistory;

/** 終了処理は同一transaction内でHistoryをロックしてから呼ぶ。状態は変更しない。 */
@Service
public class ProcessWorkCompletionValidator {
    private final ProcessWorkRepository works;
    private final ProcessWorkItemRepository items;

    public ProcessWorkCompletionValidator(ProcessWorkRepository works, ProcessWorkItemRepository items) {
        this.works = works;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public void validateCompletable(ProcessHistory history) {
        if (history == null || history.getId() == null) {
            throw new BusinessException("工程履歴を指定してください。");
        }
        var work = works.findByProcessHistoryId(history.getId());
        if (work.isEmpty()) return;
        var workItems = items.findByProcessWorkIdOrderByItemOrderAsc(work.get().getId());
        if (workItems.isEmpty()) {
            throw new BusinessException("作業項目が存在しないため工程終了できません。");
        }
        long remaining = workItems.stream()
                .filter(item -> item.getStatus() != ProcessWorkItemStatus.COMPLETED).count();
        if (remaining > 0) {
            throw new BusinessException("未完了作業が" + remaining + "件あるため工程終了できません。");
        }
    }
}
