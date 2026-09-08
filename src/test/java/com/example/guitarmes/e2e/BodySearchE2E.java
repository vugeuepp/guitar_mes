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

class BodySearchE2E extends PlaywrightTestBase {
    private static final String E2E_DB_URL = System.getProperty(
            "e2e.db.url", "jdbc:postgresql://localhost:5432/guitar_mes_e2e");
    private static final String E2E_DB_USER = System.getProperty(
            "e2e.db.user", "naokiyamada");
    private static final String E2E_DB_PASSWORD = System.getProperty(
            "e2e.db.password", "");
    private final List<Long> bodyIds = new ArrayList<>();
    private Long bodyMasterId;
    private String firstSerial;
    private String secondSerial;
    private String otherSerial;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("body-search");
    }

    @Test
    @DisplayName("ボディ一覧を複数条件で検索して条件をクリアできる")
    void searchBodies() throws Exception {
        try {
            prepareTestData();
            verifySerialSearch();
            verifyIndividualFilters();
            verifyCombinedSearch();
            verifyNoResultsAndClear();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        findReferences();
        String suffix = String.valueOf(System.currentTimeMillis());
        firstSerial = "E2EBODY-SEARCH-A-" + suffix;
        secondSerial = "E2EBODY-SEARCH-B-" + suffix;
        otherSerial = "E2EBODY-SEARCH-X-" + suffix;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                bodyIds.add(insertBody(connection, firstSerial,
                        "E2E Search Body", "塗装後検品", "WAITING_INSPECTION"));
                bodyIds.add(insertBody(connection, secondSerial,
                        "E2E Search Body", "塗装後検品", "WAITING_INSPECTION"));
                bodyIds.add(insertBody(connection, otherSerial,
                        "E2E Other Body", "パーツ付け", "WORKING"));
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void findReferences() throws Exception {
        String sql = "SELECT id, body_master_id FROM m_product "
                + "WHERE body_master_id IS NOT NULL ORDER BY id LIMIT 1";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next(), "E2Eで使用できるProductが必要です。");
            bodyMasterId = resultSet.getLong("body_master_id");
        }
    }

    private Long insertBody(Connection connection, String serialNo,
            String modelName, String currentProcess, String status) throws Exception {
        String sql = """
                INSERT INTO t_body (
                    serial_no, model_name, current_process, status,
                    color, body_master_id
                ) VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, modelName);
            statement.setString(3, currentProcess);
            statement.setString(4, status);
            statement.setString(5, "E2E Color");
            statement.setLong(6, bodyMasterId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getLong("id");
            }
        }
    }

    private void verifySerialSearch() {
        page.navigate(BASE_URL + "/bodies/view");
        page.waitForLoadState();
        assertThat(page.locator(".guitar-search-panel")).isVisible();
        page.locator("#serial").fill(firstSerial);
        search();
        assertThat(page.locator(".body-management-table tbody tr")).hasCount(1);
        assertThat(bodyRow(firstSerial)).isVisible();
        assertThat(page.locator(".guitar-search-result")).containsText("検索結果");
        assertThat(page.locator(".guitar-search-result")).containsText("1件");
    }

    private void verifyIndividualFilters() {
        page.locator("#serial").fill("");
        page.locator("#modelName").fill("Search Body");
        search();
        assertThat(bodyRow(firstSerial)).isVisible();
        assertThat(page.locator(".body-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(otherSerial))).hasCount(0);
        page.locator("#modelName").fill("");
        page.locator("#currentProcess").selectOption("パーツ付け");
        search();
        assertThat(bodyRow(otherSerial)).isVisible();
        assertThat(page.locator(".body-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(firstSerial))).hasCount(0);
        page.locator("#currentProcess").selectOption("");
        page.locator("#status").selectOption("WORKING");
        search();
        assertThat(bodyRow(otherSerial)).isVisible();
        assertThat(page.locator(".body-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(secondSerial))).hasCount(0);
    }

    private void verifyCombinedSearch() {
        page.locator("#serial").fill("");
        page.locator("#modelName").fill("Search Body");
        page.locator("#currentProcess").selectOption("塗装後検品");
        page.locator("#status").selectOption("WAITING_INSPECTION");
        search();
        assertThat(bodyRow(firstSerial)).isVisible();
        assertThat(bodyRow(secondSerial)).isVisible();
        assertEquals(0, page.locator(".body-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(otherSerial)).count());
        assertThat(page.locator("#modelName")).hasValue("Search Body");
        assertThat(page.locator("#currentProcess")).hasValue("塗装後検品");
        assertThat(page.locator("#status")).hasValue("WAITING_INSPECTION");
        assertThat(bodyRow(firstSerial).locator(".row-checkbox")).isDisabled();
        page.locator("#processId").selectOption(
                new com.microsoft.playwright.options.SelectOption().setLabel("塗装後検品"));
        assertThat(bodyRow(firstSerial).locator(".row-checkbox")).isEnabled();
        bodyRow(firstSerial).locator(".row-checkbox").check();
        assertThat(page.locator("#selected-count")).hasText("1");
        page.locator("#processId").selectOption("");
        assertThat(bodyRow(firstSerial).locator(".row-checkbox")).not().isChecked();
        assertThat(page.locator("#selected-count")).hasText("0");
        captureScreenshot("01-body-search-filter.png");
    }

    private void verifyNoResultsAndClear() {
        page.locator("#serial").fill("NO-SUCH-" + firstSerial);
        page.locator("#modelName").fill("");
        page.locator("#currentProcess").selectOption("");
        page.locator("#status").selectOption("");
        search();
        assertThat(page.locator(".empty-state"))
                .containsText("条件に一致するボディはありません。");
        assertThat(page.locator("#bulk-start-form")).hasCount(0);
        page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName("検索条件をクリア").setExact(true))
                .click();
        page.waitForLoadState();
        assertThat(page.locator("#serial")).hasValue("");
        assertThat(page.locator("#modelName")).hasValue("");
        assertThat(page.locator("#currentProcess")).hasValue("");
        assertThat(page.locator("#status")).hasValue("");
        assertThat(bodyRow(firstSerial)).isVisible();
        assertThat(bodyRow(otherSerial)).isVisible();
    }

    private void search() {
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
        page.waitForLoadState();
    }

    private Locator bodyRow(String serialNo) {
        Locator row = page.locator(".body-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(serialNo));
        assertEquals(1, row.count(), serialNo + "の行が一意に見つかりません。");
        return row;
    }

    private void cleanupSafely() throws Exception {
        try (Connection connection = openConnection()) {
            for (Long id : bodyIds) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM t_body WHERE id = ?")) {
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
