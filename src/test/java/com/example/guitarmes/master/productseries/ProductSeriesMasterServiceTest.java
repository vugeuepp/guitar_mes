package com.example.guitarmes.master.productseries;

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

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.master.productseries.ProductSeriesMaster;
import com.example.guitarmes.master.productseries.ProductSeriesMasterRepository;
import com.example.guitarmes.master.productseries.ProductSeriesMasterService;

@ExtendWith(MockitoExtension.class)
class ProductSeriesMasterServiceTest {

    @Mock
    private ProductSeriesMasterRepository repository;

    private ProductSeriesMasterService service;

    @BeforeEach
    void setUp() {
        service = new ProductSeriesMasterService(repository);
    }

    @Test
    @DisplayName("製品シリーズ一覧を取得する")
    void getProductSeriesMasters_returnsRepositoryResult() {
        List<ProductSeriesMaster> expected = List.of(series("A", "Alpha", true));
        when(repository.findAllByOrderBySeriesNameAsc()).thenReturn(expected);
        assertSame(expected, service.getProductSeriesMasters());
    }

    @Test
    @DisplayName("有効な製品シリーズ一覧を取得する")
    void getActiveProductSeriesMasters_returnsRepositoryResult() {
        List<ProductSeriesMaster> expected = List.of(series("A", "Alpha", true));
        when(repository.findByActiveTrueOrderBySeriesNameAsc()).thenReturn(expected);
        assertSame(expected, service.getActiveProductSeriesMasters());
    }

    @Test
    @DisplayName("現在使用中の無効シリーズを編集候補へ追加する")
    void getProductSeriesMastersForEdit_addsCurrentInactive() {
        ProductSeriesMaster active = series("A", "Alpha", true);
        ProductSeriesMaster inactive = series("B", "Beta", false);
        when(repository.findByActiveTrueOrderBySeriesNameAsc()).thenReturn(List.of(active));
        when(repository.findBySeriesCodeIgnoreCase("B")).thenReturn(Optional.of(inactive));
        List<ProductSeriesMaster> result = service.getProductSeriesMastersForEdit("B");
        assertEquals(2, result.size());
        assertTrue(result.contains(inactive));
    }

    @Test
    @DisplayName("現在使用中の有効シリーズを重複追加しない")
    void getProductSeriesMastersForEdit_doesNotDuplicateActive() {
        ProductSeriesMaster active = series("A", "Alpha", true);
        when(repository.findByActiveTrueOrderBySeriesNameAsc()).thenReturn(List.of(active));
        when(repository.findBySeriesCodeIgnoreCase("A")).thenReturn(Optional.of(active));
        assertEquals(1, service.getProductSeriesMastersForEdit("A").size());
    }

    @Test
    @DisplayName("存在するシリーズを取得できる")
    void getRequiredProductSeriesMaster_succeeds() {
        ProductSeriesMaster expected = series("MIJ-H2", "Hybrid II", true);
        when(repository.findBySeriesCodeIgnoreCase("MIJ-H2")).thenReturn(Optional.of(expected));
        assertSame(expected, service.getRequiredProductSeriesMaster(" mij-h2 "));
    }

    @Test
    @DisplayName("存在しないシリーズを拒否する")
    void getRequiredProductSeriesMaster_unknown_throws() {
        when(repository.findBySeriesCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getRequiredProductSeriesMaster("unknown"));
        assertTrue(ex.getMessage().contains("登録されていません"));
    }

