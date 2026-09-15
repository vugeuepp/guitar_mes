-- Specの全記録値を失う。旧アプリへの切戻しと必要な値の退避を確認して適用する。
-- CASCADEは使わず、後続の依存があれば失敗させる。既存Productは削除しない。
BEGIN;

DROP TABLE m_product_parts_spec;

COMMIT;
