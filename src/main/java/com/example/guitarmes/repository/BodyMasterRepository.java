package com.example.guitarmes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.BodyMaster;

public interface BodyMasterRepository
        extends JpaRepository<BodyMaster, Long> {

    Optional<BodyMaster>
            findFirstByModelNameIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                    String modelName,
                    String bodyType,
                    String material,
                    String color);

    Optional<BodyMaster>
            findFirstByBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                    String bodyType,
                    String material,
                    String color);

    Optional<BodyMaster>
            findTopByModelCodeStartingWithOrderByIdDesc(
                    String modelCodePrefix);

    boolean existsByModelCodeIgnoreCase(
            String modelCode);
}