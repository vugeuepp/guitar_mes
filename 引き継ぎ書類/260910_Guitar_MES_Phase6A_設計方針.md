# Guitar MES Phase 6A 設計方針

- 作成日: 2026-09-10
- 対象: Phase 6A ギターパーツ取付工程
- 改訂日: 2026-09-17
- 文書状態: 6A-1・6A-2はChatGPTレビュー・完了判定済み（ユーザー報告）。6A-3-1はChatGPT承認済み（ユーザー報告）。6A-3-2で共通完了Validatorを個別/bulk終了へ接続済み。Work操作用Controller/API/UIは後続。6A-2 SQL適用・ローカル起動成功はユーザー報告。CodexによるDB操作なし。
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

6A-2-3のST対象判定は既存InstrumentTypeMaster・MES内部モデルコードの正式分類を再利用する。`resolveProductClassification()`相当のロジックを`ProductClassificationService`へ抽出した。正式分類の利用と不足パーツ仕様の推測は別である。Productへの新規InstrumentType FKは追加していない。6A-1のSpec保存はST限定判定を追加しておらず、作業対象への接続は後続課題とする。

## 3. 6A-2 Domainの責務と関係（6A-2-2基盤実装済み）

| 構成 | 責務 |
| --- | --- |
| ProductPartsSpec | 現在の製品標準仕様。「何を作るか」 |
| ProcessHistory | 1回の工程実施。「この工程をいつ誰が実施したか」 |
| ProcessWork | その履歴の工程開始時に確定した作業指示・製品仕様snapshot |
| ProcessWorkItem | Workに属する個々の作業項目と実施状態 |

正式方針は`ProcessHistory 1 : 0..1 ProcessWork 1 : N ProcessWorkItem`。WorkはGuitar＋Processの組ではなく、1回のProcessHistoryに属する。同じGuitar・工程を将来再実施して新しい履歴を作る場合も、新しいWorkに分離できる。

Work→HistoryはWork所有の片方向`@OneToOne`、LAZY、cascadeなし、orphanRemovalなし。Historyへの逆参照は追加しない。DBは`process_history_id NOT NULL / FK / UNIQUE`。Item→Workも片方向`@ManyToOne`、LAZY、optional=false、cascadeなし。Workに子Listを追加しない。FKのON DELETE CASCADEなし。

Product master変更後も開始済みWork / Itemを再生成・自動変更しない。作業項目定義自体を直ちにDBマスタ化せず、Serviceで導出して実行時の項目を保存する。ProductPartsSpecを作業結果テーブルとして使用しない。

### 3.1 ProcessWorkと14項目snapshot

実装項目は`id`、`processHistory`、`createdAt`と次の14項目。

| 領域 | snapshot項目 |
| --- | --- |
| Bridge | bridgeType、bridgeModel、requiresStudHoleExpansion |
| Tuner | tunerModel、tunerMountingType、tunerBushRequired、tunerLayout |
| Electronics | pickupLayout、selectorPositions、controlLayout、jackMountingType |
| String | stringMaker、stringModel、stringGauge |

13項目はProductPartsSpec、pickupLayoutのみProductから開始時に値をコピーする。ProductPartsSpecへのFKは持たない。13項目は第2節のJava型・保存長を再利用。pickupLayoutはString / varchar(255)。ProductのJava型はString、明示@Column長・該当DDL・上限validationはrepositoryから確認できないため、JPA標準長255を基準に採用した。実DB長と完全一致すると断定しない。ProductServiceはPU構成を必須入力として扱う。

snapshot部分は原則immutable。WorkはcreatedAtのみを持ち、updatedAtとstatusは現時点で持たせない。Item操作でもsnapshotを書き換えず、工程全体の状態はHistory、工程内の実施状態はItemをsource of truthとする。

productIdの追加snapshotは6A-2では行わない。通常運用にGuitar生成後のProduct差し替え機能がなく、History→guitarId→Guitar→Productで追跡できるため。将来の監査要件による追加余地は残す。

6A-2-2の明示仕様でNULL制約を確定した。bridgeModel / stringMakerのみ任意、他の12 snapshot列はNOT NULL。既存ProductPartsSpecの移行用nullableとは意味を分け、有効な開始時仕様を保存する。Enumは既存4型を再利用してSTRING保存し、同じEnum CHECKを設定する。業務validation・値コピーは6A-2-3で保存前planに実装した。Entity保存への接続は6A-2-4A/Bで個別・bulk startに実装した。既存仕様を推測して補完しない。

