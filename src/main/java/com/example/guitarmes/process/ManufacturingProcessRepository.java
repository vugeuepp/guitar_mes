package com.example.guitarmes.process;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManufacturingProcessRepository
        extends JpaRepository<ManufacturingProcess, Long> {

    List<ManufacturingProcess>findAllByOrderByProcessOrderAsc();

    List<ManufacturingProcess>findByTargetTypeOrderByProcessOrderAsc(String targetType);

    Optional<ManufacturingProcess>findByTargetTypeAndProcessName(String targetType, String processName);
}