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

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class ProductionScheduleCreateE2E extends PlaywrightTestBase {

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
    private LocalDate scheduleDate;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("production-schedule-create");
    }

    @Test
    @DisplayName("日産計画を登録して割当状況とDBを確認できる")
    void createProductionScheduleAndCaptureEvidence()
            throws Exception {

        try {
            prepareProductionOrder();
            openProductionOrderDetail();
            openProductionScheduleForm();
            inputProductionSchedule();
            registerProductionSchedule();
            verifyProductionOrderDetail();
            verifyDatabaseState();
        } finally {
            cleanupSafely();
            verifyCleanup();
        }
    }

    private void prepareProductionOrder() throws Exception {
        Long productId = findProductId();
        String suffix = String.valueOf(System.currentTimeMillis());

        orderNo = "E2E-SCH-" + suffix;

        YearMonth planMonth = YearMonth.now().plusMonths(1);
        scheduleDate = planMonth.atDay(2);
        LocalDate startDate = planMonth.atDay(1);
        LocalDate dueDate = planMonth.atEndOfMonth();

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

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(1, orderNo);
            statement.setLong(2, productId);
            statement.setObject(3, planMonth.atDay(1));
            statement.setObject(4, startDate);
            statement.setObject(5, dueDate);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(
                        resultSet.next(),
                        "E2E用ProductionOrderを作成できませんでした。");

                productionOrderId = resultSet.getLong("id");
            }
        }
    }

    private Long findProductId() throws Exception {
        String sql = """
                SELECT id
                FROM m_product
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                throw new IllegalStateException(
                        "E2Eに使用できるProductがありません。");
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
        assertThat(page.locator("main.page-container"))
                .containsText("日産計画");
        assertThat(page.locator("main.page-container"))
                .containsText("日産計画はまだ登録されていません。");
        assertThat(findProgressCard("割当済数"))
                .containsText("0");
        assertThat(findProgressCard("未割当数"))
                .containsText("10");

        captureScreenshot("01-production-order-detail-before.png");
    }

    private void openProductionScheduleForm() {
        page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("日産計画を登録"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(
                        ".*/production-orders/"
                        + productionOrderId
                        + "/schedules/new"));
        assertThat(page).hasTitle(
                Pattern.compile("日産計画登録"));
        assertThat(page.locator("main.page-container"))
                .containsText(orderNo);
        assertThat(page.locator("main.page-container"))
                .containsText("未割当数");
        assertThat(page.locator("#scheduleDate")).isVisible();
        assertThat(page.locator("#plannedQuantity")).isVisible();

        captureScreenshot("02-production-schedule-form.png");
    }

    private void inputProductionSchedule() {
        page.locator("#scheduleDate")
                .fill(scheduleDate.toString());
        page.locator("#plannedQuantity")
                .fill("4");

        assertThat(page.locator("#scheduleDate"))
                .hasValue(scheduleDate.toString().replace('-', '/'));
        assertThat(page.locator("#plannedQuantity"))
                .hasValue("4");

        captureScreenshot("03-production-schedule-input.png");
    }

    private void registerProductionSchedule() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("日産計画を登録"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(
                        ".*/production-orders/"
                        + productionOrderId
                        + "/view"));
    }

    private void verifyProductionOrderDetail() {
        assertThat(page.locator("main.page-container"))
                .containsText(scheduleDate.toString());
        assertThat(findScheduleRow(scheduleDate.toString()))
                .containsText("4");
        assertThat(findScheduleRow(scheduleDate.toString()))
                .containsText("計画中");
        assertThat(findProgressCard("割当済数"))
                .containsText("4");
        assertThat(findProgressCard("未割当数"))
                .containsText("6");

        captureScreenshot("04-production-order-detail-after.png");
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

    private void verifyDatabaseState() throws Exception {
        String sql = """
                SELECT
                    id,
                    schedule_date,
                    planned_quantity,
                    status
                FROM t_production_schedule
                WHERE production_order_id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setLong(1, productionOrderId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(
                        resultSet.next(),
                        "登録した日産計画をDBで確認できません。");

                productionScheduleId = resultSet.getLong("id");

                assertEquals(
                        scheduleDate,
                        resultSet.getObject(
                                "schedule_date",
                                LocalDate.class));
                assertEquals(
                        4,
                        resultSet.getInt("planned_quantity"));
                assertEquals(
                        "PLANNED",
                        resultSet.getString("status"));
                assertTrue(
                        !resultSet.next(),
                        "日産計画が重複して登録されています。");
            }
        }

        captureScreenshot("05-database-verified.png");
    }

    private void cleanupSafely() throws Exception {
        if (productionOrderId == null) {
            return;
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try {
                deleteProductionSchedule(connection);
                deleteProductionOrder(connection);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void deleteProductionSchedule(
            Connection connection) throws Exception {

        String sql = """
                DELETE FROM t_production_schedule
                WHERE production_order_id = ?
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.setLong(1, productionOrderId);
            statement.executeUpdate();
        }
    }

    private void deleteProductionOrder(
            Connection connection) throws Exception {

        String sql = """
                DELETE FROM t_production_order
                WHERE id = ?
                  AND order_no = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM t_production_schedule
                      WHERE production_order_id =
                            t_production_order.id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM t_guitar
                      WHERE production_order_id =
                            t_production_order.id
                  )
                """;

        try (PreparedStatement statement =
                connection.prepareStatement(sql)) {

            statement.setLong(1, productionOrderId);
            statement.setString(2, orderNo);

            assertEquals(
                    1,
                    statement.executeUpdate(),
                    "E2E用ProductionOrderを削除できませんでした。"
                    + " 手動確認してください: "
                    + orderNo);
        }
    }

    private void verifyCleanup() throws Exception {
        if (orderNo == null) {
            return;
        }

        String sql = """
                SELECT
                    COUNT(*) AS order_count,
                    (
                        SELECT COUNT(*)
                        FROM t_production_schedule
                        WHERE id = ?
                    ) AS schedule_count
                FROM t_production_order
                WHERE order_no = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            if (productionScheduleId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, productionScheduleId);
            }
            statement.setString(2, orderNo);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt("order_count"));
                assertEquals(0, resultSet.getInt("schedule_count"));
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
