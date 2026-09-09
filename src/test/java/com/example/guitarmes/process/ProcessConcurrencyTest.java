package com.example.guitarmes.process;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.guitarmes.body.*;
import com.example.guitarmes.body.process.*;
import com.example.guitarmes.neck.*;
import com.example.guitarmes.neck.process.*;
import com.example.guitarmes.guitar.*;
import com.example.guitarmes.productionorder.*;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.exception.BusinessException;

/** 独立した実DBトランザクションで、個別操作と一括操作の競合を検証する。 */
@SpringBootTest(properties = {"spring.jpa.show-sql=false", "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.example.guitarmes.process.ProcessConcurrencyTest$PageInspector"})
@ActiveProfiles("e2e")
class ProcessConcurrencyTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired BodyRepository bodies;
    @Autowired NeckRepository necks;
    @Autowired GuitarRepository guitars;
    @Autowired BodyProcessService bodyService;
    @Autowired NeckProcessService neckService;
    @Autowired ProcessService guitarService;
    @Autowired BodyService bodyLists;
    @Autowired NeckService neckLists;
    @Autowired GuitarService guitarLists;
    @Autowired ProductionOrderService orderLists;
    @Autowired ProductionOrderRepository orders;
    @Autowired com.example.guitarmes.assembly.AssemblyService assemblyService;
    private final Map<String, List<Long>> owned = new LinkedHashMap<>();
    private String prefix;
    private TransactionTemplate tx;

    @BeforeEach void setup() {
        tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(s -> assertEquals("guitar_mes_e2e", em.createNativeQuery("select current_database()", String.class).getSingleResult()));
        prefix = "CC" + UUID.randomUUID().toString().substring(0, 8);
    }
    private long persist(Object value, String table) {
        em.persist(value); em.flush();
        long id = ((Number) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(value)).longValue();
        owned.computeIfAbsent(table, k -> new ArrayList<>()).add(id); return id;
    }
    @AfterEach void cleanup() {
        tx.executeWithoutResult(s -> {
            // 履歴は各fixture個体IDだけを対象にする。マスタ・他テストのデータには触れない。
            for (String kind : List.of("body", "neck", "guitar")) {
                String history = kind.equals("guitar") ? "t_process_history" : "t_" + kind + "_process_history";
                for (Long id : owned.getOrDefault("t_" + kind, List.of()))
                    em.createNativeQuery("delete from " + history + " where " + kind + "_id=:id").setParameter("id", id).executeUpdate();
            }
            var tables = new ArrayList<>(owned.keySet()); Collections.reverse(tables);
            for (String table : tables) for (Long id : owned.get(table))
                em.createNativeQuery("delete from " + table + " where id=:id").setParameter("id", id).executeUpdate();
        });
    }
    private ManufacturingProcess process(String kind, boolean last) {
        return em.createQuery("select p from ManufacturingProcess p where p.targetType=:type order by p.processOrder " + (last ? "desc" : "asc") + ", p.id", ManufacturingProcess.class)
                .setParameter("type", kind.toUpperCase(Locale.ROOT)).setMaxResults(1).getSingleResult();
    }
    private ProductionOrder order() {
        Product product = new Product(); product.setProductName(prefix); product.setModelNo(prefix);
        persist(product, "m_product");
        ProductionOrder order = new ProductionOrder(); order.setOrderNo(prefix); order.setProduct(product);
        order.setPlannedQuantity(2); order.setStartedQuantity(2); order.setCompletedQuantity(0); order.setStatus("IN_PROGRESS"); order.setPlanMonth(java.time.YearMonth.of(2026, 9)); order.setDueDate(java.time.LocalDate.of(2026, 9, 30));
        persist(order, "t_production_order"); return order;
    }
    private long entity(String kind, ManufacturingProcess process, ProductionOrder order) {
        if (kind.equals("body")) {
            Body body = new Body(); body.setSerialNo(prefix); body.setStatus("WAITING"); body.setCurrentProcess(process.getProcessName());
            return persist(body, "t_body");
        }
        if (kind.equals("neck")) {
            Neck neck = new Neck(); neck.setSerialNo(prefix); neck.setStatus("WAITING"); neck.setCurrentProcess(process.getProcessName());
            return persist(neck, "t_neck");
        }
        Guitar guitar = new Guitar(prefix + owned.getOrDefault("t_guitar", List.of()).size(), process.getProcessName());
        guitar.setProduct(order.getProduct()); guitar.setProductionOrder(order); return persist(guitar, "t_guitar");
    }
    private void lock(String kind, long id) {
        switch (kind) {
            case "body" -> bodies.findForUpdate(id).orElseThrow();
            case "neck" -> necks.findForUpdate(id).orElseThrow();
            default -> guitars.findForUpdate(id).orElseThrow();
        }
    }
    private long start(String kind, long id, long processId, boolean bulk) {
        return switch (kind) {
            case "body" -> (bulk ? bodyService.startProcesses(List.of(id), processId, prefix).get(0) : bodyService.startProcess(id, processId, prefix)).getId();
            case "neck" -> (bulk ? neckService.startProcesses(List.of(id), processId, prefix).get(0) : neckService.startProcess(id, processId, prefix)).getId();
            default -> (bulk ? guitarService.startProcesses(List.of(id), processId, prefix).get(0) : guitarService.startProcess(id, processId, prefix)).getId();
        };
    }
    private void end(String kind, long id, String result, boolean bulk) {
        switch (kind) {
            case "body" -> { if (bulk) bodyService.endProcesses(List.of(id), result, ""); else bodyService.endProcess(id, result, ""); }
            case "neck" -> { if (bulk) neckService.endProcesses(List.of(id), result, ""); else neckService.endProcess(id, result, ""); }
            default -> { if (bulk) guitarService.endProcesses(List.of(id)); else guitarService.endProcess(id); }
        }
    }
    private void await(CountDownLatch latch) {
        try { assertTrue(latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
    /** 先行処理のロック保持中に後続が送信されることを保証する。 */
    private void collision(Runnable acquire, Runnable first, Runnable second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1), entered = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            Future<?> winner = pool.submit(() -> tx.executeWithoutResult(s -> { acquire.run(); locked.countDown(); await(release); first.run(); }));
            await(locked);
            Future<?> loser = pool.submit(() -> { entered.countDown(); second.run(); });
            await(entered);
            assertThrows(TimeoutException.class, () -> loser.get(200, TimeUnit.MILLISECONDS), "先行操作の確定まで待機する");
            release.countDown(); winner.get(10, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class, () -> loser.get(10, TimeUnit.SECONDS));
            assertInstanceOf(BusinessException.class, failure.getCause());
        } finally { release.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }
    @ParameterizedTest @ValueSource(strings = {"body", "neck", "guitar"})
    void concurrentSingleAndBulkStartAndEndAreNotDuplicated(String kind) throws Exception {
        long[] fixture = tx.execute(s -> {
            ManufacturingProcess p = process(kind, false);
            long id = entity(kind, p, kind.equals("guitar") ? order() : null);
            return new long[]{id, p.getId()};
        });
        long id = fixture[0], processId = fixture[1];
        collision(() -> lock(kind, id), () -> start(kind, id, processId, false), () -> start(kind, id, processId, true));
        String historyTable = kind.equals("guitar") ? "t_process_history" : "t_" + kind + "_process_history";
        long historyId = tx.execute(s -> {
            List<?> ids = em.createNativeQuery("select id from " + historyTable + " where " + kind + "_id=:id").setParameter("id", id).getResultList();
            assertEquals(1, ids.size()); return ((Number) ids.get(0)).longValue();
        });
        String result = kind.equals("body") ? "PASSED" : "COMPLETED";
        collision(() -> em.createNativeQuery("select id from " + historyTable + " where id=:id for update").setParameter("id", historyId).getSingleResult(),
                () -> end(kind, historyId, result, false), () -> end(kind, historyId, result, true));
        tx.executeWithoutResult(s -> assertEquals(1L, ((Number) em.createNativeQuery("select count(*) from " + historyTable + " where " + kind + "_id=:id and end_time is not null").setParameter("id", id).getSingleResult()).longValue()));
    }
    @ParameterizedTest @ValueSource(strings = {"body", "neck", "guitar"})
    void staleBulkSelectionChangesNoneOfTheTargets(String kind) {
        long[] fixture = tx.execute(s -> {
            ManufacturingProcess p = process(kind, false);
            ProductionOrder order = kind.equals("guitar") ? order() : null;
            long a = entity(kind, p, order), b = entity(kind, p, order);
            if (kind.equals("guitar")) em.find(Guitar.class, b).setCurrentProcess("完成");
            else if (kind.equals("body")) em.find(Body.class, b).setStatus("RETURNED");
            else em.find(Neck.class, b).setStatus("RETURNED");
            return new long[]{a, b, p.getId()};
        });
        assertThrows(BusinessException.class, () -> {
            var ids = List.of(fixture[0], fixture[1]);
            switch (kind) {
                case "body" -> bodyService.startProcesses(ids, fixture[2], prefix);
                case "neck" -> neckService.startProcesses(ids, fixture[2], prefix);
                default -> guitarService.startProcesses(ids, fixture[2], prefix);
            }
        });
        tx.executeWithoutResult(s -> {
            String table = kind.equals("guitar") ? "t_process_history" : "t_" + kind + "_process_history";
            assertEquals(0L, ((Number) em.createNativeQuery("select count(*) from " + table + " where " + kind + "_id in (:a,:b)").setParameter("a", fixture[0]).setParameter("b", fixture[1]).getSingleResult()).longValue());
        });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void assemblyRejectsConcurrentReuseOrCancellation(boolean cancel) throws Exception {
        long[] fixture = tx.execute(s -> {
            var bm = new com.example.guitarmes.master.body.BodyMaster(); bm.setModelCode(prefix); bm.setModelName(prefix); persist(bm, "m_body");
            var nm = new com.example.guitarmes.master.neck.NeckMaster(); nm.setModelCode(prefix); nm.setModelName(prefix); persist(nm, "m_neck");
            ProductionOrder order = order(); order.setStartedQuantity(0); order.setStatus("PLANNED");
            order.getProduct().setBodyMaster(bm); order.getProduct().setNeckMaster(nm);
            var schedule = new com.example.guitarmes.productionschedule.ProductionSchedule(order, java.time.LocalDate.now(), 2, "CONFIRMED"); persist(schedule, "t_production_schedule");
            Body body = new Body(); body.setSerialNo(prefix); body.setStatus("AVAILABLE"); body.setBodyMaster(bm); body.setProductionOrder(order); body.setProductionSchedule(schedule); body.setAvailableAt(LocalDateTime.of(2026, 9, 1, 10, 0)); persist(body, "t_body");
            Neck neck = new Neck(); neck.setSerialNo(prefix); neck.setStatus("AVAILABLE"); neck.setNeckMaster(nm); neck.setProductionOrder(order); neck.setProductionSchedule(schedule); neck.setAvailableAt(body.getAvailableAt()); persist(neck, "t_neck");
            return new long[]{order.getId(), schedule.getId(), neck.getId(), body.getId()};
        });
        collision(() -> orders.findForUpdate(fixture[0]).orElseThrow(), () -> {
            var assembly = assemblyService.createAssembly(fixture[0], fixture[1], fixture[2], fixture[3], prefix);
            owned.computeIfAbsent("t_guitar", k -> new ArrayList<>()).add(assembly.getGuitar().getId());
            owned.computeIfAbsent("t_assembly", k -> new ArrayList<>()).add(assembly.getId());
        }, () -> {
            if (cancel) orderLists.cancelProductionOrder(fixture[0]);
            else assemblyService.createAssemblies(fixture[0], fixture[1], List.of(fixture[2]), List.of(fixture[3]), prefix);
        });
        tx.executeWithoutResult(s -> {
            assertEquals(1, em.find(ProductionOrder.class, fixture[0]).getStartedQuantity());
            assertEquals("IN_PROGRESS", em.find(ProductionOrder.class, fixture[0]).getStatus());
            assertEquals("ASSEMBLED", em.find(Body.class, fixture[3]).getStatus());
            assertEquals(LocalDateTime.of(2026, 9, 1, 10, 0), em.find(Body.class, fixture[3]).getAvailableAt());
        });
    }

    public static class PageInspector implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final ThreadLocal<Runnable> beforeContent = new ThreadLocal<>();
        static final ThreadLocal<String> table = new ThreadLocal<>();
        @Override public String inspect(String sql) {
            Runnable action = beforeContent.get();
            if (action != null && sql.startsWith("select ") && !sql.contains("count(") && sql.contains("from " + table.get() + " ")) {
                beforeContent.remove(); action.run();
            }
            return sql;
        }
    }
    @ParameterizedTest @ValueSource(strings = {"body", "neck", "guitar", "production_order"})
    void countAndPageUseSameSnapshotDuringConcurrentDeletion(String kind) throws Exception {
        long id = tx.execute(s -> kind.equals("production_order") ? order().getId()
                : entity(kind, process(kind, false), kind.equals("guitar") ? order() : null));
        ExecutorService writer = Executors.newSingleThreadExecutor();
        java.util.concurrent.atomic.AtomicBoolean deleted = new java.util.concurrent.atomic.AtomicBoolean();
        PageInspector.table.set("t_" + kind);
        PageInspector.beforeContent.set(() -> {
            try {
                writer.submit(() -> tx.executeWithoutResult(s -> em.createNativeQuery("delete from t_" + kind + " where id=:id").setParameter("id", id).executeUpdate())).get(10, TimeUnit.SECONDS);
                deleted.set(true);
            } catch (Exception e) { throw new IllegalStateException(e); }
        });
        try {
            org.springframework.data.domain.Page<?> result = switch (kind) {
                case "body" -> bodyLists.searchBodiesPaged("active", prefix, "", "", "", 0);
                case "neck" -> neckLists.searchNecksPaged("active", prefix, "", "", "", 0);
                case "guitar" -> guitarLists.searchGuitarsPaged(guitarService, "active", prefix, "", "", "", 0);
                default -> orderLists.searchProductionOrdersPaged("active", prefix, "", "", "", "", "", 0);
            };
            assertTrue(deleted.get(), "countの実行後、contentの実行前に別接続で削除する");
            assertEquals(1, result.getTotalElements()); assertEquals(1, result.getNumberOfElements());
        } finally {
            PageInspector.beforeContent.remove(); PageInspector.table.remove(); writer.shutdownNow();
            assertTrue(writer.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test void concurrentCompletionsDoNotLoseOrderQuantity() throws Exception {
        long[] fixture = tx.execute(s -> {
            ProductionOrder order = order(); ManufacturingProcess p = process("guitar", true);
            long a = entity("guitar", p, order), b = entity("guitar", p, order);
            ProcessHistory ha = new ProcessHistory(a, p.getId(), prefix, LocalDateTime.now()); em.persist(ha);
            ProcessHistory hb = new ProcessHistory(b, p.getId(), prefix, LocalDateTime.now()); em.persist(hb); em.flush();
            return new long[]{order.getId(), ha.getId(), hb.getId()};
        });
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch go = new CountDownLatch(1);
        try {
            Future<?> a = pool.submit(() -> { await(go); guitarService.endProcess(fixture[1]); });
            Future<?> b = pool.submit(() -> { await(go); guitarService.endProcesses(List.of(fixture[2])); });
            go.countDown(); a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
        tx.executeWithoutResult(s -> { ProductionOrder order = em.find(ProductionOrder.class, fixture[0]); assertEquals(2, order.getCompletedQuantity()); assertEquals("COMPLETED", order.getStatus()); assertNotNull(order.getCompletedAt()); });
    }
}
