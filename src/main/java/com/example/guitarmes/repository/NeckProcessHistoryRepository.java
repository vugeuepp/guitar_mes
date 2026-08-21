package com.example.guitarmes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.NeckProcessHistory;

public interface NeckProcessHistoryRepository
        extends JpaRepository<NeckProcessHistory, Long> {

    List<NeckProcessHistory>
            findByNeckIdOrderByStartTimeAsc(
                    Long neckId);

    Optional<NeckProcessHistory>
            findFirstByNeckIdAndEndTimeIsNullOrderByStartTimeDesc(
                    Long neckId);

    boolean existsByNeckIdAndEndTimeIsNull(
            Long neckId);
}