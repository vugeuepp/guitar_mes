package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class ProductImageUploadE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL =
            System.getProperty(
                    "e2e.db.url",
                    "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER =
            System.getProperty("e2e.db.user", "naokiyamada");

    private static final String E2E_DB_PASSWORD =
            System.getProperty("e2e.db.password", "");

    private static final byte[] TEST_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC"
            + "AAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    private Long productId;
    private Path temporaryImage;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("product-image");
    }

    @Test
    @DisplayName("製品画像を登録・表示・削除して証跡を保存できる")
    void uploadDisplayAndDeleteProductImage() throws Exception {
        try {
            productId = findProductWithoutImage();
            temporaryImage = createTemporaryImage();

            openProductDetail();
            uploadImage();
            verifyUploadedImage();
            deleteImage();
            verifyDeletedImage();
        } finally {
            cleanupSafely();
        }
    }

    private Long findProductWithoutImage() throws Exception {
        String sql = """
                SELECT id
                FROM m_product
                WHERE image_file_name IS NULL
                   OR image_file_name = ''
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                throw new IllegalStateException(
                        "画像未登録のProductがE2E用DBにありません。");
            }
            return resultSet.getLong("id");
        }
    }

    private Path createTemporaryImage() throws Exception {
        Path directory = Path.of(
                "target",
                "playwright",
                "temp");
        Files.createDirectories(directory);

        Path image = directory.resolve("e2e-product-image.png");
        Files.write(image, TEST_PNG);
        return image.toAbsolutePath();
    }

    private void openProductDetail() {
        page.navigate(BASE_URL + "/products/" + productId + "/view");
        page.waitForLoadState();

        assertThat(page).hasTitle(Pattern.compile("製品詳細"));
        assertThat(page.locator(".product-image-placeholder"))
                .hasText("画像未登録");
        captureScreenshot("01-product-detail-before.png");
    }

    private void uploadImage() {
        page.locator("#imageFile")
                .setInputFiles(temporaryImage);
        assertThat(page.locator("#imageFile"))
                .isVisible();
        captureScreenshot("02-file-selected.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("画像を登録"))
                .click();
        page.waitForLoadState();
    }

    private void verifyUploadedImage() throws Exception {
        assertThat(page).hasURL(
                Pattern.compile(".*/products/" + productId + "/view"));
        assertThat(page.locator("img.product-detail-image"))
                .isVisible();
        assertThat(page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("画像を削除")))
                .isVisible();

        String savedFileName = findImageFileName();
        if (savedFileName == null || savedFileName.isBlank()) {
            throw new AssertionError(
                    "画像登録後もimage_file_nameが空です。");
        }
        captureScreenshot("03-product-image-uploaded.png");
    }

    private void deleteImage() {
        captureScreenshot("04-before-delete.png");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("画像を削除"))
                .click();
        page.waitForLoadState();
    }

    private void verifyDeletedImage() throws Exception {
        assertThat(page).hasURL(
                Pattern.compile(".*/products/" + productId + "/view"));
        assertThat(page.locator(".product-image-placeholder"))
                .hasText("画像未登録");
        assertThat(page.locator("img.product-detail-image"))
                .hasCount(0);
        assertNull(
                findImageFileName(),
                "画像削除後もimage_file_nameが残っています。");
        captureScreenshot("05-product-image-deleted.png");
    }

    private String findImageFileName() throws Exception {
        String sql = """
                SELECT image_file_name
                FROM m_product
                WHERE id = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(
                            "対象Productが存在しません: " + productId);
                }
                return resultSet.getString("image_file_name");
            }
        }
    }

    private void cleanupSafely() throws Exception {
        if (temporaryImage != null) {
            Files.deleteIfExists(temporaryImage);
        }
        if (productId == null) {
            return;
        }

        String fileName = findImageFileName();
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        page.navigate(BASE_URL + "/products/" + productId + "/view");
        page.waitForLoadState();
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("画像を削除"))
                .click();
        page.waitForLoadState();

        assertEquals(
                null,
                findImageFileName(),
                "後処理で製品画像を削除できませんでした。");
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                E2E_DB_URL,
                E2E_DB_USER,
                E2E_DB_PASSWORD);
    }
}
