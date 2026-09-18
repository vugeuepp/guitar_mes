package com.example.guitarmes.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.master.instrumenttype.*;
import com.example.guitarmes.master.productseries.*;

class ProductClassificationServiceTest {
    @Test void resolvesFormalCodesIncludingInactiveMastersAndHyphenatedSeries() {
        var types = mock(InstrumentTypeMasterService.class);
        var series = mock(ProductSeriesMasterService.class);
        when(types.getInstrumentTypeMasters()).thenReturn(List.of(
                new InstrumentTypeMaster("ST", "Stratocaster", "body", "neck", false)));
        when(series.getRequiredProductSeriesMaster("MIJ-HER50"))
                .thenReturn(new ProductSeriesMaster("MIJ-HER50", "series", false));
        var service = new ProductClassificationService(series, types);
        assertEquals(new ProductClassificationService.Classification("MIJ-HER50", "ST"),
                service.classify(" mij-her50-st ").orElseThrow());
    }

    @Test void missingMalformedAndUnknownCodesRemainUnclassifiable() {
        var types = mock(InstrumentTypeMasterService.class);
        var series = mock(ProductSeriesMasterService.class);
        var service = new ProductClassificationService(series, types);
        assertTrue(service.classify(null).isEmpty());
        assertTrue(service.classify(" ").isEmpty());
        verifyNoInteractions(types, series);
        when(types.getInstrumentTypeMasters()).thenReturn(List.of(
                new InstrumentTypeMaster("ST", "Stratocaster", "body", "neck", true)));
        when(series.getRequiredProductSeriesMaster("UNKNOWN")).thenThrow(new BusinessException("unknown"));
        for (String code : List.of("Stratocaster", "SSS", "-ST", "SERIES-TL", "UNKNOWN-ST")) {
            assertTrue(service.classify(code).isEmpty(), code);
        }
    }
}
