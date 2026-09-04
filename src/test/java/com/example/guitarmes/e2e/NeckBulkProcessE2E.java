package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class NeckBulkProcessE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL = System.getProperty(
            "e2e.db.url",
            "jdbc:postgresql://localhost:5432/guitar_mes_e2e");
    private static final String E2E_DB_USER = System.getProperty(
            "e2e.db.user",
            "naokiyamada");
    private static final String E2E_DB_PASSWORD = System.getProperty(
            "e2e.db.password",
            "");

    private static final String WORKER_NAME = "E2E Neck Bulk Worker";
    private static final String PLEK = "PLEK";
    private static final String FRET_FINISH =
            "フレット擦り合わせ・ナット手成形";
    private static final String PARTS = "ネックパーツ付け";

    private final List<Long> neckIds = new ArrayList<>();
    private final List<Long> historyIds = new ArrayList<>();

    private Long productId;
    private Long neckMasterId;
    private Long plekProcessId;
    private Long partsProcessId;
    private String firstSerial;
    private String secondSerial;
    private String otherSerial;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("bulk-neck-process");
    }

    @Test
    @DisplayName("Neck工程を工程別に選択して一括開始・終了できる")
    void bulkStartAndEndNeckProcesses() throws Exception {
        try {
            prepareTestData();
            openNeckListAndVerifyInitialState();
            selectPlekAndVerifyFiltering();
            startProcessesInBulk();
            verifyStartedInDatabase();
            openBulkEndAndVerifyInitialState();
            selectPlekForBulkEnd();
            verifyNgRequiresNote();
            endProcessesInBulk();
            verifyEndedInDatabase();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        findReferences();

        String suffix = String.valueOf(System.currentTimeMillis());
        firstSerial = "E2ENECK-A-" + suffix;
        secondSerial = "E2ENECK-B-" + suffix;
        otherSerial = "E2ENECK-X-" + suffix;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                neckIds.add(insertNeck(
                        connection,
                        firstSerial,
                        PLEK,
                        "WAITING"));
                neckIds.add(insertNeck(
                        connection,
                        secondSerial,
                        PLEK,
                        "WAITING"));
                neckIds.add(insertNeck(
                        connection,
                        otherSerial,
                        PARTS,
                        "WAITING"));
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void findReferences() throws Exception {
        String productSql = """
                SELECT id, neck_master_id
                FROM m_product
                WHERE neck_master_id IS NOT NULL
                ORDER BY id
                LIMIT 1
                """;
        String processSql = """
                SELECT id, process_name
                FROM m_process
                WHERE target_type = 'NECK'
                  AND process_name IN (?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement productStatement =
                     connection.prepareStatement(productSql);
             PreparedStatement processStatement =
                     connection.prepareStatement(processSql)) {

            try (ResultSet resultSet = productStatement.executeQuery()) {
                if (resultSet.next()) {
                    productId = resultSet.getLong("id");
                    neckMasterId = resultSet.getLong("neck_master_id");
                }
            }

            processStatement.setString(1, PLEK);
            processStatement.setString(2, PARTS);
            try (ResultSet resultSet = processStatement.executeQuery()) {
                while (resultSet.next()) {
                    if (PLEK.equals(resultSet.getString("process_name"))) {
                        plekProcessId = resultSet.getLong("id");
                    }
                    if (PARTS.equals(resultSet.getString("process_name"))) {
                        partsProcessId = resultSet.getLong("id");
                    }
                }
            }
        }

        assertNotNull(productId, "E2Eで使用できるProductが必要です。");
        assertNotNull(neckMasterId, "NeckMasterが必要です。");
        assertNotNull(plekProcessId, "PLEK工程マスタが必要です。");
        assertNotNull(partsProcessId, "ネックパーツ付け工程マスタが必要です。");
    }

    private Long insertNeck(
            Connection connection,
            String serialNo,
            String currentProcess,
            String status) throws Exception {
        String sql = """
                INSERT INTO t_neck (
                    serial_no,
                    model_name,
                    current_process,
                    status,
                    product_id,
                    neck_master_id
                ) VALUES (?, 'E2E Bulk Neck', ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, currentProcess);
            statement.setString(3, status);
            statement.setLong(4, productId);
            statement.setLong(5, neckMasterId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "E2E用Neckを作成できませんでした。");
                return resultSet.getLong("id");
            }
        }
    }

    private void openNeckListAndVerifyInitialState() {
        page.navigate(BASE_URL + "/necks/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("ネック管理一覧"));
        assertThat(page.locator(".bulk-process-guidance"))
                .containsText("対象工程を選択すると、一致する個体だけを選択できます。");
        assertThat(neckCheckbox(firstSerial)).isDisabled();
        assertThat(neckCheckbox(secondSerial)).isDisabled();
        assertThat(neckCheckbox(otherSerial)).isDisabled();
        assertThat(page.locator("#selected-count")).hasText("0");
        assertThat(neckRow(firstSerial).locator(".component-model-name"))
                .hasText("E2E Bulk Neck");
        assertThat(neckRow(firstSerial).locator(".process-badge"))
                .isVisible();
        Number checkboxWidth = (Number) neckRow(firstSerial)
                .locator(".bulk-checkbox-cell")
                .evaluate("element => element.getBoundingClientRect().width");
        Number modelWidth = (Number) neckRow(firstSerial)
                .locator(".component-model-cell")
                .evaluate("element => element.getBoundingClientRect().width");
        assertTrue(checkboxWidth.doubleValue()
                        < modelWidth.doubleValue() * 0.2,
                "チェックボックス列はモデル列の20%未満である必要があります。");
        assertThat(page.locator("#processId")).containsText(PLEK);
        assertThat(page.locator("#processId")).containsText(PARTS);

        captureScreenshot("01-neck-list-initial.png");
    }

    private void selectPlekAndVerifyFiltering() {
        page.locator("#processId")
                .selectOption(String.valueOf(plekProcessId));

        assertThat(neckCheckbox(firstSerial)).isEnabled();
        assertThat(neckCheckbox(secondSerial)).isEnabled();
        assertThat(neckCheckbox(otherSerial)).isDisabled();

        neckCheckbox(firstSerial).check();
        neckCheckbox(secondSerial).check();
        assertThat(page.locator("#selected-count")).hasText("2");

        page.locator("#processId")
                .selectOption(String.valueOf(partsProcessId));
        assertThat(page.locator("#selected-count")).hasText("0");
        assertThat(neckCheckbox(firstSerial)).isDisabled();
        assertThat(neckCheckbox(secondSerial)).isDisabled();
        assertThat(neckCheckbox(otherSerial)).isEnabled();

        page.locator("#processId")
                .selectOption(String.valueOf(plekProcessId));
        neckCheckbox(firstSerial).check();
        neckCheckbox(secondSerial).check();
        page.locator("#workerName").fill(WORKER_NAME);

        captureScreenshot("02-neck-targets-selected.png");
    }

    private void startProcessesInBulk() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("一括工程開始"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(Pattern.compile(".*/necks/view"));
        assertThat(page.locator(".success-message"))
                .containsText("2件のネック工程を一括開始しました。");
        assertThat(neckRow(firstSerial)).containsText("作業中");
        assertThat(neckRow(secondSerial)).containsText("作業中");
        assertThat(neckRow(otherSerial)).containsText("工程待ち");

        captureScreenshot("03-neck-bulk-started.png");
    }

    private void verifyStartedInDatabase() throws Exception {
        String sql = """
                SELECT id, neck_id, process_id, worker_name, end_time
                FROM t_neck_process_history
                WHERE neck_id IN (?, ?)
                ORDER BY neck_id
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, neckIds.get(0));
            statement.setLong(2, neckIds.get(1));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    historyIds.add(resultSet.getLong("id"));
                    assertEquals(
                            plekProcessId.longValue(),
                            resultSet.getLong("process_id"));
                    assertEquals(
                            WORKER_NAME,
                            resultSet.getString("worker_name"));
                    assertEquals(null, resultSet.getTimestamp("end_time"));
                }
            }
        }

        assertEquals(2, historyIds.size());
    }

    private void openBulkEndAndVerifyInitialState() {
        page.navigate(BASE_URL + "/neck-processes/bulk/end/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("ネック工程一括終了"));
        assertThat(historyCheckbox(historyIds.get(0))).isDisabled();
        assertThat(historyCheckbox(historyIds.get(1))).isDisabled();
        assertThat(page.locator("table.bulk-select-table")).containsText(PLEK);
        assertEquals(
                0,
                page.locator("table.bulk-select-table")
                        .getByText("工程ID")
                        .count());

        assertThat(page.locator("table.bulk-select-table"))
                .containsText(firstSerial);
        assertEquals(0, page.locator("table.bulk-select-table")
                .getByText("履歴ID").count());
        captureScreenshot("04-neck-bulk-end-initial.png");
    }

    private void selectPlekForBulkEnd() {
        page.locator("#targetProcessId")
                .selectOption(String.valueOf(plekProcessId));

        assertThat(historyCheckbox(historyIds.get(0))).isEnabled();
        assertThat(historyCheckbox(historyIds.get(1))).isEnabled();

        List<String> labels = page.locator("#result option")
                .allTextContents();
        assertTrue(labels.contains("完了"));
        assertTrue(labels.contains("NG"));
        assertFalse(labels.contains("合格"));

        page.locator("#select-all").check();
        assertThat(page.locator("#selected-count")).hasText("2");

        captureScreenshot("05-neck-histories-selected.png");
    }

    private void verifyNgRequiresNote() {
        page.locator("#result").selectOption("NG");
        assertThat(page.locator("#note")).hasAttribute("required", "");

        page.locator("#result").selectOption("COMPLETED");
        assertFalse((Boolean) page.locator("#note").evaluate(
                "element => element.required"));

        captureScreenshot("06-neck-ng-validation.png");
    }

    private void endProcessesInBulk() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("一括工程終了"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(Pattern.compile(".*/necks/view"));
        assertThat(page.locator(".success-message"))
                .containsText("2件のネック工程を一括終了しました。");
        assertThat(neckRow(firstSerial)).containsText(FRET_FINISH);
        assertThat(neckRow(firstSerial)).containsText("工程待ち");
        assertThat(neckRow(secondSerial)).containsText(FRET_FINISH);

        captureScreenshot("07-neck-bulk-ended.png");
    }

    private void verifyEndedInDatabase() throws Exception {
        String historySql = """
                SELECT COUNT(*) AS completed_count
                FROM t_neck_process_history
                WHERE id IN (?, ?)
                  AND result = 'COMPLETED'
                  AND end_time IS NOT NULL
                """;
        String neckSql = """
                SELECT COUNT(*) AS moved_count
                FROM t_neck
                WHERE id IN (?, ?)
                  AND current_process = ?
                  AND status = 'WAITING'
                """;

        try (Connection connection = openConnection();
             PreparedStatement history = connection.prepareStatement(historySql);
             PreparedStatement neck = connection.prepareStatement(neckSql)) {
            history.setLong(1, historyIds.get(0));
            history.setLong(2, historyIds.get(1));
            try (ResultSet resultSet = history.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(2, resultSet.getInt("completed_count"));
            }

            neck.setLong(1, neckIds.get(0));
            neck.setLong(2, neckIds.get(1));
            neck.setString(3, FRET_FINISH);
            try (ResultSet resultSet = neck.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(2, resultSet.getInt("moved_count"));
            }
        }
    }

    private Locator neckRow(String serialNo) {
        Locator row = page.locator("table.neck-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(serialNo));
        assertEquals(
                1,
                row.count(),
                serialNo + "の行が一意に見つかりません。");
        return row;
    }

    private Locator neckCheckbox(String serialNo) {
        return neckRow(serialNo).locator("input.row-checkbox");
    }

    private Locator historyCheckbox(Long historyId) {
        return page.locator(
                "tr[data-history-id='" + historyId + "'] input.row-checkbox");
    }

    private void cleanupSafely() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Long neckId : neckIds) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM t_neck_process_history WHERE neck_id = ?")) {
                        statement.setLong(1, neckId);
                        statement.executeUpdate();
                    }
                }
                for (Long neckId : neckIds) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM t_neck WHERE id = ?")) {
                        statement.setLong(1, neckId);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                E2E_DB_URL,
                E2E_DB_USER,
                E2E_DB_PASSWORD);
    }
}
