package com.example.guitarmes.neck;

import java.util.List;

/** Serviceで正規化済みの一覧検索条件。 */
public record NeckSearchCriteria(String category, List<String> statuses,
        String serial, String modelName, String currentProcess, String status) {
    public NeckSearchCriteria { statuses = List.copyOf(statuses); }
}
