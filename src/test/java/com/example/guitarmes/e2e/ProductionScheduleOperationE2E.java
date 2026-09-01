package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class ProductionScheduleOperationE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");

    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");

    private Long productionOrderId;
    private Long productionScheduleId;
    private String orderNo;
    private LocalDate originalDate;
    private LocalDate updatedDate;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("production-schedule-operation");
    }

    @Test
    @DisplayName("日産計画を編集し確定後に取消できる")
    void editConfirmAndCancelProductionSchedule()
            throws Exception {

        try {
            prepareTestData();
            openProductionOrderDetail();
            editProductionSchedule();
            confirmProductionSchedule();
            cancelProductionSchedule();
            verifyDatabaseState();
        } finally {
            cleanupSafely();
            verifyCleanup();
        }
    }

    private void prepareTestData() throws Exception {
        Long productId = findProductId();
        String suffix = String.valueOf(System.currentTimeMillis());
        YearMonth planMonth = YearMonth.now().plusMonths(1);

        orderNo = "E2E-SCH-OP-" + suffix;
        originalDate = planMonth.atDay(3);
        updatedDate = planMonth.atDay(4);

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
                        originalDate);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private Long findProductId() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM m_product ORDER BY id LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                throw new IllegalStateException(
                        "E2Eに使用できるProductがありません。");
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
                ) VALUES (?, ?, 10, 0, 0, ?, ?, ?, 'PLANNED')
                RETURNING id
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.setString(1, orderNo);
            statement.setLong(2, productId);
            statement.setObject(3, planMonth.atDay(1));
            statement.setObject(4, planMonth.atDay(1));
            statement.setObject(5, planMonth.atEndOfMonth());

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
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
                ) VALUES (?, ?, 4, 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.setLong(1, orderId);
            statement.setObject(2, date);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
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
        assertThat(findScheduleRow(originalDate.toString()))
                .containsText("計画中");
        assertThat(findScheduleRow(originalDate.toString())
                .getByRole(
                        AriaRole.LINK,
                        new Locator.GetByRoleOptions()
                                .setName("編集")))
                .isVisible();

        captureScreenshot("01-planned-before-edit.png");
    }

    private void editProductionSchedule() {
        findScheduleRow(originalDate.toString())
                .getByRole(
                        AriaRole.LINK,
                        new Locator.GetByRoleOptions()
                                .setName("編集"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("日産計画編集"));
        assertThat(page.locator("#scheduleDate"))
                .hasValue(originalDate.toString());
        assertThat(page.locator("#plannedQuantity"))
                .hasValue("4");

        captureScreenshot("02-edit-form-before.png");

        page.locator("#scheduleDate")
                .fill(updatedDate.toString());
        page.locator("#plannedQuantity").fill("5");

        captureScreenshot("03-edit-form-input.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("変更内容を保存"))
                .click();
        page.waitForLoadState();

        assertThat(findScheduleRow(updatedDate.toString()))
                .containsText("5");
        assertThat(findProgressCard("割当済数"))
                .containsText("5");
        assertThat(findProgressCard("未割当数"))
                .containsText("5");

        captureScreenshot("04-detail-after-edit.png");
    }

    private void confirmProductionSchedule() {
        acceptNextDialog("この日産計画を確定しますか？");

        findScheduleRow(updatedDate.toString())
                .getByRole(
                        AriaRole.BUTTON,
                        new Locator.GetByRoleOptions()
                                .setName("確定"))
                .click();
        page.waitForLoadState();

        Locator row = findScheduleRow(updatedDate.toString());
        assertThat(row).containsText("確定");
        assertThat(row.getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions()
                        .setName("編集")))
                .hasCount(0);
        assertThat(row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("取消")))
                .isVisible();

        captureScreenshot("05-detail-after-confirm.png");
    }

    private void cancelProductionSchedule() {
        acceptNextDialog("この日産計画を取消しますか？");

        findScheduleRow(updatedDate.toString())
                .getByRole(
                        AriaRole.BUTTON,
                        new Locator.GetByRoleOptions()
                                .setName("取消"))
                .click();
        page.waitForLoadState();

        Locator row = findScheduleRow(updatedDate.toString());
        assertThat(row).containsText("取消");
        assertThat(findProgressCard("割当済数"))
                .containsText("0");
        assertThat(findProgressCard("未割当数"))
                .containsText("10");
        assertThat(row.getByRole(
                AriaRole.BUTTON))
                .hasCount(0);
        assertThat(row.getByRole(
                AriaRole.LINK))
                .hasCount(0);

        captureScreenshot("06-detail-after-cancel.png");
    }

    private void acceptNextDialog(String expectedMessage) {
        page.onceDialog((Dialog dialog) -> {
            assertEquals(expectedMessage, dialog.message());
            dialog.accept();
        });
    }

    private Locator findScheduleRow(String dateText) {
        Locator row = page.locator("table.data-table tbody tr")
                .filter(new Locator.FilterOptions()
                        .setHasText(dateText));

        assertEquals(
                1,
                row.count(),
                dateText + "の日産計画が一意に見つかりません。");

        return row;
    }

    private Locator findProgressCard(String label) {
        Locator card = page.locator(".production-progress-card")
                .filter(new Locator.FilterOptions()
                        .setHasText(label));

        assertEquals(
                1,
                card.count(),
                label + "のカードが一意に見つかりません。");

        return card;
    }

    private void verifyDatabaseState() throws Exception {
        String sql = """
                SELECT schedule_date, planned_quantity, status
                FROM t_production_schedule
                WHERE id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setLong(1, productionScheduleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(
                        updatedDate,
                        resultSet.getObject(
                                "schedule_date",
                                LocalDate.class));
                assertEquals(5, resultSet.getInt("planned_quantity"));
                assertEquals("CANCELLED", resultSet.getString("status"));
            }
        }
    }

    private void cleanupSafely() throws Exception {
        if (productionOrderId == null) {
            return;
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement =
                        connection.prepareStatement(
                                "DELETE FROM t_production_schedule "
                                + "WHERE production_order_id = ?")) {
                    statement.setLong(1, productionOrderId);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement =
                        connection.prepareStatement(
                                "DELETE FROM t_production_order "
                                + "WHERE id = ? AND order_no = ?")) {
                    statement.setLong(1, productionOrderId);
                    statement.setString(2, orderNo);
                    assertEquals(1, statement.executeUpdate());
                }

                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void verifyCleanup() throws Exception {
        if (orderNo == null) {
            return;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM t_production_order "
                     + "WHERE order_no = ?")) {

            statement.setString(1, orderNo);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt(1));
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
