package com.example.guitarmes.common;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EntityTimestampTest {
    @ParameterizedTest
    @ValueSource(strings = {"productionorder.ProductionOrder", "body.Body", "neck.Neck", "guitar.Guitar"})
    void lifecycleKeepsCreationAndExplicitEventTimes(String name) throws Exception {
        Class<?> type = Class.forName("com.example.guitarmes." + name);
        Object entity = type.getConstructor().newInstance();
        LocalDateTime before = LocalDateTime.now();
        type.getMethod("prePersist").invoke(entity);
        LocalDateTime created = (LocalDateTime) type.getMethod("getCreatedAt").invoke(entity);
        assertNotNull(created);
        assertFalse(created.isBefore(before));
        assertEquals(created, type.getMethod("getUpdatedAt").invoke(entity));
        type.getMethod("rememberUpdatedAt").invoke(entity);

        LocalDateTime old = LocalDateTime.of(2020, 1, 1, 0, 0);
        type.getMethod("setUpdatedAt", LocalDateTime.class).invoke(entity, old);
        type.getMethod("rememberUpdatedAt").invoke(entity); // Entity loaded from DB
        type.getMethod("preUpdate").invoke(entity); // ordinary edit
        assertTrue(((LocalDateTime) type.getMethod("getUpdatedAt").invoke(entity)).isAfter(old));
        assertEquals(created, type.getMethod("getCreatedAt").invoke(entity));
        type.getMethod("rememberUpdatedAt").invoke(entity);

        LocalDateTime event = LocalDateTime.of(2021, 2, 3, 4, 5);
        type.getMethod("setUpdatedAt", LocalDateTime.class).invoke(entity, event);
        type.getMethod("preUpdate").invoke(entity);
        assertEquals(event, type.getMethod("getUpdatedAt").invoke(entity));
        type.getMethod("rememberUpdatedAt").invoke(entity);
        type.getMethod("preUpdate").invoke(entity);
        assertTrue(((LocalDateTime) type.getMethod("getUpdatedAt").invoke(entity)).isAfter(event));
    }

    @ParameterizedTest
    @ValueSource(strings = {"productionorder.ProductionOrder", "body.Body", "neck.Neck", "guitar.Guitar"})
    void legacyNullsAndReachedDateAreNotBackfilledByCallbacks(String name) throws Exception {
        Class<?> type = Class.forName("com.example.guitarmes." + name);
        Object entity = type.getConstructor().newInstance();
        String event = name.startsWith("body.") || name.startsWith("neck.") ? "AvailableAt" : "CompletedAt";
        type.getMethod("rememberUpdatedAt").invoke(entity);
        assertNull(type.getMethod("getCreatedAt").invoke(entity));
        assertNull(type.getMethod("getUpdatedAt").invoke(entity));
        type.getMethod("preUpdate").invoke(entity);
        assertNull(type.getMethod("getCreatedAt").invoke(entity));
        assertNull(type.getMethod("get" + event).invoke(entity));
        LocalDateTime reached = LocalDateTime.of(2020, 1, 1, 0, 0);
        type.getMethod("set" + event, LocalDateTime.class).invoke(entity, reached);
        type.getMethod("rememberUpdatedAt").invoke(entity);
        type.getMethod("preUpdate").invoke(entity);
        assertEquals(reached, type.getMethod("get" + event).invoke(entity));
        Object imported = type.getConstructor().newInstance();
        type.getMethod("setCreatedAt", LocalDateTime.class).invoke(imported, reached);
        type.getMethod("prePersist").invoke(imported);
        assertEquals(reached, type.getMethod("getCreatedAt").invoke(imported));
    }
}
