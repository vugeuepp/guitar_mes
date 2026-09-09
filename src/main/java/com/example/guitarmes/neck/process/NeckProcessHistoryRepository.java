package com.example.guitarmes.neck.process;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NeckProcessHistoryRepository
        extends JpaRepository<NeckProcessHistory, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from NeckProcessHistory e where e.id = :id")
    Optional<NeckProcessHistory> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


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
