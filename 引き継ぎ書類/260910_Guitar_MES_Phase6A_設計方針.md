# Guitar MES Phase 6A 設計方針

- 作成日: 2026-09-10
- 対象: Phase 6A ギターパーツ取付工程
- 改訂日: 2026-09-15
- 文書状態: 6A-1は実装・ChatGPTによる実画面確認完了（ユーザー報告）。実装結果を反映した本MarkdownはChatGPTレビュー待ち。6A-2 / 6A-3は未実装。
- 恒久ルール: [AGENTS.md](../AGENTS.md)
- 現在地・Git・検証状況: [DEVELOPMENT_HANDOFF.md](../DEVELOPMENT_HANDOFF.md)
- 全体計画: [新開発ロードマップ](260902_Guitar_MES_新開発ロードマップ改訂版.md)

## 1. 目的と境界

まずStratocaster系のギターパーツ取付工程を対象に、製品仕様から必要な作業を判断し、現場で作業と検査を記録できるようにする。

将来的には製品詳細ページを現場で参照できる「電子仕様書」とする。Phase 6Aで全製品系列・全工程の専用画面まで展開することは意味しない。調整・調音、最終検品、Body・Neck専用工程への展開は後続計画とする。

この文書では、ユーザーが示した基本方針と、詳細設計で決める事項を区別する。クラス名や列名の候補を確定済みスキーマとして扱わない。

## 2. 開発単位と6A-1の確定仕様

**6A-1は「Productパーツ取付仕様の基盤・登録編集・電子仕様書表示まで実装完了」**とする。次は6A-2（工程内作業記録基盤）、その後に6A-3（専用画面・工程連携）。本書の更新を理由に後続フェーズへ着手しない。

| 完了した単位 | 実装内容 |
| --- | --- |
| 6A-1-1 | ProductPartsSpec Domain / Repository / DB |
| 6A-1-2A | Service、validation、既存Productと共通の製造開始後更新制限 |
| 6A-1-2B | 登録・編集UI、フォーム全体のtransaction、JUnit・登録編集E2Eの追加 |
| 6A-1-3 | 製品詳細への読み取り専用の電子仕様書表示 |

### 2.1 保存構造・状態モデル

`Product` 1 : 0..1 `ProductPartsSpec`。Spec側からProductへの片方向one-to-oneで、Product Entityへの逆方向関連はない。LAZY、cascadeなし、orphanRemovalなし。SpecがなくてもProduct自体は存在できる。

テーブルは`m_product_parts_spec`。`id`はbigint IDENTITYの主キー、`product_id`はNOT NULL、`m_product(id)`へのFKとUNIQUEを持つ。13仕様列はNULL許可・DEFAULTなし。Enumは`EnumType.STRING`、DB側にもコードのCHECK制約がある。既存Productへの自動補完は行わない。

- **Spec Entityなし**: 「パーツ取付仕様：未設定」。Productは登録・存在可能。
- **Spec Entityあり**: アプリケーションServiceではvalidationを通過した完全な仕様だけを作成・更新する。下書き・部分保存はない。

**DB nullableはアプリケーション上の部分保存許可を意味しない。** DBへ直接投入された不完全データまで完全性を保証するものではないため、表示はNULLに防御的に対応する。

Repositoryは`findByProductId` / `existsByProductId`を提供し、画面からの取得・保存はServiceを経由する。入力は`ProductPartsSpecRequest`に13項目を持ち、Product IDはService引数で渡す。Spec IDや関連Productをフォーム入力させない。

SQL（今回は内容確認のみ。DBへの適用確認・操作なし）:

- 適用: `sql/260910_01_create_product_parts_spec.sql`
- 確認: `sql/260910_02_verify_product_parts_spec.sql`（列・型・NULL・DEFAULT・制約・index）
- ロールバック: `sql/260910_03_rollback_product_parts_spec.sql`（Specテーブルのみ削除、CASCADEなし）

### 2.2 正式な13項目

必須・任意はServiceの規則。DB列はすべてNULL許可である。

