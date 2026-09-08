package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Locator;

class ProductionOrderSearchE2E extends PlaywrightTestBase {
    private final List<Long> ids = new ArrayList<>();
    private final String prefix = "E2EOS-" + UUID.randomUUID().toString().substring(0, 8);
    private String modelNo;
    @Override protected Path getEvidenceDirectory() { return evidenceDirectory("production-order-search"); }
    private Connection connection() throws SQLException {
        return DriverManager.getConnection(System.getProperty("e2e.db.url", "jdbc:postgresql://localhost:5432/guitar_mes_e2e"),
                System.getProperty("e2e.db.user", "naokiyamada"), System.getProperty("e2e.db.password", ""));
    }
    @Test
    void searchAndClearWithDateValidation() throws Exception {
        try {
            prepare();
            verifyCategories();
            page.navigate(BASE_URL + "/production-orders/view");
            page.locator("#orderNo").fill(prefix);
            search();
            assertThat(row("A")).isVisible(); assertThat(row("B")).isVisible();
            page.locator("#product").fill(modelNo);
            search(); assertThat(row("A")).isVisible();
            page.locator("#status").selectOption("PLANNED");
            search(); assertThat(row("A")).isVisible(); assertThat(row("B")).hasCount(0);
            page.locator("#status").selectOption("");
            page.locator("#planMonth").fill("2026-09");
            search(); assertThat(row("A")).isVisible(); assertThat(row("B")).hasCount(0);
            page.locator("#planMonth").fill("");
            page.locator("#dueFrom").fill("2026-09-10");
            page.locator("#dueTo").fill("2026-09-10");
            search(); assertThat(row("A")).isVisible(); assertThat(row("B")).hasCount(0);
            page.locator("#status").selectOption("PLANNED");
            page.locator("#planMonth").fill("2026-09");
            search(); page.reload();
            assertThat(row("A")).isVisible();
            assertThat(page.locator("#orderNo")).hasValue(prefix);
            assertThat(page.locator("#product")).hasValue(modelNo);
            assertThat(page.locator("#status")).hasValue("PLANNED");
            assertThat(page.locator("#planMonth")).hasValue("2026-09");
            assertThat(page.locator("#dueFrom")).hasValue("2026-09-10");
            assertThat(page.locator("#dueTo")).hasValue("2026-09-10");
            assertThat(page.locator(".guitar-search-result strong")).hasText("1");
            captureScreenshot("01-order-search.png");
            page.locator("#dueFrom").fill("2026-10-01"); search();
            assertThat(page.locator("[role=alert]")).containsText("開始日は終了日以前");
            assertThat(page.locator(".empty-state")).hasCount(0);
            page.locator("#dueFrom").fill("2026-02-30"); search();
            assertThat(page.locator("[role=alert]")).containsText("正しく入力");
            assertThat(page.locator("#dueFrom")).hasValue("2026-02-30");
            page.locator(".guitar-search-actions a").click();
            page.locator("#orderNo").fill(prefix + "-missing"); search();
            assertThat(page.locator(".empty-state")).containsText("条件に一致する生産計画はありません。");
            page.locator(".empty-state a").click();
            for (String field : List.of("orderNo", "product", "status", "planMonth", "dueFrom", "dueTo"))
                assertThat(page.locator("#" + field)).hasValue("");
            assertThat(row("A")).isVisible(); assertThat(row("B")).isVisible();
        } finally {
            try (Connection c = connection()) {
                for (Long id : ids) try (PreparedStatement s = c.prepareStatement("DELETE FROM t_production_order WHERE id = ?")) {
                    s.setLong(1, id); s.executeUpdate();
                }
            }
        }
    }
    private void verifyCategories() throws Exception {
        page.navigate(BASE_URL + "/production-orders/view");
        assertThat(page.locator("input[name=category]")).hasValue("active");
        assertThat(row("A")).isVisible();
        assertThat(row("B")).isVisible();
        assertThat(row("C")).hasCount(0);
        assertThat(row("D")).hasCount(0);
        for (String invalid : List.of("", "invalid", "undefined")) {
            page.navigate(BASE_URL + "/production-orders/view?category=" + invalid);
            assertThat(page.locator("input[name=category]")).hasValue("active");
        }
        for (String category : List.of("active", "completed", "cancelled")) {
            page.locator("#category-" + category).click();
            for (String field : List.of("orderNo", "product", "status", "planMonth", "dueFrom", "dueTo"))
                assertThat(page.locator("#" + field)).hasValue("");
            assertThat(page.locator("#category-" + category)).hasAttribute("aria-current", "page");
            verifyCategoryCounts();
            var expected = "active".equals(category) ? List.of("A", "B")
                    : "completed".equals(category) ? List.of("C") : List.of("D");
            for (String suffix : List.of("A", "B", "C", "D"))
                assertThat(row(suffix)).hasCount(expected.contains(suffix) ? 1 : 0);
            var labels = "active".equals(category) ? List.of("計画中", "製造中")
                    : "completed".equals(category) ? List.of("完了") : List.of("中止");
            for (String label : page.locator(".production-order-table .status-badge").allTextContents())
                assertTrue(labels.contains(label.trim()), "別カテゴリの状態が表示されています: " + label);
            assertThat(page.locator("#status option")).hasCount("active".equals(category) ? 3 : 2);
            page.locator("#orderNo").fill(prefix);
            search();
            assertThat(page.locator(".production-order-table tbody tr")).hasCount(expected.size());
            verifyCategoryCounts();
            String suffix = expected.get(0);
            String status = "active".equals(category) ? "PLANNED"
                    : "completed".equals(category) ? "COMPLETED" : "CANCELLED";
            String month = "active".equals(category) ? "2026-09" : "2026-10";
            String due = month + "-10";
            page.locator("#orderNo").fill(prefix + "-" + suffix);
            page.locator("#product").fill(modelNo);
            page.locator("#status").selectOption(status);
            page.locator("#planMonth").fill(month);
            page.locator("#dueFrom").fill(due);
            page.locator("#dueTo").fill(due);
            search(); page.reload();
            assertThat(page.locator("input[name=category]")).hasValue(category);
            assertThat(page.locator("#category-" + category)).hasAttribute("aria-current", "page");
            assertThat(page.locator("#orderNo")).hasValue(prefix + "-" + suffix);
            assertThat(page.locator("#product")).hasValue(modelNo);
            assertThat(page.locator("#status")).hasValue(status);
            assertThat(page.locator("#planMonth")).hasValue(month);
            assertThat(page.locator("#dueFrom")).hasValue(due);
            assertThat(page.locator("#dueTo")).hasValue(due);
            assertThat(row(suffix)).isVisible();
            assertThat(page.locator(".guitar-search-result strong")).hasText("1");
            assertThat(row(suffix).locator("a.btn-detail"))
                    .hasAttribute("href", "/production-orders/" + ids.get(List.of("A", "B", "C", "D").indexOf(suffix)) + "/view");
            verifyCategoryCounts();
            page.locator("#dueFrom").fill("bad"); search();
            assertThat(page.locator("[role=alert]")).containsText("正しく入力");
            assertThat(page.locator("input[name=category]")).hasValue(category);
            verifyCategoryCounts();
            page.locator(".guitar-search-actions a").click();
            assertThat(page).hasURL(BASE_URL + "/production-orders/view?category=" + category);
            for (String field : List.of("orderNo", "product", "status", "planMonth", "dueFrom", "dueTo"))
                assertThat(page.locator("#" + field)).hasValue("");
            page.locator("#orderNo").fill(prefix + "-missing"); search();
            assertThat(page.locator(".empty-state")).containsText("条件に一致する生産計画はありません。");
            page.locator(".empty-state a").click();
            assertThat(page.locator("input[name=category]")).hasValue(category);
            captureScreenshot("category-" + category + ".png");
            // Populate all conditions before the next tab, to verify its reset behavior.
            page.locator("#orderNo").fill(prefix);
            page.locator("#product").fill(modelNo);
            page.locator("#status").selectOption(status);
            page.locator("#planMonth").fill(month);
            page.locator("#dueFrom").fill(due);
            page.locator("#dueTo").fill(due);
            search();
        }
        page.navigate(BASE_URL + "/production-orders/view?category=completed&status=PLANNED");
        assertThat(page.locator(".empty-state")).containsText("条件に一致する生産計画はありません。");
        assertThat(page.locator("#status")).hasValue("PLANNED");
        search();
        assertThat(page.locator("input[name=category]")).hasValue("completed");
        assertThat(page.locator(".production-order-table tbody tr")).hasCount(0);
    }

