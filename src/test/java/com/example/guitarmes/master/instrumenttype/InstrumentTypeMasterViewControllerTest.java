package com.example.guitarmes.master.instrumenttype;

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
class InstrumentTypeMasterViewControllerTest {

    @Mock
    private InstrumentTypeMasterService instrumentTypeMasterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InstrumentTypeMasterViewController controller =
                new InstrumentTypeMasterViewController(
                        instrumentTypeMasterService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("楽器タイプ一覧画面を表示できる")
    void instrumentTypeList_succeeds() throws Exception {
        when(instrumentTypeMasterService
                .getInstrumentTypeMasters())
                .thenReturn(List.of(createInstrumentType()));

        mockMvc.perform(get("/instrument-types/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument-type-list"))
                .andExpect(model().attribute(
                        "instrumentTypeMasters",
                        hasSize(1)));
    }

    @Test
    @DisplayName("楽器タイプ登録画面を表示できる")
    void newInstrumentTypeForm_succeeds() throws Exception {
        mockMvc.perform(get("/instrument-types/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument-type-form"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    @DisplayName("楽器タイプを登録して一覧へリダイレクトできる")
    void createInstrumentType_succeeds() throws Exception {
        mockMvc.perform(post("/instrument-types/create")
                        .param("instrumentCode", "DUO")
                        .param("instrumentName", "Duo-Sonic")
                        .param("bodyType", "Duo-Sonic")
                        .param("neckType", "Duo-Sonic"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instrument-types/view"));

        verify(instrumentTypeMasterService)
                .createInstrumentTypeMaster(
                        any(InstrumentTypeMaster.class));
    }

    @Test
    @DisplayName("楽器タイプ登録時の業務エラーを登録画面へ表示できる")
    void createInstrumentType_businessError_returnsForm()
            throws Exception {

        when(instrumentTypeMasterService
                .createInstrumentTypeMaster(
                        any(InstrumentTypeMaster.class)))
                .thenThrow(new BusinessException(
                        "楽器タイプコードは既に登録されています。"));

        mockMvc.perform(post("/instrument-types/create")
                        .param("instrumentCode", "ST")
                        .param("instrumentName", "Stratocaster")
                        .param("bodyType", "Stratocaster")
                        .param("neckType", "Stratocaster"))
                .andExpect(status().isOk())
                .andExpect(view().name("instrument-type-form"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "楽器タイプコードは既に登録されています。"));
    }

    @Test
    @DisplayName("楽器タイプ編集画面を現在値付きで表示できる")
    void editInstrumentTypeForm_succeeds() throws Exception {
        InstrumentTypeMaster instrumentType =
                createInstrumentType();
        when(instrumentTypeMasterService
                .getInstrumentTypeMasterById(1L))
                .thenReturn(instrumentType);

        mockMvc.perform(get("/instrument-types/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "instrument-type-edit-form"))
                .andExpect(model().attribute(
                        "request",
                        instrumentType))
                .andExpect(model().attribute(
                        "instrumentTypeId",
                        1L));
    }

    @Test
    @DisplayName("楽器タイプを更新して一覧へリダイレクトできる")
    void updateInstrumentType_succeeds() throws Exception {
        mockMvc.perform(post("/instrument-types/1/edit")
                        .param("instrumentCode", "ST")
                        .param(
                                "instrumentName",
                                "Stratocaster Updated")
                        .param("bodyType", "Stratocaster")
                        .param("neckType", "Stratocaster"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instrument-types/view"));

        verify(instrumentTypeMasterService)
                .updateInstrumentTypeMaster(
                        eq(1L),
                        any(InstrumentTypeMaster.class));
    }

    @Test
    @DisplayName("楽器タイプ更新時の業務エラーを編集画面へ表示できる")
    void updateInstrumentType_businessError_returnsEditForm()
            throws Exception {

        when(instrumentTypeMasterService
                .updateInstrumentTypeMaster(
                        eq(1L),
                        any(InstrumentTypeMaster.class)))
                .thenThrow(new BusinessException(
                        "楽器タイプ名を入力してください。"));

        mockMvc.perform(post("/instrument-types/1/edit")
                        .param("instrumentCode", "ST")
                        .param("instrumentName", "")
                        .param("bodyType", "Stratocaster")
                        .param("neckType", "Stratocaster"))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "instrument-type-edit-form"))
                .andExpect(model().attribute(
                        "instrumentTypeId",
                        1L))
                .andExpect(model().attribute(
                        "errorMessage",
                        "楽器タイプ名を入力してください。"))
                .andExpect(model().attribute(
                        "request",
                        org.hamcrest.Matchers.hasProperty(
                                "id",
                                org.hamcrest.Matchers.is(1L))));
    }

    @Test
    @DisplayName("楽器タイプの有効状態を切り替えて一覧へリダイレクトできる")
    void toggleInstrumentTypeActive_succeeds() throws Exception {
        mockMvc.perform(post(
                        "/instrument-types/1/toggle-active"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instrument-types/view"));

        verify(instrumentTypeMasterService)
                .toggleInstrumentTypeMasterActive(1L);
    }

    private InstrumentTypeMaster createInstrumentType() {
        InstrumentTypeMaster instrumentType =
                new InstrumentTypeMaster(
                        "ST",
                        "Stratocaster",
                        "Stratocaster",
                        "Stratocaster",
                        true);
        instrumentType.setId(1L);
        return instrumentType;
    }
}
