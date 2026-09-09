
package com.example.guitarmes.productionorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class ProductionOrderSearchRepositoryImpl implements ProductionOrderSearchRepository {
    private final EntityManager entityManager;

    public ProductionOrderSearchRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<ProductionOrder> search(ProductionOrderSearchCriteria criteria) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(ProductionOrder.class);
        var root = query.from(ProductionOrder.class);
        root.fetch("product", JoinType.LEFT);
        query.select(root).where(predicates(cb, root, criteria));
        query.orderBy(sort(cb, root, criteria.category()));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public Page<ProductionOrder> search(
            ProductionOrderSearchCriteria criteria,
            Pageable pageable) {
        long total = countMatching(criteria);
        int pageSize = pageable.getPageSize();
        int lastPage = total == 0 ? 0 : (int) ((total - 1) / pageSize);
        int pageNumber = Math.min(pageable.getPageNumber(), lastPage);
        Pageable corrected = PageRequest.of(pageNumber, pageSize);
        if (total == 0) {
            return new PageImpl<>(List.of(), corrected, 0);
        }
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(ProductionOrder.class);
        var root = query.from(ProductionOrder.class);
        root.fetch("product", JoinType.LEFT);
        query.select(root).where(predicates(cb, root, criteria));
        query.orderBy(sort(cb, root, criteria.category()));
        var graph = entityManager.createEntityGraph(ProductionOrder.class);
        graph.addSubgraph("product");
        var typedQuery = entityManager.createQuery(query)
                .setHint("jakarta.persistence.fetchgraph", graph);
        typedQuery.setFirstResult(Math.toIntExact(corrected.getOffset()));
        typedQuery.setMaxResults(pageSize);
        return new PageImpl<>(typedQuery.getResultList(), corrected, total);
    }

    @Override
    public long countMatching(ProductionOrderSearchCriteria criteria) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(ProductionOrder.class);
        query.select(cb.count(root)).where(predicates(cb, root, criteria));
        return entityManager.createQuery(query).getSingleResult();
    }

    /** 一覧とcountで必ず同じカテゴリ・検索述語を使う。 */
    private Predicate[] predicates(CriteriaBuilder cb, Root<ProductionOrder> root,
            ProductionOrderSearchCriteria criteria) {
        List<Predicate> conditions = new ArrayList<>();
        var states = criteria.categoryStatuses().stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
        Expression<String> status = cb.lower(cb.trim(root.get("status")));
        conditions.add(status.in(states));
        if (!criteria.orderNo().isEmpty()) {
            conditions.add(contains(cb, root.get("orderNo"), criteria.orderNo()));
        }
        if (!criteria.product().isEmpty()) {
            var product = root.join("product", JoinType.LEFT);
            conditions.add(cb.or(contains(cb, product.get("productName"), criteria.product()),
                    contains(cb, product.get("modelNo"), criteria.product())));
        }
        if (!criteria.status().isEmpty()) {
            conditions.add(cb.equal(status, criteria.status()));
        }
        if (criteria.planMonth() != null) {
            // YearMonthDateConverterを通して、DBの月初DATEと比較する。
            conditions.add(cb.equal(root.get("planMonth"), criteria.planMonth()));
        }
        if (criteria.dueFrom() != null) {
            conditions.add(cb.greaterThanOrEqualTo(root.get("dueDate"), criteria.dueFrom()));
        }
        if (criteria.dueTo() != null) {
            conditions.add(cb.lessThanOrEqualTo(root.get("dueDate"), criteria.dueTo()));
        }
        return conditions.toArray(Predicate[]::new);
    }

    private Predicate contains(CriteriaBuilder cb, Expression<String> field, String text) {
        String escaped = text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return cb.like(cb.lower(cb.trim(field)), "%" + escaped + "%", '!');
    }

    private List<Order> sort(CriteriaBuilder cb, Root<ProductionOrder> root, String category) {
        List<Order> orders = new ArrayList<>();
        if ("active".equals(category)) {
            nullsLast(cb, orders, root.get("dueDate"), true);
            nullsLast(cb, orders, root.get("updatedAt"), false);
        } else if ("completed".equals(category)) {
            nullsLast(cb, orders, root.get("completedAt"), false);
        } else {
            nullsLast(cb, orders, root.get("updatedAt"), false);
        }
        orders.add(cb.desc(root.get("id")));
        return orders;
    }

    private void nullsLast(CriteriaBuilder cb, List<Order> orders, Expression<?> field, boolean ascending) {
        orders.add(cb.asc(cb.<Integer>selectCase().when(cb.isNull(field), 1).otherwise(0)));
        orders.add(ascending ? cb.asc(field) : cb.desc(field));
    }
}
