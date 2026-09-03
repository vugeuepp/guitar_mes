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

class BodyBulkProcessE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL = System.getProperty(
            "e2e.db.url",
            "jdbc:postgresql://localhost:5432/guitar_mes_e2e");
    private static final String E2E_DB_USER = System.getProperty(
            "e2e.db.user",
            "naokiyamada");
    private static final String E2E_DB_PASSWORD = System.getProperty(
            "e2e.db.password",
            "");
    private static final String WORKER_NAME = "E2E Body Bulk Worker";
    private static final String INSPECTION = "塗装後検品";
    private static final String PARTS = "パーツ付け";

    private final List<Long> bodyIds = new ArrayList<>();
    private final List<Long> historyIds = new ArrayList<>();
    private Long inspectionProcessId;
    private Long partsProcessId;
    private Long bodyMasterId;
    private String firstSerial;
    private String secondSerial;
    private String otherSerial;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("bulk-body-process");
    }

    @Test
    @DisplayName("Body工程を工程別に選択して一括開始・終了できる")
    void bulkStartAndEndBodyProcesses() throws Exception {
        try {
            prepareTestData();
            openBodyListAndVerifyInitialState();
            selectInspectionAndVerifyFiltering();
            startProcessesInBulk();
            verifyStartedInDatabase();
            openBulkEndAndVerifyInitialState();
            selectInspectionForBulkEnd();
            endProcessesInBulk();
            verifyEndedInDatabase();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        findReferences();
        String suffix = String.valueOf(System.currentTimeMillis());
        firstSerial = "E2EBODY-A-" + suffix;
        secondSerial = "E2EBODY-B-" + suffix;
        otherSerial = "E2EBODY-X-" + suffix;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                bodyIds.add(insertBody(
                        connection, firstSerial, INSPECTION, "WAITING_INSPECTION"));
                bodyIds.add(insertBody(
                        connection, secondSerial, INSPECTION, "WAITING_INSPECTION"));
                bodyIds.add(insertBody(
                        connection, otherSerial, PARTS, "WAITING"));
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void findReferences() throws Exception {
        String processSql = """
                SELECT id, process_name
                FROM m_process
                WHERE target_type = 'BODY'
                  AND process_name IN (?, ?)
                """;
        String masterSql = """
                SELECT body_master_id AS id
                FROM m_product
                WHERE body_master_id IS NOT NULL
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement processStatement =
                     connection.prepareStatement(processSql);
             PreparedStatement masterStatement =
                     connection.prepareStatement(masterSql)) {
            processStatement.setString(1, INSPECTION);
            processStatement.setString(2, PARTS);
            try (ResultSet resultSet = processStatement.executeQuery()) {
                while (resultSet.next()) {
                    if (INSPECTION.equals(resultSet.getString("process_name"))) {
                        inspectionProcessId = resultSet.getLong("id");
                    }
                    if (PARTS.equals(resultSet.getString("process_name"))) {
                        partsProcessId = resultSet.getLong("id");
                    }
                }
            }
            try (ResultSet resultSet = masterStatement.executeQuery()) {
                if (resultSet.next()) {
                    bodyMasterId = resultSet.getLong("id");
                }
            }
        }

        assertNotNull(inspectionProcessId, "塗装後検品の工程マスタが必要です。");
        assertNotNull(partsProcessId, "パーツ付けの工程マスタが必要です。");
        assertNotNull(bodyMasterId, "BodyMasterが必要です。");
    }

    private Long insertBody(
            Connection connection,
            String serialNo,
            String currentProcess,
            String status) throws Exception {
        String sql = """
                INSERT INTO t_body (
                    serial_no,
                    model_name,
                    color,
                    current_process,
                    status,
                    body_master_id
                ) VALUES (?, 'E2E Bulk Body', 'E2E Color', ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, currentProcess);
            statement.setString(3, status);
            statement.setLong(4, bodyMasterId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "E2E用Bodyを作成できませんでした。");
                return resultSet.getLong("id");
            }
        }
    }

    private void openBodyListAndVerifyInitialState() {
        page.navigate(BASE_URL + "/bodies/view");
        page.waitForLoadState();
        assertThat(page).hasTitle(Pattern.compile("ボディ管理一覧"));
        assertThat(page.locator(".bulk-process-guidance"))
                .containsText("先に対象工程を選択してください");
        assertThat(bodyCheckbox(firstSerial)).isDisabled();
        assertThat(bodyCheckbox(secondSerial)).isDisabled();
        assertThat(bodyCheckbox(otherSerial)).isDisabled();
        assertThat(page.locator("#selected-count")).hasText("0");
        assertThat(page.locator("#processId"))
                .containsText(INSPECTION);
        assertThat(page.locator("#processId"))
                .containsText(PARTS);
        captureScreenshot("01-body-list-initial.png");
    }

    private void selectInspectionAndVerifyFiltering() {
        page.locator("#processId")
                .selectOption(String.valueOf(inspectionProcessId));
        assertThat(bodyCheckbox(firstSerial)).isEnabled();
        assertThat(bodyCheckbox(secondSerial)).isEnabled();
        assertThat(bodyCheckbox(otherSerial)).isDisabled();

        bodyCheckbox(firstSerial).check();
        bodyCheckbox(secondSerial).check();
        assertThat(page.locator("#selected-count")).hasText("2");

        page.locator("#processId")
                .selectOption(String.valueOf(partsProcessId));
        assertThat(page.locator("#selected-count")).hasText("0");
        assertThat(bodyCheckbox(firstSerial)).isDisabled();
        assertThat(bodyCheckbox(secondSerial)).isDisabled();
        assertThat(bodyCheckbox(otherSerial)).isEnabled();

        page.locator("#processId")
                .selectOption(String.valueOf(inspectionProcessId));
        bodyCheckbox(firstSerial).check();
        bodyCheckbox(secondSerial).check();
        page.locator("#workerName").fill(WORKER_NAME);
        captureScreenshot("02-body-targets-selected.png");
    }

    private void startProcessesInBulk() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("一括工程開始"))
                .click();
        page.waitForLoadState();
        assertThat(page).hasURL(Pattern.compile(".*/bodies/view"));
        assertThat(page.locator(".success-message"))
                .containsText("2件のボディ工程を一括開始しました。");
        assertThat(bodyRow(firstSerial)).containsText("作業中");
        assertThat(bodyRow(secondSerial)).containsText("作業中");
        assertThat(bodyRow(otherSerial)).containsText("工程待ち");
        captureScreenshot("03-body-bulk-started.png");
    }

    private void verifyStartedInDatabase() throws Exception {
        String sql = """
                SELECT id, body_id, process_id, worker_name, end_time
                FROM t_body_process_history
                WHERE body_id IN (?, ?)
                ORDER BY body_id
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bodyIds.get(0));
            statement.setLong(2, bodyIds.get(1));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    historyIds.add(resultSet.getLong("id"));
                    assertEquals(
                            inspectionProcessId.longValue(),
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
        page.navigate(BASE_URL + "/body-processes/bulk/end/view");
        page.waitForLoadState();
        assertThat(page).hasTitle(Pattern.compile("ボディ工程一括終了"));
        assertThat(historyCheckbox(historyIds.get(0))).isDisabled();
        assertThat(historyCheckbox(historyIds.get(1))).isDisabled();
        assertThat(page.locator("table.bulk-select-table"))
                .containsText(INSPECTION);
        assertEquals(
                0,
                page.locator("table.bulk-select-table").getByText("工程ID").count());
        assertThat(page.locator("table.bulk-select-table"))
                .containsText(firstSerial);
        assertEquals(0, page.locator("table.bulk-select-table")
                .getByText("履歴ID").count());
        captureScreenshot("04-body-bulk-end-initial.png");
    }

    private void selectInspectionForBulkEnd() {
        page.locator("#targetProcessId")
                .selectOption(String.valueOf(inspectionProcessId));
        assertThat(historyCheckbox(historyIds.get(0))).isEnabled();
        assertThat(historyCheckbox(historyIds.get(1))).isEnabled();

        List<String> labels = page.locator("#result option")
                .allTextContents();
        assertTrue(labels.contains("合格"));
        assertTrue(labels.contains("手直し"));
        assertTrue(labels.contains("不合格"));
        assertFalse(labels.contains("完了"));

        page.locator("#select-all").check();
        assertThat(page.locator("#selected-count")).hasText("2");
        page.locator("#result").selectOption("PASSED");
        captureScreenshot("05-body-histories-selected.png");
    }

    private void endProcessesInBulk() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("一括工程終了"))
                .click();
        page.waitForLoadState();
        assertThat(page).hasURL(Pattern.compile(".*/bodies/view"));
        assertThat(page.locator(".success-message"))
                .containsText("2件のボディ工程を一括終了しました。");
        assertThat(bodyRow(firstSerial)).containsText(PARTS);
        assertThat(bodyRow(firstSerial)).containsText("工程待ち");
        assertThat(bodyRow(secondSerial)).containsText(PARTS);
        captureScreenshot("06-body-bulk-ended.png");
    }

    private void verifyEndedInDatabase() throws Exception {
        String historySql = """
                SELECT COUNT(*) AS completed_count
                FROM t_body_process_history
                WHERE id IN (?, ?)
                  AND result = 'PASSED'
                  AND end_time IS NOT NULL
                """;
        String bodySql = """
                SELECT COUNT(*) AS moved_count
                FROM t_body
                WHERE id IN (?, ?)
                  AND current_process = ?
                  AND status = 'WAITING'
                """;
        try (Connection connection = openConnection();
             PreparedStatement history = connection.prepareStatement(historySql);
             PreparedStatement body = connection.prepareStatement(bodySql)) {
            history.setLong(1, historyIds.get(0));
            history.setLong(2, historyIds.get(1));
            try (ResultSet resultSet = history.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(2, resultSet.getInt("completed_count"));
            }
            body.setLong(1, bodyIds.get(0));
            body.setLong(2, bodyIds.get(1));
            body.setString(3, PARTS);
            try (ResultSet resultSet = body.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(2, resultSet.getInt("moved_count"));
            }
        }
    }

    private Locator bodyRow(String serialNo) {
        Locator row = page.locator("table.body-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(serialNo));
        assertEquals(1, row.count(), serialNo + "の行が一意に見つかりません。");
        return row;
    }

    private Locator bodyCheckbox(String serialNo) {
        return bodyRow(serialNo).locator("input.row-checkbox");
    }

    private Locator historyCheckbox(Long historyId) {
        return page.locator(
                "tr[data-history-id='" + historyId + "'] input.row-checkbox");
    }

    private void cleanupSafely() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Long bodyId : bodyIds) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM t_body_process_history WHERE body_id = ?")) {
                        statement.setLong(1, bodyId);
                        statement.executeUpdate();
                    }
                }
                for (Long bodyId : bodyIds) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM t_body WHERE id = ?")) {
                        statement.setLong(1, bodyId);
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
