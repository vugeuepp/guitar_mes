package com.example.guitarmes.body;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.body.process.BodyProcessService;
import com.example.guitarmes.master.body.BodyMasterService;

class BodyViewControllerTest {
    @Test
    void defaultsCountsCategoriesAndPreservesSearchConditions() throws Exception {
        Body active = body("DB-ACTIVE", "Active Model", "塗装後検品", "WAITING_INSPECTION");
        Body attention = body("DB-ATTENTION", "Attention Model", "バフがけ", "REWORK");
        Body returned = body("DB-RETURNED", "Returned Model", "塗装前", "RETURNED");
        Body passed = body("DB-PASSED", "Passed Model", "組立待ち", "AVAILABLE");
        Body rejected = body("DB-REJECTED", "Rejected Model", "製造終了", "REJECTED");

        BodyRepository repository = mock(BodyRepository.class);
        when(repository.findAll()).thenReturn(
                List.of(active, attention, returned, passed, rejected));
        BodyProcessService processes = mock(BodyProcessService.class);
        when(processes.getBodyProcesses()).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new BodyViewController(
                new BodyService(repository, null),
                mock(BodyMasterService.class), processes)).build();

        mvc.perform(get("/bodies/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("body-list"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("bodies", List.of(active)))
                .andExpect(model().attribute("activeCount", 1))
                .andExpect(model().attribute("attentionCount", 2))
                .andExpect(model().attribute("passedCount", 2))
                .andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("filterApplied", false));

        mvc.perform(get("/bodies/view")
                        .param("category", "attention")
                        .param("serial", "ATTENTION")
                        .param("modelName", "Attention")
                        .param("currentProcess", "バフがけ")
                        .param("status", "REWORK"))
                .andExpect(model().attribute("category", "attention"))
                .andExpect(model().attribute("bodies", List.of(attention)))
                .andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("serial", "ATTENTION"))
                .andExpect(model().attribute("modelName", "Attention"))
                .andExpect(model().attribute("selectedCurrentProcess", "バフがけ"))
                .andExpect(model().attribute("selectedStatus", "REWORK"))
                .andExpect(model().attribute("filterApplied", true));

        mvc.perform(get("/bodies/view")
                        .param("category", "active")
                        .param("status", "REJECTED"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("bodies", List.of()))
                .andExpect(model().attribute("resultCount", 0));

        mvc.perform(get("/bodies/view").param("category", "invalid"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("bodies", List.of(active)));

        mvc.perform(get("/bodies/view").param("category", "passed"))
                .andExpect(model().attribute("category", "passed"))
                .andExpect(model().attribute("bodies", List.of(passed, rejected)))
                .andExpect(model().attribute("resultCount", 2));
    }

    private Body body(String serial, String model, String process, String status) {
        Body body = new Body();
        body.setSerialNo(serial);
        body.setModelName(model);
        body.setCurrentProcess(process);
        body.setStatus(status);
        return body;
    }
}
