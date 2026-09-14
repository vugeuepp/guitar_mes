package com.example.guitarmes.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMaster;
import com.example.guitarmes.master.productseries.ProductSeriesMaster;
import com.example.guitarmes.product.parts.*;
import com.example.guitarmes.productionorder.ProductionOrder;

/** 実Service・Thymeleaf・専用DBを使う。自前fixtureだけを各テストでロールバック。 */
@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@ActiveProfiles("e2e")
@Transactional
class ProductPartsFormIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ProductFormService forms;
    @Autowired ProductService products;
    @Autowired ProductPartsSpecRepository specs;
    @MockitoSpyBean ProductPartsSpecService parts;
    MockMvc mvc;
    String prefix;
    String instrument;

    @BeforeEach
    void setup() {
        assertEquals("guitar_mes_e2e", em.createNativeQuery("select current_database()", String.class).getSingleResult());
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        prefix = "PF" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        instrument = "I" + prefix;
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            em.persist(new ProductSeriesMaster(prefix, "Form test series", true));
            em.persist(new InstrumentTypeMaster(instrument, "Form test instrument", "Stratocaster", "Stratocaster", true));
            em.flush();
        });
    }

    @AfterEach
    void cleanupCommittedFixture() {
        if (TestTransaction.isActive() || prefix == null) return;
        // transactionなしで呼ぶロールバック検証だけ、事前にcommitした自前マスタを片付ける。
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            String code = prefix + "-" + instrument;
            em.createNativeQuery("delete from m_product_parts_spec where product_id in (select id from m_product where internal_model_code=:code)")
                    .setParameter("code", code).executeUpdate();
            em.createNativeQuery("delete from m_product where internal_model_code=:code")
                    .setParameter("code", code).executeUpdate();
            em.createNativeQuery("delete from m_body where product_family_code=:code")
                    .setParameter("code", code).executeUpdate();
            em.createNativeQuery("delete from m_neck where product_family_code=:code")
                    .setParameter("code", code).executeUpdate();
            em.createNativeQuery("delete from m_product_series where series_code=:code")
                    .setParameter("code", prefix).executeUpdate();
            em.createNativeQuery("delete from m_instrument_type where instrument_code=:code")
                    .setParameter("code", instrument).executeUpdate();
        });
    }

    private ProductVariationCreateRequest request(boolean withParts) {
        ProductVariationCreateRequest r = new ProductVariationCreateRequest();
        r.setProductSeries(prefix);
        r.setInstrumentType(instrument);
        r.setInternalModelCode(prefix + "-" + instrument);
        r.setProductName("Form test product");
        r.setBodyType("Stratocaster");
        r.setBodyMaterial("ALDER");
        r.setNeckType("Stratocaster");
        r.setNeckMaterial("MAPLE");
        r.setPickupLayout("SSS");
        r.setFretCount(21);
        r.setScale("GUITAR_LONG");
        r.setVariations(List.of(new ProductVariationRequest(prefix + "-A", "Black", "ROSEWOOD"),
                new ProductVariationRequest(prefix + "-B", "White", "ROSEWOOD")));
        if (withParts) r.setPartsSpec(completeParts());
        return r;
    }

    private ProductPartsSpecRequest completeParts() {
        ProductPartsSpecRequest r = new ProductPartsSpecRequest();
        r.setBridgeType(BridgeType.SIX_POINT);
        r.setRequiresStudHoleExpansion(false);
        r.setTunerModel("Test tuner");
        r.setTunerMountingType(TunerMountingType.PRESS_BUSHING);
        r.setTunerBushRequired(true);
        r.setTunerLayout(TunerLayout.SIX_IN_LINE);
        r.setSelectorPositions(5);
        r.setControlLayout("1Vol / 2Tone");
        r.setJackMountingType(JackMountingType.BOAT_PLATE);
        r.setStringModel("Test strings");
        r.setStringGauge(".009-.042");
        return r;
    }

    private MultiValueMap<String, String> params(Object request) {
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        BeanWrapperImpl bean = new BeanWrapperImpl(request);
        for (String field : List.of("productSeries", "instrumentType", "internalModelCode", "productName",
                "bodyType", "bodyMaterial", "neckType", "neckMaterial", "pickupLayout", "fretCount", "scale",
                "modelNo", "color", "fingerboardMaterial")) {
            if (bean.isReadableProperty(field) && bean.getPropertyValue(field) != null)
                result.add(field, bean.getPropertyValue(field).toString());
        }
        if (request instanceof ProductVariationCreateRequest r) {
            for (int i = 0; i < r.getVariations().size(); i++) {
                ProductVariationRequest v = r.getVariations().get(i);
                result.add("variations[" + i + "].modelNo", v.getModelNo());
                result.add("variations[" + i + "].color", v.getColor());
                result.add("variations[" + i + "].fingerboardMaterial", v.getFingerboardMaterial());
            }
        }
        return result;
    }

    private void addParts(MultiValueMap<String, String> params, ProductPartsSpecRequest request) {
        BeanWrapperImpl bean = new BeanWrapperImpl(request);
        for (String field : List.of("bridgeType", "bridgeModel", "requiresStudHoleExpansion", "tunerModel",
                "tunerMountingType", "tunerBushRequired", "tunerLayout", "selectorPositions", "controlLayout",
                "jackMountingType", "stringMaker", "stringModel", "stringGauge")) {
            Object value = bean.getPropertyValue(field);
            if (value != null) params.add("partsSpec." + field, value.toString());
        }
    }

    private void started(Product product) {
        ProductionOrder order = new ProductionOrder(prefix + "-ORDER", product, 2, YearMonth.of(2026, 9), null, null, "IN_PROGRESS");
        order.setStartedQuantity(1);
        em.persist(order);
        em.flush();
    }

    private String fieldset(String html) {
        int start = html.lastIndexOf("<fieldset", html.indexOf("id=\"partsSpecFields\""));
        assertTrue(start >= 0, "Spec fieldset must render");
        return html.substring(start, html.indexOf('>', start) + 1);
    }

    @Test
    void newFormRendersSelectsAndUnselectedBooleans() throws Exception {
        String html = mvc.perform(get("/products/new")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("name=\"partsSpec.bridgeType\""));
        assertTrue(html.contains("6点支持"));
        assertTrue(html.contains("name=\"partsSpec.tunerBushRequired\""));
        assertTrue(html.contains("未選択"));
        assertTrue(html.indexOf("id=\"partsSpecSection\"") < html.indexOf("id=\"variationList\""));
        assertFalse(fieldset(html).contains("disabled"));
    }

    @Test
    void emptySpecCreatesOnlyProductsThroughController() throws Exception {
        MultiValueMap<String, String> input = params(request(false));
        input.add("partsSpec.bridgeModel", " \t ");
        mvc.perform(post("/products/create").params(input)).andExpect(status().is3xxRedirection());
        Product product = em.createQuery("from Product where modelNo=:model", Product.class)
                .setParameter("model", prefix + "-A").getSingleResult();
        assertFalse(specs.existsByProductId(product.getId()));
    }

    @Test
    void completeSpecCreatesIndependentRowsForEachVariationThroughController() throws Exception {
        ProductVariationCreateRequest r = request(true);
        MultiValueMap<String, String> input = params(r);
        addParts(input, r.getPartsSpec());
        mvc.perform(post("/products/create").params(input)).andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        List<Product> created = em.createQuery("from Product where internalModelCode=:code order by modelNo", Product.class)
                .setParameter("code", r.getInternalModelCode()).getResultList();
        assertEquals(2, created.size());
        ProductPartsSpec a = specs.findByProductId(created.get(0).getId()).orElseThrow();
        ProductPartsSpec b = specs.findByProductId(created.get(1).getId()).orElseThrow();
        assertNotEquals(a.getId(), b.getId());
        assertNotSame(a, b);
        assertEquals(a.getTunerModel(), b.getTunerModel());
        assertEquals(a.getStringGauge(), b.getStringGauge());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void partialInputReturnsFormWithValuesAndRollsBackProducts() throws Exception {
        MultiValueMap<String, String> input = params(request(false));
        input.add("partsSpec.tunerModel", "Keep my tuner");
        var result = mvc.perform(post("/products/create").params(input))
                .andExpect(status().isOk()).andExpect(view().name("product-form"))
                .andExpect(model().attributeExists("errorMessage", "bridgeTypes", "tunerLayouts", "jackMountingTypes"))
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("Keep my tuner"));
        assertTrue(html.contains(prefix + "-A"));
        assertEquals(0L, em.createQuery("select count(p) from Product p where modelNo=:model", Long.class)
                .setParameter("model", prefix + "-A").getSingleResult());
    }

    @Test
    void invalidEnumBindingReturnsInputErrorInsteadOfServerError() throws Exception {
        MultiValueMap<String, String> input = params(request(false));
        input.add("partsSpec.bridgeType", "INVALID");
        input.add("partsSpec.stringModel", "Keep strings");
        var result = mvc.perform(post("/products/create").params(input))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("request", "partsSpec.bridgeType"))
                .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains("Keep strings"));
        assertEquals(0L, em.createQuery("select count(p) from Product p where modelNo=:model", Long.class)
                .setParameter("model", prefix + "-A").getSingleResult());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void secondVariationFailureRollsBackFirstProductAndSpec() {
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(call -> {
            if (attempts.incrementAndGet() == 2) throw new BusinessException("Second Spec failed");
            return call.callRealMethod();
        }).when(parts).create(anyLong(), any());
        assertThrows(BusinessException.class, () -> forms.createProductVariations(request(true)));
        assertEquals(2, attempts.get());
        assertEquals(0L, em.createQuery("select count(p) from Product p where internalModelCode=:code", Long.class)
                .setParameter("code", prefix + "-" + instrument).getSingleResult());
        assertEquals(0L, em.createNativeQuery("select count(*) from m_product_parts_spec s join m_product p on p.id=s.product_id where p.internal_model_code=:code", Long.class)
                .setParameter("code", prefix + "-" + instrument).getSingleResult());
    }

    @Test
    void emptySpecEditStaysEmptyAndManufacturedProductAllowsFirstSupplement() throws Exception {
        Product product = forms.createProductVariations(request(false)).get(0);
        String html = mvc.perform(get("/products/" + product.getId() + "/edit"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertFalse(fieldset(html).contains("disabled"));
        MultiValueMap<String, String> input = params(forms.getProductUpdateRequest(product.getId()));
        mvc.perform(post("/products/" + product.getId() + "/edit").params(input))
                .andExpect(status().is3xxRedirection());
        assertFalse(specs.existsByProductId(product.getId()));
        started(product);
        assertFalse(forms.isPartsLocked(product.getId()));
        addParts(input, completeParts());
        mvc.perform(post("/products/" + product.getId() + "/edit").params(input))
                .andExpect(status().is3xxRedirection());
        assertTrue(specs.existsByProductId(product.getId()));
    }

    @Test
    void existingSpecLoadsAndUpdatesBeforeProduction() throws Exception {
        Product product = forms.createProductVariations(request(true)).get(0);
        String html = mvc.perform(get("/products/" + product.getId() + "/edit"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Test tuner"));
        assertFalse(fieldset(html).contains("disabled"));
        ProductUpdateRequest request = forms.getProductUpdateRequest(product.getId());
        request.getPartsSpec().setStringModel("Updated strings");
        MultiValueMap<String, String> input = params(request);
        addParts(input, request.getPartsSpec());
        mvc.perform(post("/products/" + product.getId() + "/edit").params(input))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        assertEquals("Updated strings", specs.findByProductId(product.getId()).orElseThrow().getStringModel());
    }

    @Test
    void lockedSpecIsDisabledButProductNameCanBeSaved() throws Exception {
        Product product = forms.createProductVariations(request(true)).get(0);
        started(product);
        String html = mvc.perform(get("/products/" + product.getId() + "/edit"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(fieldset(html).contains("disabled"));
        assertTrue(html.contains("製造開始済みのためパーツ取付仕様は変更できません"));
        ProductUpdateRequest request = forms.getProductUpdateRequest(product.getId());
        request.setProductName("Renamed product");
        mvc.perform(post("/products/" + product.getId() + "/edit").params(params(request)))
                .andExpect(status().is3xxRedirection());
        assertEquals("Renamed product", product.getProductName());
        assertEquals("Test strings", specs.findByProductId(product.getId()).orElseThrow().getStringModel());
    }

    @Test
    void forgedPostCannotUpdateLockedSpec() throws Exception {
        Product product = forms.createProductVariations(request(true)).get(0);
        started(product);
        ProductUpdateRequest request = forms.getProductUpdateRequest(product.getId());
        request.getPartsSpec().setStringModel("Tampered strings");
        MultiValueMap<String, String> input = params(request);
        addParts(input, request.getPartsSpec());
        String html = mvc.perform(post("/products/" + product.getId() + "/edit").params(input))
                .andExpect(status().isOk()).andExpect(model().attributeExists("errorMessage"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Tampered strings"));
        assertTrue(fieldset(html).contains("disabled"));
        assertEquals("Test strings", specs.findByProductId(product.getId()).orElseThrow().getStringModel());
    }

    @Test
    void editValidationErrorPreservesBothProductAndPartsInput() throws Exception {
        Product product = forms.createProductVariations(request(true)).get(0);
        MultiValueMap<String, String> input = params(forms.getProductUpdateRequest(product.getId()));
        input.set("productName", "Keep product name");
        input.add("partsSpec.tunerModel", "Keep tuner");
        String html = mvc.perform(post("/products/" + product.getId() + "/edit").params(input))
                .andExpect(status().isOk()).andExpect(model().attributeExists("bridgeTypes", "errorMessage"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("Keep product name"));
        assertTrue(html.contains("Keep tuner"));
    }
}
