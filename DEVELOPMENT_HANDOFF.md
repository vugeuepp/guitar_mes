# Guitar MES Development Handoff

Updated: 2026-09-15

## Repository State

- Repository: `vugeuepp/guitar_mes`
- Current branch: `feature/phase6a-product-parts-spec`
- Local HEAD at this review: `c7c4fccff9d790a33f5fbd4f0f9206871914e958`（`6A-1-3`）
- 保存済みupstream: `origin/feature/phase6a-guitar-parts-installation`。同じHEADを指すが、今回はリモートへの最新照会なし。branch名の相違は既存設定。
- 文書更新開始時のworking treeはクリーン。今回の設計書・Handoff更新は未commit。実装コードは変更していない。

## Current State / Next Work

**Phase 6A-1完了: Productパーツ取付仕様の基盤・登録編集・電子仕様書表示まで実装完了。** ChatGPTによる実画面確認も完了したとのユーザー報告を受け、確定仕様を文書化した。本Markdownのレビューはこれから行う。

| 完了範囲 | 内容 |
| --- | --- |
| 6A-1-1 | ProductPartsSpec Entity / Enum / Repository、適用・確認・ロールバックSQL |
| 6A-1-2A | Service、完全入力validation、製造開始後の更新制限 |
| 6A-1-2B | 共通Spec入力からvariationごとに独立保存、登録・編集UI、transaction、JUnit・E2E追加 |
| 6A-1-3 | Product詳細に電子仕様書カード、表示値変換、詳細テンプレートテスト |

次は**6A-2 工程内作業記録基盤**。その後に**6A-3 専用画面・工程連携**。今回は6A-2の設計詳細化・実装には入っていない。Phase 5Cは完了済み（既存記録）。

