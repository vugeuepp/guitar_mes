package com.example.guitarmes.body;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("e2e")
class BodyRepositorySearchTest {
    @Autowired EntityManager em;
    @Autowired BodyRepository repository;
    private BodyService service;
    private final String prefix = "R" + UUID.randomUUID().toString().substring(0, 8);
    private record Row(long id, String status, LocalDateTime updated, LocalDateTime available) {}
    @BeforeEach void setup() {
        assertEquals("guitar_mes_e2e", em.createNativeQuery("select current_database()", String.class).getSingleResult());
        service = new BodyService(repository, null);
    }
    private Row add(String suffix, String state, LocalDateTime updated, LocalDateTime available) {
        long id = ((Number) em.createNativeQuery("INSERT INTO t_body (serial_no, model_name, current_process, status, updated_at, available_at) "
                + "VALUES (:serial, 'Model%_!I', 'Proc', :state, :updated, :available) RETURNING id")
                .setParameter("serial", prefix + suffix).setParameter("state", state)
                .setParameter("updated", updated).setParameter("available", available).getSingleResult()).longValue();
        return new Row(id, state, updated, available);
    }
    private Page<Body> page(String category, int number) {
        return service.searchBodiesPaged(category, prefix, "", "", "", number);
    }
    private List<Long> ids(Page<Body> page) { return page.getContent().stream().map(Body::getId).toList(); }

    @Test void literalSearchAndExactFiltersAndCategoryCounts() {
        long active = service.countCategory("active"), passed = service.countCategory("passed");
        var match = add("%_!", "WAITING", null, null);
        add("abc", "AVAILABLE", null, null);
        assertEquals(List.of(match.id()), ids(service.searchBodiesPaged("invalid", " " + prefix.toUpperCase(Locale.ROOT) + "%_! ",
                " model%_!i ", " proc ", " waiting ", 0)));
        for (String literal : List.of("%", "_", "!")) {
            assertEquals(List.of(match.id()), ids(service.searchBodiesPaged("active", prefix + "%_!", literal, "", "", 0)));
        }
        assertEquals(0, service.searchBodiesPaged("active", prefix, "", "Pr", "", 0).getTotalElements());
        assertEquals(0, service.searchBodiesPaged("active", prefix, "", "", "WAIT", 0).getTotalElements());
        assertEquals(0, service.searchBodiesPaged("passed", prefix, "", "", "WAITING", 0).getTotalElements());
        assertEquals(active + 1, service.countCategory("active"));
        assertEquals(passed + 1, service.countCategory("passed"));
    }

