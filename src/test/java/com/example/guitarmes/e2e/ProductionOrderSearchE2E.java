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
    private void search() { page.locator(".guitar-search-form button[type=submit]").click(); page.waitForLoadState(); }
    private Locator row(String suffix) {
        return page.locator(".production-order-table tbody tr").filter(new Locator.FilterOptions().setHasText(prefix + "-" + suffix));
    }
    private void prepare() throws Exception {
        try (Connection c = connection(); Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT id, model_no FROM m_product WHERE model_no IS NOT NULL AND model_no <> '' ORDER BY id LIMIT 1")) {
            assertTrue(rs.next(), "E2E用製品が必要です");
            long productId = rs.getLong(1); modelNo = rs.getString(2);
            for (String suffix : List.of("A", "B")) {
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
                    insert.setString(6, first ? "PLANNED" : "CANCELLED");
                    try (ResultSet result = insert.executeQuery()) { assertTrue(result.next()); ids.add(result.getLong(1)); }
                }
            }
        }
    }
}
