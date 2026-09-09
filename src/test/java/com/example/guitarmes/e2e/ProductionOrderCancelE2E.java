package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;

class ProductionOrderCancelE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");

    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");

    private String createdOrderNo;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("production-order-cancel");
    }

    @Test
    @DisplayName("生産計画を登録して中止し証跡を保存できる")
    void createAndCancelProductionOrder() throws Exception {
        try {
            openCreateForm();
            selectFirstAvailableProduct();
            inputPlan();
            registerProductionOrder();
            openCreatedOrderDetail();
            cancelProductionOrder();
        } finally {
            deleteCreatedOrderSafely();
        }
    }

    private void openCreateForm() {
        page.navigate(BASE_URL + "/production-orders/new");
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("生産計画登録"));
        assertThat(page.locator("#productionOrderForm"))
                .isVisible();
        captureScreenshot("01-create-form.png");
    }

    private void selectFirstAvailableProduct() {
        Locator seriesSelect = page.locator("#seriesSelect");
        Locator modelSelect = page.locator("#modelSelect");
        Locator colorSelect = page.locator("#colorSelect");
        Locator fingerboardSelect =
                page.locator("#fingerboardSelect");

        seriesSelect.selectOption(
                new SelectOption().setIndex(1));
        modelSelect.selectOption(
                new SelectOption().setIndex(1));
        colorSelect.selectOption(
                new SelectOption().setIndex(1));

        if (!fingerboardSelect.isDisabled()) {
            fingerboardSelect.selectOption(
                    new SelectOption().setIndex(1));
        }

        assertFalse(
                page.locator("#productId").inputValue().isBlank(),
                "Product IDが確定していません。");
        assertThat(page.locator("#selectedProductInfo"))
                .isVisible();
    }

    private void inputPlan() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate dueDate = startDate.plusDays(7);

        page.locator("#planMonth")
                .fill(startDate.toString().substring(0, 7));
        page.locator("#plannedQuantity").fill("1");
        page.locator("#plannedStartDate")
                .fill(startDate.toString());
        page.locator("#dueDate")
                .fill(dueDate.toString());

        assertThat(page.locator("#registerButton"))
                .isEnabled();
        captureScreenshot("02-create-input.png");
    }

    private void registerProductionOrder() throws Exception {
        long productId = Long.parseLong(page.locator("#productId").inputValue());
        LocalDate start = LocalDate.parse(page.locator("#plannedStartDate").inputValue().replace('/', '-'));
        LocalDate due = LocalDate.parse(page.locator("#dueDate").inputValue().replace('/', '-'));
        var before = matchingOrders(productId, start, due);
        page.locator("#registerButton").click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/production-orders/view"));

        var created = matchingOrders(productId, start, due);
        created.keySet().removeAll(before.keySet());
        assertEquals(1, created.size(), "作成した計画を一意に識別できません。取消操作は行いません。");
        createdOrderNo = created.values().iterator().next();
        page.locator("#orderNo").fill(createdOrderNo);
        page.locator(".guitar-search-form button[type=submit]").click();
        page.waitForLoadState();
        Locator createdRow = page.locator("tbody tr")
                .filter(new Locator.FilterOptions().setHasText(createdOrderNo));

        assertTrue(
                createdOrderNo.matches("PO\\d{6}"),
                "作成した生産指示番号を取得できませんでした。");

        createdRow.getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions()
                        .setName("詳細"))
                .click();
        page.waitForLoadState();
    }

    private Map<Long, String> matchingOrders(long productId, LocalDate start, LocalDate due) throws Exception {
        String sql = """
                SELECT id, order_no FROM t_production_order
                WHERE product_id = ? AND planned_quantity = 1
                  AND planned_start_date = ? AND due_date = ?
                  AND plan_month = ? AND status = 'PLANNED'
                """;
        try (Connection connection = DriverManager.getConnection(E2E_DB_URL, E2E_DB_USER, E2E_DB_PASSWORD)) {
            try (var check = connection.createStatement(); var result = check.executeQuery("select current_database()")) {
                assertTrue(result.next());
                assertEquals("guitar_mes_e2e", result.getString(1));
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, productId);
                statement.setObject(2, start);
                statement.setObject(3, due);
                statement.setObject(4, start.withDayOfMonth(1));
                Map<Long, String> rows = new LinkedHashMap<>();
                try (var result = statement.executeQuery()) {
                    while (result.next()) rows.put(result.getLong(1), result.getString(2));
                }
                return rows;
            }
        }
    }

    private void openCreatedOrderDetail() {
        assertThat(page).hasURL(
                Pattern.compile(".*/production-orders/\\d+/view"));
        assertThat(page.locator("main.page-container"))
                .containsText(createdOrderNo);
        assertThat(page.locator(".status-badge"))
                .hasText("計画中");
        captureScreenshot("03-detail-before-cancel.png");
    }

    private void cancelProductionOrder() {
        final boolean[] dialogHandled = {false};

        page.onceDialog((Dialog dialog) -> {
            assertEquals(
                    "この生産計画を中止しますか？",
                    dialog.message());
            dialogHandled[0] = true;
            dialog.accept();
        });

        captureScreenshot("04-before-cancel-confirm.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("中止"))
                .click();
        page.waitForLoadState();

        assertTrue(
                dialogHandled[0],
                "中止確認ダイアログが表示されませんでした。");
        assertThat(page).hasURL(
                Pattern.compile(".*/production-orders/\\d+/view"));
        assertThat(page.locator(".status-badge"))
                .hasText("中止");
        assertThat(page.locator("main.page-container"))
                .containsText("この生産計画は中止されています。");
        assertThat(page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("編集")))
                .hasCount(0);
        captureScreenshot("05-detail-after-cancel.png");
    }

    private void deleteCreatedOrderSafely() throws Exception {
        if (createdOrderNo == null || createdOrderNo.isBlank()) {
            return;
        }

        String sql = """
                DELETE FROM t_production_order
                WHERE order_no = ?
                  AND status = 'CANCELLED'
                  AND started_quantity = 0
                  AND completed_quantity = 0
                  AND NOT EXISTS (
                      SELECT 1
                      FROM t_guitar
                      WHERE production_order_id =
                            t_production_order.id
                  )
                """;

        try (Connection connection = DriverManager.getConnection(
                    E2E_DB_URL,
                    E2E_DB_USER,
                    E2E_DB_PASSWORD);
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(1, createdOrderNo);
            int deletedRows = statement.executeUpdate();
            assertEquals(
                    1,
                    deletedRows,
                    "E2Eで作成した中止済み生産計画を削除できませんでした。"
                    + " 手動確認してください: "
                    + createdOrderNo);
        }
    }
}
