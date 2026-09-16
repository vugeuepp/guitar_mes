package com.example.guitarmes.process.work;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessWorkItemRepository extends JpaRepository<ProcessWorkItem, Long> {

    List<ProcessWorkItem> findByProcessWorkIdOrderByItemOrderAsc(Long processWorkId);
}
