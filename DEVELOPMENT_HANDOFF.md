# Guitar MES Development Handoff

Updated: 2026-09-16

## Repository State

- Repository: `vugeuepp/guitar_mes`
- Current branch: `feature/phase6a-process-work`
- Local HEAD at this review: `b672eb9cf94a004e6e4446e0615545fb2eb4e9a3`（6A-2-1 processCode基盤の実装）
- 保存済みupstream: `origin/feature/phase6a-process-work`。今回はリモートへの最新照会なし。
- 作業開始時のworking treeはクリーン。6A-2-1は上記HEADでcommit済み、保存済みupstreamと一致。ChatGPT承認・push済みはユーザー報告。今回の6A-2-2変更は未commit。

## Current State / Next Work

**Phase 6A-1完了: Productパーツ取付仕様の基盤・登録編集・電子仕様書表示まで実装完了。** ChatGPTによる実画面確認も完了したとのユーザー報告を受け、確定仕様を文書化した。6A-1の確定仕様は以下のとおり。

| 完了範囲 | 内容 |
| --- | --- |
| 6A-1-1 | ProductPartsSpec Entity / Enum / Repository、適用・確認・ロールバックSQL |
| 6A-1-2A | Service、完全入力validation、製造開始後の更新制限 |
| 6A-1-2B | 共通Spec入力からvariationごとに独立保存、登録・編集UI、transaction、JUnit・E2E追加 |
| 6A-1-3 | Product詳細に電子仕様書カード、表示値変換、詳細テンプレートテスト |

次は**6A-2 工程内作業記録基盤**。その後に**6A-3 専用画面・工程連携**。現在は**6A-2-2 Work / Item Domain・DB基盤 実装済み（DB未適用・ChatGPTレビュー待ち）**。6A-2全体は進行中。Work生成・工程開始統合、Item操作Service・工程終了統合は未実装。Phase 5Cは完了済み（既存記録）。

