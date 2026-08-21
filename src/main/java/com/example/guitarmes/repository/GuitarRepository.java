package com.example.guitarmes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Guitar;

public interface GuitarRepository extends JpaRepository<Guitar, Long> {
	List<Guitar> findByProductId(Long productId);
	Optional<Guitar> findTopBySerialNoStartingWithOrderBySerialNoDesc(String prefix);
}
