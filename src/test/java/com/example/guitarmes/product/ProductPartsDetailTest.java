package com.example.guitarmes.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMasterService;
import com.example.guitarmes.master.productseries.ProductSeriesMasterService;
import com.example.guitarmes.product.image.ProductImageService;
import com.example.guitarmes.product.parts.BridgeType;
import com.example.guitarmes.product.parts.JackMountingType;
import com.example.guitarmes.product.parts.ProductPartsSpec;
import com.example.guitarmes.product.parts.ProductPartsSpecService;
import com.example.guitarmes.product.parts.TunerLayout;
import com.example.guitarmes.product.parts.TunerMountingType;

/** DBを使わず、Controllerから実際のThymeleafテンプレートまで描画する。 */
@ExtendWith(MockitoExtension.class)
class ProductPartsDetailTest {

    @Mock private ProductService productService;
    @Mock private ProductPartsSpecService partsService;
    @Mock private ProductFormService formService;
    @Mock private GuitarService guitarService;
    @Mock private ProductSeriesMasterService seriesService;
    @Mock private InstrumentTypeMasterService instrumentService;
    @Mock private ProductImageService imageService;

    private MockMvc mvc;
    private Product product;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new ProductViewController(
                productService, guitarService, seriesService, instrumentService,
                imageService, formService, partsService)).setViewResolvers(views).build();
        product = new Product();
        product.setId(10L);
        product.setProductName("Test Product");
        product.setPickupLayout("HSS");
        when(productService.getProductById(10L)).thenReturn(product);
        when(guitarService.getGuitarsByProductId(10L)).thenReturn(List.of());
    }

    @Test
    void registeredSpecRendersLabelsAndValuesInFourReadOnlyGroups() throws Exception {
        ProductPartsSpec spec = spec();
        spec.setBridgeModel(null);
        spec.setStringMaker("   ");
        when(partsService.findByProductId(10L)).thenReturn(Optional.of(spec));
        product.setImageFileName("test.jpg");

        String html = detail();
        String card = card(html);
        assertThat(card).contains("PARTS SPECIFICATIONS", "パーツ取付仕様",
                ">Bridge<", ">Tuner<", ">Electronics<", ">String<",
                "6点支持", "圧入ブッシュ式", "6連", "舟形ジャックプレート",
                "ブッシュ要否", ">必要<", ">不要<", "Test Tuner", "Test Strings", "09-42", "1Vol. 2Tone", ">5<");
        assertThat(card).doesNotContain("SIX_POINT", "PRESS_BUSHING", "SIX_IN_LINE", "BOAT_PLATE",
                ">true<", ">false<", "null", "<input", "<select", "<form");
        assertThat(card.split("class=\"detail-value\">-</div>", -1)).hasSize(3);
        assertThat(card.substring(card.indexOf(">Electronics<"), card.indexOf(">String<")))
                .contains("PU構成", ">HSS<");
        assertThat(html.indexOf("Neck Master")).isLessThan(html.indexOf("id=\"parts-specifications\""));
        assertThat(html.indexOf("id=\"parts-specifications\"")).isLessThan(html.indexOf("Related Guitars"));
        assertThat(html).contains("/product-images/test.jpg", "/products/10/edit", "/products/10/image/delete");
        verify(partsService).findByProductId(10L);
        verifyNoInteractions(formService);
    }

    @Test
    void absentSpecStillShowsCardWithoutInferringFromProduct() throws Exception {
        when(partsService.findByProductId(10L)).thenReturn(Optional.empty());
        String html = detail();
        assertThat(card(html)).contains("パーツ取付仕様：未設定")
                .doesNotContain(">Bridge<", "HSS", "6点支持");
        assertThat(html).contains("Product Information", "Body Master", "Neck Master", "Related Guitars");
    }

    @Test
    void allNullableSpecFieldsAndBlankPickupRenderAsDashes() throws Exception {
        product.setPickupLayout("  ");
        when(partsService.findByProductId(10L)).thenReturn(Optional.of(new ProductPartsSpec()));
        String card = card(detail());
        assertThat(card).doesNotContain("null", "パーツ取付仕様：未設定");
        assertThat(card.split("class=\"detail-value\">-</div>", -1)).hasSize(15);
    }

    @Test
    void otherEnumLabelsAndReverseBooleanValuesRender() throws Exception {
        ProductPartsSpec spec = spec();
        spec.setBridgeType(BridgeType.TWO_POINT);
        spec.setTunerMountingType(TunerMountingType.NUT_FASTENING);
        spec.setRequiresStudHoleExpansion(true);
        spec.setTunerBushRequired(false);
        when(partsService.findByProductId(10L)).thenReturn(Optional.of(spec));
        String card = card(detail());
        assertThat(card).contains("2点支持", "ナット固定式");
        assertThat(card.substring(card.indexOf(">Bridge<"), card.indexOf(">Tuner<"))).contains(">必要<");
        assertThat(card.substring(card.indexOf(">Tuner<"), card.indexOf(">Electronics<"))).contains(">不要<");
        spec.setBridgeType(BridgeType.FLOYD_ROSE);
        assertThat(card(detail())).contains("Floyd Rose").doesNotContain("FLOYD_ROSE");
    }

    @Test
    void imageUploadErrorRetainsPartsSpecificationCard() throws Exception {
        when(partsService.findByProductId(10L)).thenReturn(Optional.of(spec()));
        when(imageService.saveProductImage(eq(10L), any()))
                .thenThrow(new BusinessException("画像形式を確認してください。"));
        String html = mvc.perform(multipart("/products/10/image").file(new MockMultipartFile(
                        "imageFile", "bad.txt", "text/plain", new byte[] {1})))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("画像形式を確認してください。");
        assertThat(card(html)).contains("6点支持", "Test Tuner");
    }

    private String detail() throws Exception {
        return mvc.perform(get("/products/10/view")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String card(String html) {
        int start = html.indexOf("id=\"parts-specifications\"");
        assertThat(start).isGreaterThanOrEqualTo(0);
        return html.substring(start, html.indexOf("</section>", start));
    }

    private ProductPartsSpec spec() {
        ProductPartsSpec spec = new ProductPartsSpec();
        spec.setBridgeType(BridgeType.SIX_POINT);
        spec.setRequiresStudHoleExpansion(false);
        spec.setTunerModel("Test Tuner");
        spec.setTunerMountingType(TunerMountingType.PRESS_BUSHING);
        spec.setTunerBushRequired(true);
        spec.setTunerLayout(TunerLayout.SIX_IN_LINE);
        spec.setSelectorPositions(5);
        spec.setControlLayout("1Vol. 2Tone");
        spec.setJackMountingType(JackMountingType.BOAT_PLATE);
        spec.setStringModel("Test Strings");
        spec.setStringGauge("09-42");
        return spec;
    }
}
