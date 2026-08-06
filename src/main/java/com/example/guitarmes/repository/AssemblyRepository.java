package com.example.guitarmes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Assembly;

public interface AssemblyRepository extends JpaRepository<Assembly, Long> {
	Optional<Assembly> findByGuitar_Id(Long guitarId);
}
