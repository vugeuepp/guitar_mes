package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class GuitarProcessE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");

    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");

    private static final String WORKER_NAME = "E2E Worker";

    private Long productionOrderId;
    private Long productionScheduleId;
    private Long bodyId;
    private Long neckId;
    private Long guitarId;
    private Long assemblyId;
    private String orderNo;
    private String bodySerial;
    private String neckSerial;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("guitar-process");
    }

    @Test
    @DisplayName("ネック取付後の全ギター工程を完了できる")
    void completeAllGuitarProcesses() throws Exception {
        try {
            prepareTestData();
            openProductionOrderDetail();
            openAssemblyForm();
            selectComponentsAndWorker();
            registerAssembly();
            verifyGuitarDetail();
            verifyDatabaseState();
            verifyProductionOrderUpdated();
            executeProcess(
                    "ギターパーツ取付",
                    "調整・調音",
                    "07-parts");
            executeProcess(
                    "調整・調音",
                    "最終検品",
                    "08-adjustment");
            executeProcess(
                    "最終検品",
                    "完成",
                    "09-final-inspection");
            verifyProcessHistory();
            verifyCompletionInDatabase();
            verifyCompletedProductionOrder();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        ProductReference product = findProductReference();
        String suffix = String.valueOf(System.currentTimeMillis());

        orderNo = "E2E-ASM-" + suffix;
        bodySerial = "E2EB" + suffix;
        neckSerial = "E2EN" + suffix;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                productionOrderId = insertProductionOrder(
                        connection,
                        product.productId(),
                        orderNo);
                productionScheduleId = insertProductionSchedule(
                        connection,
                        productionOrderId);
                bodyId = insertBody(
                        connection,
                        product,
                        bodySerial,
                        productionOrderId,
                        productionScheduleId);
                neckId = insertNeck(
                        connection,
                        product,
                        neckSerial,
                        productionOrderId,
                        productionScheduleId);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private ProductReference findProductReference() throws Exception {
        String sql = """
                SELECT
                    id,
                    product_name,
                    color,
                    body_master_id,
                    neck_master_id
                FROM m_product
                WHERE body_master_id IS NOT NULL
                  AND neck_master_id IS NOT NULL
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                throw new IllegalStateException(
                        "BodyMasterとNeckMasterを持つProductがありません。");
            }
            return new ProductReference(
                    resultSet.getLong("id"),
                    resultSet.getString("product_name"),
                    resultSet.getString("color"),
                    resultSet.getLong("body_master_id"),
                    resultSet.getLong("neck_master_id"));
        }
    }

    private Long insertBody(
            Connection connection,
            ProductReference product,
            String serialNo,
            Long orderId,
            Long scheduleId) throws Exception {

        String sql = """
                INSERT INTO t_body (
                    serial_no,
                    model_name,
                    color,
                    current_process,
                    status,
                    body_master_id,
                    production_order_id,
                    production_schedule_id
                ) VALUES (?, ?, ?, ?, 'AVAILABLE', ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, "E2E Assembly Body");
            statement.setString(3, product.color());
            statement.setString(4, "E2E_READY");
            statement.setLong(5, product.bodyMasterId());
            statement.setLong(6, orderId);
            statement.setLong(7, scheduleId);
            return executeInsertReturningId(statement);
        }
    }

    private Long insertNeck(
            Connection connection,
            ProductReference product,
            String serialNo,
            Long orderId,
            Long scheduleId) throws Exception {

        String sql = """
                INSERT INTO t_neck (
                    serial_no,
                    model_name,
                    current_process,
                    status,
                    product_id,
                    neck_master_id,
                    production_order_id,
                    production_schedule_id
                ) VALUES (?, ?, ?, 'AVAILABLE', ?, ?, ?, ?)
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, "E2E Assembly Neck");
            statement.setString(3, "E2E_READY");
            statement.setLong(4, product.productId());
            statement.setLong(5, product.neckMasterId());
            statement.setLong(6, orderId);
            statement.setLong(7, scheduleId);
            return executeInsertReturningId(statement);
        }
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
                ) VALUES (?, ?, 1, 0, 0, ?, ?, ?, 'PLANNED')
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            LocalDate startDate = LocalDate.now().plusDays(1);
            statement.setString(1, testOrderNo);
            statement.setLong(2, productId);
            statement.setObject(3, startDate.withDayOfMonth(1));
            statement.setObject(4, startDate);
            statement.setObject(5, startDate.plusDays(7));
            return executeInsertReturningId(statement);
        }
    }

    private Long insertProductionSchedule(
            Connection connection,
            Long orderId) throws Exception {
        String sql = """
                INSERT INTO t_production_schedule (
                    production_order_id,
                    schedule_date,
                    planned_quantity,
                    status,
                    created_at,
                    updated_at
                ) VALUES (?, ?, 1, 'CONFIRMED',
                          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """;
        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setObject(2, LocalDate.now().plusDays(1));
            return executeInsertReturningId(statement);
        }
    }

    private Long executeInsertReturningId(
            PreparedStatement statement) throws Exception {

        try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException(
                        "E2EテストデータのIDを取得できませんでした。");
            }
            return resultSet.getLong("id");
        }
    }

    private void openProductionOrderDetail() {
        page.navigate(
                BASE_URL
                + "/production-orders/"
                + productionOrderId
                + "/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("生産計画詳細"));
        assertThat(page.locator("main.page-container"))
                .containsText(orderNo);
        assertThat(page.locator(
                ".status-badge.status-planned"))
                .hasText("計画中");
        captureScreenshot("01-production-order-detail.png");
    }

    private void openAssemblyForm() {
        page.navigate(
                BASE_URL
                + "/assemblies/new?productionOrderId="
                + productionOrderId
                + "&productionScheduleId="
                + productionScheduleId);
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("ネック取付登録"));
        assertThat(page.locator("main.page-container"))
                .containsText(orderNo);
        assertThat(page.locator("#neckId"))
                .containsText(neckSerial);
        assertThat(page.locator("#bodyId"))
                .containsText(bodySerial);
        captureScreenshot("02-assembly-form.png");
    }

    private void selectComponentsAndWorker() {
        page.locator("#neckId")
                .selectOption(String.valueOf(neckId));
        page.locator("#bodyId")
                .selectOption(String.valueOf(bodyId));
        page.locator("#workerName").fill(WORKER_NAME);

        assertThat(page.locator("#neckId"))
                .hasValue(String.valueOf(neckId));
        assertThat(page.locator("#bodyId"))
                .hasValue(String.valueOf(bodyId));
        assertThat(page.locator("#workerName"))
                .hasValue(WORKER_NAME);
        captureScreenshot("03-components-selected.png");
    }

    private void registerAssembly() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("ネック取付を登録"))
                .click();
        page.waitForLoadState();

        Matcher matcher = Pattern
                .compile(".*/guitars/(\\d+)/view")
                .matcher(page.url());
        assertTrue(
                matcher.matches(),
                "Guitar詳細画面へ遷移していません: " + page.url());
        guitarId = Long.valueOf(matcher.group(1));
    }

    private void verifyGuitarDetail() {
        assertThat(page).hasTitle(Pattern.compile("ギター詳細"));
        assertThat(page.locator("main.page-container"))
                .containsText("ネック取付登録済み");
        assertThat(page.locator("main.page-container"))
                .containsText(neckSerial);
        assertThat(page.locator("main.page-container"))
                .containsText(bodySerial);
        assertThat(page.locator("main.page-container"))
                .containsText(WORKER_NAME);
        captureScreenshot("04-guitar-generated.png");
    }

    private void verifyDatabaseState() throws Exception {
        String sql = """
                SELECT
                    assembly.id AS assembly_id,
                    guitar.serial_no AS guitar_serial,
                    guitar.current_process,
                    body.status AS body_status,
                    neck.status AS neck_status,
                    production_order.started_quantity,
                    production_order.status AS order_status
                FROM t_assembly assembly
                JOIN t_guitar guitar
                  ON guitar.id = assembly.guitar_id
                JOIN t_body body
                  ON body.id = assembly.body_id
                JOIN t_neck neck
                  ON neck.id = assembly.neck_id
                JOIN t_production_order production_order
                  ON production_order.id = guitar.production_order_id
                WHERE assembly.guitar_id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, guitarId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(
                        resultSet.next(),
                        "生成されたAssemblyをDBで確認できません。");
                assemblyId = resultSet.getLong("assembly_id");
                assertNotNull(resultSet.getString("guitar_serial"));
                assertTrue(
                        !resultSet.getString("current_process").isBlank());
                assertEquals(
                        "ASSEMBLED",
                        resultSet.getString("body_status"));
                assertEquals(
                        "ASSEMBLED",
                        resultSet.getString("neck_status"));
                assertEquals(
                        1,
                        resultSet.getInt("started_quantity"));
                assertEquals(
                        "IN_PROGRESS",
                        resultSet.getString("order_status"));
            }
        }
        captureScreenshot("05-database-verified.png");
    }

    private void verifyProductionOrderUpdated() {
        page.navigate(
                BASE_URL
                + "/production-orders/"
                + productionOrderId
                + "/view");
        page.waitForLoadState();

        assertThat(page.locator(
                ".status-badge.status-working"))
                .hasText("製造中");
        assertThat(page.locator("main.page-container"))
                .containsText("計画数に達しています");
        assertThat(page.locator(".guitar-serial-number"))
                .hasCount(1);
        captureScreenshot("06-production-order-updated.png");
    }


    private void executeProcess(
            String processName,
            String expectedCurrentProcess,
            String evidencePrefix) {

        page.navigate(BASE_URL + "/guitars/" + guitarId + "/view");
        page.waitForLoadState();

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("工程開始"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("ギター工程開始"));
        assertThat(page.locator("main.page-container"))
                .containsText(processName);
        page.locator("#workerName")
                .fill("E2E " + processName);
        captureScreenshot(evidencePrefix + "-01-start-form.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("工程開始"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/guitars/" + guitarId + "/view"));
        assertThat(findProcessRow(processName))
                .containsText("実施中");
        assertThat(findProcessRow(processName))
                .containsText("E2E " + processName);
        captureScreenshot(evidencePrefix + "-02-running.png");

        page.navigate(
                BASE_URL
                + "/processes/end/view?guitarId="
                + guitarId);
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("工程終了"));
        assertThat(page.locator("main.page-container"))
                .containsText("E2E " + processName);
        captureScreenshot(evidencePrefix + "-03-end-form.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("工程終了"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/guitars/" + guitarId + "/view"));
        assertThat(findProcessRow(processName))
                .containsText("完了");
        assertThat(page.locator("main.page-container"))
                .containsText(expectedCurrentProcess);
        captureScreenshot(evidencePrefix + "-04-completed.png");
    }

    private com.microsoft.playwright.Locator findProcessRow(
            String processName) {

        com.microsoft.playwright.Locator row =
                page.locator("table.data-table tbody tr")
                        .filter(
                                new com.microsoft.playwright.Locator
                                        .FilterOptions()
                                        .setHasText(processName));
        assertEquals(
                1,
                row.count(),
                processName + "の工程行が一意に見つかりません。");
        return row;
    }

    private void verifyProcessHistory() {
        page.navigate(
                BASE_URL
                + "/guitars/"
                + guitarId
                + "/history");
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("工程履歴"));
        assertThat(page.locator("tbody tr")).hasCount(3);
        assertThat(page.locator("main.page-container"))
                .containsText("ギターパーツ取付");
        assertThat(page.locator("main.page-container"))
                .containsText("調整・調音");
        assertThat(page.locator("main.page-container"))
                .containsText("最終検品");
        captureScreenshot("10-process-history.png");
    }

    private void verifyCompletionInDatabase() throws Exception {
        String sql = """
                SELECT
                    guitar.current_process,
                    production_order.completed_quantity,
                    production_order.status,
                    COUNT(history.id) AS history_count,
                    COUNT(history.end_time) AS completed_history_count
                FROM t_guitar guitar
                JOIN t_production_order production_order
                  ON production_order.id = guitar.production_order_id
                LEFT JOIN t_process_history history
                  ON history.guitar_id = guitar.id
                WHERE guitar.id = ?
                GROUP BY
                    guitar.current_process,
                    production_order.completed_quantity,
                    production_order.status
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, guitarId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("完成", resultSet.getString("current_process"));
                assertEquals(1, resultSet.getInt("completed_quantity"));
                assertEquals("COMPLETED", resultSet.getString("status"));
                assertEquals(3, resultSet.getInt("history_count"));
                assertEquals(3, resultSet.getInt("completed_history_count"));
            }
        }
    }

    private void verifyCompletedProductionOrder() {
        page.navigate(
                BASE_URL
                + "/production-orders/"
                + productionOrderId
                + "/view");
        page.waitForLoadState();

        assertThat(page.locator(".detail-section")
                .first()
                .locator(".status-badge"))
                .hasText("完了");
        assertThat(page.locator("main.page-container"))
                .containsText("この生産計画は完了しています。");
        assertThat(page.locator(".guitar-serial-number"))
                .hasCount(1);
        captureScreenshot("11-production-order-completed.png");
    }

    private void cleanupSafely() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                deleteProcessHistories(connection);
                deleteById(connection, "t_assembly", assemblyId);
                deleteById(connection, "t_guitar", guitarId);
                deleteById(connection, "t_body", bodyId);
                deleteById(connection, "t_neck", neckId);
                deleteById(
                        connection,
                        "t_production_schedule",
                        productionScheduleId);
                deleteById(
                        connection,
                        "t_production_order",
                        productionOrderId);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }


    private void deleteProcessHistories(Connection connection)
            throws Exception {
        if (guitarId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM t_process_history WHERE guitar_id = ?")) {
            statement.setLong(1, guitarId);
            statement.executeUpdate();
        }
    }

    private void deleteById(
            Connection connection,
            String tableName,
            Long id) throws Exception {

        if (id == null) {
            return;
        }

        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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

    private record ProductReference(
            Long productId,
            String productName,
            String color,
            Long bodyMasterId,
            Long neckMasterId) {
    }
}
