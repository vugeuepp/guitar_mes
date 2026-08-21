package com.example.guitarmes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Product;

public interface ProductRepository
        extends JpaRepository<Product, Long> {
	List<Product> findByProductNameContaining(String keyword);
}