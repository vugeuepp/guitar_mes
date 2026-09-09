package com.example.guitarmes.guitar;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.example.guitarmes.process.ManufacturingProcess;
import com.example.guitarmes.process.ProcessHistory;
import com.example.guitarmes.process.common.ProcessTargetConstants;
import com.example.guitarmes.product.Product;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("e2e")
class GuitarRepositorySearchTest {
    @Autowired EntityManager entityManager;
    @Autowired GuitarRepository repository;

    private String prefix;
    private Product product;
    private ManufacturingProcess guitarProcess;
    private ManufacturingProcess nonGuitarProcess;

    @BeforeEach
    void setup() {
        assertEquals("guitar_mes_e2e", entityManager
                .createNativeQuery("select current_database()", String.class)
                .getSingleResult());
        prefix = "GR" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 10);
        product = new Product();
        product.setModelNo(prefix + "-MODEL");
        product.setProductName(prefix + " Product");
        entityManager.persist(product);
        guitarProcess = new ManufacturingProcess(
                ProcessTargetConstants.GUITAR, prefix + " Guitar Process", 1);
        nonGuitarProcess = new ManufacturingProcess(
                ProcessTargetConstants.BODY, prefix + " Body Process", 1);
        entityManager.persist(guitarProcess);
        entityManager.persist(nonGuitarProcess);
        entityManager.flush();
    }

    private Guitar add(
            String suffix,
            String currentProcess,
            Product selectedProduct,
            LocalDateTime updatedAt,
            LocalDateTime completedAt) {
        Guitar guitar = new Guitar(prefix + suffix, currentProcess);
        guitar.setProduct(selectedProduct);
        entityManager.persist(guitar);
        entityManager.flush();
        entityManager.createNativeQuery("update t_guitar set updated_at=:updated, "
                + "completed_at=:completed where id=:id")
                .setParameter("updated", updatedAt)
                .setParameter("completed", completedAt)
                .setParameter("id", guitar.getId())
                .executeUpdate();
        entityManager.detach(guitar);
        return guitar;
    }

    private void running(Guitar guitar, ManufacturingProcess process) {
        entityManager.persist(new ProcessHistory(
                guitar.getId(), process.getId(), prefix, LocalDateTime.now()));
        entityManager.flush();
    }

    private GuitarSearchCriteria criteria(
            String category,
            String serial,
            String productName,
            String currentProcess,
            String status) {
        return new GuitarSearchCriteria(category, serial, productName,
                currentProcess, status);
    }

    private Page<Guitar> page(
            GuitarSearchCriteria criteria,
            int number,
            int size) {
        return repository.search(criteria, PageRequest.of(number, size));
    }

    private List<Long> ids(Page<Guitar> page) {
        return page.getContent().stream().map(Guitar::getId).toList();
    }

    @Test
    void categoryFiltersAndCountsIncludeNullProcessInActive() {
        Guitar active = add("-ACTIVE", "調整・調音", product, null, null);
        Guitar nullProcess = add("-NULL", null, product, null, null);
        Guitar completed = add("-DONE", "完成", product, null,
                LocalDateTime.of(2026, 9, 1, 10, 0));

        var activeCriteria = criteria("active", prefix.toLowerCase(Locale.ROOT),
                "", "", "");
        var completedCriteria = criteria("completed", prefix.toLowerCase(Locale.ROOT),
                "", "", "");
        assertEquals(List.of(nullProcess.getId(), active.getId()),
                ids(page(activeCriteria, 0, 20)));
        assertEquals(2, repository.countMatching(activeCriteria));
        assertEquals(List.of(completed.getId()), ids(page(completedCriteria, 0, 20)));
        assertEquals(1, repository.countMatching(completedCriteria));

        var narrowed = criteria("active", (prefix + "-ACTIVE").toLowerCase(Locale.ROOT),
                "", "", "");
        assertEquals(1, repository.countMatching(narrowed));
        assertEquals(2, repository.countMatching(activeCriteria));
    }

    @Test
    void serialProductAndProcessFiltersKeepExistingMeanings() {
        Guitar match = add("-%_!MATCH", "調整・調音", product, null, null);
        add("-AZXMATCH", "最終検品", product, null, null);
        Product otherProduct = new Product();
        otherProduct.setModelNo(prefix + "-OTHER");
        otherProduct.setProductName(prefix + " Other");
        entityManager.persist(otherProduct);
        add("-%_!OTHER", "調整・調音", otherProduct, null, null);
        add("-%_!NULL", "調整・調音", null, null, null);

        String serial = (prefix + "-%_!").toLowerCase(Locale.ROOT);
        String productName = product.getProductName().toLowerCase(Locale.ROOT);
        assertEquals(List.of(match.getId()), ids(page(criteria(
                "active", serial, productName, "調整・調音", ""), 0, 20)));
        for (String literal : List.of("%", "_", "!")) {
            var result = page(criteria(
                    "active",
                    literal,
                    "", "", ""), 0, 20);
            assertTrue(ids(result).contains(match.getId()));
        }
        assertEquals(0, repository.countMatching(criteria(
                "active", prefix.toLowerCase(Locale.ROOT),
                (prefix + " pro").toLowerCase(Locale.ROOT), "", "")));
        assertEquals(0, repository.countMatching(criteria(
                "active", prefix.toLowerCase(Locale.ROOT),
                "", "調整", "")));
    }

    @Test
    void workingUsesOnlyRunningCurrentGuitarProcesses() {
        Guitar working = add("-WORKING", "調整・調音", product, null, null);
        Guitar legacyOnly = add("-LEGACY", "調整・調音", product, null, null);
        Guitar waiting = add("-WAITING", "調整・調音", product, null, null);
        running(working, guitarProcess);
        running(legacyOnly, nonGuitarProcess);

        String serial = prefix.toLowerCase(Locale.ROOT);
        assertEquals(List.of(working.getId()), ids(page(criteria(
                "active", serial, "", "", "working"), 0, 20)));
        assertEquals(List.of(waiting.getId(), legacyOnly.getId()), ids(page(criteria(
                "active", serial, "", "", "waiting"), 0, 20)));
    }

    @Test
    void businessSortAndPagingAreStableAndIgnorePageableSort() {
        List<Guitar> working = new ArrayList<>();
        List<Guitar> waiting = new ArrayList<>();
        LocalDateTime recent = LocalDateTime.of(2026, 9, 2, 10, 0);
        LocalDateTime old = LocalDateTime.of(2026, 9, 1, 10, 0);
        for (int index = 0; index < 7; index++) {
            Guitar guitar = add("-W-" + index, "調整・調音", product,
                    index == 0 ? null : recent, null);
            running(guitar, guitarProcess);
            working.add(guitar);
        }
        for (int index = 0; index < 18; index++) {
            waiting.add(add("-Q-" + index, "調整・調音", product,
                    index == 0 ? null : old, null));
        }
        var criteria = criteria("active", prefix.toLowerCase(Locale.ROOT),
                "", "", "");
        Page<Guitar> first = repository.search(criteria,
                PageRequest.of(0, 20, Sort.by("id").ascending()));
        Page<Guitar> second = page(criteria, 1, 20);
        assertEquals(25, first.getTotalElements());
        assertEquals(20, first.getNumberOfElements());
        assertEquals(5, second.getNumberOfElements());
        assertTrue(new HashSet<>(ids(first)).stream()
                .noneMatch(new HashSet<>(ids(second))::contains));
        assertTrue(ids(first).subList(0, 7).containsAll(
                working.stream().map(Guitar::getId).toList()));
        assertEquals(25, new HashSet<Long>() {{
            addAll(ids(first));
            addAll(ids(second));
        }}.size());
        assertEquals(ids(second), ids(page(criteria, 999, 20)));
    }

    @Test
    void completedSortUsesCompletedAtNullsLastThenId() {
        Guitar old = add("-DONE-OLD", "完成", product, null,
                LocalDateTime.of(2026, 9, 1, 10, 0));
        Guitar recent1 = add("-DONE-R1", "完成", product, null,
                LocalDateTime.of(2026, 9, 2, 10, 0));
        Guitar recent2 = add("-DONE-R2", "完成", product, null,
                LocalDateTime.of(2026, 9, 2, 10, 0));
        Guitar noTime1 = add("-DONE-N1", "完成", product, null, null);
        Guitar noTime2 = add("-DONE-N2", "完成", product, null, null);
        var result = page(criteria("completed",
                prefix.toLowerCase(Locale.ROOT), "", "", ""), 0, 20);
        assertEquals(List.of(recent2.getId(), recent1.getId(), old.getId(),
                noTime2.getId(), noTime1.getId()), ids(result));
    }

    @Test
    void emptyResultReturnsPageZero() {
        var result = page(criteria("active", "missing", "", "", ""), 99, 20);
        assertEquals(0, result.getNumber());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getTotalElements());
    }
}
