package com.example.guitarmes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Body;

public interface BodyRepository extends JpaRepository<Body, Long> {
	List<Body> findByStatusNot(String status);
}
