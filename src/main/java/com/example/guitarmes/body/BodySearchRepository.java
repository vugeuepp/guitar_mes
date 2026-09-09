package com.example.guitarmes.body;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BodySearchRepository {
    Page<Body> search(BodySearchCriteria criteria, Pageable pageable);
    long countMatching(BodySearchCriteria criteria);
}
