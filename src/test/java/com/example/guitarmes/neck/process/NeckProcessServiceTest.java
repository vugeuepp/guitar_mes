package com.example.guitarmes.neck.process;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.example.guitarmes.neck.NeckRepository;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.ManufacturingProcessRepository;
class NeckProcessServiceTest {
 private NeckProcessService service(){return new NeckProcessService(mock(NeckProcessHistoryRepository.class),mock(NeckRepository.class),mock(ManufacturingProcessRepository.class));}
 @Test void bulkStartRejectsEmptySelection(){assertThrows(BusinessException.class,()->service().startProcesses(List.of(),1L,"Worker"));}
 @Test void bulkStartRejectsBlankWorker(){assertThrows(BusinessException.class,()->service().startProcesses(List.of(1L),1L," "));}
 @Test void bulkEndRejectsEmptySelection(){assertThrows(BusinessException.class,()->service().endProcesses(List.of(),"COMPLETED",null));}
}
