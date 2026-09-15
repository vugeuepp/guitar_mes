-- 適用SQLと同じDB・search_pathで実行する。読み取りのみ。
SELECT current_database(), current_schema(),
       to_regclass('m_product_parts_spec') IS NOT NULL AS table_exists;

-- 15列すべてmatches_expected=true。仕様列はDEFAULTなし。
WITH expected(column_name, data_type, max_length, is_nullable) AS (
    VALUES
    ('id', 'bigint', NULL, 'NO'),
    ('product_id', 'bigint', NULL, 'NO'),
    ('bridge_type', 'character varying', 32, 'YES'),
    ('bridge_model', 'character varying', 255, 'YES'),
    ('requires_stud_hole_expansion', 'boolean', NULL, 'YES'),
    ('tuner_model', 'character varying', 255, 'YES'),
    ('tuner_mounting_type', 'character varying', 32, 'YES'),
    ('tuner_bush_required', 'boolean', NULL, 'YES'),
    ('tuner_layout', 'character varying', 32, 'YES'),
    ('selector_positions', 'integer', NULL, 'YES'),
    ('control_layout', 'character varying', 255, 'YES'),
    ('jack_mounting_type', 'character varying', 32, 'YES'),
    ('string_maker', 'character varying', 150, 'YES'),
    ('string_model', 'character varying', 255, 'YES'),
    ('string_gauge', 'character varying', 100, 'YES')
)
SELECT e.column_name, c.data_type, c.character_maximum_length,
       c.is_nullable, c.column_default, c.is_identity,
       COALESCE(c.data_type = e.data_type
           AND c.character_maximum_length IS NOT DISTINCT FROM e.max_length
           AND c.is_nullable = e.is_nullable
           AND c.column_default IS NULL
           AND c.is_identity = CASE WHEN e.column_name = 'id' THEN 'YES' ELSE 'NO' END,
           false) AS matches_expected
FROM expected e
LEFT JOIN information_schema.columns c
  ON c.table_schema = (SELECT n.nspname FROM pg_class t
      JOIN pg_namespace n ON n.oid = t.relnamespace
      WHERE t.oid = to_regclass('m_product_parts_spec'))
 AND c.table_name = 'm_product_parts_spec' AND c.column_name = e.column_name
ORDER BY e.column_name;

-- PK / UNIQUE(product_id) / FK(product_id -> m_product.id) / Enum CHECK 4件。
WITH expected(name) AS (
    VALUES ('pk_product_parts_spec'), ('uk_product_parts_spec_product'),
           ('fk_product_parts_spec_product'), ('ck_product_parts_spec_bridge_type'),
           ('ck_product_parts_spec_tuner_mounting_type'),
           ('ck_product_parts_spec_tuner_layout'), ('ck_product_parts_spec_jack_mounting_type')
)
SELECT e.name, c.oid IS NOT NULL AS constraint_exists,
       pg_get_constraintdef(c.oid) AS definition
FROM expected e
LEFT JOIN pg_constraint c ON c.conrelid = to_regclass('m_product_parts_spec')
                         AND c.conname = e.name
ORDER BY e.name;

SELECT indexrelid::regclass AS index_name, pg_get_indexdef(indexrelid)
FROM pg_index WHERE indrelid = to_regclass('m_product_parts_spec');
