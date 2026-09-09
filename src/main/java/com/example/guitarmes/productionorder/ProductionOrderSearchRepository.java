package com.example.guitarmes.productionorder;

import java.util.List;

public interface ProductionOrderSearchRepository {
    List<ProductionOrder> search(ProductionOrderSearchCriteria criteria);
    long countMatching(ProductionOrderSearchCriteria criteria);
}
