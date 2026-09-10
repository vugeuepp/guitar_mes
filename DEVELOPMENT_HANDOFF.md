# Guitar MES Development Handoff

Updated: 2026-09-10

## Repository State

- Repository: `vugeuepp/guitar_mes`
- Current working branch: `feature/phase6a-product-parts-spec`
- Local HEAD at documentation review: `2d808bee7b78afddabc9b9d18045b1fddb05d682`
- Local `main` and saved `origin/main` point to the same HEAD.
- Upstream recorded locally: `origin/feature/phase6a-guitar-parts-installation`（ローカルbranchと名称が異なる。設定変更はしていない）
- Working tree was clean before this documentation update. Remote最新照会は今回未実施。

## Current State

Phase 5C completed and merged into main.

Previously recorded verification results（既存Handoffから継承。今回はログ再検証・テスト再実行なし）:

- Normal tests: 339 passed, 0 failures, 0 errors, 0 skipped
- E2E tests: 26 passed, 0 failures, 0 errors, 0 skipped

Recent completion summary for Phase 5C:

- timestamps
- sorting
- pagination
- performance improvements
- concurrency control
- current-page bulk selection
- datetime display

## Current Phase / Next Work

Phase 6A「ギターパーツ取付工程」の設計文書を整備中。今回の文書変更はユーザー確認待ちであり、Java・HTML・DB等の実装には着手しない。

ユーザー指定の開発順序:

1. **6A-1 製品仕様拡張**: Bridge / Tuner / Electronicsの不足仕様 / Stringを対象候補に、作業判断に必要な製品仕様を整える。
2. **6A-2 工程内作業記録基盤**: 汎用`ProcessHistory`を維持し、工程固有情報を別の作業記録層へ分離する。
3. **6A-3 専用画面・工程連携**: 製品仕様に応じた作業チェック・検査・途中保存・完了判定を行い、既存`ProcessService.endProcess()`へ接続する。

最初の対象はStratocaster系。詳細・対象作業・未確定事項は[Phase 6A設計方針](引き継ぎ書類/260910_Guitar_MES_Phase6A_設計方針.md)、開発順は[ロードマップ第11節](引き継ぎ書類/260902_Guitar_MES_新開発ロードマップ改訂版.md#11-phase-6-工程別専用ページ工程内作業)を参照する。

文書確認後の次の作業候補は、6A-1に必要な既存製品仕様の棚卸しと不足項目・保存形式の設計。具体的な列や型はまだ確定していない。実装開始はユーザーの次の指示に従う。

今回の確認範囲は指定文書とGit状態。Phase 6Aのコード・実DBスキーマの棚卸しは未実施。`AGENTS.md`は恒久ルールのまま維持する。

## AI Responsibilities

- ChatGPT: planning / design / GitHub review / delegation / completion judgment
- Codex: repo-aware implementation / investigation / targeted tests
- Copilot: Bundle-only implementation / targeted validation
- User / Mac: full normal test / full E2E / manual UI / commit / push

## Temporary Constraints

- 旧記載の「2026-09-10 04:55 JSTまでCodex利用不可」は、今回の確認時点で期限経過済みのため削除した。現在の利用残量を確認したという意味ではない。
- 今回は文書更新のみ。実装・テスト実行・commit・push・mergeは行わず、文書更新後にユーザー確認を待つ。

## New Chat Startup

新しいChatGPTチャットでは、次を順に確認して照合してから次の作業を提案します。

1. `DEVELOPMENT_HANDOFF.md`
2. `AGENTS.md`
3. actual GitHub / current branch HEAD

このHandoffに書かれたbranch、HEAD、テスト件数、Temporary Constraintsを永続的な真実として扱いません。actual Git状態と矛盾する場合は、現在の実状態を優先します。

ChatGPTチャットが長くなった場合や、Phase・大きな作業単位の区切りでは、新しいチャットへ切り替えられます。Phase途中でも会話が重くなった場合は、このHandoffを更新して移行できます。新しいチャットは過去チャットの完全な記憶を前提にしません。
