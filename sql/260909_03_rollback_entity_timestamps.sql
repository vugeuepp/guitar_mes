-- 新列の記録値は失われる。アプリ旧版への切戻し・値の退避を確認して適用する。

BEGIN;

ALTER TABLE t_production_order
    DROP COLUMN completed_at,
    DROP COLUMN updated_at,
    DROP COLUMN created_at;

ALTER TABLE t_body
    DROP COLUMN available_at,
    DROP COLUMN updated_at,
    DROP COLUMN created_at;

ALTER TABLE t_neck
    DROP COLUMN available_at,
    DROP COLUMN updated_at,
    DROP COLUMN created_at;

ALTER TABLE t_guitar
    DROP COLUMN completed_at,
    DROP COLUMN updated_at,
    DROP COLUMN created_at;

COMMIT;