Work.createdAtはLocalDateTime / timestamp without time zone、NOT NULL、updatable=false。@PrePersistで未設定時のみ現在時刻を設定する。snapshotの業務上の変更禁止は後続Serviceの責務とし、今回のsetterを更新許可APIとして扱わない。

### 3.2 ProcessWorkItem

実装項目は`id`、`processWork`、`itemKey`、`itemOrder`、`status`、`completedAt`、`createdAt`、`updatedAt`。順序名は既存processOrderと揃え、sequenceではなくitemOrderを採用する方針。生成時の作業順序を保存する。

itemKeyはProcessWorkItemKeyをEnumType.STRING、VARCHAR(64)＋CHECKで保存する。穴あけは各取付作業に含める。今回getLabel()等の表示APIは追加しない。本番保存後の安易なEnum名renameは永続コードを変えるため禁止する設計意図とする。ordinal保存はしない。

| 日時 | 意味 |
| --- | --- |
| createdAt | Itemが生成された時刻 |
| updatedAt | Itemの状態を最後に変更した時刻 |
| completedAt | 現在のCOMPLETED状態になった時刻 |

statusはProcessWorkItemStatusのNOT_STARTED / COMPLETEDのみ。EnumType.STRING / VARCHAR(32)＋CHECK、NOT NULL。初期値NOT_STARTEDはJava field initializerで設定し、DB DEFAULTは設けない。

| 状態・操作 | completedAt / updatedAt |
| --- | --- |
| NOT_STARTED | completedAtはnull |
| COMPLETED | completedAtは非null |
| 工程終了前のチェック解除 | NOT_STARTED、completedAt=null、updatedAt=now |
| 再チェック | COMPLETED、completedAt=now、updatedAt=同じnow |

6A-2-2でstatusとcompletedAtの上記整合をDB CHECKとして実装した。保存時に両列を同時に正しく設定する前提であり、callbackはstatus / completedAtを変更しない。complete / uncomplete等の業務操作は今回追加しない。

Item.createdAt / updatedAtはLocalDateTime / timestamp without time zone、NOT NULL、createdAtはupdatable=false。新規時に未指定ならcreatedAt=now、updatedAt=createdAtとして揃える。明示された時刻は保持する。@PreUpdateは既存のpersistedUpdatedAt比較方式で通常編集をnowへ更新し、明示イベント時刻は保持する。DB DEFAULTは設けない。

itemOrderはInteger / integer、NOT NULL、CHECK(item_order > 0)。開始番号1を想定し、同一Work内のUNIQUEを設定する。

誤操作修正のため工程終了前のチェック解除を可能にする方向。History.endTime設定後はWork / Itemをread-onlyにする業務制御を後続Serviceで実装する（今回未接続）。NG / RETEST_REQUIRED / comment等は未確定であり、ここへ先回りして追加しない。

### 3.3 一意制約と並行性

6A-2-2で実装したUNIQUEは以下。

- Work: UNIQUE(process_history_id) — 同じ履歴への二重生成防止。
- Item: UNIQUE(process_work_id, item_key) — 同じ作業キーの二重生成防止。
- Item: UNIQUE(process_work_id, item_order) — 同じWork内の順序重複防止。

存在確認だけで並行性を保証せず、既存GuitarのPESSIMISTIC_WRITE、transaction、DB UNIQUEを組み合わせる。将来同じitemKeyを複数回必要とするならitemIndex等を含むモデルを再検討し、現時点では追加しない。

### 3.4 基盤実装の配置・SQL

- package: `process.work`。Work / Item / ItemKey / ItemStatusと2 Repository。
- `ProcessWorkRepository.findByProcessHistoryId(Long)`。
- `ProcessWorkItemRepository.findByProcessWorkIdOrderByItemOrderAsc(Long)`。
- 適用: `sql/260916_04_create_process_work.sql`。t_process_work（17列）とt_process_work_item（8列）を作成。PK 2、FK 2、UNIQUE 3、CHECK 8。制約名はpk_ / fk_ / uk_ / ck_の既存規約。
- 確認: `sql/260916_05_verify_process_work.sql`。全列の型・長さ・NULL・DEFAULT・IDENTITYと15制約の存在・種別・定義、indexを確認。
- rollback: `sql/260916_06_rollback_process_work.sql`。Item→Workの順に削除、CASCADEなし。既存テーブルは変更しない。
- DB接続・SQL適用は行っていない。Domainテスト実績とRepositoryテスト未実行の詳細はHandoffを参照。Work生成・工程開始・終了処理は未接続。

