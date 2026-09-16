-- 全Work snapshot・Item実績を失う。旧アプリへの切戻しと退避を確認して実行。
-- 子→親の順。CASCADEなし。既存ProcessHistory / Product等は変更しない。
BEGIN;

DROP TABLE t_process_work_item;
DROP TABLE t_process_work;

COMMIT;
