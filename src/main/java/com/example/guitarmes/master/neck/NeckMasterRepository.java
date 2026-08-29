package com.example.guitarmes.master.neck;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
            findFirstByProductFamilyCodeIgnoreCaseAndNeckTypeIgnoreCaseAndNeckMaterialIgnoreCaseAndFingerboardMaterialIgnoreCaseAndFretCountAndScaleIgnoreCase(
                    String productFamilyCode,
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