package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

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

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class AssemblyCreateE2E extends PlaywrightTestBase {

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
    private final List<Long> bodyIds = new ArrayList<>();
    private final List<Long> neckIds = new ArrayList<>();
    private final List<Long> guitarIds = new ArrayList<>();
    private final List<Long> assemblyIds = new ArrayList<>();
    private final List<String> bodySerials = new ArrayList<>();
    private final List<String> neckSerials = new ArrayList<>();
    private String orderNo;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("assembly-create");
    }

    @Test
    @DisplayName("2件のネック取付を登録予定から一括登録できる")
    void createAssembliesAndGuitarsInBulk() throws Exception {
        try {
            prepareTestData();
            openProductionOrderDetail();
            openAssemblyForm();
            addFirstPair();
            removeFirstPair();
            addFirstPair();
            addRemainingPairAutomatically();
            enterWorkerAndRegister();
            verifyProductionOrderUpdated();
            verifyDatabaseState();
        } finally {
            cleanupSafely();
        }
    }

    private void prepareTestData() throws Exception {
        ProductReference product = findProductReference();
        String suffix = String.valueOf(System.currentTimeMillis());
        orderNo = "E2E-BULK-ASM-" + suffix;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                productionOrderId = insertProductionOrder(
                        connection, product.productId(), orderNo);
                productionScheduleId = insertProductionSchedule(
                        connection, productionOrderId);

                for (int index = 1; index <= 2; index++) {
                    String bodySerial = "E2EB" + suffix + index;
                    String neckSerial = "E2EN" + suffix + index;
                    bodySerials.add(bodySerial);
                    neckSerials.add(neckSerial);
                    bodyIds.add(insertBody(
                            connection,
                            product,
                            bodySerial,
                            productionOrderId,
                            productionScheduleId));
                    neckIds.add(insertNeck(
                            connection,
                            product,
                            neckSerial,
                            productionOrderId,
                            productionScheduleId));
                }
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
                ) VALUES (?, ?, 2, 0, 0, ?, ?, ?, 'PLANNED')
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
                ) VALUES (?, ?, 2, 'CONFIRMED',
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

        assertThat(page).hasTitle(
                Pattern.compile("生産計画詳細"));

        assertThat(page.locator("main.page-container"))
                .containsText(orderNo);

        assertThat(page.locator(
                ".status-badge.status-planned"))
                .hasText("計画中");

        assertThat(page.locator("main.page-container"))
                .containsText("発行済み");

        assertThat(page.locator("main.page-container"))
                .containsText("ネック取付");

        captureScreenshot(
                "01-production-order-detail.png");
    }

    private void openAssemblyForm() {
        page.navigate(
                BASE_URL
                + "/assemblies/new?productionOrderId="
                + productionOrderId
                + "&productionScheduleId="
                + productionScheduleId);

        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("ネック取付登録"));

        assertThat(page.locator("main.page-container"))
                .containsText(orderNo);

        assertThat(page.locator("main.page-container"))
                .containsText("対象日産計画");

        assertThat(page.locator("#neckId"))
                .containsText(neckSerials.get(0));

        assertThat(page.locator("#bodyId"))
                .containsText(bodySerials.get(0));

        assertThat(page.locator("#auto-pair")).isEnabled();
        assertThat(page.locator("#remaining-quantity")).hasText("2");
        assertThat(page.locator("#remaining-after-count")).hasText("2");
        captureScreenshot("02-assembly-form.png");
    }

    private void addFirstPair() {
        addPair(0);
        assertThat(page.locator("#pair-count")).hasText("1");
        assertThat(page.locator("#queue-body tr")).hasCount(1);
        assertThat(page.locator("#bulk-submit"))
                .hasText("1件を一括登録");
        captureScreenshot("03-first-pair-added.png");
    }

    private void removeFirstPair() {
        page.locator(".remove-pair").first().click();
        assertThat(page.locator("#pair-count")).hasText("0");
        assertThat(page.locator("#queue-body tr")).hasCount(0);
        assertThat(page.locator("#bulk-submit")).isDisabled();
        captureScreenshot("04-first-pair-removed.png");
    }

    private void addRemainingPairAutomatically() {
        assertThat(page.locator("#auto-pair")).isEnabled();
        page.locator("#auto-pair").click();

        assertThat(page.locator("#auto-pair-message"))
                .hasText("1件を自動で組み合わせました。");
        assertThat(page.locator("#pair-count")).hasText("2");
        assertThat(page.locator("#remaining-after-count")).hasText("0");
        assertThat(page.locator("#queue-body tr")).hasCount(2);
        assertThat(page.locator("#queue-body"))
                .containsText(neckSerials.get(0));
        assertThat(page.locator("#queue-body"))
                .containsText(neckSerials.get(1));
        assertThat(page.locator("#queue-body"))
                .containsText(bodySerials.get(0));
        assertThat(page.locator("#queue-body"))
                .containsText(bodySerials.get(1));
        assertThat(page.locator("#bulk-submit"))
                .hasText("2件を一括登録");
        assertThat(page.locator("#auto-pair")).isDisabled();
        captureScreenshot("05-auto-pair-completed.png");
    }
    private void addPair(int index) {
        page.locator("#neckId")
                .selectOption(String.valueOf(neckIds.get(index)));
        page.locator("#bodyId")
                .selectOption(String.valueOf(bodyIds.get(index)));
        page.locator("#add-pair").click();
    }

    private void enterWorkerAndRegister() {
        page.locator("#workerName").fill(WORKER_NAME);
        assertThat(page.locator("#workerName")).hasValue(WORKER_NAME);
        page.locator("#bulk-submit").click();
        page.waitForLoadState();

        String expectedDetailUrl =
                BASE_URL
                + "/production-orders/"
                + productionOrderId
                + "/view";

        assertTrue(
                page.url().startsWith(expectedDetailUrl),
                "生産計画詳細へ遷移していません。"
                + " expectedPrefix="
                + expectedDetailUrl
                + ", actual="
                + page.url());
        assertThat(page.locator("main.page-container"))
                .containsText("2件のネック取付を一括登録しました");
        captureScreenshot("06-bulk-registered.png");
    }

    private void verifyDatabaseState() throws Exception {
        String sql = """
                SELECT
                    assembly.id AS assembly_id,
                    guitar.id AS guitar_id,
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
                WHERE guitar.production_order_id = ?
                ORDER BY assembly.id
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productionOrderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                int count = 0;
                while (resultSet.next()) {
                    assemblyIds.add(resultSet.getLong("assembly_id"));
                    guitarIds.add(resultSet.getLong("guitar_id"));
                    assertEquals("ASSEMBLED", resultSet.getString("body_status"));
                    assertEquals("ASSEMBLED", resultSet.getString("neck_status"));
                    assertEquals(2, resultSet.getInt("started_quantity"));
                    assertEquals("IN_PROGRESS", resultSet.getString("order_status"));
                    count++;
                }
                assertEquals(2, count, "Assemblyが2件生成されていません。");
            }
        }
        captureScreenshot("08-database-verified.png");
    }
    private void verifyProductionOrderUpdated() {
        assertThat(page.locator(".status-badge.status-working"))
                .hasText("製造中");
        assertThat(page.locator("main.page-container"))
                .containsText("計画数に達しています");
        assertThat(page.locator(".guitar-serial-number")).hasCount(2);
        assertEquals(0, page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("ネック取付")
                        .setExact(true)).count());

        page.navigate(
                BASE_URL
                + "/assemblies/new?productionOrderId="
                + productionOrderId
                + "&productionScheduleId="
                + productionScheduleId);
        page.waitForLoadState();
        assertThat(page.locator("main.page-container"))
                .containsText("ネック取付は登録できません");
        assertEquals(
                0,
                page.locator("form[action='/assemblies/bulk/create']").count());
        captureScreenshot("07-production-order-updated.png");
    }
    private void cleanupSafely() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try {
                deleteAssembliesByComponents(connection);
                deleteGuitarsByProductionOrder(connection);

                for (Long bodyId : bodyIds) {
                    deleteById(
                            connection,
                            "t_body",
                            bodyId);
                }

                for (Long neckId : neckIds) {
                    deleteById(
                            connection,
                            "t_neck",
                            neckId);
                }

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
    
    private void deleteAssembliesByComponents(
            Connection connection) throws Exception {

        if (bodyIds.isEmpty() && neckIds.isEmpty()) {
            return;
        }

        for (Long bodyId : bodyIds) {
            String sql = """
                    DELETE FROM t_assembly
                    WHERE body_id = ?
                    """;

            try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {
                statement.setLong(1, bodyId);
                statement.executeUpdate();
            }
        }

        for (Long neckId : neckIds) {
            String sql = """
                    DELETE FROM t_assembly
                    WHERE neck_id = ?
                    """;

            try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {
                statement.setLong(1, neckId);
                statement.executeUpdate();
            }
        }
    }
    
    private void deleteGuitarsByProductionOrder(
            Connection connection) throws Exception {

        if (productionOrderId == null) {
            return;
        }

        String sql = """
                DELETE FROM t_guitar
                WHERE production_order_id = ?
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {
            statement.setLong(
                    1,
                    productionOrderId);
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