| 領域 | 項目 | Java型 / 保存長 | Service入力 |
| --- | --- | --- | --- |
| Bridge | `bridgeType` | BridgeType / 32 | 必須 |
| Bridge | `bridgeModel` | String / 255 | 任意 |
| Bridge | `requiresStudHoleExpansion` | Boolean | 必須、方式別制約あり |
| Tuner | `tunerModel` | String / 255 | 必須 |
| Tuner | `tunerMountingType` | TunerMountingType / 32 | 必須 |
| Tuner | `tunerBushRequired` | Boolean | 必須、方式別制約あり |
| Tuner | `tunerLayout` | TunerLayout / 32 | 必須 |
| Electronics | `selectorPositions` | Integer | 必須、0より大きい |
| Electronics | `controlLayout` | String / 255 | 必須、表示・確認用 |
| Electronics | `jackMountingType` | JackMountingType / 32 | 必須 |
| String | `stringMaker` | String / 150 | 任意 |
| String | `stringModel` | String / 255 | 必須 |
| String | `stringGauge` | String / 100 | 必須、厳しい形式regexなし |

`pickupLayout`は既存`Product.pickupLayout`を再利用し、Specへ重複保存しない。

正式名称は`JackMountingType` / `jackMountingType`。旧候補の`JackType` / `jackType`から変更した理由は、`BOAT_PLATE`が電気的種別でなく取付形状を表すためである。

`bridgeInstallationProcess`は追加していない。Floyd Roseの取付工程等は仕様から導く将来のService作業ルールに属する。`tunerType`もメーカー・構造・取付方式の意味が曖昧なため追加していない。

### 2.3 Enumとvalidation

| Enum | 保存コードと既存表示名 |
| --- | --- |
| BridgeType | SIX_POINT＝6点支持、TWO_POINT＝2点支持、FLOYD_ROSE＝Floyd Rose |
| TunerMountingType | PRESS_BUSHING＝圧入ブッシュ式、NUT_FASTENING＝ナット固定式 |
| TunerLayout | SIX_IN_LINE＝6連 |
| JackMountingType | BOAT_PLATE＝舟形ジャックプレート |

- SIX_POINT: `requiresStudHoleExpansion=false`のみ。true / nullは不可。
- TWO_POINT / FLOYD_ROSE: `requiresStudHoleExpansion`はtrue / falseを明示し、null不可。方式から値を自動推測しない。
- PRESS_BUSHING: `tunerBushRequired=true`のみ。false / nullは不可。
- NUT_FASTENING: `tunerBushRequired`はtrue / falseを許可し、null不可。
- `selectorPositions`は必須かつ正数。pickupLayoutから算出しない。
- 文字列はtrimし、必須blankはエラー、任意blankはnullに正規化する。長さはDBのvarcharと揃え、Unicodeコードポイント数で検証する。

### 2.4 登録とtransaction

登録UIは「共通仕様＋複数variation」を維持し、パーツ仕様を共通欄で1回入力する。保存時は各Productへ独立したSpecを生成し、複数Productで同じSpec行を共有しない。

`ProductFormService.createProductVariations()`がProduct群と各Specの作成を1 transactionで扱う。途中のSpec作成が失敗すれば先に作成したProduct / Specもロールバックする。

全項目未入力ならProductのみ作成する。Booleanのfalseも入力済みとして扱い、部分入力は未設定として無視せずService validationエラーにする。

### 2.5 初回補完と製造開始後更新制限

**移行期の初回補完**: SpecなしProductには、Guitarが参照済み、または生産計画の開始・完成実績があっても初回登録を許可する。導入前の既存Productを補完するための措置だが、現在は作成時期を判定していない。新規ProductがSpec未設定のまま製造へ進んだ場合も同じルールが適用される。これは受容している移行期仕様であり、対象・期限の見直しをdesign debtとして残す。

Specの削除機能はアプリケーションのService / UIにはないため、削除→再登録で制限を回避する通常操作経路はない。Repositoryの汎用CRUDや直接DB操作まで存在しないという意味ではない。

**登録済みSpec**: 製造開始前は更新可能。次のいずれかを満たすと`ProductService.validateManufacturingSpecificationChange()`の共通判定で更新禁止になる。

- GuitarがそのProductを参照している。
- 関連ProductionOrderの`startedQuantity > 0`。
- 関連ProductionOrderの`completedQuantity > 0`。

同じ値を再送してもSpecの更新処理を許可しない。初回補完は恒久的な「製造開始後の仕様変更許可」ではない。

