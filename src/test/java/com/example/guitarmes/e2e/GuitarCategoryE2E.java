package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class GuitarCategoryE2E extends PlaywrightTestBase {

    private static final String E2E_DB_URL = System.getProperty(
            "e2e.db.url",
            "jdbc:postgresql://localhost:5432/guitar_mes_e2e");

    private static final String E2E_DB_USER = System.getProperty(
            "e2e.db.user",
            "naokiyamada");

    private static final String E2E_DB_PASSWORD = System.getProperty(
            "e2e.db.password",
            "");

    private static final String WORKER_NAME = "E2E Bulk Worker";

    private Long productionOrderId;
    private final List<Long> guitarIds = new ArrayList<>();
    private final List<Long> historyIds = new ArrayList<>();
    private String orderNo;
    private String firstSerial;
    private String secondSerial;
    private String otherSerial;
    private String completedSerial;
    private String productName;
    private String productColor;
    private ProcessReference firstProcess;
    private ProcessReference secondProcess;

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("guitar-category");
    }

    private void prepareTestData() throws Exception {
        ProductReference product = findProductReference();
        List<ProcessReference> processes = findGuitarProcesses();
        if (processes.size() < 2) {
            throw new IllegalStateException("Guitar工程マスタが2件以上必要です。");
        }
        firstProcess = processes.get(0);
        secondProcess = processes.get(1);

        String suffix = String.valueOf(System.currentTimeMillis());
        orderNo = "E2E-CAT-" + suffix;
        firstSerial = "E2ECAT-A-" + suffix;
        secondSerial = "E2ECAT-B-" + suffix;
        completedSerial = "E2ECAT-D-" + suffix;
        otherSerial = "E2ECAT-X-" + suffix;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                productionOrderId = insertProductionOrder(
                        connection,
                        product.productId(),
                        orderNo);
                guitarIds.add(insertGuitar(
                        connection,
                        product.productId(),
                        productionOrderId,
                        firstSerial,
                        firstProcess.processName()));
                guitarIds.add(insertGuitar(
                        connection,
                        product.productId(),
                        productionOrderId,
                        secondSerial,
                        firstProcess.processName()));
                guitarIds.add(insertGuitar(
                        connection,
                        product.productId(),
                        productionOrderId,
                        otherSerial,
                        secondProcess.processName()));
                guitarIds.add(insertGuitar(connection, product.productId(), productionOrderId,
                        completedSerial, "完成"));
                try (PreparedStatement history = connection.prepareStatement(
                        "INSERT INTO t_process_history (guitar_id, process_id, worker_name, start_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                    history.setLong(1, guitarIds.get(1)); history.setLong(2, firstProcess.processId());
                    history.setString(3, WORKER_NAME); history.executeUpdate();
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private ProductReference findProductReference() throws Exception {
        String sql = """
                SELECT id, product_name, color
                FROM m_product
                ORDER BY id
                LIMIT 1
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("E2Eで使用できるProductがありません。");
            }
            productName = resultSet.getString("product_name");
            productColor = resultSet.getString("color");
            return new ProductReference(resultSet.getLong("id"));
        }
    }

    private List<ProcessReference> findGuitarProcesses() throws Exception {
        String sql = """
                SELECT id, process_name, process_order
                FROM m_process
                WHERE target_type = 'GUITAR'
                ORDER BY process_order
                """;
        List<ProcessReference> processes = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                processes.add(new ProcessReference(
                        resultSet.getLong("id"),
                        resultSet.getString("process_name"),
                        resultSet.getInt("process_order")));
            }
        }
        return processes;
    }

    private Long insertProductionOrder(
            Connection connection,
            Long productId,
            String testOrderNo) throws Exception {
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
                ) VALUES (?, ?, 2, 2, 0, ?, ?, ?, 'IN_PROGRESS')
                RETURNING id
                """;
        LocalDate startDate = LocalDate.now().plusDays(1);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, testOrderNo);
            statement.setLong(2, productId);
            statement.setObject(3, startDate.withDayOfMonth(1));
            statement.setObject(4, startDate);
            statement.setObject(5, startDate.plusDays(7));
            return executeInsertReturningId(statement);
        }
    }

    private Long insertGuitar(
            Connection connection,
            Long productId,
            Long orderId,
            String serialNo,
            String currentProcess) throws Exception {
        String sql = """
                INSERT INTO t_guitar (
                    serial_no,
                    current_process,
                    product_id,
                    production_order_id
                ) VALUES (?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serialNo);
            statement.setString(2, currentProcess);
            statement.setLong(3, productId);
            statement.setLong(4, orderId);
            return executeInsertReturningId(statement);
        }
    }

    private Long executeInsertReturningId(
            PreparedStatement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("E2EテストデータのIDを取得できませんでした。");
            }
            return resultSet.getLong("id");
        }
    }


    @Test
    void categoriesSearchAndClear() throws Exception {
        try {
            prepareTestData();
            page.navigate(BASE_URL + "/guitars/view");
            assertThat(page.locator("#category-active")).hasAttribute("aria-current", "page");
            filterFixtures();
            assertThat(row(firstSerial)).isVisible();
            assertThat(row(completedSerial)).hasCount(0);
            assertCategoryRows(false);
            long active = categoryCount("active"), completed = categoryCount("completed");
            assertDatabaseCounts(active, completed);
            assertThat(page.locator("#status option")).hasText(new String[]{"すべて", "工程待ち", "作業中"});
            page.locator("#serial").fill(firstSerial);
            page.locator("#product").selectOption(productName);
            page.locator("#currentProcess").selectOption(firstProcess.processName());
            page.locator("#status").selectOption("WAITING"); search();
            assertThat(row(firstSerial)).isVisible(); assertThat(row(secondSerial)).hasCount(0);
            assertThat(page.locator(".guitar-search-result strong")).hasText("1");
            assertThat(page.locator("input[name=category]")).hasValue("active");
            assertEquals(active, categoryCount("active")); assertEquals(completed, categoryCount("completed"));
            page.locator("#serial").fill(secondSerial); page.locator("#status").selectOption("WORKING"); search();
            assertThat(row(secondSerial)).isVisible(); assertThat(row(firstSerial)).hasCount(0);
            page.locator(".guitar-search-actions a").click();
            assertThat(page.locator("input[name=category]")).hasValue("active");
            assertThat(page.locator("#status")).hasValue("");
            filterFixtures();
            assertThat(row(firstSerial)).isVisible(); assertThat(row(completedSerial)).hasCount(0);
            captureScreenshot("01-active.png");
            page.locator("#category-completed").click();
            assertCategoryRows(true);
            filterFixtures();
            assertThat(row(completedSerial)).isVisible(); assertThat(row(firstSerial)).hasCount(0);
            assertThat(page.locator("#category-completed")).hasAttribute("aria-current", "page");
            assertThat(page.locator("#status")).hasCount(0);
            assertThat(page.locator("#bulk-start-form")).hasCount(0);
            assertThat(page.locator("a[href='/processes/end/view']")).hasCount(0);
            assertThat(page.locator(".row-checkbox")).hasCount(0);
            page.locator("#serial").fill(completedSerial); page.locator("#product").selectOption(productName);
            page.locator("#currentProcess").selectOption("完成"); search(); page.reload();
            assertThat(row(completedSerial)).isVisible();
            assertThat(page.locator("input[name=category]")).hasValue("completed");
            assertEquals(completed, categoryCount("completed"));
            assertEquals(active, categoryCount("active"));
            captureScreenshot("02-completed.png");
            page.locator(".guitar-search-actions a").click();
            assertThat(page.locator("input[name=category]")).hasValue("completed");
            for (String field : List.of("serial", "product", "currentProcess"))
                assertThat(page.locator("#" + field)).hasValue("");
            filterFixtures();
            assertThat(row(completedSerial)).isVisible(); assertThat(row(firstSerial)).hasCount(0);
            page.locator("#serial").fill(firstSerial); search();
            assertThat(page.locator(".empty-state")).containsText("条件に一致する");
            page.locator(".empty-state a").click();
            assertThat(page.locator("input[name=category]")).hasValue("completed");
            filterFixtures();
            assertThat(row(completedSerial)).isVisible();
        } finally { cleanup(); }
    }
    private void filterFixtures() {
        page.locator("#serial").fill(firstSerial.substring(firstSerial.lastIndexOf('-') + 1));
        search();
    }

    private Locator row(String serial) {
        return page.locator(".guitar-management-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(serial));
    }
    private void search() { page.locator(".guitar-search-actions button").click(); page.waitForLoadState(); }
    private long categoryCount(String category) {
        return Long.parseLong(page.locator("#category-" + category + " .category-count").innerText());
    }
    private void assertCategoryRows(boolean completed) {
        for (String process : page.locator(".guitar-management-table tbody .process-badge").allTextContents())
            assertEquals(completed, "完成".equals(process.trim()));
    }
    private void assertDatabaseCounts(long active, long completed) throws Exception {
        try (Connection c = openConnection(); PreparedStatement s = c.prepareStatement(
                "SELECT COUNT(*) FILTER (WHERE current_process IS DISTINCT FROM '完成'), COUNT(*) FILTER (WHERE current_process = '完成') FROM t_guitar");
                ResultSet rs = s.executeQuery()) {
            assertTrue(rs.next()); assertEquals(rs.getLong(1), active); assertEquals(rs.getLong(2), completed);
        }
    }
    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(E2E_DB_URL, E2E_DB_USER, E2E_DB_PASSWORD);
    }
    private void cleanup() throws Exception {
        try (Connection c = openConnection()) {
            for (Long id : guitarIds) {
                for (String sql : List.of("DELETE FROM t_process_history WHERE guitar_id = ?", "DELETE FROM t_guitar WHERE id = ?"))
                    try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); s.executeUpdate(); }
            }
            if (productionOrderId != null) try (PreparedStatement s = c.prepareStatement("DELETE FROM t_production_order WHERE id = ?")) {
                s.setLong(1, productionOrderId); s.executeUpdate();
            }
        }
    }
    private record ProductReference(Long productId) {}
    private record ProcessReference(Long processId, String processName, int processOrder) {}
}
