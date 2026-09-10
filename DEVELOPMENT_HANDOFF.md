# Guitar MES Development Handoff

Updated: 2026-09-10

## Repository State

- Repository: `vugeuepp/guitar_mes`
- Current working branch: `feature/phase6a-product-parts-spec`
- Local HEAD at documentation review: `615cda289f9a9ecd0942c0da01d18db16575e163`
- Local `main` and saved `origin/main` remain at `2d808bee7b78afddabc9b9d18045b1fddb05d682`（作業branchのHEADとは異なる）。
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

Phase 6A-1の既存仕様調査はユーザーがChatGPTで確認済み。その設計判断をPhase 6A設計書へ反映した段階。今回の文書差分は改めてChatGPT確認待ちであり、実装には着手しない。

ユーザー指定の開発順序:

1. **6A-1 製品仕様拡張**: Bridge / Tuner / Electronicsの不足仕様 / Stringを対象候補に、作業判断に必要な製品仕様を整える。
2. **6A-2 工程内作業記録基盤**: 汎用`ProcessHistory`を維持し、工程固有情報を別の作業記録層へ分離する。
3. **6A-3 専用画面・工程連携**: 製品仕様に応じた作業チェック・検査・途中保存・完了判定を行い、既存`ProcessService.endProcess()`へ接続する。

最初の対象はStratocaster系。詳細・対象作業・未確定事項は[Phase 6A設計方針](引き継ぎ書類/260910_Guitar_MES_Phase6A_設計方針.md)、開発順は[ロードマップ第11節](引き継ぎ書類/260902_Guitar_MES_新開発ロードマップ改訂版.md#11-phase-6-工程別専用ページ工程内作業)を参照する。

採用した第一案はProduct 1 : 0..1 ProductPartsSpec。正式項目候補、ST分類再利用、共通入力からProductごとに独立保存、既存製品への初回仕様補完と製造開始後の編集制限を設計書第2節へ集約した。

次の候補は6A-1-1の実装設計。Enum・必須条件・物理スキーマ・初回補完の対象識別等を確定してから、Entity／保存・検証Service／適用・確認・ロールバックSQL／関連テストの範囲を切り出す。これは実装開始の指示ではない。ChatGPTで今回の文書変更を確認してから、ユーザーの指示に従って進む。

前回調査でProduct関連コード・テンプレート・テストと開発DBの実スキーマを確認済み。今回は指定文書・Git状態の確認と2文書の更新のみ。コード・DB変更、テスト実行はなし。AGENTS.mdとロードマップは変更しない。文書差分は未commit。

## AI Responsibilities

- ChatGPT: planning / design / GitHub review / delegation / completion judgment
- Codex: repo-aware implementation / investigation / targeted tests
- Copilot: Bundle-only implementation / targeted validation
- User / Mac: full normal test / full E2E / manual UI / commit / push

## Temporary Constraints

- 旧記載の「2026-09-10 04:55 JSTまでCodex利用不可」は、今回の確認時点で期限経過済みのため削除した。現在の利用残量を確認したという意味ではない。
- 今回は文書更新のみ。実装・テスト実行・commit・push・mergeは行わず、文書更新後に停止し、ChatGPT確認前に6A-1-1へ進まない。

## New Chat Startup

新しいChatGPTチャットでは、次を順に確認して照合してから次の作業を提案します。

1. `DEVELOPMENT_HANDOFF.md`
2. `AGENTS.md`
3. actual GitHub / current branch HEAD

このHandoffに書かれたbranch、HEAD、テスト件数、Temporary Constraintsを永続的な真実として扱いません。actual Git状態と矛盾する場合は、現在の実状態を優先します。

ChatGPTチャットが長くなった場合や、Phase・大きな作業単位の区切りでは、新しいチャットへ切り替えられます。Phase途中でも会話が重くなった場合は、このHandoffを更新して移行できます。新しいチャットは過去チャットの完全な記憶を前提にしません。
