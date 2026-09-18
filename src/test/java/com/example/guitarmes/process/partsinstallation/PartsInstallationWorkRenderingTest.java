package com.example.guitarmes.process.partsinstallation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.process.*;
import com.example.guitarmes.process.work.*;
import com.example.guitarmes.product.parts.*;

class PartsInstallationWorkRenderingTest {
    private final ProcessHistoryRepository histories = mock(ProcessHistoryRepository.class);
    private final ProcessWorkRepository works = mock(ProcessWorkRepository.class);
    private final ProcessWorkItemRepository items = mock(ProcessWorkItemRepository.class);
    private final ManufacturingProcessRepository processes = mock(ManufacturingProcessRepository.class);
    private final GuitarService guitars = mock(GuitarService.class);
    private final PartsInstallationWorkViewService service =
            new PartsInstallationWorkViewService(histories, works, items, processes, guitars);
    private ProcessHistory history;
    private ProcessWork work;
    private MockMvc mvc;

    @BeforeEach void setup() {
        history = new ProcessHistory(2L, 3L, "Worker", LocalDateTime.of(2026, 9, 17, 10, 0));
        history.setId(9L);
        work = new ProcessWork(); work.setId(10L); work.setProcessHistory(history);
        work.setBridgeType(BridgeType.SIX_POINT); work.setBridgeModel(" ");
        work.setRequiresStudHoleExpansion(false);
        work.setTunerModel("Snapshot Tuner"); work.setTunerMountingType(TunerMountingType.PRESS_BUSHING);
        work.setTunerBushRequired(true); work.setTunerLayout(TunerLayout.SIX_IN_LINE);
        work.setPickupLayout("SSS"); work.setSelectorPositions(5); work.setControlLayout("1Vol. 2Tone");
        work.setJackMountingType(JackMountingType.BOAT_PLATE);
        work.setStringModel("Snapshot Strings"); work.setStringGauge("09-42");
        when(histories.findById(9L)).thenReturn(Optional.of(history));
        when(works.findByProcessHistoryId(9L)).thenReturn(Optional.of(work));
        when(guitars.getGuitarById(2L)).thenReturn(new Guitar("TEST-SERIAL", "表示工程"));
        when(processes.findById(3L)).thenReturn(Optional.of(new ManufacturingProcess("GUITAR", "表示工程", 1)));
        var templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/"); templates.setSuffix(".html"); templates.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine); views.setCharacterEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new PartsInstallationWorkViewController(service))
                .setViewResolvers(views).build();
    }

    private ProcessWorkItem item(long id, ProcessWorkItemKey key, int order, boolean completed) {
        var item = new ProcessWorkItem(); item.setId(id); item.setProcessWork(work);
        item.setItemKey(key); item.setItemOrder(order);
        if (completed) {
            item.setStatus(ProcessWorkItemStatus.COMPLETED);
            item.setCompletedAt(LocalDateTime.of(2026, 9, 17, 11, 0));
        }
        return item;
    }

    private String html() throws Exception {
        return mvc.perform(get("/processes/9/work")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test void rendersPersistedItemsSnapshotProgressAndExistingEndRoute() throws Exception {
        when(items.findByProcessWorkIdOrderByItemOrderAsc(10L)).thenReturn(List.of(
                item(21, ProcessWorkItemKey.SPRING_HANGER_INSTALL, 1, true),
                item(22, ProcessWorkItemKey.BRIDGE_SIX_POINT_INSTALL, 2, false),
                item(23, ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, 3, false),
                item(24, ProcessWorkItemKey.TUNER_INSTALL, 4, false),
                item(25, ProcessWorkItemKey.STRING_INSTALL, 5, false)));
        String html = html();
        assertThat(html).contains("Snapshot Tuner", "Snapshot Strings", "全5ポジションで音出し確認",
                "6点支持", "圧入ブッシュ式", "6連", "舟形ジャックプレート", "1Vol. 2Tone",
                ">不要<", ">必要<", ">SSS<", ">09-42<", ">-<", "完了: ",
                "id=\"completed-count\">1", "id=\"total-count\">5",
                "action=\"/processes/end\"", "name=\"historyId\" value=\"9\"");
        assertThat(html.indexOf("data-item-id=\"21\"")).isLessThan(html.indexOf("data-item-id=\"22\""));
        assertThat(html.indexOf(">Bridge<")).isLessThan(html.indexOf(">Electronics<"));
        assertThat(html.indexOf(">Electronics<")).isLessThan(html.indexOf(">Tuner<"));
        assertThat(html.indexOf(">Tuner<")).isLessThan(html.indexOf(">String<"));
        assertThat(html).doesNotContain("保存</button>", "nullポジション");
        verify(items).findByProcessWorkIdOrderByItemOrderAsc(10L);
        verify(works, never()).save(any());
    }

    @Test void endedHistoryIsReadOnlyAndHasNoEndForm() throws Exception {
        history.setEndTime(LocalDateTime.now());
        when(items.findByProcessWorkIdOrderByItemOrderAsc(10L))
                .thenReturn(List.of(item(21, ProcessWorkItemKey.STRING_INSTALL, 1, true)));
        assertThat(html()).contains("読み取り専用", "disabled=\"disabled\"")
                .doesNotContain("action=\"/processes/end\"");
    }

    @Test void zeroItemsAreExplicitlyInconsistent() throws Exception {
        when(items.findByProcessWorkIdOrderByItemOrderAsc(10L)).thenReturn(List.of());
        assertThat(service.get(9L).inconsistent()).isTrue();
        assertThat(html()).contains("不整合データ", "工程終了可能な状態ではありません");
    }

    @Test void noWorkIsRejectedWithoutGeneration() {
        when(works.findByProcessHistoryId(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(9L));
        verifyNoInteractions(items);
        verify(works, never()).save(any());
    }

    @Test void nullAndBlankSnapshotValuesRenderAsDashes() throws Exception {
        work.setRequiresStudHoleExpansion(null); work.setTunerBushRequired(null);
        work.setBridgeType(null); work.setTunerModel(" ");
        when(items.findByProcessWorkIdOrderByItemOrderAsc(10L)).thenReturn(List.of());
        var snapshot = service.get(9L).snapshot();
        assertThat(snapshot.requiresStudHoleExpansion()).isEqualTo("-");
        assertThat(snapshot.tunerBushRequired()).isEqualTo("-");
        assertThat(snapshot.tunerModel()).isEqualTo("-");
        assertThat(html()).doesNotContain(">null<", ">true<", ">false<");
    }
}
