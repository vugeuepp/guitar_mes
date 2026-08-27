package com.example.guitarmes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.ProductSeriesMaster;

public interface ProductSeriesMasterRepository
        extends JpaRepository<ProductSeriesMaster, Long> {

    Optional<ProductSeriesMaster>
            findBySeriesCodeIgnoreCase(
                    String seriesCode);

    boolean existsBySeriesCodeIgnoreCase(
            String seriesCode);

    List<ProductSeriesMaster>
            findByActiveTrueOrderBySeriesNameAsc();

    List<ProductSeriesMaster>
            findAllByOrderBySeriesNameAsc();
}