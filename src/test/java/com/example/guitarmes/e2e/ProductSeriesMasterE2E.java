package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class ProductSeriesMasterE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");

    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");

    private static final String SERIES_CODE = "E2E-SERIES";
    private static final String INITIAL_NAME = "E2E Test Series";
    private static final String UPDATED_NAME = "E2E Test Series Updated";

    private Long createdSeriesId;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("product-series-master");
    }

    @Test
    @DisplayName("製品シリーズを登録・編集・無効化・有効化できる")
    void manageProductSeriesMaster() throws Exception {
        deleteStaleTestData();

        try {
            openList();
            openCreateForm();
            createSeries();
            verifyCreated();
            openEditForm();
            updateSeriesName();
            verifyEdited();
            disableSeries();
            verifyDisabled();
            enableSeries();
            verifyEnabled();
        } finally {
            deleteCreatedSeriesSafely();
        }
    }

    private void openList() {
        page.navigate(BASE_URL + "/product-series/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("製品シリーズマスタ一覧"));
        captureScreenshot("01-list.png");
    }

    private void openCreateForm() {
        page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("製品シリーズ登録"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("製品シリーズマスタ登録"));
        captureScreenshot("02-create-form.png");
    }

    private void createSeries() {
        page.locator("#seriesCode").fill("e2e series");
        page.locator("#seriesName").fill(INITIAL_NAME);

        assertThat(page.locator("#seriesCodePreview"))
                .hasText(SERIES_CODE);
        assertThat(page.locator("#internalModelCodePreview"))
                .hasText(SERIES_CODE + "-ST");
        captureScreenshot("03-create-input.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("製品シリーズを登録"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/product-series/view"));
    }

    private void verifyCreated() throws Exception {
        Locator row = findTargetRow();
        assertThat(row).containsText(INITIAL_NAME);
        assertThat(row.locator(".status-badge")).hasText("有効");

        createdSeriesId = findSeriesId();
        assertTrue(createdSeriesId > 0L);
        captureScreenshot("04-created.png");
    }

    private void openEditForm() {
        findTargetRow().getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions()
                        .setName("編集"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("製品シリーズマスタ編集"));
        assertThat(page.locator("#seriesCode"))
                .hasValue(SERIES_CODE);
        assertTrue(page.locator("#seriesCode").isEditable() == false);
        assertThat(page.locator("#seriesName"))
                .hasValue(INITIAL_NAME);
        captureScreenshot("05-edit-form.png");
    }

    private void updateSeriesName() {
        page.locator("#seriesName").fill(UPDATED_NAME);
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("変更内容を保存"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/product-series/view"));
    }

    private void verifyEdited() throws Exception {
        Locator row = findTargetRow();
        assertThat(row).containsText(UPDATED_NAME);
        assertEquals(UPDATED_NAME, findSeriesName());
        captureScreenshot("06-edited.png");
    }

    private void disableSeries() {
        findTargetRow().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("無効化"))
                .click();
        page.waitForLoadState();
    }

    private void verifyDisabled() throws Exception {
        Locator row = findTargetRow();
        assertThat(row.locator(".status-badge")).hasText("無効");
        assertThat(row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("有効化")))
                .isVisible();
        assertFalse(findActive());
        captureScreenshot("07-disabled.png");
    }

    private void enableSeries() {
        findTargetRow().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("有効化"))
                .click();
        page.waitForLoadState();
    }

    private void verifyEnabled() throws Exception {
        Locator row = findTargetRow();
        assertThat(row.locator(".status-badge")).hasText("有効");
        assertThat(row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("無効化")))
                .isVisible();
        assertTrue(findActive());
        captureScreenshot("08-enabled.png");
    }

    private Locator findTargetRow() {
        Locator row = page.locator("tbody tr")
                .filter(new Locator.FilterOptions()
                        .setHasText(SERIES_CODE));
        assertEquals(
                1,
                row.count(),
                SERIES_CODE + "の行が一意に見つかりません。");
        return row;
    }

    private Long findSeriesId() throws Exception {
        String sql = """
                SELECT id
                FROM m_product_series
                WHERE series_code = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SERIES_CODE);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "登録した製品シリーズがDBにありません。");
                }
                return resultSet.getLong("id");
            }
        }
    }

    private String findSeriesName() throws Exception {
        String sql = """
                SELECT series_name
                FROM m_product_series
                WHERE id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, createdSeriesId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "対象製品シリーズがDBにありません。");
                }
                return resultSet.getString("series_name");
            }
        }
    }

    private boolean findActive() throws Exception {
        String sql = """
                SELECT active
                FROM m_product_series
                WHERE id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, createdSeriesId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "対象製品シリーズがDBにありません。");
                }
                return resultSet.getBoolean("active");
            }
        }
    }

    private void deleteStaleTestData() throws Exception {
        deleteBySeriesCode(SERIES_CODE);
    }

    private void deleteCreatedSeriesSafely() throws Exception {
        deleteBySeriesCode(SERIES_CODE);
    }

    private void deleteBySeriesCode(String seriesCode) throws Exception {
        String sql = """
                DELETE FROM m_product_series series
                WHERE series.series_code = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM m_product product
                      WHERE product.internal_model_code = series.series_code
                         OR product.internal_model_code LIKE
                            series.series_code || '-%'
                  )
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, seriesCode);
            statement.executeUpdate();
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                E2E_DB_URL,
                E2E_DB_USER,
                E2E_DB_PASSWORD);
    }
}
