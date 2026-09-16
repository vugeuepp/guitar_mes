package com.example.guitarmes.process.work;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessWorkRepository extends JpaRepository<ProcessWork, Long> {

    Optional<ProcessWork> findByProcessHistoryId(Long processHistoryId);
}
