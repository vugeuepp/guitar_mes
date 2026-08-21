package com.example.guitarmes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Neck;

public interface NeckRepository
        extends JpaRepository<Neck, Long> {

    List<Neck> findByStatusNot(
            String status);

    List<Neck> findByStatus(
            String status);

    long countByStatus(
            String status);

    Optional<Neck>
            findTopBySerialNoStartingWithOrderBySerialNoDesc(
                    String prefix);
}