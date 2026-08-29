package com.example.guitarmes.assembly;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssemblyRepository extends JpaRepository<Assembly, Long> {
	Optional<Assembly> findByGuitar_Id(Long guitarId);
}
