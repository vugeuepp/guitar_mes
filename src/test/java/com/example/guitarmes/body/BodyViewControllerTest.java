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
    void getSearchPreservesConditionsAndResultCount() throws Exception {
        BodyRepository repository = mock(BodyRepository.class);
        Body body = new Body();
        body.setSerialNo("DB-001");
        body.setModelName("Test Model");
        body.setCurrentProcess("塗装後検品");
        body.setStatus("WAITING_INSPECTION");
        when(repository.findAll()).thenReturn(List.of(body));
        BodyProcessService processes = mock(BodyProcessService.class);
        when(processes.getBodyProcesses()).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new BodyViewController(
                new BodyService(repository, null), mock(BodyMasterService.class), processes)).build();
        mvc.perform(get("/bodies/view").param("serial", "001")
                .param("modelName", "Model").param("currentProcess", "塗装後検品")
                .param("status", "WAITING_INSPECTION"))
                .andExpect(status().isOk()).andExpect(view().name("body-list"))
                .andExpect(model().attribute("bodies", List.of(body)))
                .andExpect(model().attribute("serial", "001"))
                .andExpect(model().attribute("modelName", "Model"))
                .andExpect(model().attribute("selectedCurrentProcess", "塗装後検品"))
                .andExpect(model().attribute("selectedStatus", "WAITING_INSPECTION"))
                .andExpect(model().attribute("filterApplied", true))
                .andExpect(model().attribute("resultCount", 1));
        mvc.perform(get("/bodies/view").param("serial", "missing"))
                .andExpect(model().attribute("resultCount", 0))
                .andExpect(model().attribute("filterApplied", true));
        mvc.perform(get("/bodies/view"))
                .andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("filterApplied", false))
                .andExpect(model().attribute("serial", ""));
    }
}