    private void verifyCategoryCounts() throws Exception {
        try (Connection c = connection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("""
                SELECT count(*) FILTER (WHERE lower(trim(status)) IN ('planned', 'in_progress')) AS active,
                count(*) FILTER (WHERE lower(trim(status)) = 'completed') AS completed,
                count(*) FILTER (WHERE lower(trim(status)) = 'cancelled') AS cancelled
                FROM t_production_order
                """)) {
            assertTrue(rs.next());
            for (String category : List.of("active", "completed", "cancelled"))
                assertThat(page.locator("#category-" + category + " .category-count"))
                        .hasText(String.valueOf(rs.getLong(category)));
        }
    }
    private void search() { page.locator(".guitar-search-form button[type=submit]").click(); page.waitForLoadState(); }
    private Locator row(String suffix) {
        return page.locator(".production-order-table tbody tr").filter(new Locator.FilterOptions().setHasText(prefix + "-" + suffix));
    }
    private void prepare() throws Exception {
        try (Connection c = connection(); Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT id, model_no FROM m_product WHERE model_no IS NOT NULL AND model_no <> '' ORDER BY id LIMIT 1")) {
            assertTrue(rs.next(), "E2E用製品が必要です");
            long productId = rs.getLong(1); modelNo = rs.getString(2);
            for (String suffix : List.of("A", "B", "C", "D")) {
                boolean first = suffix.equals("A");
                try (PreparedStatement insert = c.prepareStatement("""
                        INSERT INTO t_production_order (order_no, product_id, planned_quantity,
                        started_quantity, completed_quantity, plan_month, planned_start_date, due_date, status)
                        VALUES (?, ?, 1, 0, 0, ?, ?, ?, ?) RETURNING id
                        """)) {
                    insert.setString(1, prefix + "-" + suffix); insert.setLong(2, productId);
                    insert.setDate(3, Date.valueOf(first ? "2026-09-01" : "2026-10-01"));
                    insert.setDate(4, Date.valueOf(first ? "2026-09-01" : "2026-10-01"));
                    insert.setDate(5, Date.valueOf(first ? "2026-09-10" : "2026-10-10"));
                    insert.setString(6, switch (suffix) { case "A" -> "PLANNED"; case "B" -> "IN_PROGRESS"; case "C" -> "COMPLETED"; default -> "CANCELLED"; });
                    try (ResultSet result = insert.executeQuery()) { assertTrue(result.next()); ids.add(result.getLong(1)); }
                }
            }
        }
    }
}
