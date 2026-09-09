package com.example.guitarmes.body.process;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyProcessHistoryRepository extends JpaRepository<BodyProcessHistory, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from BodyProcessHistory e where e.id = :id")
    Optional<BodyProcessHistory> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);


    List<BodyProcessHistory>findByBodyIdOrderByStartTimeAsc(Long bodyId);

    Optional<BodyProcessHistory> findFirstByBodyIdAndEndTimeIsNullOrderByStartTimeDesc(Long bodyId);

    boolean existsByBodyIdAndEndTimeIsNull(Long bodyId);
    List<BodyProcessHistory> findByEndTimeIsNull();
}