## 4. 初期作業項目と導出方針

Stratocaster系のギターパーツ取付を初期対象とする。以下の16 itemKeyは6A-2-2でEnumへ定義した。6A-2-3で以下の導出を保存前planに実装した。詳細作業標準の追加に応じて拡張可能。穴寸法・締付値等を推測で定めない。

| グループ・条件 | 生成する項目（記載順） |
| --- | --- |
| Bridge / SIX_POINT | BRIDGE_SIX_POINT_INSTALL → BRIDGE_MOVEMENT_CHECK → SPRING_HANGER_INSTALL |
| Bridge / TWO_POINT | requiresStudHoleExpansion=trueならSTUD_HOLE_EXPANSION → STUD_INSTALL → BRIDGE_TWO_POINT_INSTALL → SPRING_HANGER_INSTALL |
| Bridge / FLOYD_ROSE | requiresStudHoleExpansion=trueならSTUD_HOLE_EXPANSION → STUD_INSTALL → SPRING_HANGER_INSTALL |
| Electronics（レビュー確定） | PICKGUARD_INSTALL → JACK_PLATE_INSTALL → JACK_WIRING → GROUND_WIRING → ELECTRONICS_SOUND_CHECK → ELECTRONICS_PARTS_CHECK → ELECTRONICS_FINAL_FASTENING |
| Tuner | tunerBushRequired=trueならTUNER_BUSHING_INSTALL、その後TUNER_INSTALL |
| String | STRING_INSTALL |

Electronicsは6A-2-2のChatGPTレビューにより上記7キーへ確定した。チェック項目が過剰にならないよう、穴あけはピックガード／舟形ジャックプレートの各取付作業に含め、独立したWorkItemにはしない。Item総数は16のまま。

| itemKey | 作業の意味 |
| --- | --- |
| PICKGUARD_INSTALL | 穴あけを含むピックガード取付 |
| JACK_PLATE_INSTALL | 穴あけを含む舟形ジャックプレート取付 |
| JACK_WIRING | ジャックとピックガード側を接続する2本の配線 |
| GROUND_WIRING | スプリングハンガーからのアース線1本の配線 |
| ELECTRONICS_SOUND_CHECK | 全Pickup・全selector positionの音出し確認。position別Itemへ分割せずselectorPositions snapshotを作業指示に使用 |
| ELECTRONICS_PARTS_CHECK | 電装部品の不良確認 |
| ELECTRONICS_FINAL_FASTENING | 電装確認後の最終ねじ締め |

Bridgeの条件はSTUD_HOLE_EXPANSIONの有無にだけ適用し、後続の項目は含める。Floyd Roseはこの工程ではスタッド側の作業を扱い、ブリッジ本体は後工程の調整・調音で取り付ける。FLOYD_ROSE_BRIDGE_INSTALL等は追加しない。

selectorPositionsはELECTRONICS_SOUND_CHECKで「全Nポジション確認」のためにsnapshotを参照する。現時点でPositionごとにItemを分割しない。stringModel / stringGauge等は弦巻き時の参照情報として表示する方向。

Product名、modelNo、bridgeModel、tunerModel、pickupLayout等から不足仕様を推測しない。明示されたSpecと正式なProduct分類を根拠に導出する。

### 4.1 6A-2-3の保存前plan

`PartsInstallationWorkPlanGenerator.generate(Product)`は保存済みSpecをRepositoryから読み、対象なら`PartsInstallationWorkPlan`を返す。snapshotは第3.1節の14値ちょうどでEntity参照を含まない。保存値をtrim・補完・書き換えずコピーし、recordと変更不可Listで保持する。必須値欠落、不正な組合せ・文字数はBusinessExceptionとし、不完全なplanは返さない。pickupLayoutも必須・最大255文字を確認する。

各ItemはitemKeyとitemOrderのみ。全体はBridge → Electronics → Tuner → Stringの順、itemOrderは1開始・欠番なし。status / completedAtやHistory IDは含めない。ProcessHistory / ProcessWork / ProcessWorkItem Entityの生成・保存は行わない。

TunerはPRESS_BUSHINGならbush=true必須、NUT_FASTENINGならtrue/falseに従う。nullは拒否。弦やペグの型番文字列で作業を推測せず、Stringは常に1件。Floyd Rose本体・独立穴あけItemは追加しない。

## 5. 対象判定と工程開始transaction

### 5.1 生成タイミング