    @Test void categoriesSortNullsAndPagingAreStable() {
        for (String category : List.of("active", "attention", "passed")) {
            List<Row> rows = new ArrayList<>();
            var states = service.getCategoryStatuses(category);
            for (int i = 0; i < 45; i++) {
                String state = states.get(i % states.size());
                var time = i % 4 == 0 ? null : LocalDateTime.of(2026, 9, 1, 10, 0).plusHours(i % 3);
                var available = i % 5 == 0 ? null : LocalDateTime.of(2026, 9, 1, 10, 0).plusHours((i + 2) % 4);
                rows.add(add(category + i, state, time, available));
            }
            Comparator<LocalDateTime> newest = Comparator.nullsLast(Comparator.reverseOrder());
            Comparator<Row> comparator;
            if (category.equals("passed")) {
                comparator = Comparator.comparingInt((Row r) -> List.of("AVAILABLE", "ASSEMBLED", "REJECTED").indexOf(r.status()))
                        .thenComparing((a, b) -> {
                            if (a.status().equals("AVAILABLE")) {
                                int time = Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()).compare(a.available(), b.available());
                                return time != 0 ? time : Long.compare(a.id(), b.id());
                            }
                            int time = newest.compare(a.updated(), b.updated());
                            return time != 0 ? time : Long.compare(b.id(), a.id());
                        });
            } else {
                comparator = Comparator.comparingInt((Row r) -> category.equals("active") && r.status().equals("WORKING") ? 0 : 1)
                        .thenComparing(Row::updated, newest).thenComparing(Row::id, Comparator.reverseOrder());
            }
            List<Long> expected = rows.stream().sorted(comparator).map(Row::id).toList();
            List<Long> actual = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                var result = page(category, i);
                assertEquals(45, result.getTotalElements());
                assertEquals(i, result.getNumber());
                assertEquals(expected.subList(i * 20, Math.min(45, (i + 1) * 20)), ids(result));
                actual.addAll(ids(result));
            }
            assertEquals(expected, actual);
            assertEquals(45, new HashSet<>(actual).size());
            assertEquals(ids(page(category, 2)), ids(page(category, 999)));
            assertEquals(ids(page(category, 0)), ids(page(category, -1)));
            // Pageable側のsortは業務sortを上書きしない。
            var c = new BodySearchCriteria(category, states, prefix.toLowerCase(Locale.ROOT), "", "", "");
            assertEquals(expected.subList(0, 20), ids(repository.search(c, PageRequest.of(0, 20, Sort.by("id")))));
        }
    }

    @Test void literalWildcardsDoNotMatchOtherCharacters() {
        for (String symbol : List.of("%", "_", "!")) {
            var literal = add("x" + symbol, "WAITING", null, null);
            var other = add("xZ" + symbol, "WAITING", null, null);
            em.createNativeQuery("UPDATE t_body SET model_name=:name WHERE id=:id")
                    .setParameter("name", "M" + symbol).setParameter("id", literal.id()).executeUpdate();
            em.createNativeQuery("UPDATE t_body SET model_name='MZ' WHERE id=:id")
                    .setParameter("id", other.id()).executeUpdate();
            assertEquals(List.of(literal.id()), ids(service.searchBodiesPaged("active", prefix + "x" + symbol, "", "", "", 0)));
            assertEquals(List.of(literal.id()), ids(service.searchBodiesPaged("active", prefix, "M" + symbol, "", "", 0)));
        }
    }

    @Test void pageUsesOneCountAndDoesNotLoadRelatedOrder() {
        var product = new com.example.guitarmes.product.Product();
        product.setModelNo(prefix); product.setProductName(prefix);
        em.persist(product);
        var order = new com.example.guitarmes.productionorder.ProductionOrder(prefix, product, 1,
                java.time.YearMonth.of(2026, 9), null, null, "PLANNED");
        em.persist(order);
        var row = add("relation", "WAITING", null, null);
        em.createNativeQuery("UPDATE t_body SET production_order_id=:order WHERE id=:id")
                .setParameter("order", order.getId()).setParameter("id", row.id()).executeUpdate();
        em.flush(); em.clear();
        var stats = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true); stats.clear();
        try {
            var result = page("active", 0);
            assertEquals(1, result.getTotalElements());
            assertFalse(org.hibernate.Hibernate.isInitialized(result.getContent().get(0).getProductionOrder()));
            assertEquals(2, stats.getPrepareStatementCount(), "countとページ取得の2クエリだけ");
        } finally { stats.setStatisticsEnabled(wasEnabled); }
    }

    @Test void emptyAndSinglePagesAndNullFields() {
        var empty = page("active", 99);
        assertEquals(0, empty.getNumber()); assertEquals(0, empty.getTotalPages());
        var row = add("single", "WAITING", null, null);
        em.createNativeQuery("UPDATE t_body SET model_name=NULL, current_process=NULL WHERE id=:id")
                .setParameter("id", row.id()).executeUpdate();
        assertEquals(List.of(row.id()), ids(page("active", 99)));
        assertEquals(0, page("active", 99).getNumber());
        assertEquals(0, service.searchBodiesPaged("active", prefix, "missing", "", "", 0).getTotalElements());
    }
}
