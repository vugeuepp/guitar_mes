package com.example.guitarmes.master.productseries;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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