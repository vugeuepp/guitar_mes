package com.example.guitarmes.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.guitarmes.entity.Product;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.ProductRepository;

@Service
public class ProductImageService {

    private static final long MAX_FILE_SIZE =
            5L * 1024L * 1024L;

    private static final Map<String, String> ALLOWED_TYPES =
            Map.of(
                    "image/jpeg", "jpg",
                    "image/png", "png",
                    "image/webp", "webp");

    private final ProductRepository productRepository;
    private final Path storageDirectory;

    public ProductImageService(
            ProductRepository productRepository,
            @Value("${app.product-image.storage-path}")
            String storagePath) {

        this.productRepository = productRepository;
        this.storageDirectory =
                Path.of(storagePath)
                        .toAbsolutePath()
                        .normalize();
    }

    @Transactional
    public Product saveProductImage(
            Long productId,
            MultipartFile imageFile) {

        Product product = getProduct(productId);
        validateImageFile(imageFile);

        String extension = ALLOWED_TYPES.get(
                imageFile.getContentType()
                        .toLowerCase(Locale.ROOT));
        String newFileName =
                "product-"
                + productId
                + "-"
                + UUID.randomUUID()
                + "."
                + extension;
        Path newFile = resolveFile(newFileName);
        String oldFileName = product.getImageFileName();

        try {
            Files.createDirectories(storageDirectory);
            try (InputStream inputStream =
                    imageFile.getInputStream()) {
                Files.copy(
                        inputStream,
                        newFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            product.setImageFileName(newFileName);
            Product saved = productRepository.save(product);
            deleteQuietly(oldFileName);
            return saved;
        } catch (IOException exception) {
            deleteQuietly(newFileName);
            throw new BusinessException(
                    "製品画像を保存できませんでした。");
        } catch (RuntimeException exception) {
            deleteQuietly(newFileName);
            throw exception;
        }
    }

    @Transactional
    public Product deleteProductImage(
            Long productId) {

        Product product = getProduct(productId);
        String oldFileName = product.getImageFileName();

        product.setImageFileName(null);
        Product saved = productRepository.save(product);
        deleteQuietly(oldFileName);
        return saved;
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }

    private Product getProduct(
            Long productId) {

        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された製品が存在しません。"));
    }

    private void validateImageFile(
            MultipartFile imageFile) {

        if (imageFile == null || imageFile.isEmpty()) {
            throw new BusinessException(
                    "製品画像を選択してください。");
        }
        if (imageFile.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    "製品画像は5MB以下にしてください。");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null
                || !ALLOWED_TYPES.containsKey(
                        contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(
                    "製品画像はJPEG、PNG、WebP形式を選択してください。");
        }
    }

    private Path resolveFile(
            String fileName) {

        Path resolved = storageDirectory
                .resolve(fileName)
                .normalize();
        if (!resolved.startsWith(storageDirectory)) {
            throw new BusinessException(
                    "製品画像の保存先が不正です。");
        }
        return resolved;
    }

    private void deleteQuietly(
            String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveFile(fileName));
        } catch (IOException exception) {
            // DB更新を優先し、残存ファイルは運用時に清掃する。
        }
    }
}
