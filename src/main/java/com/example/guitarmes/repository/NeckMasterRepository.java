package com.example.guitarmes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.NeckMaster;

public interface NeckMasterRepository
        extends JpaRepository<NeckMaster, Long> {

    Optional<NeckMaster>
            findFirstByModelNameIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                    String modelName,
                    String neckType,
                    String neckMaterial,
                    String fingerboardMaterial,
                    Integer fretCount,
                    String scale);

    Optional<NeckMaster>
            findFirstByNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                    String neckType,
                    String neckMaterial,
                    String fingerboardMaterial,
                    Integer fretCount,
                    String scale);

    Optional<NeckMaster>
            findTopByModelCodeStartingWithOrderByIdDesc(
                    String modelCodePrefix);

    boolean existsByModelCodeIgnoreCase(
            String modelCode);
}