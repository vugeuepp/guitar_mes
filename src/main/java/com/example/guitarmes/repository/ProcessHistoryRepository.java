package com.example.guitarmes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.ProcessHistory;


public interface ProcessHistoryRepository extends JpaRepository<ProcessHistory, Long> {
	List<ProcessHistory> findByGuitarId(Long guitarId);
	List<ProcessHistory> findByEndTimeIsNull();
}
