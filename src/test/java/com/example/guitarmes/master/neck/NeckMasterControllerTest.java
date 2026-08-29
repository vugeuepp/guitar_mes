package com.example.guitarmes.master.neck;

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
class NeckMasterControllerTest {

    @Mock
    private NeckMasterService neckMasterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NeckMasterController controller =
                new NeckMasterController(neckMasterService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("ネックマスタ一覧画面を表示できる")
    void showNeckMasterList_succeeds() throws Exception {
        when(neckMasterService.getNeckMasters())
                .thenReturn(List.of(createNeckMaster()));

        mockMvc.perform(get("/neck-masters/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("neck-master-list"))
                .andExpect(model().attribute(
                        "neckMasters",
                        hasSize(1)));
    }

    @Test
    @DisplayName("ネックマスタ登録画面を表示できる")
    void showNeckMasterForm_succeeds() throws Exception {
        mockMvc.perform(get("/neck-masters/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("neck-master-form"));
    }

    @Test
    @DisplayName("ネックマスタを登録して一覧へリダイレクトできる")
    void createNeckMaster_succeeds() throws Exception {
        mockMvc.perform(post("/neck-masters/create")
                        .param("modelCode", "NM-TEST-0001")
                        .param("modelName", "Test Neck")
                        .param("productFamilyCode", "MIJ-H2-ST")
                        .param("neckType", "Stratocaster")
                        .param("neckMaterial", "Maple")
                        .param("fingerboardMaterial", "Rosewood")
                        .param("fretCount", "22")
                        .param("scale", "25.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/neck-masters/view"));

        verify(neckMasterService)
                .createNeckMaster(any(NeckMaster.class));
    }

    @Test
    @DisplayName("ネックマスタ詳細画面を表示できる")
    void showNeckMasterDetail_succeeds() throws Exception {
        NeckMaster neckMaster = createNeckMaster();
        when(neckMasterService.getNeckMasterById(1L))
                .thenReturn(neckMaster);

        mockMvc.perform(get("/neck-masters/1/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("neck-master-detail"))
                .andExpect(model().attribute(
                        "neckMaster",
                        neckMaster));
    }

    @Test
    @DisplayName("ネックマスタ編集画面を現在値付きで表示できる")
    void showNeckMasterEditForm_succeeds() throws Exception {
        NeckMasterUpdateRequest request = createUpdateRequest();
        when(neckMasterService
                .getNeckMasterUpdateRequest(1L))
                .thenReturn(request);

        mockMvc.perform(get("/neck-masters/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("neck-master-edit-form"))
                .andExpect(model().attribute("request", request))
                .andExpect(model().attribute("neckMasterId", 1L));
    }

    @Test
    @DisplayName("ネックマスタを更新して詳細へリダイレクトできる")
    void updateNeckMaster_succeeds() throws Exception {
        mockMvc.perform(post("/neck-masters/1/edit")
                        .param("modelCode", "NM-TEST-0001")
                        .param("modelName", "Updated Neck")
                        .param("productFamilyCode", "MIJ-H2-ST")
                        .param("neckType", "Stratocaster")
                        .param("neckMaterial", "Maple")
                        .param("fingerboardMaterial", "Rosewood")
                        .param("fretCount", "22")
                        .param("scale", "25.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/neck-masters/1/view"));

        verify(neckMasterService)
                .updateNeckMaster(
                        eq(1L),
                        any(NeckMasterUpdateRequest.class));
    }

    @Test
    @DisplayName("ネックマスタ更新時の業務エラーを編集画面へ表示できる")
    void updateNeckMaster_businessError_returnsEditForm()
            throws Exception {

        when(neckMasterService.updateNeckMaster(
                eq(1L),
                any(NeckMasterUpdateRequest.class)))
                .thenThrow(new BusinessException(
                        "モデル名を入力してください。"));

        mockMvc.perform(post("/neck-masters/1/edit")
                        .param("modelCode", "NM-TEST-0001")
                        .param("modelName", "")
                        .param("productFamilyCode", "MIJ-H2-ST")
                        .param("neckType", "Stratocaster")
                        .param("neckMaterial", "Maple")
                        .param("fingerboardMaterial", "Rosewood")
                        .param("fretCount", "22")
                        .param("scale", "25.5"))
                .andExpect(status().isOk())
                .andExpect(view().name("neck-master-edit-form"))
                .andExpect(model().attribute("neckMasterId", 1L))
                .andExpect(model().attribute(
                        "errorMessage",
                        "モデル名を入力してください。"));
    }

    private NeckMaster createNeckMaster() {
        NeckMaster neckMaster = new NeckMaster(
                "NM-TEST-0001",
                "Test Neck",
                "Stratocaster",
                "Maple",
                "Rosewood",
                22,
                "25.5");
        neckMaster.setProductFamilyCode("MIJ-H2-ST");
        return neckMaster;
    }

    private NeckMasterUpdateRequest createUpdateRequest() {
        NeckMasterUpdateRequest request =
                new NeckMasterUpdateRequest();
        request.setModelCode("NM-TEST-0001");
        request.setModelName("Test Neck");
        request.setProductFamilyCode("MIJ-H2-ST");
        request.setNeckType("Stratocaster");
        request.setNeckMaterial("Maple");
        request.setFingerboardMaterial("Rosewood");
        request.setFretCount(22);
        request.setScale("25.5");
        return request;
    }
}
