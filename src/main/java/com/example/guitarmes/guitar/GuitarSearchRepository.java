package com.example.guitarmes.guitar;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GuitarSearchRepository {
    Page<Guitar> search(GuitarSearchCriteria criteria, Pageable pageable);
    long countMatching(GuitarSearchCriteria criteria);
}
