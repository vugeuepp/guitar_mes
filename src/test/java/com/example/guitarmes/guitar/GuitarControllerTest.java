package com.example.guitarmes.guitar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GuitarControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new GuitarController(new GuitarService(null))).build();
    }

    @Test
    void putCannotUpdateCurrentProcessThroughRemovedEndpoint() throws Exception {
        mvc.perform(put("/api/guitars/1")
                        .contentType("application/json")
                        .content("{\"currentProcess\":\"不正な工程\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
