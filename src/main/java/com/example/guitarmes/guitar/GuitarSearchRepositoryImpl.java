package com.example.guitarmes.guitar;

import static com.example.guitarmes.process.common.GuitarProcessConstants.COMPLETED;
import static com.example.guitarmes.process.common.ProcessTargetConstants.GUITAR;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.guitarmes.process.ManufacturingProcess;
import com.example.guitarmes.process.ProcessHistory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class GuitarSearchRepositoryImpl implements GuitarSearchRepository {
    private final EntityManager entityManager;

    public GuitarSearchRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Guitar> search(
            GuitarSearchCriteria criteria,
            Pageable pageable) {
        long total = countMatching(criteria);
        int size = pageable.getPageSize();
        int lastPage = total == 0
                ? 0
                : Math.toIntExact((total - 1) / size);
        int pageNumber = Math.min(pageable.getPageNumber(), lastPage);
        Pageable corrected = PageRequest.of(pageNumber, size);
        if (total == 0) {
            return new PageImpl<>(List.of(), corrected, 0);
        }

        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Guitar.class);
        var root = query.from(Guitar.class);
        root.fetch("product", JoinType.LEFT);
        Predicate running = runningExists(query, cb, root);
        query.select(root)
                .where(predicates(query, cb, root, criteria, running))
                .orderBy(sort(cb, root, criteria.category(), running));
        var rows = entityManager.createQuery(query)
                .setFirstResult(Math.toIntExact(corrected.getOffset()))
                .setMaxResults(size)
                .getResultList();
        return new PageImpl<>(rows, corrected, total);
    }

    @Override
    public long countMatching(GuitarSearchCriteria criteria) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(Guitar.class);
        Predicate running = runningExists(query, cb, root);
        query.select(cb.count(root))
                .where(predicates(query, cb, root, criteria, running));
        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate[] predicates(
            AbstractQuery<?> query,
            CriteriaBuilder cb,
            Root<Guitar> root,
            GuitarSearchCriteria criteria,
            Predicate running) {
        List<Predicate> conditions = new ArrayList<>();
        Expression<String> process = normalized(cb, root.get("currentProcess"));
        if ("completed".equals(criteria.category())) {
            conditions.add(cb.equal(process, normalize(COMPLETED)));
        } else {
            conditions.add(cb.or(
                    cb.isNull(root.get("currentProcess")),
                    cb.notEqual(process, normalize(COMPLETED))));
        }
        if (!criteria.serial().isEmpty()) {
            conditions.add(contains(cb, root.get("serialNo"), criteria.serial()));
        }
        if (!criteria.product().isEmpty()) {
            var product = root.join("product", JoinType.LEFT);
            conditions.add(cb.equal(
                    normalized(cb, product.get("productName")),
                    criteria.product()));
        }
        if (!criteria.currentProcess().isEmpty()) {
            conditions.add(cb.equal(process, criteria.currentProcess()));
        }
        if (!criteria.status().isEmpty() && "active".equals(criteria.category())) {
            if ("working".equals(criteria.status())) {
                conditions.add(running);
            } else if ("waiting".equals(criteria.status())) {
                conditions.add(cb.not(running));
            }
        }
        return conditions.toArray(Predicate[]::new);
    }

    private Predicate runningExists(
            AbstractQuery<?> query,
            CriteriaBuilder cb,
            Root<Guitar> guitar) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<ProcessHistory> history = subquery.from(ProcessHistory.class);
        Root<ManufacturingProcess> process = subquery.from(ManufacturingProcess.class);
        subquery.select(cb.literal(1L)).where(
                cb.equal(history.get("guitarId"), guitar.get("id")),
                cb.isNull(history.get("endTime")),
                cb.equal(history.get("processId"), process.get("id")),
                cb.equal(process.get("targetType"), GUITAR));
        return cb.exists(subquery);
    }

    private Predicate contains(
            CriteriaBuilder cb,
            Expression<String> field,
            String text) {
        String escaped = text.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return cb.like(normalized(cb, field), "%" + escaped + "%", '!');
    }

    private Expression<String> normalized(
            CriteriaBuilder cb,
            Expression<String> field) {
        return cb.lower(cb.trim(field));
    }

    private String normalize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    private List<Order> sort(
            CriteriaBuilder cb,
            Root<Guitar> root,
            String category,
            Predicate running) {
        List<Order> orders = new ArrayList<>();
        if ("completed".equals(category)) {
            nullsLast(cb, orders, root.get("completedAt"), false);
        } else {
            orders.add(cb.asc(cb.<Integer>selectCase()
                    .when(running, 0)
                    .otherwise(1)));
            nullsLast(cb, orders, root.get("updatedAt"), false);
        }
        orders.add(cb.desc(root.get("id")));
        return orders;
    }

    private void nullsLast(
            CriteriaBuilder cb,
            List<Order> orders,
            Expression<?> field,
            boolean ascending) {
        orders.add(cb.asc(cb.<Integer>selectCase()
                .when(cb.isNull(field), 1)
                .otherwise(0)));
        orders.add(ascending ? cb.asc(field) : cb.desc(field));
    }
}
