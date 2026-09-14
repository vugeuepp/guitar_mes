package com.example.guitarmes.product.parts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.guitarmes.product.Product;

/** 適用SQLを適用した専用PostgreSQLで検証。自前fixtureは各テストでロールバック。 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("e2e")
class ProductPartsSpecRepositoryTest {

    @Autowired ProductPartsSpecRepository repository;
    @Autowired EntityManager em;

    @BeforeEach
    void guardDatabase() {
        assertEquals("guitar_mes_e2e",
                em.createNativeQuery("select current_database()", String.class).getSingleResult());
    }

    private Product product() {
        Product product = new Product();
        product.setModelNo("SPEC-" + UUID.randomUUID());
        product.setProductName("Spec fixture");
        em.persist(product);
        return product;
    }

    private ProductPartsSpec spec(Product product) {
        ProductPartsSpec spec = new ProductPartsSpec();
        spec.setProduct(product);
        return spec;
    }

    @Test
    void savesAndReloadsAllSpecificationsAndProduct() {
        Product product = product();
        ProductPartsSpec spec = spec(product);
        spec.setBridgeType(BridgeType.TWO_POINT);
        spec.setBridgeModel("Bridge fixture");
        spec.setRequiresStudHoleExpansion(true);
        spec.setTunerModel("Tuner fixture");
        spec.setTunerMountingType(TunerMountingType.NUT_FASTENING);
        spec.setTunerBushRequired(false);
        spec.setTunerLayout(TunerLayout.SIX_IN_LINE);
        spec.setSelectorPositions(5);
        spec.setControlLayout("1Vol / 2Tone");
        spec.setJackMountingType(JackMountingType.BOAT_PLATE);
        spec.setStringMaker("String maker fixture");
        spec.setStringModel("String model fixture");
        spec.setStringGauge(".009-.042");
        repository.saveAndFlush(spec);
        em.clear();

        ProductPartsSpec stored = repository.findByProductId(product.getId()).orElseThrow();
        assertEquals(spec.getId(), stored.getId());
        assertEquals(product.getModelNo(), stored.getProduct().getModelNo());
        assertEquals(BridgeType.TWO_POINT, stored.getBridgeType());
        assertEquals("Bridge fixture", stored.getBridgeModel());
        assertEquals(Boolean.TRUE, stored.getRequiresStudHoleExpansion());
        assertEquals("Tuner fixture", stored.getTunerModel());
        assertEquals(TunerMountingType.NUT_FASTENING, stored.getTunerMountingType());
        assertEquals(Boolean.FALSE, stored.getTunerBushRequired());
        assertEquals(TunerLayout.SIX_IN_LINE, stored.getTunerLayout());
        assertEquals(5, stored.getSelectorPositions());
        assertEquals("1Vol / 2Tone", stored.getControlLayout());
        assertEquals(JackMountingType.BOAT_PLATE, stored.getJackMountingType());
        assertEquals("String maker fixture", stored.getStringMaker());
        assertEquals("String model fixture", stored.getStringModel());
        assertEquals(".009-.042", stored.getStringGauge());
        assertTrue(repository.existsByProductId(product.getId()));

        Object[] codes = (Object[]) em.createNativeQuery("""
                select bridge_type, tuner_mounting_type, tuner_layout, jack_mounting_type
                from m_product_parts_spec where id = :id
                """).setParameter("id", spec.getId()).getSingleResult();
        assertArrayEquals(new Object[]{"TWO_POINT", "NUT_FASTENING", "SIX_IN_LINE", "BOAT_PLATE"}, codes);
    }

    @Test
    void preservesEveryEnumCodeAndBooleanThreeStates() {
        Boolean[] values = {true, false, null};
        for (int i = 0; i < BridgeType.values().length; i++) {
            ProductPartsSpec spec = spec(product());
            spec.setBridgeType(BridgeType.values()[i]);
            spec.setTunerMountingType(TunerMountingType.values()[i % 2]);
            spec.setRequiresStudHoleExpansion(values[i]);
            spec.setTunerBushRequired(values[i]);
            repository.saveAndFlush(spec);
            em.clear();
            ProductPartsSpec stored = repository.findById(spec.getId()).orElseThrow();
            assertEquals(spec.getBridgeType(), stored.getBridgeType());
            assertEquals(spec.getTunerMountingType(), stored.getTunerMountingType());
            assertEquals(values[i], stored.getRequiresStudHoleExpansion());
            assertEquals(values[i], stored.getTunerBushRequired());
            assertEquals(spec.getBridgeType().name(), em.createNativeQuery(
                    "select bridge_type from m_product_parts_spec where id=:id", String.class)
                    .setParameter("id", spec.getId()).getSingleResult());
        }
    }

    @Test
    void productWithoutSpecRemainsValidAndProductLoadingDoesNotQuerySpecs() {
        Product product = product();
        em.flush();
        em.clear();
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean enabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            stats.clear();
            assertEquals(product.getModelNo(), em.find(Product.class, product.getId()).getModelNo());
            assertEquals(1, stats.getPrepareStatementCount());
        } finally {
            stats.setStatisticsEnabled(enabled);
        }
        assertTrue(repository.findByProductId(product.getId()).isEmpty());
        assertFalse(repository.existsByProductId(product.getId()));
    }

    @Test
    void productsHaveIndependentSpecsAndUnsetFieldsRemainNull() {
        Product first = product();
        Product second = product();
        ProductPartsSpec a = repository.saveAndFlush(spec(first));
        ProductPartsSpec b = repository.saveAndFlush(spec(second));
        a.setStringModel("Only first");
        em.flush();
        em.clear();
        assertNotEquals(a.getId(), b.getId());
        assertEquals("Only first", repository.findByProductId(first.getId()).orElseThrow().getStringModel());
        ProductPartsSpec other = repository.findByProductId(second.getId()).orElseThrow();
        assertNull(other.getStringModel());
        assertNull(other.getBridgeType());
        assertNull(other.getTunerMountingType());
        assertNull(other.getTunerLayout());
        assertNull(other.getJackMountingType());
        assertNull(other.getSelectorPositions());
        assertNull(other.getRequiresStudHoleExpansion());
        assertNull(other.getTunerBushRequired());
    }

    @Test
    void databaseRejectsSecondSpecForSameProduct() {
        Product product = product();
        repository.saveAndFlush(spec(product));
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(spec(product)));
    }

    @Test
    void databaseRejectsMissingProductReference() {
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> em.createNativeQuery("""
                insert into m_product_parts_spec (product_id)
                values (-9223372036854775808)
                """).executeUpdate());
        assertEquals("23503", exception.getSQLState());
        assertEquals("fk_product_parts_spec_product", exception.getConstraintName());
    }

    @Test
    void databaseRejectsNullProductReference() {
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> em.createNativeQuery(
                "insert into m_product_parts_spec default values").executeUpdate());
        assertEquals("23502", exception.getSQLState());
    }

    @Test
    void databaseRejectsUnknownEnumCode() {
        Product product = product();
        em.flush();
        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> em.createNativeQuery("""
                insert into m_product_parts_spec (product_id, bridge_type)
                values (:id, 'UNKNOWN')
                """).setParameter("id", product.getId()).executeUpdate());
        assertEquals("23514", exception.getSQLState());
        assertEquals("ck_product_parts_spec_bridge_type", exception.getConstraintName());
    }
}
