-- 適用SQLと同じDB・search_pathで実行する。読み取りのみ。
SELECT current_database(), current_schema(),
       to_regclass('m_process') IS NOT NULL AS table_exists;

-- matches_expected=true: varchar(64)、NULL許可、DEFAULTなし。
SELECT c.column_name, c.data_type, c.character_maximum_length,
       c.is_nullable, c.column_default,
       COALESCE(c.data_type = 'character varying'
           AND c.character_maximum_length = 64
           AND c.is_nullable = 'YES'
           AND c.column_default IS NULL, false) AS matches_expected
FROM (VALUES ('process_code')) AS expected(column_name)
LEFT JOIN information_schema.columns c
  ON c.table_schema = (SELECT n.nspname FROM pg_class t
      JOIN pg_namespace n ON n.oid = t.relnamespace
      WHERE t.oid = to_regclass('m_process'))
 AND c.table_name = 'm_process' AND c.column_name = expected.column_name;

-- matches_expected=true: target_typeとの複合ではなく、process_code単独のUNIQUE。
SELECT c.conname, pg_get_constraintdef(c.oid) AS definition,
       COALESCE(c.contype = 'u' AND c.convalidated
           AND c.conkey = ARRAY[a.attnum], false) AS matches_expected
FROM (VALUES ('uk_process_process_code')) AS expected(name)
LEFT JOIN pg_constraint c ON c.conrelid = to_regclass('m_process')
                         AND c.conname = expected.name
LEFT JOIN pg_attribute a ON a.attrelid = to_regclass('m_process')
                        AND a.attname = 'process_code' AND NOT a.attisdropped;

-- code_count=1、expected_target_count=1、matches_expected=true。
SELECT count(*) AS code_count,
       count(*) FILTER (WHERE target_type = 'GUITAR'
                         AND process_name = 'ギターパーツ取付') AS expected_target_count,
       count(*) = 1 AND count(*) FILTER (WHERE target_type = 'GUITAR'
                         AND process_name = 'ギターパーツ取付') = 1 AS matches_expected
FROM m_process WHERE process_code = 'GUITAR_PARTS_INSTALLATION';

SELECT id, target_type, process_name, process_order, process_code
FROM m_process WHERE process_code = 'GUITAR_PARTS_INSTALLATION';

-- 0行であること。NULLは複数件存在してよい。
SELECT process_code, count(*) AS duplicate_count
FROM m_process WHERE process_code IS NOT NULL
GROUP BY process_code HAVING count(*) > 1;
