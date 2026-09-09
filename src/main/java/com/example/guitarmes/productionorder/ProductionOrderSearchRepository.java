package com.example.guitarmes.productionorder;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductionOrderSearchRepository {
    List<ProductionOrder> search(ProductionOrderSearchCriteria criteria);
    Page<ProductionOrder> search(
            ProductionOrderSearchCriteria criteria,
            Pageable pageable);
    long countMatching(ProductionOrderSearchCriteria criteria);
}
