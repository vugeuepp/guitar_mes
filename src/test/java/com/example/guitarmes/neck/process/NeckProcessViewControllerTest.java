package com.example.guitarmes.neck.process;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.neck.NeckService;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.ManufacturingProcess;

@ExtendWith(MockitoExtension.class)
class NeckProcessViewControllerTest {

    @Mock
    private NeckProcessService neckProcessService;

    @Mock
    private NeckService neckService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new NeckProcessViewController(
                        neckProcessService,
                        neckService))
                .build();
    }

    @Test
    void bulkStart_success_redirectsToNeckList() throws Exception {
        when(neckProcessService.startProcesses(
                List.of(1L, 2L),
                9L,
                "Worker"))
                .thenReturn(List.of(
                        new NeckProcessHistory(),
                        new NeckProcessHistory()));

        mockMvc.perform(post("/neck-processes/bulk/start")
                        .param("neckIds", "1", "2")
                        .param("processId", "9")
                        .param("workerName", "Worker"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/necks/view"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "2件のネック工程を一括開始しました。"));
    }

    @Test
    void bulkStart_businessError_redirectsToNeckList() throws Exception {
        when(neckProcessService.startProcesses(
                List.of(1L),
                9L,
                "Worker"))
                .thenThrow(new BusinessException("開始できません。"));

        mockMvc.perform(post("/neck-processes/bulk/start")
                        .param("neckIds", "1")
                        .param("processId", "9")
                        .param("workerName", "Worker"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/necks/view"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "開始できません。"));
    }

    @Test
    void bulkEndView_providesHistoriesAndProcesses() throws Exception {
        List<NeckProcessRunningResponse> histories =
                List.of(new NeckProcessRunningResponse(
                        1L, 2L, "DN260001", 3L,
                        "PLEK", "Worker", null));
        List<ManufacturingProcess> processes =
                List.of(new ManufacturingProcess());

        when(neckProcessService.getRunningProcessResponses())
                .thenReturn(histories);
        when(neckProcessService.getNeckProcesses())
                .thenReturn(processes);

        mockMvc.perform(get("/neck-processes/bulk/end/view"))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "neck-process-bulk-end-form"))
                .andExpect(model().attribute(
                        "histories",
                        histories))
                .andExpect(model().attribute(
                        "processes",
                        processes));
    }

    @Test
    void bulkEnd_success_redirectsToNeckList() throws Exception {
        when(neckProcessService.endProcesses(
                List.of(10L, 11L),
                "COMPLETED",
                ""))
                .thenReturn(List.of(
                        new NeckProcessHistory(),
                        new NeckProcessHistory()));

        mockMvc.perform(post("/neck-processes/bulk/end")
                        .param("historyIds", "10", "11")
                        .param("result", "COMPLETED")
                        .param("note", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/necks/view"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "2件のネック工程を一括終了しました。"));

        verify(neckProcessService).endProcesses(
                List.of(10L, 11L),
                "COMPLETED",
                "");
    }

    @Test
    void bulkEnd_businessError_returnsToBulkEndView() throws Exception {
        when(neckProcessService.endProcesses(
                List.of(10L),
                "COMPLETED",
                ""))
                .thenThrow(new BusinessException("終了できません。"));

        mockMvc.perform(post("/neck-processes/bulk/end")
                        .param("historyIds", "10")
                        .param("result", "COMPLETED")
                        .param("note", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/neck-processes/bulk/end/view"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "終了できません。"));
    }
}
