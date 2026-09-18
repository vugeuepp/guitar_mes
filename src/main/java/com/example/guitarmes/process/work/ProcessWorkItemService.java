package com.example.guitarmes.process.work;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.process.ProcessHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** 作業項目の状態操作。終了処理と同じ親Historyロックで直列化する。 */
@Service
public class ProcessWorkItemService {
    private final ProcessWorkItemRepository items;
    private final ProcessHistoryRepository histories;
    @PersistenceContext
    private EntityManager entityManager;

    public ProcessWorkItemService(ProcessWorkItemRepository items, ProcessHistoryRepository histories) {
        this.items = items;
        this.histories = histories;
    }

    @Transactional
    public ProcessWorkItem completeItem(Long itemId) {
        return changeStatus(itemId, ProcessWorkItemStatus.COMPLETED);
    }

    @Transactional
    public ProcessWorkItem uncompleteItem(Long itemId) {
        return changeStatus(itemId, ProcessWorkItemStatus.NOT_STARTED);
    }

    private ProcessWorkItem changeStatus(Long itemId, ProcessWorkItemStatus target) {
        if (itemId == null) throw new NotFoundException("指定された作業項目が存在しません。");
        Long historyId = items.findHistoryIdByItemId(itemId)
                .orElseThrow(() -> new NotFoundException("作業項目または親工程履歴が存在しません。"));
        ProcessHistory history = histories.findForUpdate(historyId)
                .orElseThrow(() -> new NotFoundException("指定された工程履歴が存在しません。"));
        // 同じPersistence Contextで先に読まれていても、ロック待機前の状態を使わない。
        entityManager.refresh(history);
        if (history.getEndTime() != null) {
            throw new BusinessException("終了済み工程の作業項目は変更できません。");
        }
        ProcessWorkItem item = items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("指定された作業項目が存在しません。"));
        entityManager.refresh(item);
        if (item.getProcessWork() == null || item.getProcessWork().getProcessHistory() == null
                || !historyId.equals(item.getProcessWork().getProcessHistory().getId())) {
            throw new BusinessException("作業項目と親工程履歴の関連が不正です。");
        }
        if (item.getStatus() == null
                || (item.getStatus() == ProcessWorkItemStatus.COMPLETED) != (item.getCompletedAt() != null)) {
            throw new BusinessException("作業項目の状態と完了日時が不正です。");
        }
        if (item.getStatus() == target) return item;
        LocalDateTime now = LocalDateTime.now();
        item.setStatus(target);
        item.setCompletedAt(target == ProcessWorkItemStatus.COMPLETED ? now : null);
        item.setUpdatedAt(now);
        return items.save(item);
    }
}