確定仕様は[Phase 6A設計方針 第2節](引き継ぎ書類/260910_Guitar_MES_Phase6A_設計方針.md#2-開発単位と6a-1の確定仕様)に集約する。ロードマップは今回変更していない。

## Implementation Essentials

- Product 1 : 0..1 ProductPartsSpec。Spec所有の片方向one-to-one、LAZY、cascade / orphanRemovalなし。Product逆関連なし。DBのproduct_idはNOT NULL / FK / UNIQUE。
- Bridge / Tuner / Electronics / Stringの13仕様項目。正式名称は`JackMountingType` / `jackMountingType`。PU構成は既存Product.pickupLayoutを再利用。
- DBの仕様列はnullable / DEFAULTなしだが、Serviceは完全なSpecだけを保存する。下書き・部分保存はない。任意はbridgeModel / stringMakerのみ。他の11項目は必須。
- SIX_POINTは穴拡張falseのみ。TWO_POINT / FLOYD_ROSEはtrue / falseを明示。PRESS_BUSHINGはブッシュtrueのみ、NUT_FASTENINGはtrue / falseを許可。いずれのBooleanもnull不可。
- selectorPositionsは正数。文字列はtrim・長さ検証、必須blankはエラー、任意blankはnull。ゲージの厳格な形式regexなし。モデル名等から不足仕様を推測しない。
- ProductFormServiceがProduct群＋各Spec、またはProduct編集＋Specを同一transactionにまとめる。全項目空ならSpecなし、部分入力はvalidationエラー。複数variationでSpec行を共有しない。
- **初回補完は移行措置**: Specなしなら製造開始済みでも初回作成可能。作成時期の区別がないため導入後Productも対象となることを受容中。恒久的な更新許可ではなく、将来見直し対象。
- **Spec作成後は製造開始後更新禁止**: Guitar参照あり、またはProductionOrder.startedQuantity / completedQuantityが正数で禁止。同値再送もSpec更新は不可。Serviceで判定し、UI改ざんPOSTも制限する。
- ロック済み欄の未送信はSpecを維持し、Product名等の許可項目は保存可能。アプリケーションにSpec削除機能はない。
- 電子仕様書はNeck MasterとRelated Guitarsの間。4グループの読み取り専用表示。Enum表示名、必要 / 不要、null / blankは「-」。Specなしは「パーツ取付仕様：未設定」。製造開始前後とも現在の正式仕様を表示し、個体の過去仕様表示ではない。

## Verification Record

今回はテスト・Playwright・DB操作を実行していない。過去の成功と現HEADでの全件再検証を区別する。

| 段階 | 記録された結果 | 今回の確認・出典 |
| --- | --- | --- |
| 6A-1-1 | Repository関連＋ProductService 34件成功 | ユーザー提示の過去実績。テストコードは存在するが当時の成功ログは今回未再確認 |
| 6A-1-2A | ProductPartsSpecServiceTest 56件＋ProductServiceTest 26件＝82件成功 | ユーザー提示の過去実績。テスト実装を確認、当時の成功ログは今回未再確認 |
| 6A-1-2B | 関連JUnit 104件成功、作業中に通常全414件成功 | ユーザー提示・過去作業報告。旧一時ログは今回の環境に存在せず再確認不可 |
| 登録編集E2E | ProductPartsRegistrationE2E追加済み | Git・コードで2シナリオ確認。今回実行結果のログ検証なし。全E2E成功とは記載しない |
| 6A-1-3 | ProductViewControllerTest 11件＋ProductPartsDetailTest 5件＝16件成功、失敗・エラー0 | Codexの既存ログ `/tmp/guitar-mes-6a13-targeted.log` を今回再確認（2026-09-15、ログは一時ファイル） |
| 実画面 | ChatGPT確認完了 | 今回のユーザー報告。Codexによる再確認ではない |

6A-1-3時には通常全414件・全E2Eを再実行していない。上記は各段階の件数であり、現HEADの全通常テスト件数・全件成功を表さない。

登録編集E2EはUUID付きの自前fixture、E2E DB guard、自分のデータのみcleanupを使用。variation追加削除、入力エラーと保持、保存・再編集、製造開始後ロック、Specなし登録と開始後初回補完をカバーするコードがある。詳細の5テストは実Thymeleaf描画で表示名、Boolean、NULL、未設定カード、配置、画像エラー時の再表示を確認する。

## Design Debt for 6A-2 / 6A-3

解決済みはSpec作成・更新transaction、フォーム全体のロールバック、初回作成のDB UNIQUE最終防御。未解決事項と混同しない。

- 初回補完の対象・期限の見直し。同時初回作成でUNIQUE違反になった場合の利用者向け扱い。
- Spec更新と製造開始のrace。現状の業務判定・transactionだけで競合解消済みとはしない。
- work開始タイミング、必須作業の導出条件、対象分類・不足仕様の検証接続。
- work-start snapshotのtransaction境界、二重作業生成防止、保存範囲・version方式（未確定）。
- NG / 再試験 / コメント、完了条件、generic / bulk工程終了からのvalidation迂回防止、ProcessService.endProcess()との統合。

ProductPartsSpecは製品仕様、将来のProcessWork / ProcessWorkItemは実行時作業指示・実績、ProcessHistoryは既存汎用履歴。作業開始時に仕様を読んでServiceで項目を導出・作業層へ保存し、以後は保存済み項目を使う。マスタ変更で開始済み作業が勝手に変わらない方針を維持し、今回snapshot構造を新規設計しない。

## AI Responsibilities / Temporary Constraints

恒久的な責任分担はAGENTS.mdに従う。今回の対象は本HandoffとPhase 6A設計方針のMarkdownのみ。実装・テスト・DB操作・commit / push / mergeは行わない。

**文書更新後に停止し、ChatGPTレビュー前に6A-2へ進まない。** レビュー後も具体的な作業はユーザーの指示に従う。

## New Chat Startup

1. AGENTS.md、DEVELOPMENT_HANDOFF.md、Phase 6A設計方針を読む。
2. actual Git branch / HEAD / working treeと照合する。保存済みorigin参照と最新リモート照会を区別する。
3. 本書のテスト結果は記載時点の実績として扱い、後続変更の成功へ流用しない。
4. 今回のMarkdownに対するChatGPTレビュー状況と、次の依頼範囲を確認する。
