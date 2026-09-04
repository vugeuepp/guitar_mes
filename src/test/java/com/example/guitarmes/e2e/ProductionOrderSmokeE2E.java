package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;

class ProductionOrderSmokeE2E extends PlaywrightTestBase {

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("production-order-smoke");
    }

    @Test
    @DisplayName("生産計画一覧を表示して証跡を保存できる")
    void showProductionOrderListAndCaptureEvidence() {
        page.navigate(BASE_URL + "/production-orders/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(
                Pattern.compile("生産計画一覧"));
        assertThat(page.getByRole(
                com.microsoft.playwright.options.AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("生産計画を登録")
                        .setExact(true)))
                .isVisible();
        assertThat(page.locator("main.page-container"))
                .containsText("生産計画");

        captureScreenshot("01-production-order-list.png");

        page.getByRole(
                com.microsoft.playwright.options.AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("生産計画を登録")
                        .setExact(true))
                .click();
        page.waitForLoadState();
        String todayIso = LocalDate.now().toString();
        String dueDateIso = LocalDate.now().plusDays(7).toString();
        String today = todayIso.replace('-', '/');
        String dueDate = dueDateIso.replace('-', '/');
        assertThat(page.locator("#plannedStartDate")).hasValue(today);
        assertThat(page.locator("#dueDate")).hasValue(dueDate);
        assertThat(page.locator("#plannedStartDatePicker"))
                .hasAttribute("min", todayIso);
        assertThat(page.locator("#plannedStartDatePicker"))
                .hasAttribute("max", LocalDate.now().plusYears(5).toString());
        assertThat(page.locator("input[name='plannedStartDate']")).hasCount(1);
        assertThat(page.locator("input[name='dueDate']")).hasCount(1);
        assertThat(page.locator("#plannedStartDatePicker"))
                .hasAttribute("min", todayIso);
        assertThat(page.locator("#dueDatePicker"))
                .hasAttribute("min", todayIso);
        verifyDateTextInputBehavior();
        captureScreenshot("02-production-order-form-date-defaults.png");
    }

    private void verifyDateTextInputBehavior() {
        com.microsoft.playwright.Locator startDate =
                page.locator("#plannedStartDate");
        com.microsoft.playwright.Locator dueDate =
                page.locator("#dueDate");

        startDate.fill("20261010");
        startDate.blur();
        assertThat(startDate).hasValue("2026/10/10");

        startDate.fill("2026-10-11");
        startDate.blur();
        assertThat(startDate).hasValue("2026/10/11");

        startDate.fill("2026/10/12");
        startDate.blur();
        assertThat(startDate).hasValue("2026/10/12");

        startDate.fill("202610105555");
        assertThat(startDate).hasValue("2026101055");

        startDate.fill("2026abc10/10");

        String sanitizedValue =
                startDate.inputValue();

        assertTrue(
                sanitizedValue.matches("[0-9/-]*"),
                "日付欄に使用できない文字が残っています: "
                + sanitizedValue);

        assertTrue(
                sanitizedValue.length() <= 10,
                "日付欄が10文字を超えています: "
                + sanitizedValue);

        startDate.fill("2026/02/30");
        startDate.blur();
        assertThat(startDate).hasJSProperty("validationMessage",
                "日付はyyyy/MM/dd形式で正しく入力してください。");

        startDate.fill(LocalDate.now().minusDays(1).toString());
        startDate.blur();
        assertThat(startDate).hasJSProperty(
                "validationMessage",
                "日付が入力可能範囲外です。");

        String validStart = LocalDate.now().plusDays(10).toString();
        startDate.fill(validStart);
        startDate.blur();
        assertThat(startDate).hasValue(validStart.replace('-', '/'));
        assertThat(page.locator("#dueDatePicker"))
                .hasAttribute("min", validStart);

        dueDate.fill(LocalDate.now().plusDays(9).toString());
        dueDate.blur();
        assertThat(dueDate).hasJSProperty(
                "validationMessage",
                "日付が入力可能範囲外です。");

        verifySameCalendarDateCanBeSelectedAfterClear(startDate);

        assertThat(page.locator("#plannedStartDateButton")).isVisible();
        assertThat(page.locator("#dueDateButton")).isVisible();
    }

    private void verifySameCalendarDateCanBeSelectedAfterClear(
            com.microsoft.playwright.Locator startDate) {
        String selectedDate = LocalDate.now().plusDays(20).toString();
        String selectedDateDisplay = selectedDate.replace('-', '/');
        com.microsoft.playwright.Locator picker =
                page.locator("#plannedStartDatePicker");

        picker.evaluate(
                "(element, value) => {"
                + " element.value = value;"
                + " element.dispatchEvent(new Event('change', { bubbles: true }));"
                + "}",
                selectedDate);
        assertThat(startDate).hasValue(selectedDateDisplay);

        startDate.fill("");
        picker.evaluate("element => element.value = ''");
        picker.evaluate(
                "(element, value) => {"
                + " element.value = value;"
                + " element.dispatchEvent(new Event('change', { bubbles: true }));"
                + "}",
                selectedDate);
        assertThat(startDate).hasValue(selectedDateDisplay);
    }
}
