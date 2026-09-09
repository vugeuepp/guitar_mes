package com.example.guitarmes.body;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;

public class BodySearchRepositoryImpl implements BodySearchRepository {
    private final EntityManager em;
    public BodySearchRepositoryImpl(EntityManager em) { this.em = em; }

    @Override
    public Page<Body> search(BodySearchCriteria criteria, Pageable pageable) {
        long total = countMatching(criteria);
        int size = pageable.getPageSize();
        int last = total == 0 ? 0 : Math.toIntExact((total - 1) / size);
        int number = Math.min(pageable.getPageNumber(), last);
        var corrected = PageRequest.of(number, size);
        if (total == 0) return new PageImpl<>(List.of(), corrected, 0);
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Body.class);
        var root = query.from(Body.class);
        query.select(root).where(predicates(cb, root, criteria)).orderBy(sort(cb, root, criteria.category()));
        // 一覧は本体のスカラー値だけを使用。EAGER関連の連鎖取得を抑え、collection fetchも行わない。
        var rows = em.createQuery(query).setHint("jakarta.persistence.fetchgraph", em.createEntityGraph(Body.class))
                .setFirstResult(Math.toIntExact(corrected.getOffset())).setMaxResults(size).getResultList();
        return new PageImpl<>(rows, corrected, total);
    }

    @Override
    public long countMatching(BodySearchCriteria criteria) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(Body.class);
        query.select(cb.count(root)).where(predicates(cb, root, criteria));
        return em.createQuery(query).getSingleResult();
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<Body> root, BodySearchCriteria c) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(normalized(cb, root.get("status")).in(c.statuses().stream()
                .map(s -> s.toLowerCase(Locale.ROOT)).toList()));
        if (!c.serial().isEmpty()) predicates.add(contains(cb, root.get("serialNo"), c.serial()));
        if (!c.modelName().isEmpty()) predicates.add(contains(cb, root.get("modelName"), c.modelName()));
        if (!c.currentProcess().isEmpty()) predicates.add(cb.equal(normalized(cb, root.get("currentProcess")), c.currentProcess()));
        if (!c.status().isEmpty()) predicates.add(cb.equal(normalized(cb, root.get("status")), c.status()));
        return predicates.toArray(Predicate[]::new);
    }
    private Expression<String> normalized(CriteriaBuilder cb, Expression<String> field) {
        return cb.lower(cb.trim(field));
    }
    private Predicate contains(CriteriaBuilder cb, Expression<String> field, String text) {
        String escaped = text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return cb.like(normalized(cb, field), "%" + escaped + "%", '!');
    }
    private List<Order> sort(CriteriaBuilder cb, Root<Body> root, String category) {
        List<Order> orders = new ArrayList<>();
        var state = normalized(cb, root.get("status"));
        if ("passed".equals(category)) {
            orders.add(cb.asc(cb.<Integer>selectCase().when(cb.equal(state, "available"), 0)
                    .when(cb.equal(state, "assembled"), 1).otherwise(2)));
            var available = cb.equal(state, "available");
            Expression<LocalDateTime> availableTime = cb.<LocalDateTime>selectCase()
                    .when(available, root.get("availableAt")).otherwise(cb.nullLiteral(LocalDateTime.class));
            nullsLast(cb, orders, availableTime, true);
            orders.add(cb.asc(cb.<Long>selectCase().when(available, root.get("id")).otherwise(cb.nullLiteral(Long.class))));
            Expression<LocalDateTime> updated = cb.<LocalDateTime>selectCase()
                    .when(available, cb.nullLiteral(LocalDateTime.class)).otherwise(root.get("updatedAt"));
            nullsLast(cb, orders, updated, false);
        } else {
            if ("active".equals(category)) orders.add(cb.asc(cb.<Integer>selectCase()
                    .when(cb.equal(state, "working"), 0).otherwise(1)));
            // WAITING系内部・attention内部には既存の優先順位がないため、同順位として扱う。
            nullsLast(cb, orders, root.get("updatedAt"), false);
        }
        orders.add(cb.desc(root.get("id")));
        return orders;
    }
    private void nullsLast(CriteriaBuilder cb, List<Order> orders, Expression<?> field, boolean asc) {
        orders.add(cb.asc(cb.<Integer>selectCase().when(cb.isNull(field), 1).otherwise(0)));
        orders.add(asc ? cb.asc(field) : cb.desc(field));
    }
}
