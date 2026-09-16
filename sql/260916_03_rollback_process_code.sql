-- 旧アプリへの切戻しと必要なコード値の退避を確認してから実行する。
-- process_code列全体を削除するため、後から付与したコードも失う。
-- CASCADEは使わず、後続の依存があれば失敗させる。既存工程は削除しない。
BEGIN;

UPDATE m_process SET process_code = NULL
WHERE process_code = 'GUITAR_PARTS_INSTALLATION';

ALTER TABLE m_process DROP CONSTRAINT uk_process_process_code;
ALTER TABLE m_process DROP COLUMN process_code;

COMMIT;
