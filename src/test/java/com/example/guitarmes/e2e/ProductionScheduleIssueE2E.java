package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

class ProductionScheduleIssueE2E extends PlaywrightTestBase {
    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");
    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");
    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");
    private static final int ISSUE_QUANTITY = 3;

    private Long productionOrderId;
    private Long productionScheduleId;
    private String orderNo;
    private LocalDate scheduleDate;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("production-schedule-issue");
    }

    @Test
    @DisplayName("確定済み日産計画からBodyとNeckを一括発行できる")
    void issueBodyAndNeckFromConfirmedSchedule()
            throws Exception {
        try {
            prepareTestData();
            openProductionOrderDetail();
            issueComponents();
            verifyIssueResultOnScreen();
            verifyDatabaseState();
        } finally {
            cleanupSafely();
            verifyCleanup();
        }
    }

    private void prepareTestData() throws Exception {
        Long productId = findIssuableProductId();
        String suffix = String.valueOf(System.currentTimeMillis());
        YearMonth planMonth = YearMonth.now().plusMonths(1);
        orderNo = "E2E-SCH-ISSUE-" + suffix;
        scheduleDate = planMonth.atDay(5);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                productionOrderId = insertProductionOrder(
                        connection,
                        productId,
                        planMonth);
                productionScheduleId = insertProductionSchedule(
                        connection,
                        productionOrderId,
                        scheduleDate);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private Long findIssuableProductId() throws Exception {
        String sql = """
                SELECT id
                FROM m_product
                WHERE body_master_id IS NOT NULL
                  AND neck_master_id IS NOT NULL
                ORDER BY id
                LIMIT 1
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException(
                        "BodyMasterとNeckMasterが設定された"
                        + "E2E用Productがありません。");
            }
            return resultSet.getLong("id");
        }
    }

    private Long insertProductionOrder(
            Connection connection,
            Long productId,
            YearMonth planMonth) throws Exception {
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
                ) VALUES (?, ?, ?, 0, 0, ?, ?, ?, 'PLANNED')
                RETURNING id
                """;
        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {
            statement.setString(1, orderNo);
            statement.setLong(2, productId);
            statement.setInt(3, ISSUE_QUANTITY);
            statement.setObject(4, planMonth.atDay(1));
            statement.setObject(5, planMonth.atDay(1));
            statement.setObject(6, planMonth.atEndOfMonth());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(
                        resultSet.next(),
                        "E2E用ProductionOrderを作成できませんでした。");
                return resultSet.getLong("id");
            }
        }
    }

    private Long insertProductionSchedule(
            Connection connection,
            Long orderId,
            LocalDate date) throws Exception {
        String sql = """
                INSERT INTO t_production_schedule (
                    production_order_id,
                    schedule_date,
                    planned_quantity,
                    status,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, 'CONFIRMED',
                          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """;
        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setObject(2, date);
            statement.setInt(3, ISSUE_QUANTITY);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(
                        resultSet.next(),
                        "E2E用ProductionScheduleを作成できませんでした。");
                return resultSet.getLong("id");
            }
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

        Locator row = findScheduleRow();
        assertThat(row).containsText("確定");
        assertThat(row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("部品発行")))
                .isVisible();
        captureScreenshot("01-confirmed-before-issue.png");
    }

    private void issueComponents() {
        page.onceDialog((Dialog dialog) -> {
            assertEquals(
                    "この日産計画のBodyとNeckを一括発行しますか？",
                    dialog.message());
            dialog.accept();
        });

        findScheduleRow()
                .getByRole(
                        AriaRole.BUTTON,
                        new Locator.GetByRoleOptions()
                                .setName("部品発行"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(
                        ".*/production-orders/"
                        + productionOrderId
                        + "/view"));
        captureScreenshot("02-detail-after-issue.png");
    }

    private void verifyIssueResultOnScreen() {
        Locator row = findScheduleRow();
        assertThat(row).containsText("確定");
        assertThat(row).containsText("3 / 3台");
        assertThat(row).containsText("発行済み");
        assertThat(row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("部品発行")))
                .hasCount(0);
        assertThat(row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("取消")))
                .hasCount(0);
        assertThat(page.locator("main.page-container"))
                .containsText("BodyとNeckを一括発行しました。");
        captureScreenshot("03-issued-state-visible.png");
    }

    private Locator findScheduleRow() {
        Locator row = page.locator("table.data-table tbody tr")
                .filter(new Locator.FilterOptions()
                        .setHasText(scheduleDate.toString()));
        assertEquals(
                1,
                row.count(),
                scheduleDate + "の日産計画が一意に見つかりません。");
        return row;
    }

    private void verifyDatabaseState() throws Exception {
        String sql = """
                SELECT
                    (SELECT COUNT(*)
                     FROM t_body
                     WHERE production_schedule_id = ?
                       AND production_order_id = ?) AS body_count,
                    (SELECT COUNT(*)
                     FROM t_neck
                     WHERE production_schedule_id = ?
                       AND production_order_id = ?) AS neck_count,
                    (SELECT COUNT(*)
                     FROM t_body
                     WHERE production_schedule_id = ?
                       AND (production_order_id IS NULL
                            OR production_order_id <> ?))
                        AS invalid_body_count,
                    (SELECT COUNT(*)
                     FROM t_neck
                     WHERE production_schedule_id = ?
                       AND (production_order_id IS NULL
                            OR production_order_id <> ?))
                        AS invalid_neck_count
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {
            statement.setLong(1, productionScheduleId);
            statement.setLong(2, productionOrderId);
            statement.setLong(3, productionScheduleId);
            statement.setLong(4, productionOrderId);
            statement.setLong(5, productionScheduleId);
            statement.setLong(6, productionOrderId);
            statement.setLong(7, productionScheduleId);
            statement.setLong(8, productionOrderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(
                        ISSUE_QUANTITY,
                        resultSet.getInt("body_count"));
                assertEquals(
                        ISSUE_QUANTITY,
                        resultSet.getInt("neck_count"));
                assertEquals(
                        0,
                        resultSet.getInt("invalid_body_count"));
                assertEquals(
                        0,
                        resultSet.getInt("invalid_neck_count"));
            }
        }
        captureScreenshot("04-database-verified.png");
    }

    private void cleanupSafely() throws Exception {
        if (productionOrderId == null) {
            return;
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                deleteByProductionOrder(
                        connection,
                        "DELETE FROM t_body WHERE production_order_id = ?");
                deleteByProductionOrder(
                        connection,
                        "DELETE FROM t_neck WHERE production_order_id = ?");
                deleteByProductionOrder(
                        connection,
                        "DELETE FROM t_production_schedule "
                        + "WHERE production_order_id = ?");

                try (PreparedStatement statement =
                        connection.prepareStatement(
                                "DELETE FROM t_production_order "
                                + "WHERE id = ? AND order_no = ?")) {
                    statement.setLong(1, productionOrderId);
                    statement.setString(2, orderNo);
                    assertEquals(
                            1,
                            statement.executeUpdate(),
                            "E2E用ProductionOrderを削除できませんでした。");
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void deleteByProductionOrder(
            Connection connection,
            String sql) throws Exception {
        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {
            statement.setLong(1, productionOrderId);
            statement.executeUpdate();
        }
    }

    private void verifyCleanup() throws Exception {
        if (orderNo == null) {
            return;
        }
        String sql = """
                SELECT
                    (SELECT COUNT(*)
                     FROM t_production_order
                     WHERE order_no = ?) AS order_count,
                    (SELECT COUNT(*)
                     FROM t_production_schedule
                     WHERE id = ?) AS schedule_count,
                    (SELECT COUNT(*)
                     FROM t_body
                     WHERE production_order_id = ?) AS body_count,
                    (SELECT COUNT(*)
                     FROM t_neck
                     WHERE production_order_id = ?) AS neck_count
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {
            statement.setString(1, orderNo);
            statement.setLong(2, productionScheduleId);
            statement.setLong(3, productionOrderId);
            statement.setLong(4, productionOrderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt("order_count"));
                assertEquals(0, resultSet.getInt("schedule_count"));
                assertEquals(0, resultSet.getInt("body_count"));
                assertEquals(0, resultSet.getInt("neck_count"));
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
