package com.example.guitarmes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.guitarmes.entity.Guitar;

public interface GuitarRepository extends JpaRepository<Guitar, Long> {

}
