-- 適用SQLと同じDB・search_pathで実行する。読み取りのみ。
SELECT current_database(), current_schema(),
       to_regclass('t_process_work') IS NOT NULL AS work_exists,
       to_regclass('t_process_work_item') IS NOT NULL AS item_exists;

-- Work 17列（snapshotは14列）、Item 8列。全行matches_expected=trueを確認。
WITH expected(table_name, column_name, data_type, max_length, is_nullable) AS (
    VALUES
    ('t_process_work', 'id', 'bigint', NULL, 'NO'),
    ('t_process_work', 'process_history_id', 'bigint', NULL, 'NO'),
    ('t_process_work', 'bridge_type', 'character varying', 32, 'NO'),
    ('t_process_work', 'bridge_model', 'character varying', 255, 'YES'),
    ('t_process_work', 'requires_stud_hole_expansion', 'boolean', NULL, 'NO'),
    ('t_process_work', 'tuner_model', 'character varying', 255, 'NO'),
    ('t_process_work', 'tuner_mounting_type', 'character varying', 32, 'NO'),
    ('t_process_work', 'tuner_bush_required', 'boolean', NULL, 'NO'),
    ('t_process_work', 'tuner_layout', 'character varying', 32, 'NO'),
    ('t_process_work', 'pickup_layout', 'character varying', 255, 'NO'),
    ('t_process_work', 'selector_positions', 'integer', NULL, 'NO'),
    ('t_process_work', 'control_layout', 'character varying', 255, 'NO'),
    ('t_process_work', 'jack_mounting_type', 'character varying', 32, 'NO'),
    ('t_process_work', 'string_maker', 'character varying', 150, 'YES'),
    ('t_process_work', 'string_model', 'character varying', 255, 'NO'),
    ('t_process_work', 'string_gauge', 'character varying', 100, 'NO'),
    ('t_process_work', 'created_at', 'timestamp without time zone', NULL, 'NO'),
    ('t_process_work_item', 'id', 'bigint', NULL, 'NO'),
    ('t_process_work_item', 'process_work_id', 'bigint', NULL, 'NO'),
    ('t_process_work_item', 'item_key', 'character varying', 64, 'NO'),
    ('t_process_work_item', 'item_order', 'integer', NULL, 'NO'),
    ('t_process_work_item', 'status', 'character varying', 32, 'NO'),
    ('t_process_work_item', 'completed_at', 'timestamp without time zone', NULL, 'YES'),
    ('t_process_work_item', 'created_at', 'timestamp without time zone', NULL, 'NO'),
    ('t_process_work_item', 'updated_at', 'timestamp without time zone', NULL, 'NO')
)
SELECT e.table_name, e.column_name, c.data_type, c.character_maximum_length,
       c.is_nullable, c.column_default, c.is_identity,
       COALESCE(c.data_type = e.data_type
           AND c.character_maximum_length IS NOT DISTINCT FROM e.max_length
           AND c.is_nullable = e.is_nullable AND c.column_default IS NULL
           AND c.is_identity = CASE WHEN e.column_name = 'id' THEN 'YES' ELSE 'NO' END,
           false) AS matches_expected
FROM expected e
LEFT JOIN information_schema.columns c
  ON c.table_schema = (SELECT n.nspname FROM pg_class t
      JOIN pg_namespace n ON n.oid = t.relnamespace
      WHERE t.oid = to_regclass(e.table_name))
 AND c.table_name = e.table_name AND c.column_name = e.column_name
ORDER BY e.table_name, e.column_name;

-- 全15制約の存在・種別とdefinitionを確認。FKはconfdeltype='a'（NO ACTION）。
-- definitionの対象列・参照先・CHECK値を適用SQLと照合する。
-- item_key CHECKのElectronicsは次の7値（独立した穴あけItemなし）:
-- PICKGUARD_INSTALL, JACK_PLATE_INSTALL, JACK_WIRING, GROUND_WIRING,
-- ELECTRONICS_SOUND_CHECK, ELECTRONICS_PARTS_CHECK, ELECTRONICS_FINAL_FASTENING。
WITH expected(table_name, name, constraint_type) AS (
    VALUES
    ('t_process_work', 'pk_process_work', 'p'),
    ('t_process_work', 'fk_process_work_process_history', 'f'),
    ('t_process_work', 'uk_process_work_process_history', 'u'),
    ('t_process_work', 'ck_process_work_bridge_type', 'c'),
    ('t_process_work', 'ck_process_work_tuner_mounting_type', 'c'),
    ('t_process_work', 'ck_process_work_tuner_layout', 'c'),
    ('t_process_work', 'ck_process_work_jack_mounting_type', 'c'),
    ('t_process_work_item', 'pk_process_work_item', 'p'),
    ('t_process_work_item', 'fk_process_work_item_process_work', 'f'),
    ('t_process_work_item', 'uk_process_work_item_work_key', 'u'),
    ('t_process_work_item', 'uk_process_work_item_work_order', 'u'),
    ('t_process_work_item', 'ck_process_work_item_item_key', 'c'),
    ('t_process_work_item', 'ck_process_work_item_status', 'c'),
    ('t_process_work_item', 'ck_process_work_item_order', 'c'),
    ('t_process_work_item', 'ck_process_work_item_status_completed_at', 'c')
)
SELECT e.table_name, e.name, c.oid IS NOT NULL AS constraint_exists,
       COALESCE(c.contype::text = e.constraint_type AND c.convalidated, false) AS type_matches,
       pg_get_constraintdef(c.oid) AS definition,
       CASE WHEN c.contype = 'f' THEN c.confdeltype::text END AS delete_action
FROM expected e
LEFT JOIN pg_constraint c ON c.conrelid = to_regclass(e.table_name) AND c.conname = e.name
ORDER BY e.table_name, e.name;

SELECT indrelid::regclass AS table_name, indexrelid::regclass AS index_name,
       pg_get_indexdef(indexrelid) AS definition
FROM pg_index
WHERE indrelid IN (to_regclass('t_process_work'), to_regclass('t_process_work_item'));
