package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

class GuitarPaginationE2E extends PlaywrightTestBase {
    private static final String DB_URL = System.getProperty(
            "e2e.db.url", "jdbc:postgresql://localhost:5432/guitar_mes_e2e");
    private static final String DB_USER = System.getProperty("e2e.db.user", "naokiyamada");
    private static final String DB_PASSWORD = System.getProperty("e2e.db.password", "");
    private static final int FIXTURE_COUNT = 66;

    private final String prefix = "EGP" + UUID.randomUUID().toString()
            .replace("-", "").substring(0, 9);
    private final List<Row> rows = new ArrayList<>();
    private final List<Long> historyIds = new ArrayList<>();
    private Long productionOrderId;
    private long productId;
    private String productName;
    private long firstProcessId;
    private String firstProcessName;
    private long activeCount;
    private long completedCount;

    private record Row(long id, String serial, String category,
            boolean working, LocalDateTime updatedAt, LocalDateTime completedAt) {}

    @Override
    protected Path getEvidenceDirectory() {
        return evidenceDirectory("guitar-pagination");
    }

    @BeforeEach
    void prepare() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                findReferences(c);
                productionOrderId = insertProductionOrder(c);
                for (int i = 0; i < FIXTURE_COUNT; i++) {
                    boolean working = i < 12;
                    LocalDateTime updated = i % 11 == 0 ? null
                            : LocalDateTime.of(2026, 9, 1, 10, 0).plusHours(i % 5);
                    Row row = insertGuitar(c, "A", i, firstProcessName,
                            working, updated, null);
                    rows.add(row);
                    if (working) insertRunningHistory(c, row.id());
                }
                for (int i = 0; i < FIXTURE_COUNT; i++) {
                    LocalDateTime completed = i % 13 == 0 ? null
                            : LocalDateTime.of(2026, 9, 2, 10, 0).plusHours(i % 4);
                    rows.add(insertGuitar(c, "C", i, "完成", false,
                            completed, completed));
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
        try (Connection c = connection()) {
            activeCount = scalar(c, "SELECT count(*) FROM t_guitar "
                    + "WHERE current_process IS NULL OR lower(trim(current_process)) <> lower(trim('完成'))");
            completedCount = scalar(c, "SELECT count(*) FROM t_guitar "
                    + "WHERE lower(trim(current_process)) = lower(trim('完成'))");
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                for (Long id : historyIds) delete(c, "t_process_history", id);
                for (Row row : rows) delete(c, "t_guitar", row.id());
                if (productionOrderId != null) delete(c, "t_production_order", productionOrderId);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
            try (PreparedStatement s = c.prepareStatement(
                    "SELECT count(*) FROM t_guitar WHERE serial_no LIKE ?")) {
                s.setString(1, prefix + "%");
                try (ResultSet r = s.executeQuery()) {
                    assertTrue(r.next());
                    assertEquals(0, r.getLong(1), "fixture Guitarが残存しています");
                }
            }
        }
    }

    @Test
    void categoriesSortPageBoundariesAndCorrections() {
        for (String category : List.of("active", "completed")) {
            List<String> expected = expected(category, null);
            LinkedHashMap<String, String> filters = new LinkedHashMap<>();
            filters.put("category", category);
            filters.put("serial", prefix);
            open(filters);
            assertThat(page.locator(".guitar-search-result strong")).hasText("66");
            assertThat(page.locator("#category-active .category-count"))
                    .hasText(String.valueOf(activeCount));
            assertThat(page.locator("#category-completed .category-count"))
                    .hasText(String.valueOf(completedCount));
            assertThat(page.locator("#page-position")).hasText("1 / 4");
            assertThat(page.locator("#page-previous")).isDisabled();

            List<String> actual = new ArrayList<>();
            for (int number = 0; number < 4; number++) {
                verifyPage(number, expected);
                actual.addAll(serials());
                if (number < 3) click("#page-next");
            }
            assertEquals(expected, actual);
            assertEquals(FIXTURE_COUNT, new HashSet<>(actual).size());
            assertThat(page.locator("#page-next")).isDisabled();
            assertThat(page.locator(".guitar-serial-number")).hasCount(6);
            captureScreenshot(category + "-last-page.png");
            click("#page-previous");
            verifyPage(2, expected);

            filters.put("page", "999");
            open(filters);
            verifyPage(3, expected);
            assertThat(page.locator(".guitar-serial-number")).hasCount(6);

            filters.put("page", "-8");
            open(filters);
            verifyPage(0, expected);
        }

        Row one = rows.stream().filter(r -> r.category().equals("active")).findFirst().orElseThrow();
        open(Map.of("category", "active", "serial", one.serial()));
        assertThat(page.locator(".guitar-search-result strong")).hasText("1");
        assertThat(page.locator(".component-pagination")).hasCount(0);
        open(Map.of("category", "active", "serial", prefix + "-missing"));
        assertThat(page.locator(".guitar-search-result strong")).hasText("0");
        assertThat(page.locator(".component-pagination")).hasCount(0);
        assertThat(page.locator(".empty-state")).containsText("条件に一致する");
    }

