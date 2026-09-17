package com.example.guitarmes.process.work;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessWorkItemRepository extends JpaRepository<ProcessWorkItem, Long> {

    // Entityを先にロードせず、親Historyを最初にロックするためのID取得。
    @Query("select i.processWork.processHistory.id from ProcessWorkItem i where i.id = :itemId")
    Optional<Long> findHistoryIdByItemId(@Param("itemId") Long itemId);

    List<ProcessWorkItem> findByProcessWorkIdOrderByItemOrderAsc(Long processWorkId);
}
