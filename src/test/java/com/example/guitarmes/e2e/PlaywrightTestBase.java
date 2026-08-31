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
                    "e2e.baseUrl",
                    "http://localhost:8080");

    private static final Path EVIDENCE_ROOT =
            Paths.get(
                    "target",
                    "playwright",
                    "evidence");

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    void startBrowser() throws Exception {
        Files.createDirectories(getEvidenceDirectory());

        boolean headless = Boolean.parseBoolean(
                System.getProperty("e2e.headless", "true"));

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(headless ? 0.0 : 200.0));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1440, 900));
        page = context.newPage();
    }

    @AfterEach
    void stopBrowser(TestInfo testInfo) {
        try {
            if (page != null) {
                captureScreenshot(
                        "99-final-"
                        + sanitize(testInfo.getDisplayName())
                        + ".png");
            }
        } finally {
            if (context != null) {
                context.close();
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        }
    }

    protected void captureScreenshot(String fileName) {
        page.screenshot(
                new Page.ScreenshotOptions()
                        .setPath(
                                getEvidenceDirectory()
                                        .resolve(fileName))
                        .setFullPage(true));
    }

    protected abstract Path getEvidenceDirectory();

    protected Path evidenceDirectory(String scenarioName) {
        return EVIDENCE_ROOT.resolve(scenarioName);
    }

    private String sanitize(String value) {
        return value.replaceAll(
                "[^a-zA-Z0-9_-]",
                "_");
    }
}