対象製品のギターパーツ取付工程開始時に、Historyと同じtransactionでWork / Itemを生成する。専用画面初回表示・GETでは生成しない。Historyだけが存在してWorkがない中間状態や、同時閲覧による二重生成を避ける。

概念フロー:

GuitarのPESSIMISTIC_WRITE → 既存工程開始validation → 対象判定 → Product / Spec取得 → 工程開始可能validation → Item導出 → History生成 → Work snapshot生成 → Item生成 → Guitar.currentProcess更新 → COMMIT。

途中失敗は全体rollback。6A-2-4Aで個別`startProcess`に接続した。既存検証完了後、History保存前にplanを生成する。`ProcessCodeConstants.GUITAR_PARTS_INSTALLATION`とprocessCodeが一致する場合のみ対象判定し、表示名・固定IDでは判定しない。null/別codeはWorkなしの従来開始を維持する。

保存はHistory → Work → Items → Guitar更新。`PartsInstallationWorkWriter`は保存済みHistoryとplanだけを受け取り、14 snapshot値・itemKey・itemOrderをコピーする。Spec再取得・分類再判定・導出条件の複製なし。save / saveAllを使用し、cascadeは追加しない。Item初期状態NOT_STARTED / completedAt=null、作成日時は既存Domain callbackに任せる。

transaction境界は既存`startProcess`の@Transactional。WriterはMANDATORYで既存transactionへ参加し、独立したtransactionを開始しない。例外を握りつぶさず、Work / Item保存失敗時はGuitar更新へ進まない。DB不要テストで例外伝播・Springのrollback要求を検証したが、実DB上のrollbackは未検証。

6A-2-4Bでbulk開始を統合した。all-or-nothingで、IDの重複除去・昇順PESSIMISTIC_WRITE → 全台の既存validation → 全台の分類/Spec検証・plan生成を完了してから保存する。1台の分類不能・Spec不備でもpersistへ進まず、Guitarも変更しない。

検証済みplanはGuitar IDをキーにMapで保持する。全Historyを共通startTimeでsaveAllした後、保存済みHistory.guitarIdに対応するplanだけを既存Writerへ渡し、Work → Itemsの順で保存。最後に全Guitar.currentProcess / updatedAtを更新する。保存戻り値の並び順への依存、Spec再取得、分類再判定、個別startの反復呼出しはない。

STRAT / NON_TARGET混在を許可し、Historyは全台、Work / ItemsはSTRATだけ。既存bulkの@TransactionalとWriterのMANDATORYを維持する。途中保存失敗は例外を伝播して全体rollback対象とする。DB不要テストでrollback要求まで検証し、実DB rollbackは未検証。

### 5.2 対象・対象外・分類不能

正式分類上のStratocaster系を対象とし、製品名・modelNo・pickupLayout等から推測しない。既存InstrumentTypeMaster、internalModelCode、ProductSeriesMasterを照合する正式分類を再利用する。

6A-2-3で`ProductClassificationService.classify()`へ分類責務を抽出し、ProductServiceの既存編集処理も共用する。入力はinternalModelCode、結果はOptional<Classification>（seriesCode / instrumentCode）。末尾の登録済み楽器コードと残りの登録済みシリーズコードを照合する。既存の大文字化・trim・非activeマスタの解決を維持する。正式なStrat楽器コードは`ST`（既存マスタ登録画面例・ProductServiceテストと一致）。

生成結果は`STRAT_TARGET`（planあり）、`NON_TARGET`、`UNCLASSIFIABLE`（後二者はplanなし）を区別する。対象外・分類不能ではSpecを取得しない。対象のSpec不備は例外であり分類不能として扱わない。

- 6A対象Product: 必要Specが存在し、作業生成に必要な仕様が有効ならWork生成。
- Spec未設定・必要仕様不備の対象Product: 個別・bulk startともHistory保存前に拒否。
- 6A対象外と判定できたProduct: 当面はWorkを強制せず従来工程開始を維持。
- 分類不能Product: 6A-2-4Aで開始拒否を確定。個別・bulk startともHistory保存前にBusinessException。対象外と混同しない。

ProductPartsSpecServiceはSpec自体の保存・完全性を担い、Parts Installation側は工程開始可否とItem導出可否を担う。6A-2-3で純粋な`ProductPartsSpecValidator`を共用した。保存用normalizeAndValidateは従来の正規化を維持し、validateStoredは保存済み値を変更・保存せず同じ業務規則を検証する。

