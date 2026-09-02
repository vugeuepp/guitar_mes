package com.example.guitarmes.body.process;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyProcessHistoryRepository extends JpaRepository<BodyProcessHistory, Long> {

    List<BodyProcessHistory>findByBodyIdOrderByStartTimeAsc(Long bodyId);

    Optional<BodyProcessHistory> findFirstByBodyIdAndEndTimeIsNullOrderByStartTimeDesc(Long bodyId);

    boolean existsByBodyIdAndEndTimeIsNull(Long bodyId);
    List<BodyProcessHistory> findByEndTimeIsNull();
}
