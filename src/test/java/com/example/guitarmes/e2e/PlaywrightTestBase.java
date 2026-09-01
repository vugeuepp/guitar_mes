package com.example.guitarmes.e2e;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

abstract class PlaywrightTestBase {
    protected static final String BASE_URL =
            System.getProperty(
                    "e2e.base.url",
                    "http://localhost:8080");

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    protected Page page;

    @BeforeEach
    void setUpPlaywright() {
        boolean headless = Boolean.parseBoolean(
                System.getProperty(
                        "playwright.headless",
                        "true"));
        double slowMo = parseSlowMo();

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1440, 1000));
        page = context.newPage();
    }

    @AfterEach
    void tearDownPlaywright(TestInfo testInfo) {
        captureFinalScreenshotSafely(testInfo);
        closeSafely(context);
        closeSafely(browser);
        closeSafely(playwright);
        page = null;
        context = null;
        browser = null;
        playwright = null;
    }

    protected abstract Path getEvidenceDirectory();

    protected Path evidenceDirectory(
            String scenarioName) {
        return Paths.get(
                "target",
                "playwright",
                "evidence",
                scenarioName);
    }

    protected void captureScreenshot(
            String fileName) {
        if (page == null || page.isClosed()) {
            return;
        }
        try {
            Path directory = getEvidenceDirectory();
            Files.createDirectories(directory);
            page.screenshot(
                    new Page.ScreenshotOptions()
                            .setPath(directory.resolve(fileName))
                            .setFullPage(true));
        } catch (Exception exception) {
            System.err.println(
                    "スクリーンショットを保存できませんでした: "
                    + exception.getMessage());
        }
    }

    private double parseSlowMo() {
        String value = System.getProperty(
                "playwright.slowMo",
                "0");
        try {
            double slowMo = Double.parseDouble(value);
            if (slowMo < 0) {
                throw new IllegalArgumentException(
                        "playwright.slowMoは0以上で指定してください。");
            }
            return slowMo;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "playwright.slowMoは数値で指定してください: "
                    + value,
                    exception);
        }
    }

    private void captureFinalScreenshotSafely(
            TestInfo testInfo) {
        if (page == null || page.isClosed()) {
            return;
        }
        String displayName = sanitizeFileName(
                testInfo.getDisplayName());
        captureScreenshot(
                "99-final-" + displayName + ".png");
    }

    private String sanitizeFileName(
            String value) {
        if (value == null || value.isBlank()) {
            return "test";
        }
        String sanitized = value.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_");
        if (sanitized.length() > 80) {
            return sanitized.substring(0, 80);
        }
        return sanitized;
    }

    private void closeSafely(
            AutoCloseable target) {
        if (target == null) {
            return;
        }
        try {
            target.close();
        } catch (Exception exception) {
            System.err.println(
                    "Playwrightリソースを終了できませんでした: "
                    + exception.getMessage());
        }
    }
}
