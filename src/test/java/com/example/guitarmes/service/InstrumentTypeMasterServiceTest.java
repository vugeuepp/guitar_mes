package com.example.guitarmes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.guitarmes.entity.InstrumentTypeMaster;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.repository.InstrumentTypeMasterRepository;

@ExtendWith(MockitoExtension.class)
class InstrumentTypeMasterServiceTest {

    @Mock
    private InstrumentTypeMasterRepository
            instrumentTypeMasterRepository;

    private InstrumentTypeMasterService
            instrumentTypeMasterService;

    @BeforeEach
    void setUp() {

        instrumentTypeMasterService =
                new InstrumentTypeMasterService(
                        instrumentTypeMasterRepository);
    }

    @Test
    @DisplayName("楽器タイプ一覧を名前順Repositoryから取得する")
    void getInstrumentTypeMasters_returnsRepositoryResult() {

        List<InstrumentTypeMaster> expected =
                List.of(
                        createRequest(
                                "DUO",
                                "Duo-Sonic",
                                "Duo-Sonic",
                                "Duo-Sonic"));

        when(instrumentTypeMasterRepository
                .findAllByOrderByInstrumentNameAsc())
                .thenReturn(expected);

        List<InstrumentTypeMaster> actual =
                instrumentTypeMasterService
                        .getInstrumentTypeMasters();

        assertSame(expected, actual);
    }

    @Test
    @DisplayName("有効な楽器タイプ一覧を名前順Repositoryから取得する")
    void getActiveInstrumentTypeMasters_returnsRepositoryResult() {

        List<InstrumentTypeMaster> expected =
                List.of(
                        createRequest(
                                "DUO",
                                "Duo-Sonic",
                                "Duo-Sonic",
                                "Duo-Sonic"));

        when(instrumentTypeMasterRepository
                .findByActiveTrueOrderByInstrumentNameAsc())
                .thenReturn(expected);

        List<InstrumentTypeMaster> actual =
                instrumentTypeMasterService
                        .getActiveInstrumentTypeMasters();

        assertSame(expected, actual);
    }

    @Test
    @DisplayName("楽器タイプを正常に登録できる")
    void createInstrumentTypeMaster_withValidRequest_succeeds() {

        InstrumentTypeMaster request =
                createRequest(
                        "DUO",
                        "Duo-Sonic",
                        "Duo-Sonic",
                        "Duo-Sonic");

        when(instrumentTypeMasterRepository
                .existsByInstrumentCodeIgnoreCase(
                        "DUO"))
                .thenReturn(false);

        when(instrumentTypeMasterRepository
                .save(org.mockito.ArgumentMatchers
                        .any(InstrumentTypeMaster.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        InstrumentTypeMaster result =
                instrumentTypeMasterService
                        .createInstrumentTypeMaster(
                                request);

        assertEquals("DUO", result.getInstrumentCode());
        assertEquals("Duo-Sonic", result.getInstrumentName());
        assertEquals("Duo-Sonic", result.getBodyType());
        assertEquals("Duo-Sonic", result.getNeckType());
        assertEquals(Boolean.TRUE, result.getActive());
    }

    @Test
    @DisplayName("楽器タイプコードを大文字に正規化する")
    void createInstrumentTypeMaster_normalizesCodeToUpperCase() {

        InstrumentTypeMaster request =
                createRequest(
                        "duo",
                        "Duo-Sonic",
                        "Duo-Sonic",
                        "Duo-Sonic");

        stubSuccessfulSave("DUO");

        InstrumentTypeMaster result =
                instrumentTypeMasterService
                        .createInstrumentTypeMaster(
                                request);

        assertEquals("DUO", result.getInstrumentCode());
        verify(instrumentTypeMasterRepository)
                .existsByInstrumentCodeIgnoreCase("DUO");
    }

    @Test
    @DisplayName("楽器タイプコード内の空白をハイフンに変換する")
    void createInstrumentTypeMaster_replacesWhitespaceWithHyphen() {

        InstrumentTypeMaster request =
                createRequest(
                        "duo sonic",
                        "Duo-Sonic",
                        "Duo-Sonic",
                        "Duo-Sonic");

        stubSuccessfulSave("DUO-SONIC");

        InstrumentTypeMaster result =
                instrumentTypeMasterService
                        .createInstrumentTypeMaster(
                                request);

        assertEquals(
                "DUO-SONIC",
                result.getInstrumentCode());
    }

    @Test
    @DisplayName("登録時に各文字列の前後空白を除去する")
    void createInstrumentTypeMaster_trimsTextFields() {

        InstrumentTypeMaster request =
                createRequest(
                        "  duo  ",
                        "  Duo-Sonic  ",
                        "  Duo-Sonic Body  ",
                        "  Duo-Sonic Neck  ");

        stubSuccessfulSave("DUO");

        InstrumentTypeMaster result =
                instrumentTypeMasterService
                        .createInstrumentTypeMaster(
                                request);

        assertEquals("DUO", result.getInstrumentCode());
        assertEquals("Duo-Sonic", result.getInstrumentName());
        assertEquals("Duo-Sonic Body", result.getBodyType());
        assertEquals("Duo-Sonic Neck", result.getNeckType());
    }

    @Test
    @DisplayName("大文字小文字を無視してコード重複を拒否する")
    void createInstrumentTypeMaster_withDuplicateCode_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "st",
                        "Another Stratocaster",
                        "Stratocaster",
                        "Stratocaster");

        when(instrumentTypeMasterRepository
                .existsByInstrumentCodeIgnoreCase("ST"))
                .thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage().contains("ST"));
        assertTrue(exception.getMessage().contains("既に登録"));
        verify(instrumentTypeMasterRepository, never())
                .save(org.mockito.ArgumentMatchers
                        .any(InstrumentTypeMaster.class));
    }

