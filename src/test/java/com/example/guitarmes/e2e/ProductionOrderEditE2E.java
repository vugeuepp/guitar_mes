package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class ProductionOrderEditE2E extends PlaywrightTestBase {

    private static final String ORDER_NO = "PO260006";

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("production-order-edit");
    }

    @Test
    @DisplayName("未着手の生産計画を編集して証跡を保存できる")
    void editProductionOrderAndCaptureEvidence() {
        page.navigate(BASE_URL + "/production-orders/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("生産計画一覧"));
        captureScreenshot("01-list.png");

        Locator targetRow = page.locator("tbody tr")
                .filter(new Locator.FilterOptions()
                        .setHasText(ORDER_NO));
        assertEquals(
                1,
                targetRow.count(),
                ORDER_NO + "の行が一意に見つかりません。");

        targetRow.getByRole(
                AriaRole.LINK,
                new Locator.GetByRoleOptions()
                        .setName("詳細"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/production-orders/\\d+/view"));
        assertThat(page.locator("main.page-container"))
                .containsText(ORDER_NO);
        captureScreenshot("02-detail-before.png");

        page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("編集"))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/production-orders/\\d+/edit"));

        Locator quantityInput =
                page.locator("input[name='plannedQuantity']");
        Locator startDateInput =
                page.locator("input[name='plannedStartDate']");
        Locator dueDateInput =
                page.locator("input[name='dueDate']");

        assertThat(quantityInput).isVisible();
        assertThat(startDateInput).isVisible();
        assertThat(dueDateInput).isVisible();

        String originalQuantityText = quantityInput.inputValue();
        String originalStartDate = startDateInput.inputValue();
        String originalDueDate = dueDateInput.inputValue();

        assertTrue(
                !originalStartDate.isBlank(),
                "生産開始予定日の初期値が空です。");
        assertTrue(
                !originalDueDate.isBlank(),
                "納期の初期値が空です。");

        int originalQuantity =
                Integer.parseInt(originalQuantityText);
        int updatedQuantity = originalQuantity + 1;

        captureScreenshot("03-edit-before.png");

        quantityInput.fill(String.valueOf(updatedQuantity));
        captureScreenshot("04-edit-input.png");

        try {
            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName(Pattern.compile("保存|更新")))
                    .click();
            page.waitForLoadState();

            assertThat(page).hasURL(
                    Pattern.compile(
                            ".*/production-orders/\\d+/view"));
            assertThat(page.locator("main.page-container"))
                    .containsText(String.valueOf(updatedQuantity));
            captureScreenshot("05-detail-after.png");
        } finally {
            restoreOriginalValues(
                    originalQuantity,
                    originalStartDate,
                    originalDueDate);
        }
    }

    private void restoreOriginalValues(
            int originalQuantity,
            String originalStartDate,
            String originalDueDate) {

        if (page.url().matches(
                ".*/production-orders/\\d+/view")) {
            page.getByRole(
                    AriaRole.LINK,
                    new Page.GetByRoleOptions()
                            .setName("編集"))
                    .click();
            page.waitForLoadState();
        }

        if (!page.url().matches(
                ".*/production-orders/\\d+/edit")) {
            return;
        }

        page.locator("input[name='plannedQuantity']")
                .fill(String.valueOf(originalQuantity));
        page.locator("input[name='plannedStartDate']")
                .fill(originalStartDate);
        page.locator("input[name='dueDate']")
                .fill(originalDueDate);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(Pattern.compile("保存|更新")))
                .click();
        page.waitForLoadState();

        assertThat(page).hasURL(
                Pattern.compile(".*/production-orders/\\d+/view"));
        captureScreenshot("06-detail-restored.png");
    }
}
