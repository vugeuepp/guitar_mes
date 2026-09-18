package com.example.guitarmes.process.work;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessWorkRepository extends JpaRepository<ProcessWork, Long> {

    Optional<ProcessWork> findByProcessHistoryId(Long processHistoryId);

    @Query("select w.processHistory.id from ProcessWork w where w.processHistory.id in :historyIds")
    List<Long> findProcessHistoryIdsIn(@Param("historyIds") Collection<Long> historyIds);
}
