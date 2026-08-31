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

        page.locator("#plannedQuantity").fill("1");
        page.locator("#plannedStartDate")
                .fill(startDate.toString());
        page.locator("#dueDate")
                .fill(dueDate.toString());

        assertThat(page.locator("#registerButton"))
                .isEnabled();
        captureScreenshot("02-create-input.png");
    }

    private void registerProductionOrder() {
        page.locator("#registerButton").click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/production-orders/view"));

        Locator newestRow = page.locator("tbody tr").first();
        createdOrderNo = newestRow
                .locator(".order-number")
                .textContent()
                .trim();

        assertTrue(
                createdOrderNo.matches("PO\\d{6}"),
                "作成した生産指示番号を取得できませんでした。");

        newestRow.getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions()
                        .setName("詳細"))
                .click();
        page.waitForLoadState();
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