    @Test
    void filtersResetsAndSelectionStayOnCurrentPage() {
        LinkedHashMap<String, String> waitingFilters = new LinkedHashMap<>();
        waitingFilters.put("category", "active");
        waitingFilters.put("serial", prefix);
        waitingFilters.put("product", productName);
        waitingFilters.put("currentProcess", firstProcessName);
        waitingFilters.put("status", "WAITING");
        List<String> waiting = expected("active", false);
        assertTrue(waiting.size() > 20);
        open(waitingFilters);
        verifyPage(0, waiting);
        selectAllAndAssertCurrentPage(waiting.subList(0, 20));
        click("#page-next");
        page.goBack(); page.waitForLoadState();
        assertThat(page.locator(".row-checkbox:checked")).hasCount(0);
        click("#page-next");
        verifyPage(1, waiting);
        assertFilters(waitingFilters, 1);
        assertThat(page.locator(".row-checkbox:checked")).hasCount(0);
        assertThat(page.locator("#selected-count")).hasText("0");
        int secondCount = Math.min(20, waiting.size() - 20);
        selectAllAndAssertCurrentPage(waiting.subList(20, 20 + secondCount));
        click("#page-previous");
        assertThat(page.locator(".row-checkbox:checked")).hasCount(0);
        assertThat(page.locator("#selected-count")).hasText("0");
        captureScreenshot("waiting-bulk-page.png");

        open(Map.of("category", "active", "serial", prefix, "status", "WORKING"));
        List<String> working = expected("active", true);
        assertThat(page.locator(".guitar-search-result strong"))
                .hasText(String.valueOf(working.size()));
        assertEquals(working, serials());
        assertThat(page.locator(".status-working")).hasCount(working.size());

        open(Map.of("category", "completed", "serial", prefix));
        assertThat(page.locator("#status")).hasCount(0);
        click("#page-next");
        assertEquals("", query().get("status"));
        assertTrue(serials().stream().allMatch(s -> s.contains(prefix + "C")));

        page.locator("#serial").fill(prefix + "C");
        click(".guitar-search-form button[type=submit]");
        assertEquals("0", query().getOrDefault("page", "0"));
        click("#page-next");
        click("#category-active");
        assertEquals("active", query().get("category"));
        assertFalse(query().containsKey("serial"));
        assertFalse(query().containsKey("page"));

        page.locator("#serial").fill(prefix);
        click(".guitar-search-form button[type=submit]");
        click("#page-next");
        click(".guitar-search-actions a");
        assertEquals("active", query().get("category"));
        assertFalse(query().containsKey("serial"));
        assertFalse(query().containsKey("page"));

        page.locator("#serial").fill(prefix + "-missing");
        click(".guitar-search-form button[type=submit]");
        click(".empty-state a");
        assertEquals("active", query().get("category"));
        assertFalse(query().containsKey("serial"));
        captureScreenshot("resets-complete.png");
    }