### 5.3 Spec更新との競合（6A-2-4A調査）

Spec更新はProduct取得 → Spec取得 → validation → Guitar参照有無 → ProductionOrder実績確認 → apply/saveAndFlushの順。Product・Spec・参照Guitarの明示的な悲観ロックは取得しない。通常の個別開始は既にcommit済みのGuitarを対象とするため、そのProductに対するSpec更新はGuitar参照チェックで拒否される。今回の開始統合に新しいlockは追加せず、既存Guitar lock順序を維持する。

ただし、Guitar生成より前に更新側の参照チェックが通過した場合のraceまで解消したわけではない。初回Spec補完のcommit前に開始側が読む場合は未登録として安全に拒否され、補完後に再試行できる。生成／補完を含む全フローの競合対策は既存design debtとして別途扱い、Guitar lockだけでSpec全体を排他できるとはしない。

### 5.4 package責務の候補

| package候補 | 責務 |
| --- | --- |
| process.work | ProcessWork / Item、Repository、Item状態更新、汎用取得 |
| process.partsinstallation | 対象判定、Spec開始可能validation、snapshot生成、Item導出、工程固有の完了判定 |

既存の汎用工程APIにProcessWorkControllerがあるため、将来の専用Controllerには同名を使わない。具体名・専用UIは未確定。

## 6. processCodeの段階導入方針

6A-2-1でManufacturingProcessへ表示名と別の安定processCodeを追加した。JavaはString、DBはprocess_code VARCHAR(64)、全体UNIQUE、NULL許可・DEFAULTなし。既存コード長は用途別で統一規約がないため、業務namespaceを含む安定コードとして64を採用。初期コードはProcessCodeConstants.GUITAR_PARTS_INSTALLATION。RepositoryにfindByProcessCodeを追加し、既存constructor・表示名定数・工程判定は維持する。APIは既存Entity返却を維持し、nullableのprocessCodeを追加項目として公開する。

6A固有の対象工程判定から段階利用し、既存全工程を一度にcodeへ置換しない。Body / Neck / currentProcess等の名前依存は別design debtとする。

**移行時STOP条件**: 本番・開発DBの初期投入方法・対象実データは未確認。適用SQLはtarget_type='GUITAR'＋process_name='ギターパーツ取付'がちょうど1件でなければDO blockのRAISE EXCEPTIONで失敗させる。列追加・UNIQUE・コード補完を同一transactionとし、ALTER TABLEのロックを保持して検証とUPDATE間の競合を防ぐ。未知の工程にはcodeを割り当てない。

SQLは以下を追加済み。ユーザーが6A-2-1 / 6A-2-2 SQLのローカルDB適用・アプリ起動成功を報告済み。CodexはDB接続・適用・実行検証を行っていない。

- 適用: `sql/260916_01_add_process_code.sql`
- 確認: `sql/260916_02_verify_process_code.sql`（列・長さ・NULL・DEFAULT、単独UNIQUE、初期コード1件と対象、重複）
- rollback: `sql/260916_03_rollback_process_code.sql`（初期コード解除→UNIQUE削除→列削除。後から付与したコードも失うため退避を確認。CASCADEなし）

テストの実行結果・未実施事項はHandoffを参照する。個別・bulk startのWork生成条件へprocessCodeを利用済み。終了処理には未統合。

## 7. WorkItem操作と工程終了（6A-3）

### 7.1 6A-3-1の実装

`ProcessWorkItemService.completeItem(itemId)` / `uncompleteItem(itemId)`はgeneric process.work責務。partsinstallationの条件を含めない。checkはCOMPLETEDとcompletedAt=操作時刻、uncheckはNOT_STARTEDとcompletedAt=null。どちらも状態変更時のみupdatedAtを同じ操作時刻へ更新する。同一状態への再操作は時刻・状態を維持し、saveしない。既存LocalDateTime.nowとDomain callbackを利用し、Clock基盤は追加しない。

History終了済みならcheck/uncheckを拒否する。Item/親関連の不存在・異常、statusとcompletedAtの不整合は明示的に失敗させる。

ロック順はItemからscalar queryでhistoryIdを取得 → 既存History.findForUpdate(PESSIMISTIC_WRITE) → History refresh/endTime確認 → Item取得/refresh → 状態変更・保存。先にItemをロック・Entityロードせず、ロック待機前のキャッシュ状態も使用しない。Historyロックで同じHistoryのItem操作と既存終了処理を直列化し、Guitarロックは追加しない。Item/Work親関連は生成後変更しない既存方針を維持する。実DBでの並行動作検証は未実施。

