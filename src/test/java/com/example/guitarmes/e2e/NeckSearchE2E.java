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

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("neck-search");
    }

    @Test
    @DisplayName("ネック一覧を複数条件で検索して条件をクリアできる")
    void searchNecks() throws Exception {
        try {
            prepareTestData();
            verifySerialSearch();
            verifyCombinedSearch();
            verifyNoResultsAndClear();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        findReferences();
        String suffix = String.valueOf(System.currentTimeMillis());
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
        page.locator("#serial").fill("");
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
