package com.example.guitarmes.master.instrumenttype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentTypeMasterRepository
        extends JpaRepository<InstrumentTypeMaster, Long> {

    Optional<InstrumentTypeMaster>
            findByInstrumentCodeIgnoreCase(
                    String instrumentCode);

    boolean existsByInstrumentCodeIgnoreCase(
            String instrumentCode);

    List<InstrumentTypeMaster>
            findByActiveTrueOrderByInstrumentNameAsc();

    List<InstrumentTypeMaster>
            findAllByOrderByInstrumentNameAsc();
}
