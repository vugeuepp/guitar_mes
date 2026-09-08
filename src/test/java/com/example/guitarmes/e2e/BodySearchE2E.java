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
    private String activeSerial;
    private String activeOtherSerial;
    private String reworkSerial;
    private String returnedSerial;
    private String availableSerial;
    private String assembledSerial;
    private String rejectedSerial;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("body-search");
    }

    @Test
    @DisplayName("ボディを3カテゴリに分類しカテゴリ内検索できる")
    void categoriesSearchAndClear() throws Exception {
        try {
            prepareTestData();
            verifyDefaultActiveAndCounts();
            verifyCategorySwitchResetsSearch();
            verifyAttentionSearchAndClearKeepsCategory();
            verifyPassedHasNoBulkStart();
            verifyCategoryAndStatusConflictIsEmpty();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        findReferences();
        String suffix = String.valueOf(System.currentTimeMillis());
        activeSerial = "E2EBODY-ACTIVE-A-" + suffix;
        activeOtherSerial = "E2EBODY-ACTIVE-B-" + suffix;
        reworkSerial = "E2EBODY-REWORK-" + suffix;
        returnedSerial = "E2EBODY-RETURNED-" + suffix;
        availableSerial = "E2EBODY-AVAILABLE-" + suffix;
        assembledSerial = "E2EBODY-ASSEMBLED-" + suffix;
        rejectedSerial = "E2EBODY-REJECTED-" + suffix;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                bodyIds.add(insertBody(connection, activeSerial,
                        "E2E Category Target", "塗装後検品", "WAITING_INSPECTION"));
                bodyIds.add(insertBody(connection, activeOtherSerial,
                        "E2E Category Other", "パーツ付け", "WORKING"));
                bodyIds.add(insertBody(connection, reworkSerial,
                        "E2E Category Target", "バフがけ", "REWORK"));
                bodyIds.add(insertBody(connection, returnedSerial,
                        "E2E Returned Body", "塗装前工程へ差し戻し", "RETURNED"));
                bodyIds.add(insertBody(connection, availableSerial,
                        "E2E Available Body", "組立待ち", "AVAILABLE"));
                bodyIds.add(insertBody(connection, assembledSerial,
                        "E2E Assembled Body", "組立済み", "ASSEMBLED"));
                bodyIds.add(insertBody(connection, rejectedSerial,
                        "E2E Rejected Body", "製造終了", "REJECTED"));
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void verifyDefaultActiveAndCounts() throws Exception {
        page.navigate(BASE_URL + "/bodies/view");
        page.waitForLoadState();
        assertThat(page.locator("#category-active"))
                .hasAttribute("aria-current", "page");
        assertThat(page.locator("#category-attention")).isVisible();
        assertThat(page.locator("#category-passed")).isVisible();
        assertThat(bodyRow(activeSerial)).isVisible();
        assertThat(bodyRows(reworkSerial)).hasCount(0);
        assertThat(bodyRows(availableSerial)).hasCount(0);
        assertThat(page.locator("#bulk-start-form")).isVisible();
        assertCategoryCountsMatchDatabase();
        captureScreenshot("01-body-active-category.png");
    }

    private void verifyCategorySwitchResetsSearch() {
        page.locator("#serial").fill(activeSerial);
        search();
        assertThat(page.locator("#serial")).hasValue(activeSerial);
        page.locator("#category-attention").click();
        page.waitForLoadState();
        assertThat(page.locator("#category-attention"))
                .hasAttribute("aria-current", "page");
        assertThat(page.locator("#serial")).hasValue("");
        assertThat(page.locator("#modelName")).hasValue("");
        assertThat(bodyRow(reworkSerial)).isVisible();
        assertThat(bodyRow(returnedSerial)).isVisible();
        assertThat(bodyRows(activeSerial)).hasCount(0);
        assertThat(page.locator("#bulk-start-form")).hasCount(0);
        assertThat(bodyRow(reworkSerial).getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName("工程開始"))).isVisible();
        assertThat(bodyRow(returnedSerial)).containsText("操作不可");
    }

    private void verifyAttentionSearchAndClearKeepsCategory() {
        page.locator("#serial").fill(reworkSerial);
        page.locator("#modelName").fill("Category Target");
        page.locator("#currentProcess").selectOption("バフがけ");
        page.locator("#status").selectOption("REWORK");
        search();
        assertThat(bodyRow(reworkSerial)).isVisible();
        assertThat(bodyRows(returnedSerial)).hasCount(0);
        assertThat(page.locator("input[name=category]")).hasValue("attention");
        assertThat(page.locator(".guitar-search-result strong")).hasText("1");
        page.locator(".guitar-search-actions a").click();
        page.waitForLoadState();
        assertThat(page.locator("input[name=category]")).hasValue("attention");
        assertThat(page.locator("#serial")).hasValue("");
        assertThat(bodyRow(reworkSerial)).isVisible();
        assertThat(bodyRow(returnedSerial)).isVisible();
        captureScreenshot("02-body-attention-category.png");
    }

    private void verifyPassedHasNoBulkStart() {
        page.locator("#category-passed").click();
        page.waitForLoadState();
        assertThat(page.locator("#category-passed"))
                .hasAttribute("aria-current", "page");
        assertThat(bodyRow(availableSerial)).isVisible();
        assertThat(bodyRow(assembledSerial)).isVisible();
        assertThat(bodyRow(rejectedSerial)).isVisible();
        assertThat(bodyRows(activeSerial)).hasCount(0);
        assertThat(page.locator("#bulk-start-form")).hasCount(0);
        assertThat(page.locator(".row-checkbox")).hasCount(0);
        captureScreenshot("03-body-passed-category.png");
    }

    private void verifyCategoryAndStatusConflictIsEmpty() {
        page.locator("#category-active").click();
        page.waitForLoadState();
        page.locator("#status").selectOption("REJECTED");
        search();
        assertThat(page.locator(".empty-state"))
                .containsText("条件に一致するボディはありません。");
        assertThat(page.locator("input[name=category]")).hasValue("active");
        page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("検索条件をクリア").setExact(true)).click();
        page.waitForLoadState();
        assertThat(page.locator("input[name=category]")).hasValue("active");
        assertThat(bodyRow(activeSerial)).isVisible();
    }

    private void findReferences() throws Exception {
        String sql = "SELECT body_master_id FROM m_product "
                + "WHERE body_master_id IS NOT NULL ORDER BY id LIMIT 1";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next(), "E2Eで使用できるBodyMasterが必要です。");
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

    private void assertCategoryCountsMatchDatabase() throws Exception {
        String sql = """
                SELECT
                    COUNT(*) FILTER (WHERE status IN ('WAITING_INSPECTION', 'WAITING', 'WORKING')),
                    COUNT(*) FILTER (WHERE status IN ('REWORK', 'RETURNED')),
                    COUNT(*) FILTER (WHERE status IN ('AVAILABLE', 'ASSEMBLED', 'REJECTED'))
                FROM t_body
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next());
            assertEquals(resultSet.getLong(1), categoryCount("active"));
            assertEquals(resultSet.getLong(2), categoryCount("attention"));
            assertEquals(resultSet.getLong(3), categoryCount("passed"));
        }
    }

    private long categoryCount(String category) {
        return Long.parseLong(page.locator(
                "#category-" + category + " .category-count").innerText());
    }

    private void search() {
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("検索").setExact(true)).click();
        page.waitForLoadState();
    }

    private Locator bodyRows(String serialNo) {
        return page.locator(".body-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(serialNo));
    }

    private Locator bodyRow(String serialNo) {
        Locator row = bodyRows(serialNo);
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
        return DriverManager.getConnection(
                E2E_DB_URL, E2E_DB_USER, E2E_DB_PASSWORD);
    }
}
