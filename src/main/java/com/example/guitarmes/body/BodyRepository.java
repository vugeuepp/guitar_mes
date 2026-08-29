package com.example.guitarmes.body;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyRepository
        extends JpaRepository<Body, Long> {

    List<Body> findByStatusNot(
            String status);

    List<Body> findByStatus(
            String status);

    List<Body> findByStatusAndBodyMaster_Id(
            String status,
            Long bodyMasterId);

    long countByStatus(
            String status);

    Optional<Body>
            findTopBySerialNoStartingWithOrderBySerialNoDesc(
                    String prefix);
}