    @Test
    @DisplayName("無効シリーズの新規利用を拒否する")
    void getRequiredActiveProductSeriesMaster_inactive_throws() {
        ProductSeriesMaster inactive = series("A", "Alpha", false);
        when(repository.findBySeriesCodeIgnoreCase("A")).thenReturn(Optional.of(inactive));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getRequiredActiveProductSeriesMaster("A"));
        assertTrue(ex.getMessage().contains("無効"));
    }

    @Test
    @DisplayName("現在使用中の無効シリーズは維持できる")
    void getRequiredProductSeriesMasterForUpdate_currentInactive_succeeds() {
        ProductSeriesMaster inactive = series("A", "Alpha", false);
        when(repository.findBySeriesCodeIgnoreCase("A")).thenReturn(Optional.of(inactive));
        assertSame(inactive, service.getRequiredProductSeriesMasterForUpdate("A", "A"));
    }

    @Test
    @DisplayName("別の無効シリーズへの変更を拒否する")
    void getRequiredProductSeriesMasterForUpdate_differentInactive_throws() {
        ProductSeriesMaster inactive = series("B", "Beta", false);
        when(repository.findBySeriesCodeIgnoreCase("B")).thenReturn(Optional.of(inactive));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getRequiredProductSeriesMasterForUpdate("B", "A"));
        assertTrue(ex.getMessage().contains("無効"));
    }

    @Test
    @DisplayName("製品シリーズを正常に登録できる")
    void createProductSeriesMaster_valid_succeeds() {
        ProductSeriesMaster request = series("mij new", " New Series ", false);
        when(repository.existsBySeriesCodeIgnoreCase("MIJ-NEW")).thenReturn(false);
        when(repository.save(any(ProductSeriesMaster.class))).thenAnswer(i -> i.getArgument(0));
        ProductSeriesMaster result = service.createProductSeriesMaster(request);
        assertEquals("MIJ-NEW", result.getSeriesCode());
        assertEquals("New Series", result.getSeriesName());
        assertEquals(Boolean.TRUE, result.getActive());
    }

    @Test
    @DisplayName("重複するシリーズコードを拒否する")
    void createProductSeriesMaster_duplicate_throws() {
        when(repository.existsBySeriesCodeIgnoreCase("MIJ-H2")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProductSeriesMaster(series("mij-h2", "Hybrid", true)));
        assertTrue(ex.getMessage().contains("既に登録"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("不正文字を含むシリーズコードを拒否する")
    void createProductSeriesMaster_invalidCharacter_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProductSeriesMaster(series("MIJ@", "Invalid", true)));
        assertTrue(ex.getMessage().contains("半角英数字"));
    }

    @Test
    @DisplayName("不正なハイフン位置を拒否する")
    void createProductSeriesMaster_invalidHyphen_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProductSeriesMaster(series("MIJ--H2", "Invalid", true)));
        assertTrue(ex.getMessage().contains("ハイフンの位置"));
    }

    @Test
    @DisplayName("50文字を超えるシリーズコードを拒否する")
    void createProductSeriesMaster_longCode_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProductSeriesMaster(series("A".repeat(51), "Long", true)));
        assertTrue(ex.getMessage().contains("50文字以内"));
    }

    @Test
    @DisplayName("シリーズ名未入力を拒否する")
    void createProductSeriesMaster_blankName_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProductSeriesMaster(series("A", " ", true)));
        assertTrue(ex.getMessage().contains("シリーズ名を入力"));
    }

    @Test
    @DisplayName("150文字を超えるシリーズ名を拒否する")
    void createProductSeriesMaster_longName_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createProductSeriesMaster(series("A", "N".repeat(151), true)));
        assertTrue(ex.getMessage().contains("150文字以内"));
    }

    @Test
    @DisplayName("シリーズ名を更新できコードは変更されない")
    void updateProductSeriesMaster_changesNameOnly() {
        ProductSeriesMaster current = series("MIJ-H2", "Hybrid II", true);
        current.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.save(current)).thenReturn(current);
        ProductSeriesMaster result = service.updateProductSeriesMaster(
                1L, series("CHANGED", " Hybrid II Updated ", false));
        assertEquals("MIJ-H2", result.getSeriesCode());
        assertEquals("Hybrid II Updated", result.getSeriesName());
        assertEquals(Boolean.TRUE, result.getActive());
    }

    @Test
    @DisplayName("更新時も長すぎるシリーズ名を拒否する")
    void updateProductSeriesMaster_longName_throws() {
        ProductSeriesMaster current = series("A", "Alpha", true);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProductSeriesMaster(
                        1L, series("A", "N".repeat(151), true)));
        assertTrue(ex.getMessage().contains("150文字以内"));
    }

    @Test
    @DisplayName("有効状態を切り替えられる")
    void toggleProductSeriesMasterActive_toggles() {
        ProductSeriesMaster current = series("A", "Alpha", true);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.save(current)).thenReturn(current);
        assertEquals(Boolean.FALSE,
                service.toggleProductSeriesMasterActive(1L).getActive());
    }

    @Test
    @DisplayName("存在しないIDの更新を拒否する")
    void updateProductSeriesMaster_unknownId_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProductSeriesMaster(
                        999L, series("A", "Alpha", true)));
        assertTrue(ex.getMessage().contains("存在しません"));
    }

    private ProductSeriesMaster series(
            String code,
            String name,
            boolean active) {
        return new ProductSeriesMaster(code, name, active);
    }
}