    private void selectAllAndAssertCurrentPage(List<String> expectedSerials) {
        page.locator("#processId").selectOption(String.valueOf(firstProcessId));
        page.locator("#select-all").check();
        assertThat(page.locator(".row-checkbox:checked")).hasCount(expectedSerials.size());
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) page.locator("#bulk-start-form").evaluate(
                "form => new FormData(form).getAll('guitarIds')");
        Set<String> expectedIds = rows.stream()
                .filter(r -> expectedSerials.contains(r.serial()))
                .map(r -> String.valueOf(r.id())).collect(Collectors.toSet());
        assertEquals(expectedIds, new HashSet<>(ids));
    }

    private List<String> expected(String category, Boolean working) {
        Comparator<LocalDateTime> newest = Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<Row> comparator = category.equals("completed")
                ? Comparator.comparing(Row::completedAt, newest)
                        .thenComparing(Row::id, Comparator.reverseOrder())
                : Comparator.comparingInt((Row r) -> r.working() ? 0 : 1)
                        .thenComparing(Row::updatedAt, newest)
                        .thenComparing(Row::id, Comparator.reverseOrder());
        return rows.stream().filter(r -> r.category().equals(category))
                .filter(r -> working == null || r.working() == working)
                .sorted(comparator).map(Row::serial).toList();
    }

    private void verifyPage(int number, List<String> expected) {
        for (String serial : serials()) {
            Row fixture = rows.stream().filter(r -> r.serial().equals(serial)).findFirst().orElseThrow();
            var row = page.locator("tbody tr").filter(new Locator.FilterOptions().setHas(page.locator(".guitar-serial-number", new com.microsoft.playwright.Page.LocatorOptions().setHasText(serial))));
            assertThat(row.locator(".list-updated-at")).hasText(com.example.guitarmes.common.DateTimeFormatterUtil.format(fixture.updatedAt()));
            if (fixture.category().equals("completed")) assertThat(row.locator(".list-event-at")).hasText(com.example.guitarmes.common.DateTimeFormatterUtil.format(fixture.completedAt()));
        }

        int pages = (expected.size() + 19) / 20;
        int end = Math.min(expected.size(), number * 20 + 20);
        assertThat(page.locator("#page-position")).hasText((number + 1) + " / " + pages);
        assertThat(page.locator(".guitar-serial-number")).hasCount(end - number * 20);
        assertEquals(expected.subList(number * 20, end), serials());
    }

    private void assertFilters(Map<String, String> filters, int pageNumber) {
        filters.forEach((key, value) -> {
            assertEquals(value, query().get(key));
            String selector = key.equals("category") ? "input[name=category]" : "#" + key;
            assertThat(page.locator(selector)).hasValue(value);
        });
        assertEquals(String.valueOf(pageNumber), query().get("page"));
    }

    private List<String> serials() {
        return page.locator(".guitar-serial-number").allTextContents().stream()
                .map(String::trim).toList();
    }

    private void click(String selector) {
        page.locator(selector).click();
        page.waitForLoadState();
    }

    private void open(Map<String, String> params) {
        String query = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        page.navigate(BASE_URL + "/guitars/view?" + query);
        page.waitForLoadState();
    }

    private Map<String, String> query() {
        String raw = URI.create(page.url()).getRawQuery();
        Map<String, String> result = new HashMap<>();
        if (raw != null) for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length == 1 ? "" : URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    private Connection connection() throws Exception {
        Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("select current_database()")) {
            assertTrue(r.next());
            assertEquals("guitar_mes_e2e", r.getString(1));
        } catch (Throwable e) {
            c.close();
            throw e;
        }
        return c;
    }

    private void findReferences(Connection c) throws Exception {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT id, product_name FROM m_product WHERE product_name IS NOT NULL "
                        + "AND trim(product_name) <> '' ORDER BY id LIMIT 1");
             ResultSet r = s.executeQuery()) {
            assertTrue(r.next(), "E2Eで使用できるProductが必要です");
            productId = r.getLong(1); productName = r.getString(2).trim();
        }
        try (PreparedStatement s = c.prepareStatement(
                "SELECT id, process_name FROM m_process WHERE target_type='GUITAR' "
                        + "ORDER BY process_order LIMIT 2"); ResultSet r = s.executeQuery()) {
            assertTrue(r.next(), "Guitar工程マスタが2件以上必要です");
            firstProcessId = r.getLong(1); firstProcessName = r.getString(2);
            assertTrue(r.next(), "bulk対象のhasNextProcess確保にGuitar工程マスタが2件以上必要です");
        }
    }

    private Long insertProductionOrder(Connection c) throws Exception {
        String sql = "INSERT INTO t_production_order (order_no,product_id,planned_quantity,"
                + "started_quantity,completed_quantity,plan_month,planned_start_date,due_date,status) "
                + "VALUES (?,?,132,132,66,?,?,?,'IN_PROGRESS') RETURNING id";
        LocalDate date = LocalDate.now().plusDays(1);
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, "EGP-" + prefix); s.setLong(2, productId);
            s.setObject(3, date.withDayOfMonth(1)); s.setObject(4, date); s.setObject(5, date.plusDays(7));
            try (ResultSet r = s.executeQuery()) { assertTrue(r.next()); return r.getLong(1); }
        }
    }

    private Row insertGuitar(Connection c, String kind, int index, String process,
            boolean working, LocalDateTime updated, LocalDateTime completed) throws Exception {
        String serial = prefix + kind + String.format(Locale.ROOT, "%03d", index);
        String sql = "INSERT INTO t_guitar (serial_no,current_process,product_id,production_order_id,updated_at,completed_at) "
                + "VALUES (?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, serial); s.setString(2, process); s.setLong(3, productId);
            s.setLong(4, productionOrderId); s.setObject(5, updated); s.setObject(6, completed);
            try (ResultSet r = s.executeQuery()) {
                assertTrue(r.next()); return new Row(r.getLong(1), serial,
                        kind.equals("A") ? "active" : "completed", working, updated, completed);
            }
        }
    }

    private void insertRunningHistory(Connection c, long guitarId) throws Exception {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO t_process_history (guitar_id,process_id,worker_name,start_time) "
                        + "VALUES (?,?,?,CURRENT_TIMESTAMP) RETURNING id")) {
            s.setLong(1, guitarId); s.setLong(2, firstProcessId); s.setString(3, prefix);
            try (ResultSet r = s.executeQuery()) { assertTrue(r.next()); historyIds.add(r.getLong(1)); }
        }
    }

    private long scalar(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            assertTrue(r.next()); return r.getLong(1);
        }
    }

    private void delete(Connection c, String table, Long id) throws Exception {
        try (PreparedStatement s = c.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            s.setLong(1, id); s.executeUpdate();
        }
    }
}
