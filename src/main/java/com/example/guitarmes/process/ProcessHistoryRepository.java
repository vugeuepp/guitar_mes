package com.example.guitarmes.process;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ProcessHistoryRepository extends JpaRepository<ProcessHistory, Long> {
	List<ProcessHistory> findByGuitarId(Long guitarId);
	List<ProcessHistory> findByEndTimeIsNull();
}
