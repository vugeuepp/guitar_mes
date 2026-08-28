package com.example.guitarmes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.example.guitarmes.entity.Product;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @TempDir
    Path tempDirectory;

    @Mock
    ProductRepository productRepository;

    ProductImageService service;
    Product product;

    @BeforeEach
    void setUp() {
        service = new ProductImageService(
                productRepository,
                tempDirectory.toString());
        product = new Product();
        product.setId(1L);
        lenient().when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempDirectory)) {
            try (var paths = Files.walk(tempDirectory)) {
                paths.sorted((a, b) -> b.compareTo(a))
                        .filter(path -> !path.equals(tempDirectory))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException exception) {
                                throw new RuntimeException(exception);
                            }
                        });
            }
        }
    }

    @Test
    @DisplayName("JPEG画像を保存できる")
    void saveProductImage_jpeg_succeeds() {
        when(productRepository.save(product)).thenReturn(product);
        MockMultipartFile file = file("image.jpg", "image/jpeg", new byte[] {1, 2, 3});
        Product result = service.saveProductImage(1L, file);
        assertTrue(result.getImageFileName().endsWith(".jpg"));
        assertTrue(Files.exists(tempDirectory.resolve(result.getImageFileName())));
    }

    @Test
    @DisplayName("PNG画像を保存できる")
    void saveProductImage_png_succeeds() {
        when(productRepository.save(product)).thenReturn(product);
        Product result = service.saveProductImage(
                1L, file("image.png", "image/png", new byte[] {1}));
        assertTrue(result.getImageFileName().endsWith(".png"));
    }

    @Test
    @DisplayName("WebP画像を保存できる")
    void saveProductImage_webp_succeeds() {
        when(productRepository.save(product)).thenReturn(product);
        Product result = service.saveProductImage(
                1L, file("image.webp", "image/webp", new byte[] {1}));
        assertTrue(result.getImageFileName().endsWith(".webp"));
    }

    @Test
    @DisplayName("空ファイルを拒否する")
    void saveProductImage_empty_throws() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveProductImage(
                        1L, file("empty.jpg", "image/jpeg", new byte[0])));
        assertTrue(exception.getMessage().contains("選択"));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("5MBを超える画像を拒否する")
    void saveProductImage_tooLarge_throws() {
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveProductImage(
                        1L, file("large.jpg", "image/jpeg", content)));
        assertTrue(exception.getMessage().contains("5MB"));
    }

    @Test
    @DisplayName("許可されていない形式を拒否する")
    void saveProductImage_invalidType_throws() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveProductImage(
                        1L, file("image.gif", "image/gif", new byte[] {1})));
        assertTrue(exception.getMessage().contains("JPEG"));
    }

    @Test
    @DisplayName("差し替え後に旧画像を削除する")
    void saveProductImage_replacement_deletesOldFile() throws IOException {
        String oldName = "product-1-old.jpg";
        product.setImageFileName(oldName);
        Files.write(tempDirectory.resolve(oldName), new byte[] {9});
        when(productRepository.save(product)).thenReturn(product);
        Product result = service.saveProductImage(
                1L, file("new.png", "image/png", new byte[] {1}));
        assertFalse(Files.exists(tempDirectory.resolve(oldName)));
        assertTrue(Files.exists(tempDirectory.resolve(result.getImageFileName())));
    }

    @Test
    @DisplayName("画像を削除できる")
    void deleteProductImage_succeeds() throws IOException {
        String oldName = "product-1-old.jpg";
        product.setImageFileName(oldName);
        Files.write(tempDirectory.resolve(oldName), new byte[] {9});
        when(productRepository.save(product)).thenReturn(product);
        Product result = service.deleteProductImage(1L);
        assertNull(result.getImageFileName());
        assertFalse(Files.exists(tempDirectory.resolve(oldName)));
    }

    @Test
    @DisplayName("存在しないProductを拒否する")
    void saveProductImage_unknownProduct_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.saveProductImage(
                        99L, file("image.jpg", "image/jpeg", new byte[] {1})));
    }

    @Test
    @DisplayName("生成ファイル名にProduct IDを含める")
    void saveProductImage_fileNameContainsProductId() {
        when(productRepository.save(product)).thenReturn(product);
        Product result = service.saveProductImage(
                1L, file("../../unsafe.jpg", "image/jpeg", new byte[] {1}));
        assertTrue(result.getImageFileName().startsWith("product-1-"));
        assertFalse(result.getImageFileName().contains("unsafe"));
    }

    private MockMultipartFile file(
            String name,
            String contentType,
            byte[] content) {
        return new MockMultipartFile(
                "imageFile",
                name,
                contentType,
                content);
    }
}
