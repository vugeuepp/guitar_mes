package com.example.guitarmes.process;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.process.common.GuitarProcessConstants;
import com.example.guitarmes.process.common.ProcessCodeConstants;
import com.example.guitarmes.process.common.ProcessTargetConstants;

@ExtendWith(MockitoExtension.class)
class ManufacturingProcessControllerTest {

    @Mock private ManufacturingProcessRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ManufacturingProcessController(repository)).build();
    }

    @Test
    void addsNullableCodeWhilePreservingExistingJsonFields() throws Exception {
        ManufacturingProcess coded = new ManufacturingProcess(GuitarProcessConstants.PARTS_INSTALLATION, 1);
        coded.setId(10L);
        coded.setProcessCode(ProcessCodeConstants.GUITAR_PARTS_INSTALLATION);
        ManufacturingProcess legacy = new ManufacturingProcess(ProcessTargetConstants.BODY, "Body fixture", 2);
        legacy.setId(20L);
        when(repository.findAll()).thenReturn(List.of(coded, legacy));

        mvc.perform(get("/api/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].processName").value(GuitarProcessConstants.PARTS_INSTALLATION))
                .andExpect(jsonPath("$[0].processOrder").value(1))
                .andExpect(jsonPath("$[0].targetType").value(ProcessTargetConstants.GUITAR))
                .andExpect(jsonPath("$[0].processCode").value(ProcessCodeConstants.GUITAR_PARTS_INSTALLATION))
                .andExpect(jsonPath("$[1].id").value(20))
                .andExpect(jsonPath("$[1].targetType").value(ProcessTargetConstants.BODY))
                .andExpect(jsonPath("$[1].processCode").value(nullValue()));
        verify(repository).findAll();
    }
}
