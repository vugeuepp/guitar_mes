-- Phase 6A-2-1: 安定業務コードの段階導入。未対象工程はNULLのまま。
-- 適用先DB・search_pathを確認し、ファイル全体を同一セッションで実行する。
-- 一度だけ適用する。既存列がある場合も失敗させ、再適用を黙認しない。
-- psqlでは -v ON_ERROR_STOP=1 を指定する。他クライアントでもエラー時は停止する。
-- 失敗時はCOMMITせずROLLBACK。以下のDDL・補完は全て同一transaction。
BEGIN;

ALTER TABLE m_process ADD COLUMN process_code varchar(64);
ALTER TABLE m_process
    ADD CONSTRAINT uk_process_process_code UNIQUE (process_code);

-- ALTER TABLEのロックはtransaction終了まで保持されるため、検証とUPDATE間の
-- 他transactionによる対象行の追加・変更を防ぐ。IDやprocess_orderは推測しない。
DO $$
DECLARE
    target_count bigint;
BEGIN
    SELECT count(*) INTO target_count
    FROM m_process
    WHERE target_type = 'GUITAR' AND process_name = 'ギターパーツ取付';

    IF target_count <> 1 THEN
        RAISE EXCEPTION 'processCode migration STOP: expected exactly 1 GUITAR/ギターパーツ取付, found %', target_count;
    END IF;

    UPDATE m_process
    SET process_code = 'GUITAR_PARTS_INSTALLATION'
    WHERE target_type = 'GUITAR' AND process_name = 'ギターパーツ取付';
END;
$$;

COMMIT;
