package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.*;

import java.nio.file.Path;
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
    }
}
