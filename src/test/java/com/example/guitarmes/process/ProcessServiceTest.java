package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {
 @Mock ProcessHistoryRepository historyRepository; @Mock GuitarRepository guitarRepository;
 @Mock ManufacturingProcessRepository processRepository; @Mock ProductionOrderRepository orderRepository;
 ProcessService service;
 @BeforeEach void setUp(){ service=new ProcessService(historyRepository,guitarRepository,processRepository,orderRepository); }
 @Test void bulkStart_empty_rejected(){ assertThrows(BusinessException.class,()->service.startProcesses(List.of(),1L,"Worker")); verifyNoInteractions(guitarRepository); }
 @Test void bulkStart_invalidMember_savesNothing(){
   ManufacturingProcess p=new ManufacturingProcess("GUITAR","ギターパーツ取付",1); p.setId(1L);
   Guitar ok=new Guitar(); ok.setId(10L); ok.setProductionOrder(new ProductionOrder());
   Guitar invalid=new Guitar(); invalid.setId(11L);
   when(processRepository.findById(1L)).thenReturn(Optional.of(p));
   when(guitarRepository.findById(10L)).thenReturn(Optional.of(ok));
   when(guitarRepository.findById(11L)).thenReturn(Optional.of(invalid));
   assertThrows(BusinessException.class,()->service.startProcesses(List.of(10L,11L),1L,"Worker"));
   verify(guitarRepository,never()).saveAll(any()); verify(historyRepository,never()).saveAll(any());
 }
 @Test void bulkEnd_empty_rejected(){ assertThrows(BusinessException.class,()->service.endProcesses(List.of())); verifyNoInteractions(historyRepository); }
}
