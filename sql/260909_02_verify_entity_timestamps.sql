-- 想定12列がすべて表示され、matches_expectedが全行trueであること。
-- 対象DBのsearch_pathにあるテーブルを確認する（適用SQLと同じ接続条件で実行）。
WITH expected(table_name, column_name) AS (
    VALUES
      ('t_production_order', 'created_at'), ('t_production_order', 'updated_at'),
      ('t_production_order', 'completed_at'),
      ('t_body', 'created_at'), ('t_body', 'updated_at'), ('t_body', 'available_at'),
      ('t_neck', 'created_at'), ('t_neck', 'updated_at'), ('t_neck', 'available_at'),
      ('t_guitar', 'created_at'), ('t_guitar', 'updated_at'), ('t_guitar', 'completed_at')
)
SELECT e.table_name, e.column_name, c.table_schema,
       c.column_name IS NOT NULL AS column_exists,
       c.data_type, c.is_nullable, c.column_default,
       COALESCE(c.data_type = 'timestamp without time zone'
           AND c.is_nullable = 'YES' AND c.column_default IS NULL, false) AS matches_expected
FROM expected e
LEFT JOIN pg_class t ON t.oid = to_regclass(e.table_name)
LEFT JOIN pg_namespace n ON n.oid = t.relnamespace
LEFT JOIN information_schema.columns c
       ON c.table_schema = n.nspname AND c.table_name = e.table_name
      AND c.column_name = e.column_name
ORDER BY e.table_name, e.column_name;
