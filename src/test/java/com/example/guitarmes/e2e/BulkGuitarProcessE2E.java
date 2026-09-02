package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class BulkGuitarProcessE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL = System.getProperty(
            "e2e.db.url",
            "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER = System.getProperty(
            "e2e.db.user",
            "naokiyamada");

    private static final String E2E_DB_PASSWORD = System.getProperty(
            "e2e.db.password",
            "");

    private static final String WORKER_NAME = "E2E Bulk Worker";

    private Long productionOrderId;
    private final List<Long> guitarIds = new ArrayList<>();
    private final List<Long> historyIds = new ArrayList<>();
    private String orderNo;
    private String firstSerial;
    private String secondSerial;
    private ProcessReference firstProcess;
    private ProcessReference secondProcess;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("bulk-guitar-process");
    }

    @Test
    @DisplayName("複数Guitarの工程を一括開始して一括終了できる")
    void bulkStartAndEndGuitarProcesses() throws Exception {
        try {
            prepareTestData();
            openGuitarList();
            selectTargetGuitars();
            startProcessesInBulk();
            verifyStartedInDatabase();
            openRunningProcessList();
            selectTargetHistories();
            endProcessesInBulk();
            verifyEndedInDatabase();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        ProductReference product = findProductReference();
        List<ProcessReference> processes = findGuitarProcesses();
        if (processes.size() < 2) {
            throw new IllegalStateException("Guitar工程マスタが2件以上必要です。");
        }
        firstProcess = processes.get(0);
        secondProcess = processes.get(1);

        String suffix = String.valueOf(System.currentTimeMillis());
        orderNo = "E2E-BULK-" + suffix;
        firstSerial = "E2EBULK-A-" + suffix;
        secondSerial = "E2EBULK-B-" + suffix;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                productionOrderId = insertProductionOrder(
                        connection,
                        product.productId(),
                        orderNo);
                guitarIds.add(insertGuitar(
                        connection,
                        product.productId(),
                        productionOrderId,
                        firstSerial,
                        firstProcess.processName()));
                guitarIds.add(insertGuitar(
                        connection,
                        product.productId(),
                        productionOrderId,
                        secondSerial,
                        firstProcess.processName()));
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private ProductReference findProductReference() throws Exception {
        String sql = """
                SELECT id
                FROM m_product
                ORDER BY id
                LIMIT 1
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("E2Eで使用できるProductがありません。");
            }
            return new ProductReference(resultSet.getLong("id"));
        }
    }

    private List<ProcessReference> findGuitarProcesses() throws Exception {
        String sql = """
                SELECT id, process_name, process_order
                FROM m_process
                WHERE target_type = 'GUITAR'
                ORDER BY process_order
                """;
        List<ProcessReference> processes = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                processes.add(new ProcessReference(
                        resultSet.getLong("id"),
                        resultSet.getString("process_name"),
                        resultSet.getInt("process_order")));
            }
        }
        return processes;
    }

    private Long insertProductionOrder(
            Connection connection,
            Long productId,
            String testOrderNo) throws Exception {
        String sql = """
                INSERT INTO t_production_order (
                    order_no,
                    product_id,
                    planned_quantity,
                    started_quantity,
                    completed_quantity,
                    plan_month,
                    planned_start_date,
                    due_date,
                    status
                ) VALUES (?, ?, 2, 2, 0, ?, ?, ?, 'IN_PROGRESS')
                RETURNING id
                """;
        LocalDate startDate = LocalDate.now().plusDays(1);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, testOrderNo);
            statement.setLong(2, productId);
            statement.setObject(3, startDate.withDayOfMonth(1));
            statement.setObject(4, startDate);
            statement.setObject(5, startDate.plusDays(7));
            return executeInsertReturningId(statement);
        }
    }

    private Long insertGuitar(
            Connection connection,
            Long productId,
            Long orderId,
            String serialNo,
            String currentProcess) throws Exception {
        String sql = """
                INSERT INTO t_guitar (
                    serial_no,
                    current_process,
                    product_id,
                    production_order_id
                ) VALUES (?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, currentProcess);
            statement.setLong(3, productId);
            statement.setLong(4, orderId);
            return executeInsertReturningId(statement);
        }
    }

    private Long executeInsertReturningId(
            PreparedStatement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("E2EテストデータのIDを取得できませんでした。");
            }
            return resultSet.getLong("id");
        }
    }

    private void openGuitarList() {
        page.navigate(BASE_URL + "/guitars/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("ギター管理一覧"));
        assertThat(page.locator("main.page-container")).containsText(firstSerial);
        assertThat(page.locator("main.page-container")).containsText(secondSerial);
        assertThat(page.locator("#selected-count")).hasText("0");
        captureScreenshot("01-guitar-list.png");
    }

    private void selectTargetGuitars() {
        guitarRow(firstSerial).locator("input.row-checkbox").check();
        guitarRow(secondSerial).locator("input.row-checkbox").check();

        assertThat(page.locator("#selected-count")).hasText("2");
        page.locator("#processId").selectOption(String.valueOf(firstProcess.id()));
        page.locator("#workerName").fill(WORKER_NAME);

        assertThat(page.locator("#processId"))
                .hasValue(String.valueOf(firstProcess.id()));
        assertThat(page.locator("#workerName")).hasValue(WORKER_NAME);
        captureScreenshot("02-guitars-selected.png");
    }

    private void startProcessesInBulk() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("一括工程開始"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(Pattern.compile(".*/guitars/view"));
        assertThat(page.locator(".success-message")).containsText("2件");
        assertThat(guitarRow(firstSerial)).containsText("作業中");
        assertThat(guitarRow(secondSerial)).containsText("作業中");
        captureScreenshot("03-bulk-started.png");
    }

    private void verifyStartedInDatabase() throws Exception {
        String sql = """
                SELECT id, guitar_id, process_id, worker_name, start_time, end_time
                FROM t_process_history
                WHERE guitar_id IN (?, ?)
                ORDER BY guitar_id
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guitarIds.get(0));
            statement.setLong(2, guitarIds.get(1));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    historyIds.add(resultSet.getLong("id"));
                    assertEquals(firstProcess.id(), resultSet.getLong("process_id"));
                    assertEquals(WORKER_NAME, resultSet.getString("worker_name"));
                    assertNotNull(resultSet.getTimestamp("start_time"));
                    assertEquals(null, resultSet.getTimestamp("end_time"));
                }
            }
        }
        assertEquals(2, historyIds.size());
    }

    private void openRunningProcessList() {
        page.navigate(BASE_URL + "/processes/end/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("工程終了"));
        assertThat(page.locator("main.page-container")).containsText(WORKER_NAME);
        assertThat(page.locator("#selected-count")).hasText("0");
        captureScreenshot("04-running-process-list.png");
    }

    private void selectTargetHistories() {
        historyRow(historyIds.get(0)).locator("input.row-checkbox").check();
        historyRow(historyIds.get(1)).locator("input.row-checkbox").check();

        assertThat(page.locator("#selected-count")).hasText("2");
        captureScreenshot("05-histories-selected.png");
    }

    private void endProcessesInBulk() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("一括工程終了"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(Pattern.compile(".*/processes/end/view"));
        assertThat(page.locator(".success-message")).containsText("2件");
        assertEquals(0, historyRow(historyIds.get(0)).count());
        assertEquals(0, historyRow(historyIds.get(1)).count());
        captureScreenshot("06-bulk-ended.png");
    }

    private void verifyEndedInDatabase() throws Exception {
        String historySql = """
                SELECT COUNT(*) AS completed_count
                FROM t_process_history
                WHERE id IN (?, ?)
                  AND end_time IS NOT NULL
                """;
        String guitarSql = """
                SELECT COUNT(*) AS next_process_count
                FROM t_guitar
                WHERE id IN (?, ?)
                  AND current_process = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement historyStatement = connection.prepareStatement(historySql);
             PreparedStatement guitarStatement = connection.prepareStatement(guitarSql)) {
            historyStatement.setLong(1, historyIds.get(0));
            historyStatement.setLong(2, historyIds.get(1));
            try (ResultSet resultSet = historyStatement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(2, resultSet.getInt("completed_count"));
            }

            guitarStatement.setLong(1, guitarIds.get(0));
            guitarStatement.setLong(2, guitarIds.get(1));
            guitarStatement.setString(3, secondProcess.processName());
            try (ResultSet resultSet = guitarStatement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(2, resultSet.getInt("next_process_count"));
            }
        }
    }

    private Locator guitarRow(String serialNo) {
        Locator row = page.locator("table.guitar-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(serialNo));
        assertEquals(1, row.count(), serialNo + "の行が一意に見つかりません。");
        return row;
    }

    private Locator historyRow(Long historyId) {
        return page.locator("tr[data-history-id='" + historyId + "']");
    }

    private void cleanupSafely() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                deleteByProductionOrder(connection, "t_process_history", "guitar_id");
                deleteByIdList(connection, "t_guitar", guitarIds);
                deleteById(connection, "t_production_order", productionOrderId);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void deleteByProductionOrder(
            Connection connection,
            String tableName,
            String guitarIdColumn) throws Exception {
        if (productionOrderId == null) {
            return;
        }
        String sql = "DELETE FROM " + tableName
                + " WHERE " + guitarIdColumn
                + " IN (SELECT id FROM t_guitar WHERE production_order_id = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productionOrderId);
            statement.executeUpdate();
        }
    }

    private void deleteByIdList(
            Connection connection,
            String tableName,
            List<Long> ids) throws Exception {
        for (Long id : ids) {
            deleteById(connection, tableName, id);
        }
    }

    private void deleteById(
            Connection connection,
            String tableName,
            Long id) throws Exception {
        if (id == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + tableName + " WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                E2E_DB_URL,
                E2E_DB_USER,
                E2E_DB_PASSWORD);
    }

    private record ProductReference(Long productId) {
    }

    private record ProcessReference(
            Long id,
            String processName,
            Integer processOrder) {
    }
}
