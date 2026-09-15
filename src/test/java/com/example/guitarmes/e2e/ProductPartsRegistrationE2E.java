package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

/** UUID付きマスタを自作。製品はブラウザ登録し、所有するデータのみを後始末する。 */
class ProductPartsRegistrationE2E extends PlaywrightTestBase {
    private String series;
    private String instrument;
    private String family;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("product-parts-registration");
    }

    private Connection connection() throws Exception {
        Connection c = DriverManager.getConnection(
                System.getProperty("e2e.db.url", "jdbc:postgresql://localhost:5432/guitar_mes_e2e"),
                System.getProperty("e2e.db.user", "naokiyamada"), System.getProperty("e2e.db.password", ""));
        try (var statement = c.createStatement(); var rs = statement.executeQuery("select current_database()")) {
            if (!rs.next() || !"guitar_mes_e2e".equals(rs.getString(1))) {
                c.close();
                throw new IllegalStateException("専用E2E DB以外では実行しません。");
            }
        }
        return c;
    }

    @BeforeEach
    void fixtures() throws Exception {
        series = "EP" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        instrument = "I" + series;
        family = series + "-" + instrument;
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (var s = c.prepareStatement("insert into m_product_series (series_code,series_name,active) values (?, ?, true)")) {
                s.setString(1, series); s.setString(2, "Parts E2E " + series); s.executeUpdate();
            }
            try (var s = c.prepareStatement("insert into m_instrument_type (instrument_code,instrument_name,body_type,neck_type,active) values (?, ?, 'Stratocaster','Stratocaster',true)")) {
                s.setString(1, instrument); s.setString(2, "Strat fixture " + instrument); s.executeUpdate();
            }
            c.commit();
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (family == null) return;
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            String productIds = "select id from m_product where internal_model_code=?";
            for (String sql : new String[]{
                    "delete from t_production_order where product_id in (" + productIds + ")",
                    "delete from m_product_parts_spec where product_id in (" + productIds + ")",
                    "delete from m_product where internal_model_code=?",
                    "delete from m_body where product_family_code=?",
                    "delete from m_neck where product_family_code=?"}) {
                try (var s = c.prepareStatement(sql)) { s.setString(1, family); s.executeUpdate(); }
            }
            try (var s = c.prepareStatement("delete from m_product_series where series_code=?")) {
                s.setString(1, series); s.executeUpdate();
            }
            try (var s = c.prepareStatement("delete from m_instrument_type where instrument_code=?")) {
                s.setString(1, instrument); s.executeUpdate();
            }
            c.commit();
        }
    }

    private Locator field(String name) {
        return page.locator("[name='partsSpec." + name + "']");
    }

    private void openNew() {
        page.navigate(BASE_URL + "/products/new");
        assertThat(page.locator("#partsSpecSection")).isVisible();
        // ブラウザ対象も同一E2E DBであることを、自作マスタの表示で確認してからPOSTする。
        assertThat(page.locator("#productSeries option[value='" + series + "']")).hasCount(1);
        page.locator("#productSeries").selectOption(series);
        page.locator("#instrumentType").selectOption(instrument);
        page.locator("#productName").fill("Parts browser fixture");
        page.locator("#pickupLayout").fill("SSS");
        page.locator("#fretCount").selectOption("21");
        page.locator("#scale").selectOption("GUITAR_LONG");
        page.locator("#bodyMaterial").selectOption("ALDER");
        page.locator("#neckMaterial").selectOption("MAPLE");
        variation(0, "A", "Black");
    }

    private void variation(int index, String suffix, String color) {
        Locator card = page.locator("#variationList .variation-card").nth(index);
        card.locator(".variation-model-no").fill(series + "-" + suffix);
        card.locator(".variation-color").fill(color);
        card.locator(".variation-fingerboard").selectOption("ROSEWOOD");
    }

    private void parts() {
        field("bridgeType").selectOption("SIX_POINT");
        field("requiresStudHoleExpansion").selectOption("false");
        field("tunerModel").fill("Tuner " + series);
        field("tunerMountingType").selectOption("PRESS_BUSHING");
        field("tunerBushRequired").selectOption("true");
        field("tunerLayout").selectOption("SIX_IN_LINE");
        field("selectorPositions").fill("5");
        field("controlLayout").fill("1Vol. 2Tone");
        field("jackMountingType").selectOption("BOAT_PLATE");
        field("stringModel").fill("Strings " + series);
        field("stringGauge").fill("09-42");
    }

    private void submit() { page.locator("form button[type='submit']").click(); }
    private void edit(long id) { page.navigate(BASE_URL + "/products/" + id + "/edit"); }

    private long productId(String suffix) throws Exception {
        try (Connection c = connection(); var s = c.prepareStatement("select id from m_product where model_no=? and internal_model_code=?")) {
            s.setString(1, series + "-" + suffix); s.setString(2, family);
            try (var rs = s.executeQuery()) { assertTrue(rs.next()); long id = rs.getLong(1); assertFalse(rs.next()); return id; }
        }
    }

    private long countProducts() throws Exception {
        try (Connection c = connection(); var s = c.prepareStatement("select count(*) from m_product where internal_model_code=?")) {
            s.setString(1, family);
            try (var rs = s.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private void started(long productId) throws Exception {
        try (Connection c = connection(); var s = c.prepareStatement("""
                insert into t_production_order (order_no,product_id,planned_quantity,started_quantity,
                    completed_quantity,plan_month,status) values (?, ?, 1, 1, 0, DATE '2026-09-01','IN_PROGRESS')
                """)) {
            s.setString(1, series + "-ORDER"); s.setLong(2, productId); s.executeUpdate();
        }
    }

    @Test
    void variationsValidationEditAndManufacturingLock() throws Exception {
        openNew();
        page.locator("#addVariationButton").click();
        variation(1, "REMOVE", "Red");
        page.locator("#variationList .variation-card").nth(1).locator(".variation-remove-button").click();
        assertThat(page.locator("#variationList .variation-card")).hasCount(1);
        page.locator("#addVariationButton").click();
        variation(1, "B", "White");
        assertThat(page.locator("#variationList .variation-card")).hasCount(2);
        assertThat(page.locator("#partsSpecSection")).hasCount(1);
        assertThat(page.locator("#variationList [name^='partsSpec.']")).hasCount(0);
        parts();
        field("requiresStudHoleExpansion").selectOption("true");
        submit();
        assertThat(page.locator(".error-message[role='alert']")).isVisible();
        assertThat(page.locator(".error-message[role='alert']")).containsText("スタッド穴拡張");
        assertThat(field("tunerModel")).hasValue("Tuner " + series);
        assertThat(field("requiresStudHoleExpansion")).hasValue("true");
        assertThat(page.locator("#variationList .variation-card")).hasCount(2);
        assertEquals(0, countProducts());
        captureScreenshot("01-validation.png");
        field("requiresStudHoleExpansion").selectOption("false");
        submit();
        assertThat(page).hasURL(Pattern.compile(".*/products/view"));
        assertEquals(2, countProducts());
        long first = productId("A");
        long second = productId("B");
        for (long id : new long[]{first, second}) {
            edit(id);
            assertThat(field("bridgeType")).hasValue("SIX_POINT");
            assertThat(field("requiresStudHoleExpansion")).hasValue("false");
            assertThat(field("tunerModel")).hasValue("Tuner " + series);
            assertThat(field("tunerMountingType")).hasValue("PRESS_BUSHING");
            assertThat(field("tunerBushRequired")).hasValue("true");
            assertThat(field("tunerLayout")).hasValue("SIX_IN_LINE");
            assertThat(field("selectorPositions")).hasValue("5");
            assertThat(field("controlLayout")).hasValue("1Vol. 2Tone");
            assertThat(field("jackMountingType")).hasValue("BOAT_PLATE");
            assertThat(field("stringModel")).hasValue("Strings " + series);
            assertThat(field("stringGauge")).hasValue("09-42");
        }
        edit(first);
        field("stringGauge").fill("10-46");
        submit();
        assertThat(page).hasURL(BASE_URL + "/products/" + first + "/view");
        edit(first);
        assertThat(field("stringGauge")).hasValue("10-46");
        edit(second);
        assertThat(field("stringGauge")).hasValue("09-42");
        started(first);
        edit(first);
        assertThat(field("stringGauge")).isDisabled();
        assertThat(field("bridgeType")).isDisabled();
        assertThat(page.locator("#partsSpecLockedMessage")).containsText("製造開始済み");
        captureScreenshot("02-locked.png");
    }

    @Test
    void emptySpecRegistrationAndFirstSupplementAfterManufacturing() throws Exception {
        openNew();
        assertThat(field("bridgeType")).hasValue("");
        assertThat(field("tunerBushRequired")).hasValue("");
        submit();
        assertThat(page).hasURL(Pattern.compile(".*/products/view"));
        long id = productId("A");
        try (Connection c = connection(); var s = c.prepareStatement("select count(*) from m_product_parts_spec where product_id=?")) {
            s.setLong(1, id);
            try (var rs = s.executeQuery()) { rs.next(); assertEquals(0, rs.getInt(1)); }
        }
        started(id);
        edit(id);
        assertThat(field("stringGauge")).isEnabled();
        assertThat(field("bridgeType")).hasValue("");
        assertThat(page.locator("#partsSpecLockedMessage")).hasCount(0);
        parts();
        submit();
        assertThat(page).hasURL(BASE_URL + "/products/" + id + "/view");
        edit(id);
        assertThat(field("stringGauge")).hasValue("09-42");
        assertThat(field("stringGauge")).isDisabled();
        captureScreenshot("03-first-supplement.png");
    }
}