確定仕様は[Phase 6A設計方針 第2節](引き継ぎ書類/260910_Guitar_MES_Phase6A_設計方針.md#2-開発単位と6a-1の確定仕様)に集約する。ロードマップは前回の文書更新で設計書参照へ整合済み。今回は変更なし。

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

以下は6A-1文書整理時の記録であり、表の「今回」はその確認時点を指す。下記の過去記録は今回再検証していない。6A-2-1 / 6A-2-2の検証結果は別記する。過去の成功と現HEADでの全件再検証を区別する。

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

### 6A-2-1 時点の検証（2026-09-16、Codex）

- ManufacturingProcessTest / ManufacturingProcessControllerTest: 3件成功。
- ProcessServiceTest / ProcessWorkControllerTest / ProcessViewControllerTest / ProcessPageProgressTest / BodyProcessServiceTest / NeckProcessServiceTest: 28件成功。
- Maven Wrapperをofflineで実行。初回APIテストはMockito self-attachの環境エラー。実行時のargLineでMockito 5.23.0のjavaagentを指定して再実行し成功（pom変更なし）。
- ログ: `/tmp/guitar-mes-6a21-targeted-agent.log`、`/tmp/guitar-mes-6a21-regression.log`。一時ファイル。
- ManufacturingProcessRepositoryTest 4件を追加・コンパイル済み、実行は未実施。SQL適用済みguitar_mes_e2eを前提とし、自前fixtureはtransaction rollback、初期コード確認は既存行の読み取りのみ。
- DB接続・SQL適用・SQL実行検証、通常全件、E2Eは実施していない。SQLとRepositoryの実DB検証はユーザー側に残る。

### 6A-2-2 今回の検証（2026-09-16、Codex）

- ProcessWorkTest 3件＋ProcessWorkItemTest 7件＝10件成功。
- ManufacturingProcessTest / ManufacturingProcessControllerTest / ProcessServiceTest / ProcessPageProgressTest / ProductPartsSpecServiceTest / EntityTimestampTest＝85件成功。
- Maven Wrapper offline、DB不要のtargeted testのみ。既存関連テストは実行時にMockito 5.23.0 javaagent指定（設定ファイル変更なし）。
- ログ: `/tmp/guitar-mes-6a22-targeted.log`、`/tmp/guitar-mes-6a22-regression.log`（一時ファイル）。
- ProcessWorkRepositoryTestは12ケースを作成・コンパイル済み、未実行。SQL適用後にguitar_mes_e2eで実行が必要。親Product / Guitar / 工程 / 履歴も自前作成し、transaction rollback。DB guardを使用。
- SQLは静的照合のみ。14 snapshot列、16 itemKey、15制約、制約名長、EnumとCHECK一致、削除cascadeなしを確認。DB接続・適用・実行検証、通常全件、E2Eは行っていない。

### 6A-2-2 ChatGPTレビュー修正

- ElectronicsはPICKGUARD_INSTALL / JACK_PLATE_INSTALL / JACK_WIRING / GROUND_WIRING / ELECTRONICS_SOUND_CHECK / ELECTRONICS_PARTS_CHECK / ELECTRONICS_FINAL_FASTENINGへ確定。Enum、apply CHECK、verifyの照合コメント、Unit / Repository Testを整合した。
- 穴あけは各取付作業に含め、独立Itemにしない。音出しもposition別Itemに分割しない。16キーを維持し、構造・snapshot・日時・Repository APIは変更なし。
- 修正後のProcessWorkTest / ProcessWorkItemTestは計10件成功（失敗・エラー0）。ログ: `/tmp/guitar-mes-6a22-review.log`。前回の既存関連85件は今回再実行していない。
- Repository Testはコンパイルのみ、DB接続・SQL適用なし。6A-2-3へは進まず再レビュー待ち。

## Phase 6A-2 Domain Design / Next Gate

正式方針・候補・未確定事項の詳細は[設計方針 第3〜8節](引き継ぎ書類/260910_Guitar_MES_Phase6A_設計方針.md)を参照する。

- History 1 : 0..1 Work 1 : N Item。process.workにEntity / Enum / Repositoryを追加。Work→Historyは片方向LAZY one-to-one、Item→Workは片方向LAZY many-to-one。逆参照・cascade・orphanRemovalなし。
- 将来、対象工程開始時にHistoryと同一transactionで14仕様snapshotとItemを生成する（今回未接続）。WorkはcreatedAtのみでstatus / updatedAt / productId snapshot / Spec FKなし。開始後にマスタ変更で再生成しない。
- Work snapshotは12項目NOT NULL、bridgeModel / stringMakerのみ任意。pickupLayoutはString / varchar(255)を採用（Productの実DB長はrepositoryから確認不能）。
- ItemはitemKey varchar(64)、itemOrderは正数・1始まり想定、status varchar(32)はNOT_STARTED / COMPLETED。Java初期値NOT_STARTED、DB DEFAULTなし。3 UNIQUE、Enum / 正数 / status-completedAt整合CHECKをSQLへ定義。
- Work createdAtは未設定時のみ初期化。Item作成時は未指定ならcreatedAtとupdatedAtを同時刻へ設定し、明示時刻は保持。更新時は既存のpersistedUpdatedAt比較方式を再利用。終了前の解除・終了後read-onlyの業務制御、既存Guitarロックとの統合は後続Service。
- SQLは260916_04_create_process_work / 05_verify_process_work / 06_rollback_process_work.sql。新テーブル2件、既存データ補完なし、DB未適用。
- bulkは全台の検証・導出後に保存するall-or-nothing。対象StratのSpec不備は開始拒否方向、明確な対象外は従来処理。分類不能の扱いは未確定。
- processCodeはString / VARCHAR(64)、NULL許可・全体UNIQUE。Entity、findByProcessCode、ProcessCodeConstants.GUITAR_PARTS_INSTALLATION、sql/260916_01〜03の適用・確認・rollback SQLを追加済み。対象GUITAR＋ギターパーツ取付が0件・複数件なら例外で移行STOP。DB未適用。API JSONへnullableのprocessCodeを追加。既存工程判定は変更なし。
- process.workへDomainを配置済み。process.partsinstallationの作業導出は後続候補。既存ProcessWorkController名は再利用しない。
- 終了validationの個別/bulk共通利用は主に6A-3。PUT /api/guitars/{id}のcurrentProcess直接更新は迂回経路候補として残す。

残論点は分類責務再利用・分類不能時の扱い、pickupLayout実DB長、NG等の拡張、再実施、専用UI、名前依存解消・直接更新API改修。Spec初回補完と製造開始競合等の既存design debtも継続。詳細は設計書へ集約した。

## AI Responsibilities / Temporary Constraints

恒久的な責任分担はAGENTS.mdに従う。今回の対象は6A-2-2 Work / Item Domain・DB基盤と関連targeted testのみ。DB接続・適用、commit / push / mergeは行わない。

**今回の変更をChatGPTでレビューしてから次のタスクを決める。6A-2-3 Work生成・工程開始統合へ続けて進まない。** レビュー後も具体的な作業はユーザーの指示に従う。

## New Chat Startup

1. AGENTS.md、DEVELOPMENT_HANDOFF.md、Phase 6A設計方針を読む。
2. actual Git branch / HEAD / working treeと照合する。保存済みorigin参照と最新リモート照会を区別する。
3. 本書のテスト結果は記載時点の実績として扱い、後続変更の成功へ流用しない。
4. 今回のMarkdownに対するChatGPTレビュー状況と、次の依頼範囲を確認する。
