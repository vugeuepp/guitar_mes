package com.example.guitarmes.neck;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NeckRepository
        extends JpaRepository<Neck, Long> {

    List<Neck> findByStatusNot(
            String status);

    List<Neck> findByStatus(
            String status);

    List<Neck> findByStatusAndNeckMaster_Id(
            String status,
            Long neckMasterId);

    long countByStatus(
            String status);

    Optional<Neck>
            findTopBySerialNoStartingWithOrderBySerialNoDesc(
                    String prefix);
}