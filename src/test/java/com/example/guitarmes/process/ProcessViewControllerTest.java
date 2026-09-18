package com.example.guitarmes.process;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.process.work.ProcessWorkRepository;
@ExtendWith(MockitoExtension.class) class ProcessViewControllerTest { @Mock ProcessService service; @Mock GuitarService guitarService; @Mock ManufacturingProcessRepository repository; @Mock ProcessWorkRepository workRepository; MockMvc mvc;
@BeforeEach void setUp(){mvc=MockMvcBuilders.standaloneSetup(new ProcessViewController(service,guitarService,repository,workRepository)).build();}
@Test void bulkStart_redirects() throws Exception { when(service.startProcesses(List.of(1L,2L),3L,"Worker")).thenReturn(List.of(new ProcessHistory(),new ProcessHistory())); mvc.perform(post("/processes/bulk/start").param("guitarIds","1","2").param("processId","3").param("workerName","Worker")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/guitars/view")).andExpect(flash().attribute("successMessage","2件の工程を一括開始しました。")); }
@Test void bulkEnd_redirects() throws Exception { when(service.endProcesses(List.of(4L,5L))).thenReturn(List.of(new ProcessHistory(),new ProcessHistory())); mvc.perform(post("/processes/bulk/end").param("historyIds","4","5")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/guitars/view")); }
@Test void guitarHistory_addsBatchWorkSet() throws Exception {
    var history1 = new ProcessHistory(); history1.setId(10L); history1.setProcessId(1L); history1.setWorkerName("W1");
    var history2 = new ProcessHistory(); history2.setId(20L); history2.setProcessId(2L); history2.setWorkerName("W2");
    when(service.getHistory(5L)).thenReturn(List.of(
            new ProcessHistoryResponse(){ { setHistoryId(10L); setProcessName("工程A"); } },
            new ProcessHistoryResponse(){ { setHistoryId(20L); setProcessName("工程B"); } }
    ));
    when(workRepository.findProcessHistoryIdsIn(List.of(10L, 20L))).thenReturn(List.of(10L));

    mvc.perform(get("/guitars/5/history"))
            .andExpect(status().isOk())
            .andExpect(view().name("history-list"))
            .andExpect(model().attribute("workHistoryIds", Set.of(10L)));

    verify(workRepository).findProcessHistoryIdsIn(List.of(10L, 20L));
}
}
