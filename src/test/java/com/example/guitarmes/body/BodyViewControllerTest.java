package com.example.guitarmes.body;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.guitarmes.body.process.BodyProcessService;

class BodyViewControllerTest {
    @Test
    void correctedPagesCountsAndInputsArePreserved() throws Exception {
        var repository = mock(BodyRepository.class);
        var service = new BodyService(repository, null);
        var rows = List.of(new Body());
        when(repository.search(any(), any())).thenAnswer(call -> {
            Pageable requested = call.getArgument(1);
            return new PageImpl<>(rows, PageRequest.of(Math.min(requested.getPageNumber(), 1), 20), 21);
        });
        when(repository.countMatching(any())).thenReturn(42L);
        var processes = mock(BodyProcessService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new BodyViewController(service, null, processes)).build();
        mvc.perform(get("/bodies/view"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("filterApplied", false));
        for (String category : List.of("active", "attention", "passed")) {
            mvc.perform(get("/bodies/view").param("category", category).param("page", "999")
                    .param("serial", " SERIAL ").param("modelName", " Model ")
                    .param("currentProcess", " Process ").param("status", "WAITING"))
                    .andExpect(status().isOk()).andExpect(view().name("body-list"))
                    .andExpect(model().attribute("bodies", rows))
                    .andExpect(model().attribute("category", category))
                    .andExpect(model().attribute("currentPage", 1))
                    .andExpect(model().attribute("pageSize", 20))
                    .andExpect(model().attribute("totalPages", 2))
                    .andExpect(model().attribute("resultCount", 21L))
                    .andExpect(model().attribute("hasPrevious", true))
                    .andExpect(model().attribute("hasNext", false))
                    .andExpect(model().attribute("activeCount", 42L))
                    .andExpect(model().attribute("attentionCount", 42L))
                    .andExpect(model().attribute("passedCount", 42L))
                    .andExpect(model().attribute("serial", " SERIAL "))
                    .andExpect(model().attribute("modelName", " Model "))
                    .andExpect(model().attribute("selectedCurrentProcess", " Process "))
                    .andExpect(model().attribute("selectedStatus", "WAITING"))
                    .andExpect(model().attribute("filterApplied", true));
        }
        mvc.perform(get("/bodies/view").param("category", "unknown").param("page", "-1"))
                .andExpect(model().attribute("category", "active"))
                .andExpect(model().attribute("currentPage", 0));
        verify(repository, never()).findAll();
    }

    @Test
    void serviceNormalizesInputAndUsesFixedPageSize() {
        var repository = mock(BodyRepository.class);
        var service = new BodyService(repository, null);
        service.searchBodiesPaged(" invalid ", " Ab%_! ", null, " Proc ", " WAITING ", -10);
        verify(repository).search(new BodySearchCriteria("active", service.getCategoryStatuses("active"),
                "ab%_!", "", "proc", "waiting"), PageRequest.of(0, 20));
        service.countCategory("PASSED");
        verify(repository).countMatching(new BodySearchCriteria("passed", service.getCategoryStatuses("passed"),
                "", "", "", ""));
    }

    @Test
    void zeroResultsUseCorrectedEmptyPage() throws Exception {
        var repository = mock(BodyRepository.class);
        when(repository.search(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        var mvc = MockMvcBuilders.standaloneSetup(new BodyViewController(new BodyService(repository, null),
                null, mock(BodyProcessService.class))).build();
        mvc.perform(get("/bodies/view").param("serial", "missing").param("page", "8"))
                .andExpect(model().attribute("bodies", List.of()))
                .andExpect(model().attribute("resultCount", 0L))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0))
                .andExpect(model().attribute("hasNext", false))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("filterApplied", true));
    }
}
