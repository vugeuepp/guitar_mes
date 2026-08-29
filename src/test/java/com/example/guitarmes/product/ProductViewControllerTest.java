package com.example.guitarmes.product;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.master.instrumenttype.InstrumentTypeMasterService;
import com.example.guitarmes.master.productseries.ProductSeriesMasterService;
import com.example.guitarmes.product.image.ProductImageService;

@ExtendWith(MockitoExtension.class)
class ProductViewControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private GuitarService guitarService;

    @Mock
    private ProductSeriesMasterService productSeriesMasterService;

    @Mock
    private InstrumentTypeMasterService instrumentTypeMasterService;

    @Mock
    private ProductImageService productImageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductViewController controller =
                new ProductViewController(
                        productService,
                        guitarService,
                        productSeriesMasterService,
                        instrumentTypeMasterService,
                        productImageService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("製品一覧画面を表示できる")
    void productList_succeeds() throws Exception {
        when(productService.searchProducts(null))
                .thenReturn(List.of(createProduct()));

        mockMvc.perform(get("/products/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-list"))
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(model().attribute("keyword", (Object) null));
    }

    @Test
    @DisplayName("検索キーワードを製品一覧へ渡せる")
    void productList_withKeyword_succeeds() throws Exception {
        when(productService.searchProducts("Hybrid"))
                .thenReturn(List.of(createProduct()));

        mockMvc.perform(get("/products/view")
                        .param("keyword", "Hybrid"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-list"))
                .andExpect(model().attribute("products", hasSize(1)))
                .andExpect(model().attribute("keyword", "Hybrid"));

        verify(productService).searchProducts("Hybrid");
    }

    @Test
    @DisplayName("製品登録画面を初期バリエーション付きで表示できる")
    void newProductForm_succeeds() throws Exception {
        when(productSeriesMasterService
                .getActiveProductSeriesMasters())
                .thenReturn(List.of());
        when(instrumentTypeMasterService
                .getActiveInstrumentTypeMasters())
                .thenReturn(List.of());

        mockMvc.perform(get("/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-form"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute(
                        "request",
                        org.hamcrest.Matchers.hasProperty(
                                "variations",
                                hasSize(1))))
                .andExpect(model().attributeExists(
                        "productSeriesList",
                        "instrumentTypeList",
                        "bodyMaterialTypeList",
                        "neckMaterialTypeList",
                        "fingerboardMaterialTypeList",
                        "fretCountTypeList",
                        "scaleLengthTypeList"));
    }

    @Test
    @DisplayName("製品を登録して一覧へリダイレクトできる")
    void createProduct_succeeds() throws Exception {
        mockMvc.perform(post("/products/create")
                        .param("productSeries", "MIJ-H2")
                        .param("instrumentType", "ST")
                        .param("internalModelCode", "MIJ-H2-ST")
                        .param("productName", "Hybrid II Stratocaster")
                        .param("bodyType", "Stratocaster")
                        .param("bodyMaterial", "ALDER")
                        .param("neckType", "Stratocaster")
                        .param("neckMaterial", "MAPLE")
                        .param("pickupLayout", "SSS")
                        .param("fretCount", "22")
                        .param("scale", "LONG")
                        .param("variations[0].modelNo", "TEST-0001")
                        .param("variations[0].color", "Black")
                        .param("variations[0].fingerboardMaterial", "ROSEWOOD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/view"));

        verify(productService)
                .createProductVariations(
                        any(ProductVariationCreateRequest.class));
    }

    @Test
    @DisplayName("製品詳細画面を表示できる")
    void productDetail_succeeds() throws Exception {
        Product product = createProduct();
        when(productService.getProductById(10L))
                .thenReturn(product);
        when(guitarService.getGuitarsByProductId(10L))
                .thenReturn(List.of());

        mockMvc.perform(get("/products/10/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attribute("guitars", hasSize(0)));
    }

    @Test
    @DisplayName("製品編集画面を現在値と選択肢付きで表示できる")
    void editProductForm_succeeds() throws Exception {
        ProductUpdateRequest request = createUpdateRequest();
        when(productService.getProductUpdateRequest(10L))
                .thenReturn(request);
        when(productSeriesMasterService
                .getProductSeriesMastersForEdit("MIJ-H2"))
                .thenReturn(List.of());
        when(instrumentTypeMasterService
                .getInstrumentTypeMastersForEdit("ST"))
                .thenReturn(List.of());

        mockMvc.perform(get("/products/10/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-edit-form"))
                .andExpect(model().attribute("request", request))
                .andExpect(model().attribute("productId", 10L))
                .andExpect(model().attributeExists(
                        "productSeriesList",
                        "instrumentTypeList",
                        "bodyMaterialTypeList",
                        "neckMaterialTypeList",
                        "fingerboardMaterialTypeList",
                        "fretCountTypeList",
                        "scaleLengthTypeList"));
    }

    @Test
    @DisplayName("製品を更新して詳細へリダイレクトできる")
    void updateProduct_succeeds() throws Exception {
        mockMvc.perform(post("/products/10/edit")
                        .param("productSeries", "MIJ-H2")
                        .param("instrumentType", "ST")
                        .param("internalModelCode", "MIJ-H2-ST")
                        .param("productName", "Updated Product")
                        .param("bodyType", "Stratocaster")
                        .param("bodyMaterial", "ALDER")
                        .param("neckType", "Stratocaster")
                        .param("neckMaterial", "MAPLE")
                        .param("pickupLayout", "SSS")
                        .param("fretCount", "22")
                        .param("scale", "LONG")
                        .param("modelNo", "TEST-0001")
                        .param("color", "Black")
                        .param("fingerboardMaterial", "ROSEWOOD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10/view"));

        verify(productService)
                .updateProduct(
                        eq(10L),
                        any(ProductUpdateRequest.class));
    }

    @Test
    @DisplayName("製品更新時の業務エラーを編集画面へ表示できる")
    void updateProduct_businessError_returnsEditForm()
            throws Exception {

        when(productService.updateProduct(
                eq(10L),
                any(ProductUpdateRequest.class)))
                .thenThrow(new BusinessException(
                        "公式モデル番号は既に使用されています。"));
        when(productSeriesMasterService
                .getProductSeriesMastersForEdit("MIJ-H2"))
                .thenReturn(List.of());
        when(instrumentTypeMasterService
                .getInstrumentTypeMastersForEdit("ST"))
                .thenReturn(List.of());

        mockMvc.perform(post("/products/10/edit")
                        .param("productSeries", "MIJ-H2")
                        .param("instrumentType", "ST")
                        .param("internalModelCode", "MIJ-H2-ST")
                        .param("productName", "Updated Product")
                        .param("bodyType", "Stratocaster")
                        .param("bodyMaterial", "ALDER")
                        .param("neckType", "Stratocaster")
                        .param("neckMaterial", "MAPLE")
                        .param("pickupLayout", "SSS")
                        .param("fretCount", "22")
                        .param("scale", "LONG")
                        .param("modelNo", "DUPLICATE")
                        .param("color", "Black")
                        .param("fingerboardMaterial", "ROSEWOOD"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-edit-form"))
                .andExpect(model().attribute("productId", 10L))
                .andExpect(model().attribute(
                        "errorMessage",
                        "公式モデル番号は既に使用されています。"))
                .andExpect(model().attributeExists(
                        "productSeriesList",
                        "instrumentTypeList",
                        "bodyMaterialTypeList",
                        "neckMaterialTypeList",
                        "fingerboardMaterialTypeList",
                        "fretCountTypeList",
                        "scaleLengthTypeList"));
    }

    @Test
    @DisplayName("製品画像を登録して詳細へリダイレクトできる")
    void uploadProductImage_succeeds() throws Exception {
        MockMultipartFile imageFile =
                new MockMultipartFile(
                        "imageFile",
                        "product.jpg",
                        "image/jpeg",
                        "image-data".getBytes());

        mockMvc.perform(multipart("/products/10/image")
                        .file(imageFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10/view"));

        verify(productImageService)
                .saveProductImage(
                        eq(10L),
                        any(org.springframework.web.multipart.MultipartFile.class));
    }

    @Test
    @DisplayName("製品画像登録時の業務エラーを詳細画面へ表示できる")
    void uploadProductImage_businessError_returnsDetail()
            throws Exception {

        MockMultipartFile imageFile =
                new MockMultipartFile(
                        "imageFile",
                        "product.txt",
                        "text/plain",
                        "invalid-data".getBytes());
        Product product = createProduct();

        when(productImageService.saveProductImage(
                eq(10L),
                any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(new BusinessException(
                        "JPEG、PNG、WebP形式の画像を選択してください。"));
        when(productService.getProductById(10L))
                .thenReturn(product);
        when(guitarService.getGuitarsByProductId(10L))
                .thenReturn(List.of());

        mockMvc.perform(multipart("/products/10/image")
                        .file(imageFile))
                .andExpect(status().isOk())
                .andExpect(view().name("product-detail"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attribute("guitars", hasSize(0)))
                .andExpect(model().attribute(
                        "imageErrorMessage",
                        "JPEG、PNG、WebP形式の画像を選択してください。"));
    }

    @Test
    @DisplayName("製品画像を削除して詳細へリダイレクトできる")
    void deleteProductImage_succeeds() throws Exception {
        mockMvc.perform(post("/products/10/image/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products/10/view"));

        verify(productImageService)
                .deleteProductImage(10L);
    }

    private Product createProduct() {
        Product product = new Product();
        product.setId(10L);
        product.setModelNo("TEST-0001");
        product.setInternalModelCode("MIJ-H2-ST");
        product.setProductName("Hybrid II Stratocaster");
        product.setColor("Black");
        product.setBodyMaterial("Alder");
        product.setNeckMaterial("Maple");
        product.setFingerboardMaterial("Rosewood");
        product.setPickupLayout("SSS");
        product.setFretCount(22);
        product.setScale("25.5");
        return product;
    }

    private ProductUpdateRequest createUpdateRequest() {
        ProductUpdateRequest request =
                new ProductUpdateRequest();
        request.setProductSeries("MIJ-H2");
        request.setInstrumentType("ST");
        request.setInternalModelCode("MIJ-H2-ST");
        request.setProductName("Hybrid II Stratocaster");
        request.setBodyType("Stratocaster");
        request.setBodyMaterial("ALDER");
        request.setNeckType("Stratocaster");
        request.setNeckMaterial("MAPLE");
        request.setPickupLayout("SSS");
        request.setFretCount(22);
        request.setScale("LONG");
        request.setModelNo("TEST-0001");
        request.setColor("Black");
        request.setFingerboardMaterial("ROSEWOOD");
        return request;
    }
}
