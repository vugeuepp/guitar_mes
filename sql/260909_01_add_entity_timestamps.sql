-- Phase 5C-1: 既存データは補完しない。全列NULL許可・DEFAULTなし。

BEGIN;

ALTER TABLE t_production_order
    ADD COLUMN created_at timestamp without time zone,
    ADD COLUMN updated_at timestamp without time zone,
    ADD COLUMN completed_at timestamp without time zone;

ALTER TABLE t_body
    ADD COLUMN created_at timestamp without time zone,
    ADD COLUMN updated_at timestamp without time zone,
    ADD COLUMN available_at timestamp without time zone;

ALTER TABLE t_neck
    ADD COLUMN created_at timestamp without time zone,
    ADD COLUMN updated_at timestamp without time zone,
    ADD COLUMN available_at timestamp without time zone;

ALTER TABLE t_guitar
    ADD COLUMN created_at timestamp without time zone,
    ADD COLUMN updated_at timestamp without time zone,
    ADD COLUMN completed_at timestamp without time zone;

COMMIT;
