package com.example.guitarmes.neck.process;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
    List<NeckProcessHistory> findByEndTimeIsNull();
}
