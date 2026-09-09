package com.example.guitarmes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;

/** Body/Neckページングだけに使用するfixtureと共通検証。 */
abstract class ComponentPaginationE2ESupport extends PlaywrightTestBase {
    protected abstract String kind();
    private String path() { return kind().equals("body") ? "bodies" : "necks"; }
    private final String prefix = "EP" + UUID.randomUUID().toString().substring(0, 8);
    private final String model = prefix + " Model & + % _ !";
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Long> counts = new HashMap<>();
    private String process;
    private long processId;
    private record Row(long id, String serial, String category, String state, LocalDateTime updated, LocalDateTime available) {}
    @Override protected Path getEvidenceDirectory() { return evidenceDirectory(kind() + "-pagination"); }

    private List<String> states(String category) {
        return switch (category) {
            case "passed" -> List.of("AVAILABLE", "ASSEMBLED", "REJECTED");
            case "attention" -> kind().equals("body") ? List.of("REWORK", "RETURNED") : List.of("RETURNED");
            default -> kind().equals("body") ? List.of("WORKING", "WAITING", "WAITING_INSPECTION") : List.of("WORKING", "WAITING");
        };
    }
    private Connection connection() throws SQLException {
        Connection c = DriverManager.getConnection(System.getProperty("e2e.db.url", "jdbc:postgresql://localhost:5432/guitar_mes_e2e"),
                System.getProperty("e2e.db.user", "naokiyamada"), System.getProperty("e2e.db.password", ""));
        try (var s = c.createStatement(); var r = s.executeQuery("select current_database()")) {
            assertTrue(r.next()); assertEquals("guitar_mes_e2e", r.getString(1));
        } catch (Throwable e) { c.close(); throw e; }
        return c;
    }
    @BeforeEach void preparePaginationData() throws Exception {
        assertTrue(List.of("body", "neck").contains(kind()));
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (var s = c.prepareStatement("SELECT id, process_name FROM m_process WHERE target_type=? ORDER BY id LIMIT 1")) {
                s.setString(1, kind().toUpperCase(Locale.ROOT));
                try (var r = s.executeQuery()) { assertTrue(r.next(), "対象工程マスタが必要です"); processId = r.getLong(1); process = r.getString(2); }
            }
            for (String category : List.of("active", "attention", "passed")) {
                var states = states(category);
                for (int i = 0; i < 66; i++) {
                    String serial = prefix + (category.equals("active") ? "A" : category.equals("attention") ? "T" : "P") + String.format(Locale.ROOT, "%02d", i);
                    String state = states.get(i % states.size());
                    LocalDateTime time = i % 4 == 0 ? null : LocalDateTime.of(2026, 9, 1, 10, 0).plusHours(i % 3);
                    try (var s = c.prepareStatement("INSERT INTO t_" + kind()
                            + " (serial_no, model_name, current_process, status, updated_at, available_at) VALUES (?, ?, ?, ?, ?, ?) RETURNING id")) {
                        s.setString(1, serial); s.setString(2, model); s.setString(3, process); s.setString(4, state);
                        s.setObject(5, time); s.setObject(6, time);
                        try (var r = s.executeQuery()) { assertTrue(r.next()); rows.add(new Row(r.getLong(1), serial, category, state, time, time)); }
                    }
                }
            }
            c.commit();
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT upper(trim(status)), count(*) FROM t_" + kind() + " GROUP BY upper(trim(status))")) {
                Map<String, Long> byStatus = new HashMap<>();
                while (r.next()) byStatus.put(r.getString(1), r.getLong(2));
                for (String category : List.of("active", "attention", "passed"))
                    counts.put(category, states(category).stream().mapToLong(state -> byStatus.getOrDefault(state, 0L)).sum());
            }
        }
    }
    @AfterEach void cleanupPaginationData() throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            for (Row row : rows) try (var s = c.prepareStatement("DELETE FROM t_" + kind() + " WHERE id=? AND serial_no=?")) {
                s.setLong(1, row.id()); s.setString(2, row.serial()); s.executeUpdate();
            }
            c.commit();
            try (var s = c.prepareStatement("SELECT count(*) FROM t_" + kind() + " WHERE model_name=?")) {
                s.setString(1, model);
                try (var r = s.executeQuery()) { assertTrue(r.next()); assertEquals(0, r.getLong(1)); }
            }
        }
    }

    @Test void categoriesSortPageBoundariesAndCorrections() {
        for (String category : List.of("active", "attention", "passed")) {
            List<String> expected = ordered(category).stream().map(Row::serial).toList();
            var filters = new LinkedHashMap<>(Map.of("category", category, "serial", prefix));
            open(filters);
            assertThat(page.locator("#page-previous")).isDisabled();
            List<String> all = new ArrayList<>();
            int pages = (expected.size() + 19) / 20;
            for (int n = 0; n < pages; n++) {
                verifyPage(category, n, expected);
                verifyBulk(category);
                all.addAll(serials());
                if (n + 1 < pages) click("#page-next");
            }
            assertEquals(expected, all);
            assertEquals(expected.size(), new HashSet<>(all).size());
            assertThat(page.locator("#page-next")).isDisabled();
            captureScreenshot(category + "-last.png");
            click("#page-previous"); verifyPage(category, pages - 2, expected);
            filters.put("page", "999"); open(filters); verifyPage(category, pages - 1, expected);
            filters.put("page", "-8"); open(filters); verifyPage(category, 0, expected);
        }
    }

    @Test void filterRetentionResetsAndSelectionStaysOnCurrentPage() {
        var filters = new LinkedHashMap<>(Map.of("category", "active", "serial", " " + prefix + " ",
                "modelName", model, "currentProcess", process, "status", "WAITING"));
        List<String> expected = ordered("active").stream().filter(r -> r.state().equals("WAITING")).map(Row::serial).toList();
        open(filters); verifyPage("active", 0, expected);
        page.locator("#processId").selectOption(String.valueOf(processId));
        page.locator("#select-all").check();
        assertThat(page.locator(".row-checkbox:checked")).hasCount(20);
        page.evaluate("window.dispatchEvent(new PageTransitionEvent('pageshow', {persisted: true}))");
        assertThat(page.locator(".row-checkbox:checked")).hasCount(0);
        page.locator("#select-all").check();
        assertEquals(new HashSet<>(expected.subList(0, 20)), new HashSet<>(serials()));
        // 送信するIDは現在のページ内のみ。実際の業務状態はこのテストでは変更しない。
        @SuppressWarnings("unchecked")
        List<String> submitted = (List<String>) page.locator("#bulk-start-form").evaluate(
                "form => new FormData(form).getAll('" + kind() + "Ids')");
        assertEquals(20, submitted.size());
        assertEquals(ordered("active").stream().filter(r -> expected.subList(0, 20).contains(r.serial()))
                .map(r -> String.valueOf(r.id())).collect(Collectors.toSet()), new HashSet<>(submitted));
        click("#page-next");
        page.goBack(); page.waitForLoadState();
        assertThat(page.locator(".row-checkbox:checked")).hasCount(0);
        click("#page-next"); verifyPage("active", 1, expected); assertFilters(filters, 1);
        assertThat(page.locator(".row-checkbox:checked")).hasCount(0);
        assertThat(page.locator("#selected-count")).hasText("0");
        page.locator("#processId").selectOption(String.valueOf(processId));
        page.locator("#select-all").check();
        assertThat(page.locator(".row-checkbox:checked")).hasCount(expected.size() - 20);
        click("#page-previous"); verifyPage("active", 0, expected); assertFilters(filters, 0);
        assertThat(page.locator(".row-checkbox:checked")).hasCount(0);
        click("#page-next"); page.reload(); assertFilters(filters, 1);
        page.locator("#modelName").fill(prefix);
        click(".guitar-search-form button[type=submit]"); assertReset(); verifyPage("active", 0, expected);
        click("#page-next"); click("#category-passed"); assertReset(); assertCleared("passed");
        page.locator("#serial").fill(prefix); click(".guitar-search-form button[type=submit]");
        click("#page-next"); click(".guitar-search-actions a"); assertReset(); assertCleared("passed");
        page.locator("#serial").fill(ordered("passed").get(0).serial()); click(".guitar-search-form button[type=submit]");
        assertThat(page.locator(".component-pagination")).hasCount(0);
        assertThat(page.locator(".guitar-search-result strong")).hasText("1");
        page.locator("#serial").fill(prefix + "missing"); click(".guitar-search-form button[type=submit]");
        assertThat(page.locator(".component-pagination")).hasCount(0);
        assertThat(page.locator(".guitar-search-result strong")).hasText("0");
        assertThat(page.locator(".empty-state")).containsText("条件に一致する");
        click(".empty-state a"); assertReset(); assertCleared("passed");
    }
    private void verifyBulk(String category) {
        boolean start = category.equals("active") || (kind().equals("body") && category.equals("attention"));
        assertThat(page.locator("#bulk-start-form")).hasCount(start ? 1 : 0);
        assertThat(page.locator("a[href='/" + kind() + "-processes/bulk/end/view']")).hasCount(category.equals("active") ? 1 : 0);
        assertThat(page.locator("#select-all")).hasCount(start ? 1 : 0);
        if (start && page.locator(".row-checkbox[data-base-eligible=true]").count() > 0) {
            page.locator("#processId").selectOption(String.valueOf(processId));
            assertThat(page.locator(".row-checkbox[data-base-eligible=false]:enabled")).hasCount(0);
        }
    }
    private List<Row> ordered(String category) {
        Comparator<LocalDateTime> newest = Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<Row> comparator;
        if (category.equals("passed")) {
            comparator = Comparator.comparingInt((Row r) -> states("passed").indexOf(r.state())).thenComparing((a, b) -> {
                if (a.state().equals("AVAILABLE")) {
                    int cmp = Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()).compare(a.available(), b.available());
                    return cmp == 0 ? Long.compare(a.id(), b.id()) : cmp;
                }
                int cmp = newest.compare(a.updated(), b.updated());
                return cmp == 0 ? Long.compare(b.id(), a.id()) : cmp;
            });
        } else comparator = Comparator.comparingInt((Row r) -> category.equals("active") && r.state().equals("WORKING") ? 0 : 1)
                .thenComparing(Row::updated, newest).thenComparing(Row::id, Comparator.reverseOrder());
        return rows.stream().filter(r -> r.category().equals(category)).sorted(comparator).toList();
    }
    private List<String> serials() { return page.locator(".component-serial-number").allTextContents().stream().map(String::trim).toList(); }
    private void verifyPage(String category, int number, List<String> expected) {
        for (String serial : serials()) {
            Row fixture = rows.stream().filter(r -> r.serial().equals(serial)).findFirst().orElseThrow();
            var row = page.locator("tbody tr").filter(new com.microsoft.playwright.Locator.FilterOptions().setHas(page.locator(".component-serial-number", new com.microsoft.playwright.Page.LocatorOptions().setHasText(serial))));
            assertThat(row.locator(".list-updated-at")).hasText(com.example.guitarmes.common.DateTimeFormatterUtil.format(fixture.updated()));
            if (category.equals("passed")) assertThat(row.locator(".list-event-at")).hasText(com.example.guitarmes.common.DateTimeFormatterUtil.format(fixture.available()));
        }

        assertThat(page.locator("#page-position")).hasText((number + 1) + " / " + ((expected.size() + 19) / 20));
        int end = Math.min(expected.size(), number * 20 + 20);
        assertThat(page.locator(".component-serial-number")).hasCount(end - number * 20);
        assertEquals(expected.subList(number * 20, end), serials());
        assertThat(page.locator("input[name=category]")).hasValue(category);
        assertThat(page.locator(".guitar-search-result strong")).hasText(String.valueOf(expected.size()));
        counts.forEach((key, count) -> assertThat(page.locator("#category-" + key + " .category-count")).hasText(String.valueOf(count)));
    }
    private void assertFilters(Map<String, String> filters, int number) {
        filters.forEach((key, value) -> {
            assertEquals(value, query().get(key));
            assertThat(page.locator(key.equals("category") ? "input[name=category]" : "#" + key)).hasValue(value);
        });
        assertEquals(String.valueOf(number), query().get("page"));
    }
    private void assertReset() { assertEquals("0", query().getOrDefault("page", "0")); }
    private void assertCleared(String category) {
        assertThat(page.locator("input[name=category]")).hasValue(category);
        for (String field : List.of("serial", "modelName", "currentProcess", "status")) assertThat(page.locator("#" + field)).hasValue("");
    }
    private void click(String selector) { page.locator(selector).click(); page.waitForLoadState(); }
    private void open(Map<String, String> filters) {
        page.navigate(BASE_URL + "/" + path() + "/view?" + filters.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&")));
    }
    private Map<String, String> query() {
        String raw = URI.create(page.url()).getRawQuery();
        Map<String, String> result = new HashMap<>();
        if (raw != null) for (String pair : raw.split("&")) {
            var parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), parts.length == 1 ? "" : URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return result;
    }
}
