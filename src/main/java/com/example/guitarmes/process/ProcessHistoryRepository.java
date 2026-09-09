package com.example.guitarmes.process;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ProcessHistoryRepository extends JpaRepository<ProcessHistory, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from ProcessHistory e where e.id = :id")
    Optional<ProcessHistory> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

	List<ProcessHistory> findByGuitarId(Long guitarId);
    List<ProcessHistory> findByGuitarIdInOrderByIdAsc(List<Long> guitarIds);
	List<ProcessHistory> findByEndTimeIsNull();
}
