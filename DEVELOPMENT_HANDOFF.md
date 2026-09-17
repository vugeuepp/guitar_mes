# Guitar MES Development Handoff

Updated: 2026-09-17

## Repository State

- Repository: `vugeuepp/guitar_mes`
- Current branch: `feature/phase6a-process-work`
- 作業開始HEAD: `d78cbed23e7ed00ce5fd95c172775405e61aad43`（6A-3-3、Copilot成果物適用）
- 保存済みupstream: `origin/feature/phase6a-process-work`。作業開始時は保存済みupstreamとHEADが一致。6A-3-3仕上げのcommit / push結果はGit履歴と最新リモート照会で確認する。
- 作業開始時のworking treeはクリーン。6A-3-3 Copilot成果物は上記HEADでcommit済み、保存済みupstreamと一致。ChatGPT承認・push済みはユーザー報告。今回から、問題なく実装・検証できた場合のCodexによるcommit / pushをユーザーが許可。

## Current State / Next Work

**Phase 6A-1完了: Productパーツ取付仕様の基盤・登録編集・電子仕様書表示まで実装完了。** ChatGPTによる実画面確認も完了したとのユーザー報告を受け、確定仕様を文書化した。6A-1の確定仕様は以下のとおり。

| 完了範囲 | 内容 |
| --- | --- |
| 6A-1-1 | ProductPartsSpec Entity / Enum / Repository、適用・確認・ロールバックSQL |
| 6A-1-2A | Service、完全入力validation、製造開始後の更新制限 |
| 6A-1-2B | 共通Spec入力からvariationごとに独立保存、登録・編集UI、transaction、JUnit・E2E追加 |
| 6A-1-3 | Product詳細に電子仕様書カード、表示値変換、詳細テンプレートテスト |

**Phase 6A-2はChatGPTレビュー・完了判定済み（ユーザー報告）。6A-3-1はChatGPT承認済み（ユーザー報告）、6A-3-2で共通完了Validatorを個別/bulk終了へ接続済み。** 6A-3-3のWork表示・Item操作API/UIも実装済み。今回Copilot成果物をrepo文脈でレビューし、テストと最小修正を追加。6A-2 SQLのローカルDB適用・起動成功はユーザー報告。今回Codexは専用E2E DBでテストを実行したが、SQL/schemaの変更・適用は行っていない。

