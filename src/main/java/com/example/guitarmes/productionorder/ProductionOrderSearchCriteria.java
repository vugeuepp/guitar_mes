package com.example.guitarmes.productionorder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Serviceで正規化・検証済みの検索条件。 */
public record ProductionOrderSearchCriteria(String category, List<String> categoryStatuses,
        String orderNo, String product, String status, YearMonth planMonth,
        LocalDate dueFrom, LocalDate dueTo) {
    public ProductionOrderSearchCriteria {
        categoryStatuses = List.copyOf(categoryStatuses);
    }
}
