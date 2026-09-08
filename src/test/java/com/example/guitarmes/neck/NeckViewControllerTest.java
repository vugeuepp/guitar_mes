package com.example.guitarmes.neck;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.neck.process.NeckProcessService;
import com.example.guitarmes.master.neck.NeckMasterService;

class NeckViewControllerTest {
    @Test
    void defaultsCountsCategoriesAndPreservesSearchConditions() throws Exception {
        Neck active = neck("DB-ACTIVE", "Active Model", "PLEK", "WAITING");
        Neck attention = neck("DB-ATTENTION", "Attention Model", "PLEK", "RETURNED");
        Neck returned = neck("DB-RETURNED", "Returned Model", "塗装前", "RETURNED");
        Neck passed = neck("DB-PASSED", "Passed Model", "組立待ち", "AVAILABLE");
        Neck rejected = neck("DB-REJECTED", "Rejected Model", "製造終了", "REJECTED");

        NeckRepository repository = mock(NeckRepository.class);
        when(repository.findAll()).thenReturn(
                List.of(active, attention, returned, passed, rejected));
        NeckProcessService processes = mock(NeckProcessService.class);
        when(processes.getNeckProcesses()).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new NeckViewController(
                new NeckService(repository, null),
                mock(NeckMasterService.class), processes)).build();

        mvc.perform(get("/necks/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("neck-list"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("necks", List.of(active)))
                .andExpect(model().attribute("activeCount", 1))
                .andExpect(model().attribute("attentionCount", 2))
                .andExpect(model().attribute("passedCount", 2))
                .andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("filterApplied", false));

        mvc.perform(get("/necks/view")
                        .param("category", "attention")
                        .param("serial", "ATTENTION")
                        .param("modelName", "Attention")
                        .param("currentProcess", "PLEK")
                        .param("status", "RETURNED"))
                .andExpect(model().attribute("category", "attention"))
                .andExpect(model().attribute("necks", List.of(attention)))
                .andExpect(model().attribute("resultCount", 1))
                .andExpect(model().attribute("serial", "ATTENTION"))
                .andExpect(model().attribute("modelName", "Attention"))
                .andExpect(model().attribute("selectedCurrentProcess", "PLEK"))
                .andExpect(model().attribute("selectedStatus", "RETURNED"))
                .andExpect(model().attribute("filterApplied", true))
                .andExpect(model().attribute("attentionCount", 2));

        mvc.perform(get("/necks/view")
                        .param("category", "active")
                        .param("status", "REJECTED"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("necks", List.of()))
                .andExpect(model().attribute("resultCount", 0));

        for (String category : List.of("", " ", "invalid", "undefined")) {
            mvc.perform(get("/necks/view").param("category", category))
                    .andExpect(model().attribute("category", "active"))
                    .andExpect(model().attribute("necks", List.of(active)));
        }

        mvc.perform(get("/necks/view").param("category", "passed"))
                .andExpect(model().attribute("category", "passed"))
                .andExpect(model().attribute("necks", List.of(passed, rejected)))
                .andExpect(model().attribute("resultCount", 2));
    }

    private Neck neck(String serial, String model, String process, String status) {
        Neck neck = new Neck();
        neck.setSerialNo(serial);
        neck.setModelName(model);
        neck.setCurrentProcess(process);
        neck.setStatus(status);
        return neck;
    }
}
