package com.example.guitarmes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.BodyProcessHistory;

public interface BodyProcessHistoryRepository extends JpaRepository<BodyProcessHistory, Long> {

    List<BodyProcessHistory>findByBodyIdOrderByStartTimeAsc(Long bodyId);

    Optional<BodyProcessHistory> findFirstByBodyIdAndEndTimeIsNullOrderByStartTimeDesc(Long bodyId);

    boolean existsByBodyIdAndEndTimeIsNull(Long bodyId);
}