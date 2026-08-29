package com.example.guitarmes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.guitarmes.dto.NeckMasterUpdateRequest;
import com.example.guitarmes.entity.NeckMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.repository.NeckMasterRepository;

@ExtendWith(MockitoExtension.class)
class NeckMasterServiceTest {

    @Mock
    private NeckMasterRepository neckMasterRepository;

    private NeckMasterService neckMasterService;

    @BeforeEach
    void setUp() {
        neckMasterService =
                new NeckMasterService(neckMasterRepository);
    }

    @Test
    @DisplayName("ネックマスタ一覧を取得できる")
    void getNeckMasters_returnsRepositoryResult() {
        List<NeckMaster> expected = List.of(createNeckMaster());
        when(neckMasterRepository.findAll()).thenReturn(expected);
        assertSame(expected, neckMasterService.getNeckMasters());
    }

    @Test
    @DisplayName("IDでネックマスタを取得できる")
    void getNeckMasterById_succeeds() {
        NeckMaster expected = createNeckMaster();
        when(neckMasterRepository.findById(1L))
                .thenReturn(Optional.of(expected));
        assertSame(expected, neckMasterService.getNeckMasterById(1L));
    }

    @Test
    @DisplayName("存在しないネックマスタを拒否する")
    void getNeckMasterById_unknown_throws() {
        when(neckMasterRepository.findById(999L))
                .thenReturn(Optional.empty());
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> neckMasterService.getNeckMasterById(999L));
        assertTrue(exception.getMessage().contains("存在しません"));
    }

    @Test
    @DisplayName("編集画面用リクエストへ全項目を変換できる")
    void getNeckMasterUpdateRequest_mapsFields() {
        NeckMaster neckMaster = createNeckMaster();
        when(neckMasterRepository.findById(1L))
                .thenReturn(Optional.of(neckMaster));
        NeckMasterUpdateRequest request =
                neckMasterService.getNeckMasterUpdateRequest(1L);
        assertEquals("NM-TEST-0001", request.getModelCode());
        assertEquals("Test Neck", request.getModelName());
        assertEquals("MIJ-HER50-ST", request.getProductFamilyCode());
        assertEquals("Stratocaster", request.getNeckType());
        assertEquals("Maple", request.getNeckMaterial());
        assertEquals("Rosewood", request.getFingerboardMaterial());
        assertEquals(21, request.getFretCount());
        assertEquals("25.5", request.getScale());
    }

    @Test
    @DisplayName("ネックマスタを登録できる")
    void createNeckMaster_succeeds() {
        NeckMaster neckMaster = createNeckMaster();
        when(neckMasterRepository.save(neckMaster))
                .thenReturn(neckMaster);
        assertSame(neckMaster,
                neckMasterService.createNeckMaster(neckMaster));
    }

    @Test
    @DisplayName("モデル名を更新できる")
    void updateNeckMaster_changesModelNameOnly() {
        NeckMaster neckMaster = createNeckMaster();
        NeckMasterUpdateRequest request = createUpdateRequest();
        request.setModelName(" Updated Neck Name ");
        request.setModelCode("CHANGED-CODE");
        request.setProductFamilyCode("CHANGED-FAMILY");
        request.setNeckType("Telecaster");
        request.setNeckMaterial("Mahogany");
        request.setFingerboardMaterial("Maple");
        request.setFretCount(22);
        request.setScale("24.75");
        when(neckMasterRepository.findById(1L))
                .thenReturn(Optional.of(neckMaster));
        when(neckMasterRepository.save(neckMaster))
                .thenReturn(neckMaster);
        NeckMaster result = neckMasterService.updateNeckMaster(1L, request);
        assertEquals("Updated Neck Name", result.getModelName());
        assertEquals("NM-TEST-0001", result.getModelCode());
        assertEquals("MIJ-HER50-ST", result.getProductFamilyCode());
        assertEquals("Stratocaster", result.getNeckType());
        assertEquals("Maple", result.getNeckMaterial());
        assertEquals("Rosewood", result.getFingerboardMaterial());
        assertEquals(21, result.getFretCount());
        assertEquals("25.5", result.getScale());
    }

    @Test
    @DisplayName("モデル名未入力を拒否する")
    void updateNeckMaster_blankModelName_throws() {
        NeckMasterUpdateRequest request = createUpdateRequest();
        request.setModelName(" ");
        when(neckMasterRepository.findById(1L))
                .thenReturn(Optional.of(createNeckMaster()));
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> neckMasterService.updateNeckMaster(1L, request));
        assertTrue(exception.getMessage().contains("モデル名を入力"));
        verify(neckMasterRepository, never()).save(any(NeckMaster.class));
    }

    @Test
    @DisplayName("存在しないIDの更新を拒否する")
    void updateNeckMaster_unknownId_throws() {
        when(neckMasterRepository.findById(999L))
                .thenReturn(Optional.empty());
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> neckMasterService.updateNeckMaster(
                        999L, createUpdateRequest()));
        assertTrue(exception.getMessage().contains("存在しません"));
    }

    @Test
    @DisplayName("nullの更新リクエストを拒否する")
    void updateNeckMaster_nullRequest_throws() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> neckMasterService.updateNeckMaster(1L, null));
        assertTrue(exception.getMessage().contains("指定されていません"));
        verify(neckMasterRepository, never()).findById(any());
    }

    private NeckMaster createNeckMaster() {
        NeckMaster neckMaster = new NeckMaster(
                "NM-TEST-0001",
                "Test Neck",
                "Stratocaster",
                "Maple",
                "Rosewood",
                21,
                "25.5");
        neckMaster.setProductFamilyCode("MIJ-HER50-ST");
        return neckMaster;
    }

    private NeckMasterUpdateRequest createUpdateRequest() {
        NeckMasterUpdateRequest request = new NeckMasterUpdateRequest();
        request.setModelCode("NM-TEST-0001");
        request.setModelName("Test Neck");
        request.setProductFamilyCode("MIJ-HER50-ST");
        request.setNeckType("Stratocaster");
        request.setNeckMaterial("Maple");
        request.setFingerboardMaterial("Rosewood");
        request.setFretCount(21);
        request.setScale("25.5");
        return request;
    }
}
