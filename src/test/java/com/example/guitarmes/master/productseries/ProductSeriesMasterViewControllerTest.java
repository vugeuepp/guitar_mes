package com.example.guitarmes.master.productseries;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.guitarmes.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ProductSeriesMasterViewControllerTest {

    @Mock
    private ProductSeriesMasterService productSeriesMasterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductSeriesMasterViewController controller =
                new ProductSeriesMasterViewController(
                        productSeriesMasterService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("製品シリーズ一覧画面を表示できる")
    void productSeriesList_succeeds() throws Exception {
        when(productSeriesMasterService
                .getProductSeriesMasters())
                .thenReturn(List.of(createProductSeries()));

        mockMvc.perform(get("/product-series/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-series-list"))
                .andExpect(model().attribute(
                        "productSeriesMasters",
                        hasSize(1)));
    }

    @Test
    @DisplayName("製品シリーズ登録画面を表示できる")
    void newProductSeriesForm_succeeds() throws Exception {
        mockMvc.perform(get("/product-series/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-series-form"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    @DisplayName("製品シリーズを登録して一覧へリダイレクトできる")
    void createProductSeries_succeeds() throws Exception {
        mockMvc.perform(post("/product-series/create")
                        .param("seriesCode", "MIJ-HER70")
                        .param(
                                "seriesName",
                                "Made in Japan Heritage 70s"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product-series/view"));

        verify(productSeriesMasterService)
                .createProductSeriesMaster(
                        any(ProductSeriesMaster.class));
    }

    @Test
    @DisplayName("製品シリーズ登録時の業務エラーを登録画面へ表示できる")
    void createProductSeries_businessError_returnsForm()
            throws Exception {

        when(productSeriesMasterService
                .createProductSeriesMaster(
                        any(ProductSeriesMaster.class)))
                .thenThrow(new BusinessException(
                        "シリーズコードは既に登録されています。"));

        mockMvc.perform(post("/product-series/create")
                        .param("seriesCode", "MIJ-H2")
                        .param(
                                "seriesName",
                                "Made in Japan Hybrid II"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-series-form"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute(
                        "errorMessage",
                        "シリーズコードは既に登録されています。"));
    }

    @Test
    @DisplayName("製品シリーズ編集画面を現在値付きで表示できる")
    void editProductSeriesForm_succeeds() throws Exception {
        ProductSeriesMaster productSeries =
                createProductSeries();
        when(productSeriesMasterService
                .getProductSeriesMasterById(1L))
                .thenReturn(productSeries);

        mockMvc.perform(get("/product-series/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "product-series-edit-form"))
                .andExpect(model().attribute(
                        "request",
                        productSeries))
                .andExpect(model().attribute(
                        "productSeriesId",
                        1L));
    }

    @Test
    @DisplayName("製品シリーズを更新して一覧へリダイレクトできる")
    void updateProductSeries_succeeds() throws Exception {
        mockMvc.perform(post("/product-series/1/edit")
                        .param("seriesCode", "MIJ-H2")
                        .param(
                                "seriesName",
                                "Made in Japan Hybrid II Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product-series/view"));

        verify(productSeriesMasterService)
                .updateProductSeriesMaster(
                        eq(1L),
                        any(ProductSeriesMaster.class));
    }

    @Test
    @DisplayName("製品シリーズ更新時の業務エラーを編集画面へ表示できる")
    void updateProductSeries_businessError_returnsEditForm()
            throws Exception {

        when(productSeriesMasterService
                .updateProductSeriesMaster(
                        eq(1L),
                        any(ProductSeriesMaster.class)))
                .thenThrow(new BusinessException(
                        "シリーズ名を入力してください。"));

        mockMvc.perform(post("/product-series/1/edit")
                        .param("seriesCode", "MIJ-H2")
                        .param("seriesName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "product-series-edit-form"))
                .andExpect(model().attribute(
                        "productSeriesId",
                        1L))
                .andExpect(model().attribute(
                        "errorMessage",
                        "シリーズ名を入力してください。"))
                .andExpect(model().attribute(
                        "request",
                        org.hamcrest.Matchers.hasProperty(
                                "id",
                                org.hamcrest.Matchers.is(1L))));
    }

    @Test
    @DisplayName("製品シリーズの有効状態を切り替えて一覧へリダイレクトできる")
    void toggleProductSeriesActive_succeeds() throws Exception {
        mockMvc.perform(post(
                        "/product-series/1/toggle-active"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product-series/view"));

        verify(productSeriesMasterService)
                .toggleProductSeriesMasterActive(1L);
    }

    private ProductSeriesMaster createProductSeries() {
        ProductSeriesMaster productSeries =
                new ProductSeriesMaster(
                        "MIJ-H2",
                        "Made in Japan Hybrid II",
                        true);
        productSeries.setId(1L);
        return productSeries;
    }
}
