package com.example.guitarmes.master.body;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class BodyMasterControllerTest {

    @Mock
    private BodyMasterService bodyMasterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BodyMasterController controller =
                new BodyMasterController(bodyMasterService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("ボディマスタ一覧画面を表示できる")
    void showBodyMasterList_succeeds() throws Exception {
        when(bodyMasterService.getBodyMasters())
                .thenReturn(List.of(createBodyMaster()));

        mockMvc.perform(get("/body-masters/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("body-master-list"))
                .andExpect(model().attribute(
                        "bodyMasters",
                        hasSize(1)));
    }

    @Test
    @DisplayName("ボディマスタ登録画面を表示できる")
    void showBodyMasterForm_succeeds() throws Exception {
        mockMvc.perform(get("/body-masters/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("body-master-form"));
    }

    @Test
    @DisplayName("ボディマスタを登録して一覧へリダイレクトできる")
    void createBodyMaster_succeeds() throws Exception {
        mockMvc.perform(post("/body-masters/create")
                        .param("modelCode", "BM-TEST-0001")
                        .param("modelName", "Test Body")
                        .param("bodyType", "Stratocaster")
                        .param("material", "Alder")
                        .param("color", "Black"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/body-masters/view"));

        verify(bodyMasterService)
                .createBodyMaster(any(BodyMaster.class));
    }

    @Test
    @DisplayName("ボディマスタ詳細画面を表示できる")
    void showBodyMasterDetail_succeeds() throws Exception {
        BodyMaster bodyMaster = createBodyMaster();
        when(bodyMasterService.getBodyMasterById(1L))
                .thenReturn(bodyMaster);

        mockMvc.perform(get("/body-masters/1/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("body-master-detail"))
                .andExpect(model().attribute(
                        "bodyMaster",
                        bodyMaster));
    }

    @Test
    @DisplayName("ボディマスタ編集画面を現在値付きで表示できる")
    void showBodyMasterEditForm_succeeds() throws Exception {
        BodyMasterUpdateRequest request = createUpdateRequest();
        when(bodyMasterService
                .getBodyMasterUpdateRequest(1L))
                .thenReturn(request);

        mockMvc.perform(get("/body-masters/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("body-master-edit-form"))
                .andExpect(model().attribute("request", request))
                .andExpect(model().attribute("bodyMasterId", 1L));
    }

    @Test
    @DisplayName("ボディマスタを更新して詳細へリダイレクトできる")
    void updateBodyMaster_succeeds() throws Exception {
        mockMvc.perform(post("/body-masters/1/edit")
                        .param("modelCode", "BM-TEST-0001")
                        .param("modelName", "Updated Body")
                        .param("productFamilyCode", "MIJ-H2-ST")
                        .param("bodyType", "Stratocaster")
                        .param("material", "Alder")
                        .param("color", "Black"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/body-masters/1/view"));

        verify(bodyMasterService)
                .updateBodyMaster(
                        eq(1L),
                        any(BodyMasterUpdateRequest.class));
    }

    @Test
    @DisplayName("ボディマスタ更新時の業務エラーを編集画面へ表示できる")
    void updateBodyMaster_businessError_returnsEditForm()
            throws Exception {

        when(bodyMasterService.updateBodyMaster(
                eq(1L),
                any(BodyMasterUpdateRequest.class)))
                .thenThrow(new BusinessException(
                        "モデル名を入力してください。"));

        mockMvc.perform(post("/body-masters/1/edit")
                        .param("modelCode", "BM-TEST-0001")
                        .param("modelName", "")
                        .param("productFamilyCode", "MIJ-H2-ST")
                        .param("bodyType", "Stratocaster")
                        .param("material", "Alder")
                        .param("color", "Black"))
                .andExpect(status().isOk())
                .andExpect(view().name("body-master-edit-form"))
                .andExpect(model().attribute("bodyMasterId", 1L))
                .andExpect(model().attribute(
                        "errorMessage",
                        "モデル名を入力してください。"));
    }

    private BodyMaster createBodyMaster() {
        BodyMaster bodyMaster = new BodyMaster();
        bodyMaster.setId(1L);
        bodyMaster.setModelCode("BM-TEST-0001");
        bodyMaster.setModelName("Test Body");
        bodyMaster.setProductFamilyCode("MIJ-H2-ST");
        bodyMaster.setBodyType("Stratocaster");
        bodyMaster.setMaterial("Alder");
        bodyMaster.setColor("Black");
        return bodyMaster;
    }

    private BodyMasterUpdateRequest createUpdateRequest() {
        BodyMasterUpdateRequest request =
                new BodyMasterUpdateRequest();
        request.setModelCode("BM-TEST-0001");
        request.setModelName("Test Body");
        request.setProductFamilyCode("MIJ-H2-ST");
        request.setBodyType("Stratocaster");
        request.setMaterial("Alder");
        request.setColor("Black");
        return request;
    }
}
