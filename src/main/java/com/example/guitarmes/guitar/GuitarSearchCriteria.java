package com.example.guitarmes.guitar;

/** Serviceで正規化済みのGuitar一覧検索条件。 */
public record GuitarSearchCriteria(
        String category,
        String serial,
        String product,
        String currentProcess,
        String status) {
}
