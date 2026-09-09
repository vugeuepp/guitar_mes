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

class ProductionOrderPaginationE2E extends PlaywrightTestBase {
    private final String prefix = "E2E" + UUID.randomUUID().toString().substring(0, 8);
    private final String productName = prefix + " Guitar & + % _ !";
    private final List<Fixture> fixtures = new ArrayList<>();
    private final Map<String, Long> categoryCounts = new HashMap<>();
    private Long productId;

    private record Fixture(long id, String number, String category, String status,
            LocalDate due, LocalDateTime updated, LocalDateTime completed) {}

    @Override protected Path getEvidenceDirectory() { return evidenceDirectory("production-order-pagination"); }

    private Connection connection() throws SQLException {
        Connection c = DriverManager.getConnection(
                System.getProperty("e2e.db.url", "jdbc:postgresql://localhost:5432/guitar_mes_e2e"),
                System.getProperty("e2e.db.user", "naokiyamada"), System.getProperty("e2e.db.password", ""));
        try (var s = c.createStatement(); var r = s.executeQuery("select current_database()")) {
            assertTrue(r.next());
            assertEquals("guitar_mes_e2e", r.getString(1), "専用E2E DB以外では実行しません。");
        } catch (Throwable failure) {
            c.close();
            throw failure;
        }
        return c;
    }