確定仕様は[Phase 6A設計方針 第2節](引き継ぎ書類/260910_Guitar_MES_Phase6A_設計方針.md#2-開発単位と6a-1の確定仕様)に集約する。ロードマップは前回の文書更新で設計書参照へ整合済み。今回はロードマップに残る6A-2未着手の旧記述のみ、完了の事実へ更新。

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
- Repository Testはコンパイルのみ、DB接続・SQL適用なし。この修正はその後ChatGPTレビュー・commit済み（ユーザー報告）。

### 6A-2-3 今回の実装・検証（2026-09-17、Codex）

- `ProductClassificationService`へ既存分類を抽出。正式な登録済みシリーズ＋楽器コードから`ST`を判定し、製品名・modelNo・pickupLayoutでは推測しない。対象／対象外／分類不能を区別し、分類不能時の工程開始可否は未決定。
- `ProductPartsSpecValidator`を保存と生成で共用。保存時のtrim等は維持。生成時は保存済みSpecを再検証するが変更・保存せず、ProductのpickupLayoutと合わせて14項目を値コピーする。
- `PartsInstallationWorkPlanGenerator`が不変のsnapshotと`itemKey / itemOrder`を返す。Bridge → Electronics → Tuner → String、1開始の欠番なし。Bridge3種、条件付き穴拡張、Floyd Roseのスタッド＋ハンガー、Electronics7件（穴あけ内包・音出し1件）、Spec値によるブッシュ、弦巻1件を実装。詳細はPhase6A設計書第4節。
- DB不要の新規テスト32件、既存ProductService26件・ProductPartsSpecService56件・Work/Item Domain10件、合計124件成功（失敗・エラー・skipなし）。対象6クラスをMaven Wrapper offline＋Mockito javaagentで実行。ログ: `/tmp/guitar-mes-6a23-final-tests.log`。
- 全ソース・テストのコンパイル成功。DB必須テスト・全通常テスト・E2Eは未実行。DB接続・SQL変更/適用、ProcessService・Controller/UI変更なし。

### 6A-2-4A 今回の実装・検証（2026-09-17、Codex）

- 個別`ProcessService.startProcess`のみ統合。既存Guitar PESSIMISTIC_WRITEと検証順を維持し、`processCode=GUITAR_PARTS_INSTALLATION`の場合だけHistory保存前にplanを生成する。
- STRAT_TARGETはHistory → Work → Items → Guitar更新。NON_TARGETとnull/別工程コードは従来のHistory開始。UNCLASSIFIABLEは今回の確定方針として開始拒否。対象のSpec不備もHistory保存前に拒否する。
- `PartsInstallationWorkWriter`はplanの14値・itemKey・itemOrderをそのまま使用し、Specを再取得しない。MANDATORYで開始transactionへの参加を要求。初期status・時刻は既存Domainを使用。例外は伝播する。
- Spec更新はProduct取得 → Spec取得 → validation → Guitar参照/ProductionOrder実績チェック → 保存。明示的lockなし。既にcommit済みの対象Guitarが存在する通常の個別開始ではSpec更新が参照チェックで拒否されるため、新規lockは追加しない。Guitar生成前にチェックを通過した更新とのrace、初回補完との可視性は別の既存課題で、全面解消済みとはしない。
- DB不要のtargeted/regression 11クラス、計160件成功（失敗・エラー・skip 0）。新規ProcessPartsInstallationStartTest 14件、既存ProcessService 16件・PageProgress 2件・Controller 4件、6A-2-3関連124件。Maven Wrapper offline＋Mockito javaagentで実行。ログ: `/tmp/guitar-mes-6a24a-final-tests.log`。
- 初回は例外設定時のMock回答呼出しでテスト側2エラー。doThrowへ修正し、限定再実行31件成功後、上記160件が成功。業務コードの回避変更なし。
- Spring transaction interceptor＋記録用transaction managerでHistory / Work / Item失敗時のrollback要求を検証。実DB上の行のrollbackは未検証。Writerの独立transaction開始拒否も検証。
- 全テストソースをコンパイル。DB必須テスト、全通常テスト、E2Eは未実行。DB操作・SQL変更・bulk/end/Controller/API/UI変更なし。

### 6A-2-4B 今回の実装・検証（2026-09-17、Codex）

- bulkはID重複除去・昇順PESSIMISTIC_WRITE → 全件既存validation → 全件plan生成 → 全History保存 → 対象Work/Items保存 → 全Guitar更新。途中のvalidation失敗では保存しないall-or-nothing。
- 個別と同じprocessCode判定・3分類。STRATのみWorkあり、NON_TARGET混在を許可、分類不能・Spec不備は全体拒否。Guitar IDでplanを保持し、保存済みHistoryのguitarIdで照合する。Spec再取得・再導出なし。
- 共通startTime、workerNameのtrim、重複ID除去を維持。既存WriterとMANDATORYを変更せず再利用。個別start・end・Entity・SQL・Controller/API/UIは変更なし。
- 新規bulk Unit Test 12件。全台Strat、混在、null/別code、最後のplan失敗、既存validation優先、昇順lock、共通時刻、snapshot14値、Item、保存結果の順序を反転した対応確認、途中保存失敗のrollback要求を検証。
- targeted 3クラス42件成功後、DB不要と確認した46クラス430件成功（失敗・エラー・skip 0）。個別start14件、generator・分類、Product/Spec、Domainを含む。Maven Wrapper offline＋Mockito javaagentを使用。ログ: `/tmp/guitar-mes-6a24b-targeted.log`、`/tmp/guitar-mes-6a24b-dbfree.log`。選択一覧: `/tmp/guitar-mes-6a24b-test-selection.txt`。
- DB必須9クラスは未実行、全テストソースはコンパイル済み。通常全件・E2Eは未実行。実DBのrollback・並行実行は未検証。DB接続・SQL変更/適用なし。Spec更新とGuitar生成の既知raceは持ち越し、新規lockなし。
- processCode、Domain/DB、plan導出、個別/bulk統合まで実装済み。ロードマップには過去の「設計中・未着手」記述が残るため、正式な6A-2完了・6A-3移行はChatGPT確認後とする（今回はロードマップ未変更）。

### 6A-3-1 実装・検証（2026-09-17、Codex）

- generic process.workへProcessWorkItemService / ProcessWorkCompletionValidatorを追加。Item ID単位でcheck / uncheckをtransactionalに保存する。実際の変更時だけcompletedAt / updatedAtを同じ操作時刻で更新し、再操作は時刻保持・saveなし。終了済みHistoryでは冪等な再操作も拒否する。
- Itemから親History IDだけをscalar queryで取得 → 既存findForUpdateでHistoryをPESSIMISTIC_WRITE → History再読込・終了確認 → Item取得・再読込 → 状態変更・保存。ロック待機前のPersistence Context内の古い値をrefreshで排除する。Item / Work / Guitarを先にロックしない。
- 同じHistoryのItem操作と既存endは親Historyロックで直列化する。単独Item操作はHistoryロック1件のみ取得し、終了処理のHistory → Guitar順を逆転させない。6A-3-2の終了Validator呼出しもHistoryロック取得後・同一transaction内とする。実DB並行テストは未実施。
- Validatorは受け取ったHistoryを再取得・再ロックしない。Workなしは許可、Workありなら全Item COMPLETEDが必要。0件・未完了・null statusは拒否。状態を変更しない。今回はendへ未接続。
- targeted 4クラス32件成功後、DB不要48クラス452件成功（失敗・エラー・skip 0）。新規Item Service16件・Validator6件。既存個別/bulk開始・generator・分類・Product/Spec・Domainも再実行。ログ: `/tmp/guitar-mes-6a31-targeted.log`、`/tmp/guitar-mes-6a31-regression.log`。選択一覧: `/tmp/guitar-mes-6a31-test-selection.txt`。
- 親History ID問い合わせのRepository Testを1件追加し、全テストソースをコンパイル。DB必須9クラスは未実行。DB操作・SQL/schema・ProcessService・Controller/API/UI変更なし。通常全件・E2Eは未実行。
- 6A-3-2は個別/bulk終了へ共通Validatorを同時接続する。Workなし互換、全件検証後更新、History lock維持を確認する。既知のSpec更新race・直接currentProcess APIの迂回対策は持ち越し。

### 6A-3-2 実装・検証（2026-09-17、Codex）

- ProcessServiceの個別/bulk終了へ既存ProcessWorkCompletionValidatorを接続。Workなしはlegacy互換、Workありは1件以上のItemが全件COMPLETED必須。0件・NOT_STARTED・null statusを拒否。Product / Spec / classification / snapshotの再判定なし。
- 個別はHistoryのPESSIMISTIC_WRITE取得・既存終了済み/工程検証後、Guitar取得前に検証。bulkはHistory ID重複除去・昇順ロック、既存Guitarロック・工程検証を維持し、全件Work検証後にOrderロック・終了更新へ進む。最後の検証失敗でも先行History/Guitar/Orderを変更しない。
- 検証と終了更新は既存の同一transaction。Item操作とendは同じHistoryロックで直列化する。Validator / ItemService / start処理・終了日時・完成数量の既存ロジックは変更なし。実DBでの並行テストは未実施。
- Web/APIの個別/bulk終了4経路がProcessServiceへ委譲することを確認。Controller/API/UI変更なし。専用UIは未実装。direct currentProcess APIの迂回対策は持ち越し。
- 新規ProcessWorkEndIntegrationTestは実Validatorとmock Repositoryを使うDB不要14件。既存4テストクラスはconstructor依存追加へ追従。初回testCompileでテスト修正の引数誤りを検出・修正後、targeted 4クラス52件成功。DB不要49クラス466件成功（失敗・エラー・skip 0）。個別/bulk開始、Item操作、generator/分類、ProductionOrder完成の回帰を含む。
- ログ: `/tmp/guitar-mes-6a32-targeted.log`、`/tmp/guitar-mes-6a32-regression.log`。選択一覧: `/tmp/guitar-mes-6a32-test-selection.txt`。全テストソースのcompile成功。DB必須9クラス・全通常テスト・E2Eは未実行。DB操作・SQL/schema変更なし。
- 6A-3-3 UIへは進まず、commit / push後のGitHub差分をChatGPTでレビューしてから次の指示を受ける。

### 6A-3-3 仕上げ・検証（2026-09-17、Codex）

- 開始時HEAD d78cbedを親634b28dとの差分でレビュー。remote HEADも照会して一致を確認。History基準、保存済みWork snapshot14項目・itemOrder、4グループ、日本語表示、即保存API委譲、read-only、Workなし拒否・0件不整合、既存終了routeの再利用を確認。
- 通常テスト8 Errorsを修正。GuitarRepositorySearchTestのProcessServiceは4クエリ性能検証に必要なため維持し、検索で使わない開始/終了依存3Beanのみmock。ProcessConcurrencyTestは自前のシリーズ・楽器マスタとinternalModelCodeで正式分類できるNON_TARGET fixtureに追従し、自分のデータだけcleanupする。本番分類・Work生成・開始終了の業務ロジックは未変更。
- checkboxの並行応答で進捗が古く表示される可能性を防ぐため、保存中は同一作業票のcheckboxと工程終了ボタンを一時無効化。失敗時はチェックを復元しメッセージ表示、操作を再開する。
- targeted DB 2クラス20件、表示/API targeted 4クラス11件、通常テスト全554件成功（すべて失敗・エラー・skip 0）。ログ: `/tmp/guitar-mes-6a33-targeted-db.log`、`/tmp/guitar-mes-6a33-ui-targeted.log`、`/tmp/guitar-mes-6a33-all-tests.log`。
- PartsInstallationWorkE2Eを追加。e2e profile・random portでブラウザ対象アプリを自動起動し、fixtureと同じguitar_mes_e2eを使用。独自History/Work/Item/Guitar/工程を作成・ID限定cleanup。即保存・再読込・解除・失敗復元、個別/bulk終了画面のリンク、既存route終了、終了後API拒否・read-only・0件不整合の2シナリオ成功。ログ: `/tmp/guitar-mes-6a33-e2e-retry.log`。初回はテストのFormData型を修正。サンドボックス内のブラウザ初期化停止は中断し、許可された環境で再実行成功。全E2Eは未実行。
- 実テンプレート描画・ブラウザ証跡も確認。DB/schema/SQL、enum値、Product/Spec、ProcessService業務ロジックは未変更。テストは専用DBの既存スキーマを利用。
- AGENTS.mdにfallback入口、COPILOT_WORKFLOW.mdに完全版TXT・fresh chat・成果物配置確認・復帰後repoレビューの恒久ルールを追記。6A-3-4 navigation/UX、6A-3-5 direct currentProcess迂回、spec-start raceへは進まない。
- 終了一覧のWork有無問い合わせは現状Historyごとの取得。6A-3-4で一覧規模と合わせて一括取得を検討する。設計書7.2の「未実装」は6A-3-3着手前の記述であり、今回の実装事実は本節を参照する。

## Phase 6A-2 Domain Design / Next Gate

正式方針・候補・未確定事項の詳細は[設計方針 第3〜8節](引き継ぎ書類/260910_Guitar_MES_Phase6A_設計方針.md)を参照する。

- History 1 : 0..1 Work 1 : N Item。process.workにEntity / Enum / Repositoryを追加。Work→Historyは片方向LAZY one-to-one、Item→Workは片方向LAZY many-to-one。逆参照・cascade・orphanRemovalなし。
- 対象の個別・bulk工程開始時にHistoryと同一transactionで14仕様snapshotとItemを生成する（6A-2-4A/B）。WorkはcreatedAtのみでstatus / updatedAt / productId snapshot / Spec FKなし。開始後にマスタ変更で再生成しない。
- Work snapshotは12項目NOT NULL、bridgeModel / stringMakerのみ任意。pickupLayoutはString / varchar(255)を採用（Productの実DB長はrepositoryから確認不能）。
- ItemはitemKey varchar(64)、itemOrderは正数・1始まり想定、status varchar(32)はNOT_STARTED / COMPLETED。Java初期値NOT_STARTED、DB DEFAULTなし。3 UNIQUE、Enum / 正数 / status-completedAt整合CHECKをSQLへ定義。
- Work createdAtは未設定時のみ初期化。Item作成時は未指定ならcreatedAtとupdatedAtを同時刻へ設定し、明示時刻は保持。更新時は既存のpersistedUpdatedAt比較方式を再利用。終了前のcheck/uncheck・終了後read-onlyは6A-3-1 Serviceへ実装済み。Item操作では親Historyをロックする。
- SQLは260916_04_create_process_work / 05_verify_process_work / 06_rollback_process_work.sql。新テーブル2件、既存データ補完なし。ユーザー側でローカルDB適用済み（ユーザー報告）。
- bulkは全台の検証・導出後に保存するall-or-nothing。個別・bulkとも対象StratのSpec不備・分類不能は開始拒否、明確な対象外は従来処理。bulkは全件validate/plan後に保存する。
- processCodeはString / VARCHAR(64)、NULL許可・全体UNIQUE。Entity、findByProcessCode、ProcessCodeConstants.GUITAR_PARTS_INSTALLATION、sql/260916_01〜03の適用・確認・rollback SQLを追加済み。対象GUITAR＋ギターパーツ取付が0件・複数件なら例外で移行STOP。ユーザー側でローカルDB適用済み（ユーザー報告）。API JSONへnullableのprocessCodeを追加。個別・bulk startのWork対象判定へ使用し、他の工程判定は変更なし。
- process.workへDomainを配置済み。process.partsinstallationに保存前planの作業導出を実装済み。個別・bulk startとも接続済み。既存ProcessWorkController名は再利用しない。
- 終了validationの個別/bulk共通利用は6A-3-2で実装済み。PUT /api/guitars/{id}のcurrentProcess直接更新は迂回経路候補として残す。

残論点はpickupLayout実DB長、NG等の拡張、再実施、専用UI、名前依存解消・直接更新API改修。Spec初回補完と製造開始競合等の既存design debtも継続。詳細は設計書へ集約した。

## AI Responsibilities / Temporary Constraints

恒久的な責任分担はAGENTS.mdに従う。今回の対象は6A-3-3仕上げ・通常テスト全件・対象E2E・fallbackルール整備。専用E2E DBでテスト実行可。DB/schema/SQL変更・mergeは禁止。仕様未決事項や重大問題がなければcommit / pushを行う（今回の明示指示）。

**commit / push後、GitHub差分をChatGPTでレビューしてから次のタスクを決める。6A-3-4 / 6A-3-5へ続けて進まない。** レビュー後も具体的な作業はユーザーの指示に従う。

## New Chat Startup

1. AGENTS.md、DEVELOPMENT_HANDOFF.md、Phase 6A設計方針を読む。
2. actual Git branch / HEAD / working treeと照合する。保存済みorigin参照と最新リモート照会を区別する。
3. 本書のテスト結果は記載時点の実績として扱い、後続変更の成功へ流用しない。
4. 今回のMarkdownに対するChatGPTレビュー状況と、次の依頼範囲を確認する。