| 編集画面の状態 | 現在の挙動 |
| --- | --- |
| Specなし | 空欄。全項目未入力なら未設定を維持、入力すれば初回登録 |
| Specあり・製造開始前 | 保存値表示、更新可能。全項目消去は削除ではなくvalidationエラー |
| Specあり・製造開始後 | 欄を非活性化し理由表示。未送信のSpecは維持し、Product名等の許可項目は保存可能 |
| Specなし・製造開始後 | 移行措置として初回補完可能 |

`ProductFormService.updateProduct()`でProduct / Spec更新を同一transactionにまとめる。非活性欄を改ざんして値をPOSTしてもSpec Serviceの制限を通す。製造開始と更新が同時進行する競合への保証は別課題（第8節）。

### 2.6 電子仕様書表示

製品詳細の順序はProduct Information → Body Master → Neck Master → Parts Specifications → Related Guitars。既存の製品画像表示・操作は維持する。

1枚のパーツ取付仕様カードにBridge / Tuner / Electronics / Stringを配置し、ElectronicsのPU構成は`Product.pickupLayout`を表示する。Specありは読み取り専用、なしは「パーツ取付仕様：未設定」。Enumは既存`getLabel()`、Booleanは必要 / 不要、null / blankは「-」。詳細の`tunerBushRequired`ラベルは「ブッシュ要否」（登録・編集の「ペグブッシュ」は今回変更していない）。

Controllerは既存`ProductPartsSpecService`から取得し、小さな`ProductPartsSpecView`で表示値を整える。画像登録エラーによる再表示でも同じカードを表示する。製造開始前後を問わず現在のProduct正式仕様を参照する画面であり、個体の過去仕様・snapshot表示ではない。

### 2.7 推測禁止と対象分類

Product masterに明示された値をsource of truthとする。Product名、modelNo、internalModelCode、pickupLayout、bridgeModel、tunerModel等から不足仕様を勝手に推測・補完しない。

将来のST対象判定は既存InstrumentTypeMaster・MES内部モデルコードの正式分類と`resolveProductClassification()`相当のロジックを再利用する方針を維持する。正式分類の利用と不足パーツ仕様の推測は別である。Productへの新規InstrumentType FKは追加していない。6A-1のSpec保存はST限定判定を追加しておらず、作業対象への接続は後続課題とする。

## 3. 情報の責務分離

| 情報 | 意味 | Phase 6Aの扱い |
| --- | --- | --- |
| 製品仕様 | 何を作る製品か | ProductPartsSpecにパーツ仕様を保存し、既存Product.pickupLayoutも再利用する |
| 作業指示 | その仕様なら何をする必要があるか | 製品仕様からService側のルールで必要作業を決定する方向で検討 |
| 作業実績 | 実際に何を行ったか | チェックや途中保存を工程内作業記録として保持 |
| 検査結果 | 結果がOK / NGのどちらだったか | 作業実施のチェックと区別し、NGコメントも記録 |

作業項目定義そのものをPhase 6AでいきなりDBマスタ化しない。これは「実行時に確定した作業項目をDBに保存しない」という意味ではない。実行時の指示と記録は保存し、再表示で復元できる構造にする。

既存`ProcessHistory`はGuitar・工程・作業者・開始日時・終了日時を管理する汎用履歴として維持する。工程固有のチェック項目や検査結果を直接追加しない。

別層の名前は`ProcessWork` / `ProcessWorkItem`等を候補とする。履歴に対応する作業記録と、その配下の確定済み項目・実施結果を表現する方向で検討するが、関連の多重度、具体的なEntity名、DB列は未確定。旧ロードマップの`ProcessTaskDefinition` / `ProcessTaskHistory`は旧候補であり、そのまま実装する指示ではない。

## 4. 最初の対象作業

| 作業グループ | 製品仕様による分岐・対象作業 |
| --- | --- |
| トレモロユニット・スプリングハンガー取付 | 6点支持 / 2点支持 / Floyd Roseで作業内容を分岐 |
| ピックガード・舟形ジャック取付 | 穴加工、配線、半田付け、PU音出し、セレクター全ポジション確認、コントロール確認、不具合確認、固定 |
| ペグ取付 | 使用ペグ（tunerModel）、ブッシュ有無、固定方式によって分岐。Kluson / Schaller等を曖昧なtunerType分類として追加しない |
| 弦巻 | 製品仕様に登録された指定弦を使用 |

