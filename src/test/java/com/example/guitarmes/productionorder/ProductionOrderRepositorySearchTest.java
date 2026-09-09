
package com.example.guitarmes.productionorder;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import com.example.guitarmes.product.Product;

/** 専用PostgreSQLを使用。作成データ・日時fixture更新は各テスト終了時に全てロールバック。 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("e2e")
class ProductionOrderRepositorySearchTest {
    @Autowired ProductionOrderRepository repository;
    @Autowired EntityManager em;
    private ProductionOrderService service;
    private Product product;
    private String prefix;

    @BeforeEach
    void fixtures() {
        assertEquals("guitar_mes_e2e", em.createNativeQuery("select current_database()", String.class).getSingleResult());
        service = new ProductionOrderService(repository, null, null);
        prefix = "Q" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        product = new Product();
        product.setModelNo(prefix + "-MODEL%_!I");
        product.setProductName(prefix + " Guitar%_!I");
        em.persist(product);
    }

    private ProductionOrder order(String suffix, String status, String due, String updated, String completed) {
        var order = new ProductionOrder(prefix + suffix, product, 1, YearMonth.of(2026, 9), null,
                due == null ? null : LocalDate.parse(due), status);
        em.persist(order);
        em.flush();
        // Callbackを経由せず、5C-1以前のNULLや過去時刻を自身のfixtureだけに再現する。
        em.createNativeQuery("update t_production_order set updated_at=cast(:updated as timestamp), "
                + "completed_at=cast(:completed as timestamp) where id=:id")
                .setParameter("updated", updated).setParameter("completed", completed)
                .setParameter("id", order.getId()).executeUpdate();
        em.detach(order);
        return order;
    }

    private ProductionOrderService.SearchResult search(String category, String number, String productText,
            String status, String month, String from, String to) {
        return service.searchProductionOrders(category, number, productText, status, month, from, to);
    }

    private Page<ProductionOrder> searchPage(
            String category, String number, int page) {
        var criteria = new ProductionOrderSearchCriteria(
                service.normalizeCategory(category),
                service.getCategoryStatuses(category),
                number.toLowerCase(java.util.Locale.ROOT),
                "", "", null, null, null);
        return repository.search(criteria, PageRequest.of(page, 20));
    }

    private void assertPageIds(
            Page<ProductionOrder> page,
            List<ProductionOrder> expected,
            long total,
            int pageNumber) {
        assertEquals(expected.stream().map(ProductionOrder::getId).toList(),
                page.getContent().stream().map(ProductionOrder::getId).toList());
        assertEquals(total, page.getTotalElements());
        assertEquals(pageNumber, page.getNumber());
        assertTrue(page.getNumberOfElements() <= 20);
    }

    private void assertRows(ProductionOrderService.SearchResult result, ProductionOrder... expected) {
        assertEquals(List.of(expected).stream().map(ProductionOrder::getId).toList(),
                result.orders().stream().map(ProductionOrder::getId).toList());
        assertEquals(expected.length, result.resultCount());
    }

    @Test
    void categoriesCountsAndForeignStatusRemainIndependentOfSearch() {
        long active = service.countCategory("active"), completed = service.countCategory("completed"),
                cancelled = service.countCategory("cancelled");
        var p = order("P", "PLANNED", "2026-09-01", null, null);
        var w = order("W", "IN_PROGRESS", "2026-09-02", null, null);
        var c = order("C", "COMPLETED", null, null, null);
        var x = order("X", "CANCELLED", null, null, null);
        assertRows(search("active", prefix, "", "", "", "", ""), p, w);
        assertRows(search("unknown", prefix, "", "", "", "", ""), p, w);
        assertRows(search("completed", prefix, "", "", "", "", ""), c);
        assertRows(search("cancelled", prefix, "", "", "", "", ""), x);
        assertRows(search("completed", prefix, "", "PLANNED", "", "", ""));
        assertRows(search("active", prefix + "P", "", "", "", "", ""), p);
        assertEquals(active + 2, service.countCategory("active"));
        assertEquals(completed + 1, service.countCategory("completed"));
        assertEquals(cancelled + 1, service.countCategory("cancelled"));
    }

    @Test
    void filtersUseAndInclusiveDatesAndConvertedYearMonth() {
        var p = order("-Match", "PLANNED", "2026-09-10", null, null);
        var noDue = order("-Null", "PLANNED", null, null, null);
        var october = order("-October", "PLANNED", "2026-10-10", null, null);
        em.createNativeQuery("update t_production_order set plan_month=DATE '2026-10-01' where id=:id")
                .setParameter("id", october.getId()).executeUpdate();
        assertRows(search("active", " " + prefix.toUpperCase(java.util.Locale.ROOT) + "-mAtCh ",
                " GUITAR%_!i ", " planned ", "2026-09", "2026-09-10", "2026-09-10"), p);
        assertRows(search("active", prefix + "-Match", "model%_!i", "", "", "", ""), p);
        assertRows(search("active", prefix, "", "", "2026-10", "", ""), october);
        assertRows(search("active", prefix, "", "", "", "2026-09-10", "2026-09-10"), p);
        assertRows(search("active", prefix, "", "", "", "2026-10-10", ""), october);
        assertRows(search("active", prefix, "", "", "", "", "2026-09-10"), p);
        assertRows(search("active", prefix + "-Match", "missing", "", "", "", ""));
        assertRows(search("active", prefix, "", "PLAN", "", "", ""));
        assertRows(search("active", prefix, "", "COMPLETED", "", "", ""));
        assertRows(search("active", prefix + "-Null", "", "", "", "", ""), noDue);
        assertRows(search("active", prefix + "-Null", "", "", "", "2026-01-01", ""));
    }

    @Test
    void likeMetacharactersAreLiteralInNumberAndProduct() {
        order("-abc", "PLANNED", null, null, null);
        for (String character : List.of("%", "_", "!")) {
            var literal = order("-" + character, "PLANNED", null, null, null);
            assertRows(search("active", prefix + "-" + character, "", "", "", "", ""), literal);
        }
        // リテラルを含まない別製品も同じ注文番号prefixで用意し、ワイルドカード誤一致を検出。
        var other = new Product();
        other.setModelNo(prefix + "-MODELabcI");
        other.setProductName(prefix + " GuitarabcI");
        em.persist(other);
        Product original = product;
        product = other;
        order("-Other", "PLANNED", null, null, null);
        product = original;
        for (String text : List.of("Guitar%", "Guitar%_", "Guitar%_!", "MODEL%", "MODEL%_", "MODEL%_!")) {
            var result = search("active", prefix, text, "", "", "", "");
            assertEquals(4, result.resultCount());
            assertTrue(result.orders().stream().allMatch(o -> o.getProduct().getId().equals(original.getId())));
        }
    }

    @Test
    void activeSortUsesDueThenUpdatedNullsLastThenId() {
        var early = order("E", "PLANNED", "2026-09-01", null, null);
        var old = order("O", "IN_PROGRESS", "2026-09-02", "2026-09-01 10:00:00", null);
        var recent1 = order("R1", "PLANNED", "2026-09-02", "2026-09-02 10:00:00", null);
        var recent2 = order("R2", "PLANNED", "2026-09-02", "2026-09-02 10:00:00", null);
        var noUpdated = order("U", "PLANNED", "2026-09-02", null, null);
        var noDue = order("D", "PLANNED", null, "2026-09-03 10:00:00", null);
        var bothNull1 = order("N1", "PLANNED", null, null, null);
        var bothNull2 = order("N2", "PLANNED", null, null, null);
        assertRows(search("active", prefix, "", "", "", "", ""),
                early, recent2, recent1, old, noUpdated, noDue, bothNull2, bothNull1);
    }

    @Test
    void completedAndCancelledSortTimestampsNullsLastThenId() {
        for (String category : List.of("completed", "cancelled")) {
            String status = category.toUpperCase(java.util.Locale.ROOT);
            String key = status.substring(0, 3);
            var old = order(key + "O", status, "2026-01-01", "2026-09-01 10:00:00", "2026-09-01 10:00:00");
            var recent1 = order(key + "R1", status, null, "2026-09-02 10:00:00", "2026-09-02 10:00:00");
            var recent2 = order(key + "R2", status, null, "2026-09-02 10:00:00", "2026-09-02 10:00:00");
            // 並び順に使わない日時を逆転させ、completed/cancelledの列の取り違えも検出する。
            em.createNativeQuery("update t_production_order set "
                    + (category.equals("completed") ? "updated_at" : "completed_at")
                    + "=TIMESTAMP '2099-01-01 00:00:00' where id=:id")
                    .setParameter("id", old.getId()).executeUpdate();
            var noTime1 = order(key + "N1", status, null, null, null);
            var noTime2 = order(key + "N2", status, null, null, null);
            assertRows(search(category, prefix, "", "", "", "", ""), recent2, recent1, old, noTime2, noTime1);
        }
    }
    @Test
    void pagingUsesDatabaseLimitOffsetAndTotalCount() {
        List<ProductionOrder> expected = new java.util.ArrayList<>();
        for (int index = 0; index < 45; index++) {
            expected.add(order("-PAGE-" + index, "PLANNED",
                    "2026-09-10", "2026-09-01 10:00:00", null));
        }
        expected.sort(java.util.Comparator.comparing(
                ProductionOrder::getId).reversed());

        var first = searchPage("active", prefix + "-page-", 0);
        var second = searchPage("active", prefix + "-page-", 1);
        var last = searchPage("active", prefix + "-page-", 2);
        var overflow = searchPage("active", prefix + "-page-", 999);

        assertPageIds(first, expected.subList(0, 20), 45, 0);
        assertPageIds(second, expected.subList(20, 40), 45, 1);
        assertPageIds(last, expected.subList(40, 45), 45, 2);
        assertPageIds(overflow, expected.subList(40, 45), 45, 2);
        assertEquals(20, first.getNumberOfElements());
        assertEquals(5, last.getNumberOfElements());
        var combined = new java.util.ArrayList<Long>();
        combined.addAll(first.map(ProductionOrder::getId).getContent());
        combined.addAll(second.map(ProductionOrder::getId).getContent());
        combined.addAll(last.map(ProductionOrder::getId).getContent());
        assertEquals(45, combined.stream().distinct().count());
    }

    @Test
    void pagingHandlesAtMostTwentyAndZeroResults() {
        var one = order("-ONE", "PLANNED", null, null, null);
        assertPageIds(searchPage("active", prefix + "-one", 0),
                List.of(one), 1, 0);
        assertPageIds(searchPage("active", prefix + "-missing", 5),
                List.of(), 0, 0);
    }

    @Test
    void categorySearchAndSortRemainStableAcrossPageBoundaries() {
        for (String category : List.of("active", "completed", "cancelled")) {
            String status = switch (category) {
                case "completed" -> "COMPLETED";
                case "cancelled" -> "CANCELLED";
                default -> "PLANNED";
            };
            List<ProductionOrder> expected = new java.util.ArrayList<>();
            for (int index = 0; index < 21; index++) {
                String completed = "completed".equals(category)
                        ? "2026-09-02 10:00:00" : null;
                String updated = "completed".equals(category)
                        ? null : "2026-09-02 10:00:00";
                expected.add(order("-" + category + "-" + index, status,
                        "active".equals(category) ? "2026-09-02" : null,
                        updated, completed));
            }
            expected.sort(java.util.Comparator.comparing(
                    ProductionOrder::getId).reversed());
            var first = searchPage(category, prefix + "-" + category + "-", 0);
            var second = searchPage(category, prefix + "-" + category + "-", 1);
            assertPageIds(first, expected.subList(0, 20), 21, 0);
            assertPageIds(second, expected.subList(20, 21), 21, 1);
            assertFalse(first.getContent().stream().map(ProductionOrder::getId)
                    .anyMatch(second.getContent().stream()
                            .map(ProductionOrder::getId).toList()::contains));
        }
    }

    @Test
    void activePagingKeepsNullsLastOrdering() {
        List<ProductionOrder> dated = new java.util.ArrayList<>();
        for (int index = 0; index < 20; index++) {
            dated.add(order("-DATED-" + index, "PLANNED",
                    "2026-09-01", null, null));
        }
        var nullDue = order("-NULL-DUE", "PLANNED", null, null, null);
        var first = searchPage("active", prefix + "-", 0);
        var allIds = first.getContent().stream()
                .map(ProductionOrder::getId).toList();
        assertFalse(allIds.contains(nullDue.getId()));
        var second = searchPage("active", prefix + "-", 1);
        assertTrue(second.getContent().stream()
                .map(ProductionOrder::getId)
                .anyMatch(nullDue.getId()::equals));
    }

}