`ProcessWorkCompletionValidator.validateCompletable(history)`は呼出元のロック済みHistoryを受け取り、再取得・再ロックしない読み取り専用検証。Workなしは許可。WorkありならItemsは1件以上かつ全件COMPLETEDが必要。0件・未完了・null statusを拒否し、Work/Item/Historyは変更しない。6A-3-2でHistoryロック取得後、終了更新と同一transactionでProcessServiceから呼ぶよう接続済み。

### 7.2 後続UIの確定方針（未実装）

checkboxクリック時に即時保存し、保存ボタンは設けない。completedAtを小さく表示し、History終了後はread-only。完了数/全件数とWork snapshot14項目を表示し、Product現在値を作業票に使わない。ELECTRONICS_SOUND_CHECKは1件のまま、selectorPositionsから「全Nポジションで音出し確認」を表示する。

### 7.3 終了処理への接続（6A-3-2、実装済み）

ProcessService.endProcess() / endProcesses()の両方で既存ProcessWorkCompletionValidatorを共通利用する。Workなしはlegacyとして従来どおり終了可能。WorkありはItemが1件以上かつ全件COMPLETED必須であり、0件・NOT_STARTED・null statusは拒否する。Product / Spec / classification / processCode / snapshot値を終了時に再判定しない。

個別はHistoryロック・既存終了済み/工程検証後、Guitarロック前に完了検証する。bulkは既存のHistory ID重複除去・昇順ロック、Guitar昇順ロック、工程検証の順序を維持し、全対象のWork検証を完了してからOrderロック・endTime・currentProcess・数量更新へ進む。未完了が最後にあっても先行対象の状態を変更しない。

Historyロック取得から検証・終了更新まで既存の同一transactionで実施するため、Item check/uncheckと同じHistoryロックで直列化する。ValidatorとItemServiceは変更せず、ロック順逆転も追加しない。既存の開始処理、工程進行・完成数量・timestamp、Controller/API、SQL/schemaは変更しない。DB不要テストで順序・状態不変を検証したが、実DB並行テストは未実施。UIは6A-3-3以降であり、終了ロジックを画面へコピーしない。

以下の既存4終了入口はすべて上記ProcessServiceへ委譲することを確認済み（Controller/API変更なし）:

- POST /processes/end
- POST /processes/bulk/end
- POST /api/process/end
- POST /api/process/bulk/end

**迂回経路対策（6A-3-5完了）**: 旧PUT /api/guitars/{id}、GuitarService#updateGuitar、GuitarUpdateRequestを削除した。Guitar.currentProcessの外部/APIからの直接更新経路はなく、工程変更はProcessServiceの正式なstart/end入口を使用する。Guitar#setCurrentProcessは生成・正式遷移・fixtureのため維持する。

再実施時に新History→新Workを持てるDomainとするが、現行履歴表示・進捗は同一工程原則1回を前提とする。再実施の業務フローは6A-2で実装しない。

## 8. 未確定事項と後続の確認

以下は今回の決定事項へ混ぜず、未確定として残す。

- Work専用画面、直接currentProcess更新の迂回対策は6A-3-3〜5で実装済み。Item check/uncheckと共通Validator単体は6A-3-1、個別/bulk終了統合は6A-3-2で実装済み。
- NG、RETEST_REQUIRED、comment、測定値、Item単位の作業者。
- 同じitemKeyの複数回実施モデル、再実施工程そのものの業務フロー。
- 詳細な弦巻き標準、Electronicsの将来的なposition単位検査。
- processCodeの全工程展開時期、Body / Neckの名前依存解消。
- currentProcess直接更新APIの具体的な改修方法。
- Work専用UI・Controller名。
- pickupLayoutの実DB長の照合。Work側255文字は今回採用済み。

既存design debtとして、Spec初回補完の対象・期限、同時初回作成のエラー扱い、Spec更新と製造開始のrace、既存開始済み履歴との互換性、製品重複判定とパーツ差異の整合性も保持する。Guitarロック方針を決めたことだけでSpec側を含む競合が解消済みとはしない。

6A-1は完了済み。6A-2はprocessCode、Work/Item Domain・DB、plan導出、個別・bulk開始統合まで実装済み。6A-2のChatGPT完了判定済み（ユーザー報告）。6A-3-2まで実装し、commit / push後のChatGPTレビューを待つ。6A-3-3 Work UIへ自動的に進まない。
