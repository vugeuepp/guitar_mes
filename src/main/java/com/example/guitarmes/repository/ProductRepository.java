package com.example.guitarmes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Product;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

}