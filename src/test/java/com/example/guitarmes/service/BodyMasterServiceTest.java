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

import com.example.guitarmes.dto.BodyMasterUpdateRequest;
import com.example.guitarmes.entity.BodyMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.repository.BodyMasterRepository;

@ExtendWith(MockitoExtension.class)
class BodyMasterServiceTest {

    @Mock
    private BodyMasterRepository bodyMasterRepository;

    private BodyMasterService bodyMasterService;

    @BeforeEach
    void setUp() {
        bodyMasterService =
                new BodyMasterService(
                        bodyMasterRepository);
    }

    @Test
    @DisplayName("ボディマスタ一覧を取得できる")
    void getBodyMasters_returnsRepositoryResult() {
        List<BodyMaster> expected =
                List.of(createBodyMaster());
        when(bodyMasterRepository.findAll())
                .thenReturn(expected);
        assertSame(
                expected,
                bodyMasterService.getBodyMasters());
    }

    @Test
    @DisplayName("IDでボディマスタを取得できる")
    void getBodyMasterById_succeeds() {
        BodyMaster expected = createBodyMaster();
        when(bodyMasterRepository.findById(1L))
                .thenReturn(Optional.of(expected));
        assertSame(
                expected,
                bodyMasterService.getBodyMasterById(1L));
    }

    @Test
    @DisplayName("存在しないボディマスタを拒否する")
    void getBodyMasterById_unknown_throws() {
        when(bodyMasterRepository.findById(999L))
                .thenReturn(Optional.empty());
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> bodyMasterService
                        .getBodyMasterById(999L));
        assertTrue(exception.getMessage()
                .contains("存在しません"));
    }

    @Test
    @DisplayName("編集画面用リクエストへ全項目を変換できる")
    void getBodyMasterUpdateRequest_mapsFields() {
        BodyMaster bodyMaster = createBodyMaster();
        when(bodyMasterRepository.findById(1L))
                .thenReturn(Optional.of(bodyMaster));
        BodyMasterUpdateRequest request =
                bodyMasterService
                        .getBodyMasterUpdateRequest(1L);
        assertEquals("BM-TEST-0001", request.getModelCode());
        assertEquals("Test Body", request.getModelName());
        assertEquals("MIJ-HER50-ST", request.getProductFamilyCode());
        assertEquals("Stratocaster", request.getBodyType());
        assertEquals("Alder", request.getMaterial());
        assertEquals("3-Color Sunburst", request.getColor());
    }

    @Test
    @DisplayName("ボディマスタを登録できる")
    void createBodyMaster_succeeds() {
        BodyMaster bodyMaster = createBodyMaster();
        when(bodyMasterRepository.save(bodyMaster))
                .thenReturn(bodyMaster);
        assertSame(
                bodyMaster,
                bodyMasterService
                        .createBodyMaster(bodyMaster));
    }

    @Test
    @DisplayName("モデル名を更新できる")
    void updateBodyMaster_changesModelName() {
        BodyMaster bodyMaster = createBodyMaster();
        BodyMasterUpdateRequest request =
                createUpdateRequest();
        request.setModelName(" Updated Body Name ");
        request.setModelCode("CHANGED-CODE");
        request.setProductFamilyCode("CHANGED-FAMILY");
        request.setBodyType("Telecaster");
        request.setMaterial("Mahogany");
        request.setColor("Black");
        when(bodyMasterRepository.findById(1L))
                .thenReturn(Optional.of(bodyMaster));
        when(bodyMasterRepository.save(bodyMaster))
                .thenReturn(bodyMaster);
        BodyMaster result =
                bodyMasterService.updateBodyMaster(
                        1L,
                        request);
        assertEquals("Updated Body Name", result.getModelName());
        assertEquals("BM-TEST-0001", result.getModelCode());
        assertEquals("MIJ-HER50-ST", result.getProductFamilyCode());
        assertEquals("Stratocaster", result.getBodyType());
        assertEquals("Alder", result.getMaterial());
        assertEquals("3-Color Sunburst", result.getColor());
    }

    @Test
    @DisplayName("モデル名未入力を拒否する")
    void updateBodyMaster_blankModelName_throws() {
        BodyMaster bodyMaster = createBodyMaster();
        BodyMasterUpdateRequest request =
                createUpdateRequest();
        request.setModelName(" ");
        when(bodyMasterRepository.findById(1L))
                .thenReturn(Optional.of(bodyMaster));
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> bodyMasterService.updateBodyMaster(
                        1L,
                        request));
        assertTrue(exception.getMessage()
                .contains("モデル名を入力"));
        verify(bodyMasterRepository, never())
                .save(any(BodyMaster.class));
    }

    @Test
    @DisplayName("存在しないIDの更新を拒否する")
    void updateBodyMaster_unknownId_throws() {
        when(bodyMasterRepository.findById(999L))
                .thenReturn(Optional.empty());
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> bodyMasterService.updateBodyMaster(
                        999L,
                        createUpdateRequest()));
        assertTrue(exception.getMessage()
                .contains("存在しません"));
    }

    @Test
    @DisplayName("nullの更新リクエストを拒否する")
    void updateBodyMaster_nullRequest_throws() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> bodyMasterService.updateBodyMaster(
                        1L,
                        null));
        assertTrue(exception.getMessage()
                .contains("指定されていません"));
        verify(bodyMasterRepository, never())
                .findById(any());
    }

    private BodyMaster createBodyMaster() {
        BodyMaster bodyMaster = new BodyMaster();
        bodyMaster.setId(1L);
        bodyMaster.setModelCode("BM-TEST-0001");
        bodyMaster.setModelName("Test Body");
        bodyMaster.setProductFamilyCode("MIJ-HER50-ST");
        bodyMaster.setBodyType("Stratocaster");
        bodyMaster.setMaterial("Alder");
        bodyMaster.setColor("3-Color Sunburst");
        return bodyMaster;
    }

    private BodyMasterUpdateRequest
            createUpdateRequest() {
        BodyMasterUpdateRequest request =
                new BodyMasterUpdateRequest();
        request.setModelCode("BM-TEST-0001");
        request.setModelName("Test Body");
        request.setProductFamilyCode("MIJ-HER50-ST");
        request.setBodyType("Stratocaster");
        request.setMaterial("Alder");
        request.setColor("3-Color Sunburst");
        return request;
    }
}
