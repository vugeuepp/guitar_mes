package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.process.ManufacturingProcess;
import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.process.work.*;
import com.example.guitarmes.product.parts.*;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.RequestOptions;

/** ブラウザ対象も同一e2e profile・DBで起動。既存アプリ/業務データには依存しない。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.jpa.show-sql=false", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("e2e")
class PartsInstallationWorkE2E extends PlaywrightTestBase {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Value("${local.server.port}") int port;
    private TransactionTemplate tx;
    private Long processId, noWorkProcessId, guitarId, historyId, noWorkHistoryId, workId;
    private final List<Long> itemIds = new ArrayList<>();
    private String base;

    @Override protected Path getEvidenceDirectory() { return evidenceDirectory("parts-installation-work"); }

    @BeforeEach void fixture() {
        tx = new TransactionTemplate(manager);
        base = "http://localhost:" + port;
        tx.executeWithoutResult(s -> {
            assertEquals("guitar_mes_e2e", em.createNativeQuery("select current_database()", String.class).getSingleResult());
            String prefix = "PW" + UUID.randomUUID().toString().substring(0, 8);
            var process = new ManufacturingProcess("GUITAR", prefix, 99999);
            em.persist(process); em.flush(); processId = process.getId();
            var noWorkProcess = new ManufacturingProcess("GUITAR", prefix + "-NO-WORK", 100000);
            em.persist(noWorkProcess); em.flush(); noWorkProcessId = noWorkProcess.getId();
            var guitar = new Guitar(prefix, prefix);
            em.persist(guitar); em.flush(); guitarId = guitar.getId();
            var history = new ProcessHistory(guitarId, processId, prefix, LocalDateTime.now());
            em.persist(history); em.flush(); historyId = history.getId();
            var noWorkHistory = new ProcessHistory(guitarId, noWorkProcessId, prefix + "-NO-WORK", LocalDateTime.now());
            em.persist(noWorkHistory); em.flush(); noWorkHistoryId = noWorkHistory.getId();
            var work = new ProcessWork(); work.setProcessHistory(history);
            work.setBridgeType(BridgeType.SIX_POINT); work.setRequiresStudHoleExpansion(false);
            work.setTunerModel("Snapshot Tuner"); work.setTunerMountingType(TunerMountingType.PRESS_BUSHING);
            work.setTunerBushRequired(true); work.setTunerLayout(TunerLayout.SIX_IN_LINE);
            work.setPickupLayout("SSS"); work.setSelectorPositions(5); work.setControlLayout("1Vol. 2Tone");
            work.setJackMountingType(JackMountingType.BOAT_PLATE);
            work.setStringModel("Snapshot Strings"); work.setStringGauge("09-42");
            em.persist(work); em.flush(); workId = work.getId();
            int order = 1;
            for (var key : List.of(ProcessWorkItemKey.BRIDGE_SIX_POINT_INSTALL,
                    ProcessWorkItemKey.ELECTRONICS_SOUND_CHECK, ProcessWorkItemKey.STRING_INSTALL)) {
                var item = new ProcessWorkItem(); item.setProcessWork(work);
                item.setItemKey(key); item.setItemOrder(order++);
                em.persist(item); em.flush(); itemIds.add(item.getId());
            }
        });
    }

    @AfterEach void cleanup() {
        if (tx == null) return;
        tx.executeWithoutResult(s -> {
            for (Long id : itemIds) delete("t_process_work_item", id);
            delete("t_process_work", workId);
            delete("t_process_history", historyId);
            delete("t_process_history", noWorkHistoryId);
            delete("t_guitar", guitarId);
            delete("m_process", processId);
            delete("m_process", noWorkProcessId);
        });
    }

    private void delete(String table, Long id) {
        if (id != null) em.createNativeQuery("delete from " + table + " where id=:id")
                .setParameter("id", id).executeUpdate();
    }

    private String url() { return base + "/processes/" + historyId + "/work"; }

    @Test void immediateSaveReloadUncheckAndFailureRecovery() {
        page.navigate(url());
        assertThat(page.locator(".work-checkbox")).hasCount(3);
        assertThat(page.getByText("全5ポジションで音出し確認")).isVisible();
        assertThat(page.getByText("Snapshot Tuner")).isVisible();
        assertThat(page.locator("#completed-count")).hasText("0");
        var first = page.locator("[data-item-id='" + itemIds.get(0) + "']");
        first.locator("input").check();
        assertThat(page.locator("#completed-count")).hasText("1");
        assertThat(first.locator(".completed-at")).containsText("完了:");
        assertNotNull(tx.execute(s -> em.find(ProcessWorkItem.class, itemIds.get(0)).getCompletedAt()));
        page.reload();
        assertThat(first.locator("input")).isChecked();
        first.locator("input").uncheck();
        assertThat(page.locator("#completed-count")).hasText("0");
        assertThat(first.locator(".completed-at")).isHidden();

        String endpoint = "**/api/process-work-items/" + itemIds.get(0) + "/complete";
        page.route(endpoint, route -> {
            assertThat(page.locator(".work-checkbox:disabled")).hasCount(3);
            assertThat(page.locator("button[type='submit']")).isDisabled();
            route.fulfill(new Route.FulfillOptions().setStatus(409).setContentType("application/json")
                    .setBody("{\"message\":\"保存拒否テスト\"}"));
        });
        first.locator("input").check();
        assertThat(page.locator("#work-error")).hasText("保存拒否テスト");
        assertThat(first.locator("input")).not().isChecked();
        assertThat(first.locator("input")).isEnabled();
        assertThat(page.locator("#completed-count")).hasText("0");
        page.unroute(endpoint);
        first.locator("input").check();
        assertThat(page.locator("#completed-count")).hasText("1");

        String workLink = "/processes/" + historyId + "/work";
        page.navigate(base + "/guitars/" + guitarId + "/view");
        assertThat(page.locator("a[href='" + workLink + "']")).hasCount(1);
        assertThat(page.locator("a[href='/processes/" + noWorkHistoryId + "/work']")).hasCount(0);
        page.locator("a[href='" + workLink + "']").click();
        assertEquals(base + workLink, page.url());
        page.getByText("ギター詳細へ戻る").click();
        assertEquals(base + "/guitars/" + guitarId + "/view", page.url());
    }

    @Test void endLinksReadOnlyApiRejectionAndInconsistentWork() {
        page.navigate(base + "/processes/end/view?guitarId=" + guitarId);
        assertThat(page.locator("a[href='/processes/" + historyId + "/work']")).isVisible();
        page.navigate(base + "/processes/end/view");
        assertThat(page.locator("a[href='/processes/" + historyId + "/work']")).isVisible();
        page.navigate(url());
        for (Long id : itemIds) {
            var box = page.locator("[data-item-id='" + id + "'] input");
            box.check();
            assertThat(box).isEnabled();
        }
        assertThat(page.locator("#completed-count")).hasText("3");
        // 実際の既存終了routeへ送信し、History終了を確認する。
        var response = page.request().post(base + "/processes/end",
                RequestOptions.create().setForm(com.microsoft.playwright.options.FormData.create()
                        .set("historyId", historyId.toString())).setMaxRedirects(0));
        assertEquals(302, response.status());
        assertNotNull(tx.execute(s -> em.find(ProcessHistory.class, historyId).getEndTime()));
        page.reload();
        assertThat(page.locator(".work-checkbox:disabled")).hasCount(3);
        assertThat(page.locator("form[action='/processes/end']")).hasCount(0);
        assertEquals(409, page.request().delete(base + "/api/process-work-items/" + itemIds.get(0) + "/complete").status());
        assertEquals(409, page.request().put(base + "/api/process-work-items/" + itemIds.get(0) + "/complete").status());

        String workLink = "/processes/" + historyId + "/work";
        page.navigate(base + "/guitars/" + guitarId + "/history");
        assertThat(page.locator("a[href='" + workLink + "']")).hasCount(1);
        assertThat(page.locator("a[href='/processes/" + noWorkHistoryId + "/work']")).hasCount(0);
        page.locator("a[href='" + workLink + "']").click();
        assertEquals(base + workLink, page.url());
        assertThat(page.locator(".work-checkbox:disabled")).hasCount(3);
        page.getByText("ギター詳細へ戻る").click();
        assertEquals(base + "/guitars/" + guitarId + "/view", page.url());

        tx.executeWithoutResult(s -> {
            for (Long id : itemIds) delete("t_process_work_item", id);
            em.find(ProcessHistory.class, historyId).setEndTime(null);
        });
        page.navigate(url());
        page.reload();
        assertThat(page.getByText("作業項目が存在しない不整合データです。工程終了可能な状態ではありません。")).isVisible();
        assertThat(page.locator(".work-checkbox")).hasCount(0);
    }
}
