package com.example.guitarmes.neck;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NeckSearchRepository {
    Page<Neck> search(NeckSearchCriteria criteria, Pageable pageable);
    long countMatching(NeckSearchCriteria criteria);
}
