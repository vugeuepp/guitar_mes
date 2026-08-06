package com.example.guitarmes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.ManufacturingProcess;

public interface ManufacturingProcessRepository extends JpaRepository<ManufacturingProcess, Long> {
	List<ManufacturingProcess> findAllByOrderByProcessOrderAsc();
}