    @BeforeEach
    void prepare() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (var s = c.prepareStatement("INSERT INTO m_product (model_no, product_name, color, fingerboard_material) "
                    + "VALUES (?, ?, 'E2E', 'Rosewood') RETURNING id")) {
                s.setString(1, prefix + "-MODEL"); s.setString(2, productName);
                try (var r = s.executeQuery()) { assertTrue(r.next()); productId = r.getLong(1); }
            }
            for (String category : List.of("active", "completed", "cancelled")) {
                for (int i = 0; i < (category.equals("active") ? 27 : 21); i++) {
                    String categoryCode = switch (category) { case "active" -> "A"; case "completed" -> "C"; default -> "X"; };
                    String number = prefix + categoryCode + String.format(Locale.ROOT, "%02d", i);
                    String status = switch (category) {
                        case "completed" -> "COMPLETED";
                        case "cancelled" -> "CANCELLED";
                        default -> i == 0 ? "IN_PROGRESS" : "PLANNED";
                    };
                    LocalDate due = i >= 25 ? null : LocalDate.of(2026, 9, 10).plusDays(i % 3);
                    LocalDateTime updated = i % 4 == 0 ? null : LocalDateTime.of(2026, 9, 1, 10, 0).plusHours(i % 2);
                    LocalDateTime completed = category.equals("completed") ? updated : null;
                    try (var s = c.prepareStatement("""
                            INSERT INTO t_production_order (order_no, product_id, planned_quantity,
                            started_quantity, completed_quantity, plan_month, planned_start_date,
                            due_date, status, updated_at, completed_at)
                            VALUES (?, ?, 1, 0, 0, DATE '2026-09-01', DATE '2026-09-01', ?, ?, ?, ?) RETURNING id
                            """)) {
                        s.setString(1, number); s.setLong(2, productId); s.setObject(3, due);
                        s.setString(4, status); s.setObject(5, updated); s.setObject(6, completed);
                        try (var r = s.executeQuery()) {
                            assertTrue(r.next());
                            fixtures.add(new Fixture(r.getLong(1), number, category, status, due, updated, completed));
                        }
                    }
                }
            }
            c.commit();
            try (var s = c.createStatement(); var r = s.executeQuery("""
                    SELECT count(*) FILTER (WHERE lower(trim(status)) IN ('planned', 'in_progress')) AS active,
                    count(*) FILTER (WHERE lower(trim(status)) = 'completed') AS completed,
                    count(*) FILTER (WHERE lower(trim(status)) = 'cancelled') AS cancelled FROM t_production_order
                    """)) {
                assertTrue(r.next());
                for (String category : List.of("active", "completed", "cancelled"))
                    categoryCounts.put(category, r.getLong(category));
            }
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (productId == null) return;
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            for (Fixture fixture : fixtures) {
                try (var s = c.prepareStatement("DELETE FROM t_production_order WHERE id=? AND order_no=? AND product_id=?")) {
                    s.setLong(1, fixture.id()); s.setString(2, fixture.number()); s.setLong(3, productId);
                    s.executeUpdate();
                }
            }
            try (var s = c.prepareStatement("DELETE FROM m_product WHERE id=? AND model_no=?")) {
                s.setLong(1, productId); s.setString(2, prefix + "-MODEL"); s.executeUpdate();
            }
            c.commit();
            try (var s = c.prepareStatement("SELECT count(*) FROM t_production_order WHERE product_id=?")) {
                s.setLong(1, productId);
                try (var r = s.executeQuery()) { assertTrue(r.next()); assertEquals(0, r.getLong(1)); }
            }
        }
    }

    @Test
    void pagesPreserveCategoryTotalsAndBusinessSortWithoutDuplicatesOrMissingRows() {
        for (String category : List.of("active", "completed", "cancelled")) {
            Map<String, String> conditions = new LinkedHashMap<>();
            conditions.put("orderNo", prefix);
            if (!category.equals("active")) conditions.put("category", category);
            open(conditions); // activeはcategory未指定も検証。
            List<String> expected = ordered(category).stream().map(Fixture::number).toList();
            assertPage(category, 1, expected.subList(0, 20), expected.size());
            assertThat(page.locator("#production-order-previous")).isDisabled();
            var all = new ArrayList<>(numbers());
            click("#production-order-next");
            assertPage(category, 2, expected.subList(20, expected.size()), expected.size());
            assertEquals(category, query().get("category"));
            assertEquals(prefix, query().get("orderNo"));
            assertThat(page.locator("#production-order-next")).isDisabled();
            all.addAll(numbers());
            assertEquals(expected, all);
            assertEquals(expected.size(), new HashSet<>(all).size());
            captureScreenshot(category + "-last-page.png");
            click("#production-order-previous");
            assertPage(category, 1, expected.subList(0, 20), expected.size());
            conditions.put("page", "999"); open(conditions);
            assertPage(category, 2, expected.subList(20, expected.size()), expected.size());
            conditions.put("page", "-1"); open(conditions);
            assertPage(category, 1, expected.subList(0, 20), expected.size());
        }
    }

    @Test
    void allFiltersSurviveNavigationAndSearchTabClearResetPage() {
        Map<String, String> filters = new LinkedHashMap<>(Map.of(
                "category", "active", "orderNo", " " + prefix + " ", "product", productName,
                "status", "PLANNED", "planMonth", "2026-09", "dueFrom", "2026-09-10", "dueTo", "2026-09-12"));
        List<String> expected = ordered("active").stream()
                .filter(f -> f.status().equals("PLANNED") && f.due() != null).map(Fixture::number).toList();
        open(filters);
        assertPage("active", 1, expected.subList(0, 20), expected.size());
        click("#production-order-next");
        assertFilters(filters, 1);
        assertPage("active", 2, expected.subList(20, expected.size()), expected.size());
        page.reload(); assertFilters(filters, 1);
        click("#production-order-previous"); assertFilters(filters, 0);
        click("#production-order-next");
        page.locator("#product").fill(prefix); // 検索を更新しても同じ対象を返す条件。
        click(".guitar-search-form button[type=submit]");
        assertReset();
        assertPage("active", 1, expected.subList(0, 20), expected.size());
        click("#production-order-next");
        click("#category-completed");
        assertReset(); assertCleared("completed"); assertCounts();
        page.locator("#orderNo").fill(prefix);
        click(".guitar-search-form button[type=submit]");
        click("#production-order-next");
        click(".guitar-search-actions a");
        assertReset(); assertCleared("completed"); assertCounts();

        // 自分の1件・0件だけを検索し、既存DB全体の件数に依存せずページャー非表示を検証。
        page.locator("#orderNo").fill(ordered("completed").get(0).number());
        click(".guitar-search-form button[type=submit]");
        assertThat(page.locator(".production-order-pagination")).hasCount(0);
        assertThat(page.locator(".guitar-search-result strong")).hasText("1");
        page.locator("#orderNo").fill(prefix + "-missing");
        click(".guitar-search-form button[type=submit]");
        assertThat(page.locator(".production-order-pagination")).hasCount(0);
        assertThat(page.locator(".guitar-search-result strong")).hasText("0");
        assertThat(page.locator(".empty-state")).containsText("条件に一致する生産計画はありません。");
        click(".empty-state a"); assertReset(); assertCleared("completed");
        page.locator("#dueFrom").fill("bad");
        click(".guitar-search-form button[type=submit]");
        assertThat(page.locator(".production-order-pagination")).hasCount(0);
        assertThat(page.locator("[role=alert]")).containsText("正しく入力"); assertCounts();
    }

    private List<Fixture> ordered(String category) {
        Comparator<LocalDateTime> times = Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<Fixture> sort = switch (category) {
            case "completed" -> Comparator.comparing(Fixture::completed, times);
            case "cancelled" -> Comparator.comparing(Fixture::updated, times);
            default -> Comparator.comparing(Fixture::due, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Fixture::updated, times);
        };
        return fixtures.stream().filter(f -> f.category().equals(category))
                .sorted(sort.thenComparing(Fixture::id, Comparator.reverseOrder())).toList();
    }

    private List<String> numbers() {
        return page.locator(".production-order-table .order-number").allTextContents().stream().map(String::trim).toList();
    }
    private void assertPage(String category, int displayPage, List<String> expected, int total) {
        for (String number : numbers()) {
            Fixture fixture = fixtures.stream().filter(f -> f.number().equals(number)).findFirst().orElseThrow();
            var row = page.locator("tbody tr").filter(new com.microsoft.playwright.Locator.FilterOptions().setHas(page.locator(".order-number", new com.microsoft.playwright.Page.LocatorOptions().setHasText(number))));
            assertThat(row.locator(".list-updated-at")).hasText(com.example.guitarmes.common.DateTimeFormatterUtil.format(fixture.updated()));
            if (category.equals("completed")) assertThat(row.locator(".list-event-at")).hasText(com.example.guitarmes.common.DateTimeFormatterUtil.format(fixture.completed()));
        }

        assertThat(page.locator("#production-order-page-position")).hasText(displayPage + " / 2");
        assertThat(page.locator(".production-order-table tbody tr")).hasCount(expected.size());
        assertEquals(expected, numbers());
        assertThat(page.locator(".guitar-search-result strong")).hasText(String.valueOf(total));
        assertThat(page.locator("input[name=category]")).hasValue(category);
        assertCounts();
    }
    private void assertCounts() {
        categoryCounts.forEach((category, count) -> assertThat(page.locator("#category-" + category + " .category-count"))
                .hasText(String.valueOf(count)));
    }
    private void assertFilters(Map<String, String> filters, int expectedPage) {
        filters.forEach((key, value) -> {
            assertEquals(value, query().get(key));
            assertThat(page.locator(key.equals("category") ? "input[name=category]" : "#" + key)).hasValue(value);
        });
        assertEquals(String.valueOf(expectedPage), query().get("page"));
    }
    private void assertReset() { assertEquals("0", query().getOrDefault("page", "0")); }
    private void assertCleared(String category) {
        assertThat(page.locator("input[name=category]")).hasValue(category);
        for (String field : List.of("orderNo", "product", "status", "planMonth", "dueFrom", "dueTo"))
            assertThat(page.locator("#" + field)).hasValue("");
    }
    private void click(String selector) { page.locator(selector).click(); page.waitForLoadState(); }
    private void open(Map<String, String> conditions) {
        page.navigate(BASE_URL + "/production-orders/view?" + conditions.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&")));
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private Map<String, String> query() {
        String raw = URI.create(page.url()).getRawQuery();
        Map<String, String> values = new HashMap<>();
        if (raw != null) for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    parts.length < 2 ? "" : URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }
}
