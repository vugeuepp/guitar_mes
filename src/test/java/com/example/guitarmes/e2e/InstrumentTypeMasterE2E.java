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

class InstrumentTypeMasterE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");

    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");

    private static final String INSTRUMENT_CODE = "E2E-INST";
    private static final String INITIAL_NAME = "E2E Test Instrument";
    private static final String UPDATED_NAME =
            "E2E Test Instrument Updated";
    private static final String INITIAL_BODY_TYPE = "E2E Body";
    private static final String UPDATED_BODY_TYPE = "E2E Body Updated";
    private static final String INITIAL_NECK_TYPE = "E2E Neck";
    private static final String UPDATED_NECK_TYPE = "E2E Neck Updated";

    private Long createdInstrumentTypeId;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("instrument-type-master");
    }

    @Test
    @DisplayName("楽器タイプを登録・編集・無効化・有効化できる")
    void manageInstrumentTypeMaster() throws Exception {
        deleteStaleTestData();

        try {
            openList();
            openCreateForm();
            createInstrumentType();
            verifyCreated();
            openEditForm();
            updateInstrumentType();
            verifyEdited();
            disableInstrumentType();
            verifyDisabled();
            enableInstrumentType();
            verifyEnabled();
        } finally {
            deleteCreatedInstrumentTypeSafely();
        }
    }

    private void openList() {
        page.navigate(BASE_URL + "/instrument-types/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("楽器タイプマスタ一覧"));
        captureScreenshot("01-list.png");
    }

    private void openCreateForm() {
        page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("楽器タイプ登録"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("楽器タイプマスタ登録"));
        captureScreenshot("02-create-form.png");
    }

    private void createInstrumentType() {
        page.locator("#instrumentCode").fill("e2e inst");
        page.locator("#instrumentName").fill(INITIAL_NAME);
        page.locator("#bodyType").fill(INITIAL_BODY_TYPE);
        page.locator("#neckType").fill(INITIAL_NECK_TYPE);

        assertThat(page.locator("#instrumentCodePreview"))
                .hasText(INSTRUMENT_CODE);
        assertThat(page.locator("#internalModelCodePreview"))
                .hasText("MIJ-HER50-" + INSTRUMENT_CODE);
        assertThat(page.locator("#bodyTypePreview"))
                .hasText(INITIAL_BODY_TYPE);
        assertThat(page.locator("#neckTypePreview"))
                .hasText(INITIAL_NECK_TYPE);
        captureScreenshot("03-create-input.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("楽器タイプを登録"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/instrument-types/view"));
    }

    private void verifyCreated() throws Exception {
        Locator row = findTargetRow();
        assertThat(row).containsText(INITIAL_NAME);
        assertThat(row).containsText(INITIAL_BODY_TYPE);
        assertThat(row).containsText(INITIAL_NECK_TYPE);
        assertThat(row.locator(".status-badge")).hasText("有効");

        createdInstrumentTypeId = findInstrumentTypeId();
        assertTrue(createdInstrumentTypeId > 0L);
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
                Pattern.compile("楽器タイプマスタ編集"));
        assertThat(page.locator("#instrumentCode"))
                .hasValue(INSTRUMENT_CODE);
        assertFalse(page.locator("#instrumentCode").isEditable());
        assertThat(page.locator("#instrumentName"))
                .hasValue(INITIAL_NAME);
        assertThat(page.locator("#bodyType"))
                .hasValue(INITIAL_BODY_TYPE);
        assertThat(page.locator("#neckType"))
                .hasValue(INITIAL_NECK_TYPE);
        captureScreenshot("05-edit-form.png");
    }

    private void updateInstrumentType() {
        page.locator("#instrumentName").fill(UPDATED_NAME);
        page.locator("#bodyType").fill(UPDATED_BODY_TYPE);
        page.locator("#neckType").fill(UPDATED_NECK_TYPE);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("変更内容を保存"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/instrument-types/view"));
    }

    private void verifyEdited() throws Exception {
        Locator row = findTargetRow();
        assertThat(row).containsText(UPDATED_NAME);
        assertThat(row).containsText(UPDATED_BODY_TYPE);
        assertThat(row).containsText(UPDATED_NECK_TYPE);

        InstrumentTypeRecord record = findInstrumentType();
        assertEquals(UPDATED_NAME, record.instrumentName());
        assertEquals(UPDATED_BODY_TYPE, record.bodyType());
        assertEquals(UPDATED_NECK_TYPE, record.neckType());
        captureScreenshot("06-edited.png");
    }

    private void disableInstrumentType() {
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
        assertFalse(findInstrumentType().active());
        captureScreenshot("07-disabled.png");
    }

    private void enableInstrumentType() {
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
        assertTrue(findInstrumentType().active());
        captureScreenshot("08-enabled.png");
    }

    private Locator findTargetRow() {
        Locator row = page.locator("tbody tr")
                .filter(new Locator.FilterOptions()
                        .setHasText(INSTRUMENT_CODE));
        assertEquals(
                1,
                row.count(),
                INSTRUMENT_CODE + "の行が一意に見つかりません。");
        return row;
    }

    private Long findInstrumentTypeId() throws Exception {
        return findInstrumentType().id();
    }

    private InstrumentTypeRecord findInstrumentType()
            throws Exception {

        String sql = """
                SELECT
                    id,
                    instrument_name,
                    body_type,
                    neck_type,
                    active
                FROM m_instrument_type
                WHERE instrument_code = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(1, INSTRUMENT_CODE);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "対象楽器タイプがDBにありません。");
                }
                return new InstrumentTypeRecord(
                        resultSet.getLong("id"),
                        resultSet.getString("instrument_name"),
                        resultSet.getString("body_type"),
                        resultSet.getString("neck_type"),
                        resultSet.getBoolean("active"));
            }
        }
    }

    private void deleteStaleTestData() throws Exception {
        deleteByInstrumentCode(INSTRUMENT_CODE);
    }

    private void deleteCreatedInstrumentTypeSafely()
            throws Exception {
        deleteByInstrumentCode(INSTRUMENT_CODE);
    }

    private void deleteByInstrumentCode(String instrumentCode)
            throws Exception {

        String sql = """
                DELETE FROM m_instrument_type instrument
                WHERE instrument.instrument_code = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM m_product product
                      WHERE product.internal_model_code =
                            instrument.instrument_code
                         OR product.internal_model_code LIKE
                            '%-' || instrument.instrument_code
                  )
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(1, instrumentCode);
            statement.executeUpdate();
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                E2E_DB_URL,
                E2E_DB_USER,
                E2E_DB_PASSWORD);
    }

    private record InstrumentTypeRecord(
            Long id,
            String instrumentName,
            String bodyType,
            String neckType,
            boolean active) {
    }
}
