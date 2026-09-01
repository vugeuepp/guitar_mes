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

class ProductionOrderEditE2E extends PlaywrightTestBase {
    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");
    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");
    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");

    private Long productionOrderId;
    private String orderNo;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("production-order-edit");
    }

    @Test
    @DisplayName("未着手の生産計画を編集して証跡を保存できる")
    void editProductionOrderAndCaptureEvidence() throws Exception {
        try {
            prepareProductionOrder();
            openProductionOrderList();
            openProductionOrderDetail();
            editProductionOrder();
            verifyDatabaseState();
        } finally {
            cleanupSafely();
            verifyCleanup();
        }
    }

    private void prepareProductionOrder() throws Exception {
        Long productId = findProductId();
        orderNo = "E2E-ORDER-EDIT-" + System.currentTimeMillis();
        YearMonth planMonth = YearMonth.now().plusMonths(1);
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
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderNo);
            statement.setLong(2, productId);
            statement.setObject(3, planMonth.atDay(1));
            statement.setObject(4, planMonth.atDay(2));
            statement.setObject(5, planMonth.atEndOfMonth());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                productionOrderId = resultSet.getLong("id");
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

    private void openProductionOrderList() {
        page.navigate(BASE_URL + "/production-orders/view");
        page.waitForLoadState();
        assertThat(page).hasTitle(Pattern.compile("生産計画一覧"));
        captureScreenshot("01-list.png");
    }

    private void openProductionOrderDetail() {
        Locator targetRow = page.locator("tbody tr")
                .filter(new Locator.FilterOptions().setHasText(orderNo));
        assertEquals(
                1,
                targetRow.count(),
                orderNo + "の行が一意に見つかりません。");
        targetRow.getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions().setName("詳細"))
                .click();
        page.waitForLoadState();
        assertThat(page).hasURL(Pattern.compile(
                ".*/production-orders/" + productionOrderId + "/view"));
        assertThat(page.locator("main.page-container"))
                .containsText(orderNo);
        captureScreenshot("02-detail-before.png");
    }

    private void editProductionOrder() {
        page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName("編集"))
                .click();
        page.waitForLoadState();

        Locator quantityInput =
                page.locator("input[name='plannedQuantity']");
        assertThat(quantityInput).hasValue("10");
        captureScreenshot("03-edit-before.png");

        quantityInput.fill("11");
        captureScreenshot("04-edit-input.png");
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(Pattern.compile("保存|更新")))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(Pattern.compile(
                ".*/production-orders/" + productionOrderId + "/view"));
        assertThat(page.locator("main.page-container"))
                .containsText("11");
        captureScreenshot("05-detail-after.png");
    }

    private void verifyDatabaseState() throws Exception {
        String sql = """
                SELECT planned_quantity
                FROM t_production_order
                WHERE id = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productionOrderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(11, resultSet.getInt("planned_quantity"));
            }
        }
    }

    private void cleanupSafely() throws Exception {
        if (productionOrderId == null) {
            return;
        }
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM t_production_order "
                     + "WHERE id = ? AND order_no = ?")) {
            statement.setLong(1, productionOrderId);
            statement.setString(2, orderNo);
            statement.executeUpdate();
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
