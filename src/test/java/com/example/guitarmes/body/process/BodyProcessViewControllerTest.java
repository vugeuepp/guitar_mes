package com.example.guitarmes.body.process;

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

import com.example.guitarmes.body.BodyService;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.ManufacturingProcess;

@ExtendWith(MockitoExtension.class)
class BodyProcessViewControllerTest {

    @Mock
    private BodyProcessService bodyProcessService;

    @Mock
    private BodyService bodyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new BodyProcessViewController(
                        bodyProcessService,
                        bodyService))
                .build();
    }

    @Test
    void bulkStart_success_redirectsToBodyList() throws Exception {
        when(bodyProcessService.startProcesses(
                List.of(1L, 2L),
                6L,
                "Worker"))
                .thenReturn(List.of(
                        new BodyProcessHistory(),
                        new BodyProcessHistory()));

        mockMvc.perform(post("/body-processes/bulk/start")
                        .param("bodyIds", "1", "2")
                        .param("processId", "6")
                        .param("workerName", "Worker"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bodies/view"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "2件のボディ工程を一括開始しました。"));
    }

    @Test
    void bulkStart_businessError_redirectsToBodyList() throws Exception {
        when(bodyProcessService.startProcesses(
                List.of(1L),
                6L,
                "Worker"))
                .thenThrow(new BusinessException("開始できません。"));

        mockMvc.perform(post("/body-processes/bulk/start")
                        .param("bodyIds", "1")
                        .param("processId", "6")
                        .param("workerName", "Worker"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bodies/view"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "開始できません。"));
    }

    @Test
    void bulkEndView_providesHistoriesAndProcesses() throws Exception {
        List<BodyProcessRunningResponse> histories =
                List.of(new BodyProcessRunningResponse(
                        1L, 2L, "DB260001", 3L,
                        "塗装後検品", "Worker", null));
        List<ManufacturingProcess> processes =
                List.of(new ManufacturingProcess());

        when(bodyProcessService.getRunningProcessResponses())
                .thenReturn(histories);
        when(bodyProcessService.getBodyProcesses())
                .thenReturn(processes);

        mockMvc.perform(get("/body-processes/bulk/end/view"))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "body-process-bulk-end-form"))
                .andExpect(model().attribute(
                        "histories",
                        histories))
                .andExpect(model().attribute(
                        "processes",
                        processes));
    }

    @Test
    void bulkEnd_success_redirectsToBodyList() throws Exception {
        when(bodyProcessService.endProcesses(
                List.of(10L, 11L),
                "PASSED",
                ""))
                .thenReturn(List.of(
                        new BodyProcessHistory(),
                        new BodyProcessHistory()));

        mockMvc.perform(post("/body-processes/bulk/end")
                        .param("historyIds", "10", "11")
                        .param("result", "PASSED")
                        .param("note", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bodies/view"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "2件のボディ工程を一括終了しました。"));

        verify(bodyProcessService).endProcesses(
                List.of(10L, 11L),
                "PASSED",
                "");
    }

    @Test
    void bulkEnd_businessError_returnsToBulkEndView() throws Exception {
        when(bodyProcessService.endProcesses(
                List.of(10L),
                "COMPLETED",
                ""))
                .thenThrow(new BusinessException("終了できません。"));

        mockMvc.perform(post("/body-processes/bulk/end")
                        .param("historyIds", "10")
                        .param("result", "COMPLETED")
                        .param("note", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/body-processes/bulk/end/view"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "終了できません。"));
    }
}
