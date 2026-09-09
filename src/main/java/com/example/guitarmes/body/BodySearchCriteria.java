package com.example.guitarmes.body;

import java.util.List;

/** Serviceで正規化済みの一覧検索条件。 */
public record BodySearchCriteria(String category, List<String> statuses,
        String serial, String modelName, String currentProcess, String status) {
    public BodySearchCriteria { statuses = List.copyOf(statuses); }
}