これらは作業グループと分岐軸の指定であり、個々の必須チェック、検査基準、作業順、数値規格をすべて確定したものではない。穴寸法、締付値、弦ゲージなどを推測で設定しない。

## 5. 開始時点の作業項目の保存

目指す流れ:

1. 作業開始時点の製品仕様を参照し、必要仕様の不足をServiceで検証する。不足時は開始しない。
2. Service側のルールから、その個体に必要な作業項目を決定する。
3. 決定した項目を工程内作業記録側へ保存する。
4. 途中保存・再表示・完了判定では、保存済みの項目を使用する。

後から製品マスタを変更しても、開始済み個体の作業内容が勝手に変わらないことを原則とする。画面を開くたびに最新マスタから項目を再生成する方式は、この原則と整合しない。

確定項目に加えて、分岐根拠の仕様値、表示文言、ルールの版などをどこまで保存するかは6A-2で判断する。「作業開始」が既存工程開始と同時なのか、専用画面での作業開始なのかも設計時に確定する。

## 6. 専用画面と既存工程処理の接続

6A-3の対象機能は、必要作業の表示、作業チェック、OK / NG検査、NGコメント、途中保存、完了条件判定、工程終了。

工程終了そのものは既存`ProcessService.endProcess()`を再利用する。工程遷移、履歴終了、業務日時や完成数量更新を専用画面側へ複製しない。完了条件は画面だけでなくService側で判定する。

必須作業が未完了なら工程終了できないというロードマップの原則を維持する。ただし、NG時の終了可否、再検査、コメント必須条件、必須・任意項目の区別などは詳細仕様が必要。既存の個別・一括終了や直接リクエストが完了条件を迂回しない接続方法も、6A-3着手前に決める。

作業記録の保存と工程終了のトランザクション境界、途中保存と終了の同時操作、二重終了・作業項目の重複生成の防止は、既存の排他・全件検証方針と照合して設計する。

## 7. 段階ごとの完了判定の観点

- **6A-1（完了）**: 第2節の基盤・登録編集・電子仕様書を実装済み。テストの確認時点・出典はHandoffに記録する。
- **6A-2**: 汎用履歴と工程固有記録が分離され、必要項目が開始時点で保存されること。途中保存・再表示で復元でき、製品変更で開始済み記録が変化しないこと。
- **6A-3**: 各分岐の必要作業と検査を記録でき、確定した完了条件をServiceで判定して既存工程終了に接続できること。

実装時の検証候補は、仕様分岐、仕様未設定、開始後のマスタ変更、途中保存と再読込、未完了・NGの終了制御、個別・一括経路、同時保存・終了、既存工程の回帰。具体的なテストは各段階の仕様確定後に作成する。検証の責任分担・E2E専用DB・fixture cleanupはAGENTS.mdに従う。

## 8. 6A-2以降へ引き継ぐ設計課題

解決済み: Spec初回作成は存在確認に加えてDB UNIQUEで二重行を防御する。create / updateおよびフォーム全体のtransactionは実装済み。UNIQUE競合はDB整合性例外として伝播しロールバックする。これをすべての並行性問題の解決とみなさない。

未解決 / 要レビュー:

1. 初回補完の対象・期限。導入後Productにも適用される移行措置の見直し。
2. 同時初回作成の競合時の利用者向けエラー扱い、およびSpec更新と製造開始のrace。現在の存在確認・業務判定・transactionだけで競合を解消済みとはしない。
3. work開始のタイミング、必要仕様検証、必須作業項目の導出条件、ST分類不能・未対応製品の扱い。
4. work-start時のsnapshot transaction境界と二重作業生成防止。snapshotのEntity構造・保存範囲・version方式は未確定。
5. NG、再試験、コメント、未完了時の扱い、途中保存と完了の整合性。
6. generic / bulk工程終了が専用validationを迂回しない設計と`ProcessService.endProcess()`への統合。
7. 既存開始済み履歴との互換性、製品重複判定とパーツ差異の整合性、controlLayoutを構造化検査へ使う場合の再検討。

第3〜6節の責務分離・作業開始時snapshot原則を維持する。ProductPartsSpec自体をチェックリスト・作業結果テーブルとして使用しない。今回はこれらの課題を引き継ぐだけで、6A-2のEntity / API / UIを詳細設計しない。

本MarkdownをChatGPTでレビューしてから、ユーザーの次の指示に従う。文書更新だけで6A-2へ進まない。
