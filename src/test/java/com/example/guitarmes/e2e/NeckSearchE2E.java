package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class NeckSearchE2E extends PlaywrightTestBase {
    private static final String E2E_DB_URL = System.getProperty(
            "e2e.db.url", "jdbc:postgresql://localhost:5432/guitar_mes_e2e");
    private static final String E2E_DB_USER = System.getProperty(
            "e2e.db.user", "naokiyamada");
    private static final String E2E_DB_PASSWORD = System.getProperty(
            "e2e.db.password", "");
    private final List<Long> neckIds = new ArrayList<>();
    private Long productId;
    private Long neckMasterId;
    private String firstSerial;
    private String secondSerial;
    private String otherSerial;
    private String suffix;
    private final java.util.Map<String, String> categorySerials = new java.util.LinkedHashMap<>();

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("neck-search");
    }

    @Test
    @DisplayName("ネック一覧を複数条件で検索して条件をクリアできる")
    void searchNecks() throws Exception {
        try {
            prepareTestData();
            verifyCategories();
            verifySerialSearch();
            verifyCombinedSearch();
            verifyNoResultsAndClear();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        findReferences();
        suffix = java.util.UUID.randomUUID().toString().substring(0, 18);
        firstSerial = "E2ENECK-SEARCH-A-" + suffix;
        secondSerial = "E2ENECK-SEARCH-B-" + suffix;
        otherSerial = "E2ENECK-SEARCH-X-" + suffix;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                neckIds.add(insertNeck(connection, firstSerial,
                        "E2E Search Neck", "PLEK", "WAITING"));
                neckIds.add(insertNeck(connection, secondSerial,
                        "E2E Search Neck", "PLEK", "WAITING"));
                neckIds.add(insertNeck(connection, otherSerial,
                        "E2E Other Neck", "ネックパーツ付け", "WORKING"));
                categorySerials.put("WAITING", firstSerial);
                categorySerials.put("WORKING", otherSerial);
                for (String state : List.of("RETURNED", "AVAILABLE", "ASSEMBLED", "REJECTED")) {
                    String serial = "E2ENECK-" + state + "-" + suffix;
                    categorySerials.put(state, serial);
                    neckIds.add(insertNeck(connection, serial, "E2E Category Neck",
                            "RETURNED".equals(state) ? "塗装前工程へ差し戻し" : "組立待ち", state));
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void findReferences() throws Exception {
        String sql = "SELECT id, neck_master_id FROM m_product "
                + "WHERE neck_master_id IS NOT NULL ORDER BY id LIMIT 1";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next(), "E2Eで使用できるProductが必要です。");
            productId = resultSet.getLong("id");
            neckMasterId = resultSet.getLong("neck_master_id");
        }
    }

    private Long insertNeck(Connection connection, String serialNo,
            String modelName, String currentProcess, String status) throws Exception {
        String sql = """
                INSERT INTO t_neck (
                    serial_no, model_name, current_process, status,
                    product_id, neck_master_id
                ) VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, modelName);
            statement.setString(3, currentProcess);
            statement.setString(4, status);
            statement.setLong(5, productId);
            statement.setLong(6, neckMasterId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getLong("id");
            }
        }
    }

    private void verifyCategories() throws Exception {
        page.navigate(BASE_URL + "/necks/view");
        assertThat(page.locator("#category-active")).hasAttribute("aria-current", "page");
        page.locator("#serial").fill(suffix); search();
        assertThat(neckRow(firstSerial)).isVisible();
        assertThat(neckRow(otherSerial)).isVisible();
        for (String invalid : List.of("", "invalid", "undefined")) {
            page.navigate(BASE_URL + "/necks/view?category=" + invalid);
            assertThat(page.locator("input[name=category]")).hasValue("active");
        }
        var states = java.util.Map.of(
                "active", List.of("WAITING", "WORKING"),
                "attention", List.of("RETURNED"),
                "passed", List.of("AVAILABLE", "ASSEMBLED", "REJECTED"));
        var labels = java.util.Map.of("WAITING", "工程待ち", "WORKING", "作業中",
                "RETURNED", "塗装前工程へ差し戻し", "AVAILABLE", "組立可能",
                "ASSEMBLED", "組立済み", "REJECTED", "不合格");
        for (String category : List.of("active", "attention", "passed")) {
            page.locator("#category-" + category).click();
            assertThat(page.locator("#serial")).hasValue("");
            assertThat(page.locator("#modelName")).hasValue("");
            assertThat(page.locator("#currentProcess")).hasValue("");
            assertThat(page.locator("#status")).hasValue("");
            assertThat(page.locator("input[name=category]")).hasValue(category);
            assertThat(page.locator("#category-" + category)).hasAttribute("aria-current", "page");
            verifyCategoryCounts();
            for (String text : page.locator(".neck-management-table .status-cell").allTextContents()) {
                assertTrue(states.get(category).stream().anyMatch(state -> labels.get(state).equals(text.trim())));
            }
            page.locator("#serial").fill(suffix); search();
            for (var entry : categorySerials.entrySet()) {
                Locator row = page.locator(".neck-management-table tbody tr")
                        .filter(new Locator.FilterOptions().setHasText(entry.getValue()));
                assertThat(row).hasCount(states.get(category).contains(entry.getKey()) ? 1 : 0);
            }
            assertThat(page.locator(".page-toolbar-actions a")).hasCount(1);
            Locator bulkEnd = page.locator("a[href='/neck-processes/bulk/end/view']");
            if ("active".equals(category)) {
                assertThat(page.locator("#bulk-start-form")).isVisible();
                assertThat(bulkEnd).isVisible();
                assertThat(page.locator("#select-all")).isVisible();
                assertThat(neckRow(firstSerial).locator("input.row-checkbox"))
                        .hasAttribute("form", "bulk-start-form");
                assertThat(neckRow(firstSerial).locator(".component-operation-cell")).containsText("工程開始");
                assertThat(neckRow(otherSerial).locator(".component-operation-cell")).containsText("工程終了");
            } else {
                assertThat(page.locator("#bulk-start-form")).hasCount(0);
                assertThat(bulkEnd).hasCount(0);
                assertThat(page.locator("#select-all, input.row-checkbox")).hasCount(0);
                for (String state : states.get(category)) {
                    Locator row = neckRow(categorySerials.get(state));
                    assertThat(row.locator(".component-operation-cell")).hasText("操作不可");
                    assertThat(row.locator(".component-operation-cell a")).hasCount(0);
                    assertThat(row.locator(".component-history-cell a")).hasText("工程履歴");
                }
            }
            // A unique suffix limits results to this test's own data, even with existing DB rows.
            page.locator("#serial").fill(suffix);
            search();
            assertThat(page.locator(".neck-management-table tbody tr"))
                    .hasCount("active".equals(category) ? 3 : states.get(category).size());
            verifyCategoryCounts();
            String state = states.get(category).get(0);
            String serial = categorySerials.get(state);
            page.locator("#serial").fill(serial);
            page.locator("#modelName").fill("active".equals(category) ? "Search Neck" : "Category Neck");
            page.locator("#currentProcess").selectOption("active".equals(category) ? "PLEK"
                    : "attention".equals(category) ? "塗装前工程へ差し戻し" : "組立待ち");
            page.locator("#status").selectOption(state);
            search();
            String searchUrl = page.url();
            assertTrue(searchUrl.contains("category=" + category));
            page.navigate(searchUrl);
            assertThat(neckRow(serial)).isVisible();
            assertThat(page.locator(".neck-management-table tbody tr")).hasCount(1);
            assertThat(page.locator("#status")).hasValue(state);
            assertThat(page.locator("#serial")).hasValue(serial);
            assertThat(page.locator("input[name=category]")).hasValue(category);
            assertThat(page.locator(".guitar-search-result strong")).hasText("1");
            verifyCategoryCounts();
            page.locator(".guitar-search-actions a").click();
            assertThat(page).hasURL(BASE_URL + "/necks/view?category=" + category);
            assertThat(page.locator("#serial")).hasValue("");
            assertThat(page.locator("#status")).hasValue("");
            page.locator("#serial").fill("NO-SUCH-" + suffix);
            search();
            assertThat(page.locator(".empty-state")).containsText("条件に一致するネックはありません。");
            page.locator(".empty-state a").click();
            assertThat(page.locator("input[name=category]")).hasValue(category);
            captureScreenshot("category-" + category + ".png");
            // Leave all fields filled to verify that the next tab resets them.
            page.locator("#serial").fill(serial);
            page.locator("#modelName").fill("Neck");
            page.locator("#currentProcess").selectOption("PLEK");
            page.locator("#status").selectOption(state);
            search();
        }
        page.locator("#category-active").click();
        assertThat(page.locator("#serial")).hasValue("");
    }

    private void verifyCategoryCounts() throws Exception {
        String sql = """
                SELECT
                  count(*) FILTER (WHERE lower(trim(status)) IN ('waiting', 'working')) AS active,
                  count(*) FILTER (WHERE lower(trim(status)) = 'returned') AS attention,
                  count(*) FILTER (WHERE lower(trim(status)) IN ('available', 'assembled', 'rejected')) AS passed
                FROM t_neck
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            for (String category : List.of("active", "attention", "passed")) {
                assertThat(page.locator("#category-" + category + " .category-count"))
                        .hasText(String.valueOf(result.getLong(category)));
            }
        }
    }

    private void verifySerialSearch() {
        page.navigate(BASE_URL + "/necks/view");
        page.waitForLoadState();
        assertThat(page.locator(".guitar-search-panel")).isVisible();
        page.locator("#serial").fill(firstSerial);
        search();
        assertThat(page.locator(".neck-management-table tbody tr")).hasCount(1);
        assertThat(neckRow(firstSerial)).isVisible();
        assertThat(page.locator(".guitar-search-result")).containsText("検索結果");
        assertThat(page.locator(".guitar-search-result")).containsText("1件");
    }

    private void verifyCombinedSearch() {
        page.locator("#serial").fill(suffix);
        page.locator("#modelName").fill("Search Neck");
        page.locator("#currentProcess").selectOption("PLEK");
        page.locator("#status").selectOption("WAITING");
        search();
        assertThat(neckRow(firstSerial)).isVisible();
        assertThat(neckRow(secondSerial)).isVisible();
        assertEquals(0, page.locator(".neck-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(otherSerial)).count());
        assertThat(page.locator("#modelName")).hasValue("Search Neck");
        assertThat(page.locator("#currentProcess")).hasValue("PLEK");
        assertThat(page.locator("#status")).hasValue("WAITING");
        captureScreenshot("01-neck-search-filter.png");
    }

    private void verifyNoResultsAndClear() {
        page.locator("#serial").fill("NO-SUCH-NECK");
        page.locator("#modelName").fill("");
        page.locator("#currentProcess").selectOption("");
        page.locator("#status").selectOption("");
        search();
        assertThat(page.locator(".empty-state"))
                .containsText("条件に一致するネックはありません。");
        page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName("検索条件をクリア").setExact(true))
                .click();
        page.waitForLoadState();
        assertThat(page.locator("#serial")).hasValue("");
        assertThat(page.locator("#modelName")).hasValue("");
    }

    private void search() {
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
        page.waitForLoadState();
    }

    private Locator neckRow(String serialNo) {
        Locator row = page.locator(".neck-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(serialNo));
        assertEquals(1, row.count(), serialNo + "の行が一意に見つかりません。");
        return row;
    }

    private void cleanupSafely() throws Exception {
        try (Connection connection = openConnection()) {
            for (Long id : neckIds) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM t_neck WHERE id = ?")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
            }
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(E2E_DB_URL, E2E_DB_USER, E2E_DB_PASSWORD);
    }
}
