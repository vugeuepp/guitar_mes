package com.example.guitarmes.master.body;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
            findFirstByProductFamilyCodeIgnoreCaseAndBodyTypeIgnoreCaseAndMaterialIgnoreCaseAndColorIgnoreCase(
                    String productFamilyCode,
                    String bodyType,
                    String material,
                    String color);

    Optional<BodyMaster>
            findTopByModelCodeStartingWithOrderByIdDesc(
                    String modelCodePrefix);

    boolean existsByModelCodeIgnoreCase(
            String modelCode);
}