    @Test
    @DisplayName("コードに不正文字が含まれる場合は拒否する")
    void createInstrumentTypeMaster_withInvalidCharacter_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "ST@",
                        "Invalid",
                        "Invalid",
                        "Invalid");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage()
                .contains("半角英数字とハイフン"));
        verifyNoRepositoryWrite();
    }

    @Test
    @DisplayName("先頭ハイフンのコードを拒否する")
    void createInstrumentTypeMaster_withLeadingHyphen_throws() {

        assertInvalidHyphenPosition("-ST");
    }

    @Test
    @DisplayName("末尾ハイフンのコードを拒否する")
    void createInstrumentTypeMaster_withTrailingHyphen_throws() {

        assertInvalidHyphenPosition("ST-");
    }

    @Test
    @DisplayName("連続ハイフンのコードを拒否する")
    void createInstrumentTypeMaster_withConsecutiveHyphens_throws() {

        assertInvalidHyphenPosition("ST--CUSTOM");
    }

    @Test
    @DisplayName("20文字を超えるコードを拒否する")
    void createInstrumentTypeMaster_withTooLongCode_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "ABCDEFGHIJKLMNOPQRSTU",
                        "Too Long",
                        "Body",
                        "Neck");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage().contains("20文字以内"));
        verifyNoRepositoryWrite();
    }

    @Test
    @DisplayName("楽器タイプコード未入力を拒否する")
    void createInstrumentTypeMaster_withoutCode_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        " ",
                        "Duo-Sonic",
                        "Duo-Sonic",
                        "Duo-Sonic");

        assertRequiredFieldError(
                request,
                "楽器タイプコード");
    }

    @Test
    @DisplayName("楽器タイプ名未入力を拒否する")
    void createInstrumentTypeMaster_withoutName_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "DUO",
                        " ",
                        "Duo-Sonic",
                        "Duo-Sonic");

        assertRequiredFieldError(
                request,
                "楽器タイプ名");
    }

    @Test
    @DisplayName("ボディタイプ未入力を拒否する")
    void createInstrumentTypeMaster_withoutBodyType_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "DUO",
                        "Duo-Sonic",
                        " ",
                        "Duo-Sonic");

        assertRequiredFieldError(
                request,
                "ボディタイプ");
    }

    @Test
    @DisplayName("ネックタイプ未入力を拒否する")
    void createInstrumentTypeMaster_withoutNeckType_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "DUO",
                        "Duo-Sonic",
                        "Duo-Sonic",
                        " ");

        assertRequiredFieldError(
                request,
                "ネックタイプ");
    }

    @Test
    @DisplayName("100文字を超える楽器タイプ名を拒否する")
    void createInstrumentTypeMaster_withTooLongName_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "DUO",
                        "A".repeat(101),
                        "Body",
                        "Neck");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage()
                .contains("楽器タイプ名は100文字以内"));
        verifyNoRepositoryWrite();
    }

    @Test
    @DisplayName("100文字を超えるボディタイプを拒否する")
    void createInstrumentTypeMaster_withTooLongBodyType_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "DUO",
                        "Duo-Sonic",
                        "B".repeat(101),
                        "Neck");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage()
                .contains("ボディタイプは100文字以内"));
        verifyNoRepositoryWrite();
    }

    @Test
    @DisplayName("100文字を超えるネックタイプを拒否する")
    void createInstrumentTypeMaster_withTooLongNeckType_throws() {

        InstrumentTypeMaster request =
                createRequest(
                        "DUO",
                        "Duo-Sonic",
                        "Body",
                        "N".repeat(101));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage()
                .contains("ネックタイプは100文字以内"));
        verifyNoRepositoryWrite();
    }

    @Test
    @DisplayName("存在する有効な楽器タイプを取得できる")
    void getRequiredActiveInstrumentTypeMaster_withActiveType_succeeds() {

        InstrumentTypeMaster expected =
                createRequest(
                        "ST",
                        "Stratocaster",
                        "Stratocaster",
                        "Stratocaster");
        expected.setActive(true);

        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("ST"))
                .thenReturn(Optional.of(expected));

        InstrumentTypeMaster actual =
                instrumentTypeMasterService
                        .getRequiredActiveInstrumentTypeMaster(
                                " st ");

        assertSame(expected, actual);
        verify(instrumentTypeMasterRepository)
                .findByInstrumentCodeIgnoreCase("ST");
    }

    @Test
    @DisplayName("存在しない楽器タイプを拒否する")
    void getRequiredActiveInstrumentTypeMaster_withUnknownType_throws() {

        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("UNKNOWN"))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .getRequiredActiveInstrumentTypeMaster(
                                        "unknown"));

        assertTrue(exception.getMessage()
                .contains("登録されていません"));
    }

    @Test
    @DisplayName("無効な楽器タイプを拒否する")
    void getRequiredActiveInstrumentTypeMaster_withInactiveType_throws() {

        InstrumentTypeMaster inactive =
                createRequest(
                        "ST",
                        "Stratocaster",
                        "Stratocaster",
                        "Stratocaster");
        inactive.setActive(false);

        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("ST"))
                .thenReturn(Optional.of(inactive));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .getRequiredActiveInstrumentTypeMaster(
                                        "ST"));

        assertTrue(exception.getMessage().contains("無効"));
        assertTrue(exception.getMessage()
                .contains("Stratocaster"));
    }

    @Test
    @DisplayName("編集候補として有効な楽器タイプ一覧を返す")
    void getInstrumentTypeMastersForEdit_withActiveTypes_returnsActiveTypes() {

        InstrumentTypeMaster stratocaster =
                createInstrumentType(
                        "ST",
                        "Stratocaster",
                        true);
        InstrumentTypeMaster telecaster =
                createInstrumentType(
                        "TL",
                        "Telecaster",
                        true);
        List<InstrumentTypeMaster> activeTypes =
                List.of(
                        stratocaster,
                        telecaster);

        when(instrumentTypeMasterRepository
                .findByActiveTrueOrderByInstrumentNameAsc())
                .thenReturn(activeTypes);
        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("ST"))
                .thenReturn(Optional.of(stratocaster));

        List<InstrumentTypeMaster> result =
                instrumentTypeMasterService
                        .getInstrumentTypeMastersForEdit("ST");

        assertEquals(2, result.size());
        assertSame(stratocaster, result.get(0));
        assertSame(telecaster, result.get(1));
    }

    @Test
    @DisplayName("現在使用中の無効な楽器タイプを編集候補へ追加する")
    void getInstrumentTypeMastersForEdit_withCurrentInactiveType_addsCurrentType() {

        InstrumentTypeMaster stratocaster =
                createInstrumentType(
                        "ST",
                        "Stratocaster",
                        true);
        InstrumentTypeMaster duoSonic =
                createInstrumentType(
                        "DUO",
                        "Duo-Sonic",
                        false);

        when(instrumentTypeMasterRepository
                .findByActiveTrueOrderByInstrumentNameAsc())
                .thenReturn(List.of(stratocaster));
        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("DUO"))
                .thenReturn(Optional.of(duoSonic));

        List<InstrumentTypeMaster> result =
                instrumentTypeMasterService
                        .getInstrumentTypeMastersForEdit("duo");

        assertEquals(2, result.size());
        assertTrue(result.contains(stratocaster));
        assertTrue(result.contains(duoSonic));
    }

    @Test
    @DisplayName("現在使用中の有効な楽器タイプを編集候補へ重複追加しない")
    void getInstrumentTypeMastersForEdit_withCurrentActiveType_doesNotDuplicate() {

        InstrumentTypeMaster stratocaster =
                createInstrumentType(
                        "ST",
                        "Stratocaster",
                        true);

        when(instrumentTypeMasterRepository
                .findByActiveTrueOrderByInstrumentNameAsc())
                .thenReturn(List.of(stratocaster));
        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("ST"))
                .thenReturn(Optional.of(stratocaster));

        List<InstrumentTypeMaster> result =
                instrumentTypeMasterService
                        .getInstrumentTypeMastersForEdit("ST");

        assertEquals(1, result.size());
        assertSame(stratocaster, result.get(0));
    }

    @Test
    @DisplayName("現在使用中の無効な楽器タイプは維持できる")
    void getRequiredInstrumentTypeMasterForUpdate_withCurrentInactiveType_succeeds() {

        InstrumentTypeMaster inactive =
                createInstrumentType(
                        "DUO",
                        "Duo-Sonic",
                        false);

        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("DUO"))
                .thenReturn(Optional.of(inactive));

        InstrumentTypeMaster result =
                instrumentTypeMasterService
                        .getRequiredInstrumentTypeMasterForUpdate(
                                "duo",
                                "DUO");

        assertSame(inactive, result);
    }

    @Test
    @DisplayName("別の無効な楽器タイプへの変更を拒否する")
    void getRequiredInstrumentTypeMasterForUpdate_toDifferentInactiveType_throws() {

        InstrumentTypeMaster inactive =
                createInstrumentType(
                        "DUO",
                        "Duo-Sonic",
                        false);

        when(instrumentTypeMasterRepository
                .findByInstrumentCodeIgnoreCase("DUO"))
                .thenReturn(Optional.of(inactive));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .getRequiredInstrumentTypeMasterForUpdate(
                                        "DUO",
                                        "ST"));

        assertTrue(exception.getMessage().contains("無効"));
        assertTrue(exception.getMessage().contains("Duo-Sonic"));
    }

    private InstrumentTypeMaster createInstrumentType(
            String instrumentCode,
            String instrumentName,
            boolean active) {

        InstrumentTypeMaster type =
                createRequest(
                        instrumentCode,
                        instrumentName,
                        instrumentName,
                        instrumentName);
        type.setActive(active);
        return type;
    }

    private InstrumentTypeMaster createRequest(
            String instrumentCode,
            String instrumentName,
            String bodyType,
            String neckType) {

        return new InstrumentTypeMaster(
                instrumentCode,
                instrumentName,
                bodyType,
                neckType,
                true);
    }

    private void stubSuccessfulSave(
            String normalizedCode) {

        when(instrumentTypeMasterRepository
                .existsByInstrumentCodeIgnoreCase(
                        normalizedCode))
                .thenReturn(false);

        when(instrumentTypeMasterRepository
                .save(org.mockito.ArgumentMatchers
                        .any(InstrumentTypeMaster.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
    }

    private void assertInvalidHyphenPosition(
            String instrumentCode) {

        InstrumentTypeMaster request =
                createRequest(
                        instrumentCode,
                        "Invalid",
                        "Invalid",
                        "Invalid");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage()
                .contains("ハイフンの位置が不正"));
        verifyNoRepositoryWrite();
    }

    private void assertRequiredFieldError(
            InstrumentTypeMaster request,
            String fieldName) {

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> instrumentTypeMasterService
                                .createInstrumentTypeMaster(
                                        request));

        assertTrue(exception.getMessage()
                .contains(fieldName + "を入力してください"));
        verifyNoRepositoryWrite();
    }

    private void verifyNoRepositoryWrite() {

        verify(instrumentTypeMasterRepository, never())
                .save(org.mockito.ArgumentMatchers
                        .any(InstrumentTypeMaster.class));
    }
}
