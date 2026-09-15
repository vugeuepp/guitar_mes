# Guitar MES Phase 6A 設計方針

- 作成日: 2026-09-10
- 対象: Phase 6A ギターパーツ取付工程
- 改訂日: 2026-09-15
- 文書状態: 6A-1は実装・ChatGPTによる実画面確認完了（ユーザー報告）。6A-2はDomain設計中で、ChatGPTレビューによる決定を本書へ反映。今回のMarkdown差分は再レビュー待ち。6A-2のJava / SQL / test実装は未着手、6A-3も未実装。
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

## 3. 6A-2 Domainの責務と関係（設計方針・未実装）

| 構成 | 責務 |
| --- | --- |
| ProductPartsSpec | 現在の製品標準仕様。「何を作るか」 |
| ProcessHistory | 1回の工程実施。「この工程をいつ誰が実施したか」 |
| ProcessWork | その履歴の工程開始時に確定した作業指示・製品仕様snapshot |
| ProcessWorkItem | Workに属する個々の作業項目と実施状態 |

正式方針は`ProcessHistory 1 : 0..1 ProcessWork 1 : N ProcessWorkItem`。WorkはGuitar＋Processの組ではなく、1回のProcessHistoryに属する。同じGuitar・工程を将来再実施して新しい履歴を作る場合も、新しいWorkに分離できる。

Work→HistoryはWork所有の片方向`@OneToOne`、LAZY、cascadeなし、orphanRemovalなし。Historyへの逆参照は追加しない。DB候補は`process_history_id NOT NULL / FK / UNIQUE`。ItemはWorkの子とするが、子関連の具体的なJPA設定は今回指定していない。

Product master変更後も開始済みWork / Itemを再生成・自動変更しない。作業項目定義自体を直ちにDBマスタ化せず、Serviceで導出して実行時の項目を保存する。ProductPartsSpecを作業結果テーブルとして使用しない。

### 3.1 ProcessWorkと14項目snapshot

候補項目は`id`、`processHistory`、`createdAt`と次の14項目。

| 領域 | snapshot項目 |
| --- | --- |
| Bridge | bridgeType、bridgeModel、requiresStudHoleExpansion |
| Tuner | tunerModel、tunerMountingType、tunerBushRequired、tunerLayout |
| Electronics | pickupLayout、selectorPositions、controlLayout、jackMountingType |
| String | stringMaker、stringModel、stringGauge |

13項目はProductPartsSpec、pickupLayoutのみProductから開始時に値をコピーする。ProductPartsSpecへのFKは持たない。型・長さの既存基準は第2節を参照するが、pickupLayoutの実DB長は未確認。

snapshot部分は原則immutable。WorkはcreatedAtのみを持ち、updatedAtとstatusは現時点で持たせない。Item操作でもsnapshotを書き換えず、工程全体の状態はHistory、工程内の実施状態はItemをsource of truthとする。

productIdの追加snapshotは6A-2では行わない。通常運用にGuitar生成後のProduct差し替え機能がなく、History→guitarId→Guitar→Productで追跡できるため。将来の監査要件による追加余地は残す。

将来の対象製品拡張を考慮し、snapshot列を機械的にすべてNOT NULLにしない。生成時の完全性はServiceで保証する。各列の最終NULL制約はSQL実装前に確認する。

### 3.2 ProcessWorkItem

正式候補項目は`id`、`processWork`、`itemKey`、`itemOrder`、`status`、`completedAt`、`createdAt`、`updatedAt`。順序名は既存processOrderと揃え、sequenceではなくitemOrderを採用する方針。生成時の作業順序を保存する。

itemKeyは安定した業務識別子としてEnumType.STRINGで保存する方向。DBはVARCHAR＋CHECK、VARCHAR(64)を第一候補とする。表示文言とは分離し、EnumのgetLabel()等で表示する。本番保存後の安易なEnum名renameは永続コードを変えるため禁止する設計意図とする。ordinal保存はしない。

| 日時 | 意味 |
| --- | --- |
| createdAt | Itemが生成された時刻 |
| updatedAt | Itemの状態を最後に変更した時刻 |
| completedAt | 現在のCOMPLETED状態になった時刻 |

初期statusはNOT_STARTED / COMPLETEDのみ。EnumType.STRINGを候補とする。

| 状態・操作 | completedAt / updatedAt |
| --- | --- |
| NOT_STARTED | completedAtはnull |
| COMPLETED | completedAtは非null |
| 工程終了前のチェック解除 | NOT_STARTED、completedAt=null、updatedAt=now |
| 再チェック | COMPLETED、completedAt=now、updatedAt=同じnow |

誤操作修正のため工程終了前のチェック解除を可能にする方向。History.endTime設定後はWork / Itemをread-onlyにする。NG / RETEST_REQUIRED / comment等は未確定であり、ここへ先回りして追加しない。

### 3.3 一意制約と並行性

採用方向の制約は以下。

- Work: UNIQUE(process_history_id) — 同じ履歴への二重生成防止。
- Item: UNIQUE(process_work_id, item_key) — 同じ作業キーの二重生成防止。
- Item: UNIQUE(process_work_id, item_order) — 同じWork内の順序重複防止。

存在確認だけで並行性を保証せず、既存GuitarのPESSIMISTIC_WRITE、transaction、DB UNIQUEを組み合わせる。将来同じitemKeyを複数回必要とするならitemIndex等を含むモデルを再検討し、現時点では追加しない。

## 4. 初期作業項目と導出方針

Stratocaster系のギターパーツ取付を初期対象とする。以下は初期itemKey候補と現在の導出方針であり、詳細作業標準の追加に応じて拡張可能。穴寸法・締付値等を推測で定めない。

| グループ・条件 | 生成する項目（記載順） |
| --- | --- |
| Bridge / SIX_POINT | BRIDGE_SIX_POINT_INSTALL → BRIDGE_MOVEMENT_CHECK → SPRING_HANGER_INSTALL |
| Bridge / TWO_POINT | requiresStudHoleExpansion=trueならSTUD_HOLE_EXPANSION → STUD_INSTALL → BRIDGE_TWO_POINT_INSTALL → SPRING_HANGER_INSTALL |
| Bridge / FLOYD_ROSE | requiresStudHoleExpansion=trueならSTUD_HOLE_EXPANSION → STUD_INSTALL → SPRING_HANGER_INSTALL |
| Electronics（現時点の候補） | PICKGUARD_INSTALL → JACK_PLATE_INSTALL → JACK_WIRING → GROUND_WIRING → ELECTRONICS_SOUND_CHECK → ELECTRONICS_PARTS_CHECK → ELECTRONICS_FINAL_FASTENING |
| Tuner | tunerBushRequired=trueならTUNER_BUSHING_INSTALL、その後TUNER_INSTALL |
| String | STRING_INSTALL |

Bridgeの条件はSTUD_HOLE_EXPANSIONの有無にだけ適用し、後続の項目は含める。Floyd Roseはこの工程ではスタッド側の作業を扱い、ブリッジ本体は後工程の調整・調音で取り付ける。FLOYD_ROSE_BRIDGE_INSTALL等は追加しない。

selectorPositionsはELECTRONICS_SOUND_CHECKで「全Nポジション確認」のためにsnapshotを参照する。現時点でPositionごとにItemを分割しない。stringModel / stringGauge等は弦巻き時の参照情報として表示する方向。

Product名、modelNo、bridgeModel、tunerModel、pickupLayout等から不足仕様を推測しない。明示されたSpecと正式なProduct分類を根拠に導出する。

## 5. 対象判定と工程開始transaction

### 5.1 生成タイミング

対象製品のギターパーツ取付工程開始時に、Historyと同じtransactionでWork / Itemを生成する。専用画面初回表示・GETでは生成しない。Historyだけが存在してWorkがない中間状態や、同時閲覧による二重生成を避ける。

概念フロー:

GuitarのPESSIMISTIC_WRITE → 既存工程開始validation → 対象判定 → Product / Spec取得 → 工程開始可能validation → Item導出 → History生成 → Work snapshot生成 → Item生成 → Guitar.currentProcess更新 → COMMIT。

途中失敗は全体rollback。これは実装予定の流れであり、現行Serviceへ接続済みではない。

bulk開始もall-or-nothing。全対象Guitarを既存順序でlockし、全台の既存検証、分類確認、対象Spec確認、生成内容導出を済ませてから保存する。1台のSpec不備でも先行個体だけ開始済みにはせず、何も残さない。

### 5.2 対象・対象外・分類不能

正式分類上のStratocaster系を対象とし、製品名・modelNo・pickupLayout等から推測しない。既存InstrumentTypeMaster、internalModelCode、resolveProductClassification()相当の正式分類を再利用する方向。

分類メソッドは現状private。単純なpublic化とは決めず、分類責務を再利用可能にする具体的方法を実装前の論点とする。

- 6A対象Product: 必要Specが存在し、作業生成に必要な仕様が有効ならWork生成。
- Spec未設定・必要仕様不備の対象Product: ギターパーツ取付工程を開始させない方向。
- 6A対象外と判定できたProduct: 当面はWorkを強制せず従来工程開始を維持。
- 分類不能Product: 開始拒否か従来処理か未確定。対象外と混同しない。

ProductPartsSpecServiceはSpec自体の保存・完全性を担い、Parts Installation側は工程開始可否とItem導出可否を担う。private normalizeAndValidate()を保存以外の目的でそのまま呼ぶ構造にはしない。

### 5.3 package責務の候補

| package候補 | 責務 |
| --- | --- |
| process.work | ProcessWork / Item、Repository、Item状態更新、汎用取得 |
| process.partsinstallation | 対象判定、Spec開始可能validation、snapshot生成、Item導出、工程固有の完了判定 |

既存の汎用工程APIにProcessWorkControllerがあるため、将来の専用Controllerには同名を使わない。具体名・専用UIは未確定。

## 6. processCodeの段階導入方針

B案としてManufacturingProcess / m_processへ表示名と別の安定processCodeを追加する方向。全体UNIQUE、初期NULL許可、6A対象工程へ設定する。初期コード候補はGUITAR_PARTS_INSTALLATION。

6A固有の対象工程判定から段階利用し、既存全工程を一度にcodeへ置換しない。Body / Neck / currentProcess等の名前依存は別design debtとする。

**移行時STOP条件**: m_processの本番・開発DB初期投入方法は確認不能。SQL実装時にはtargetType＋processName等で対象を限定し、想定するギターパーツ取付行が1件であることをverifyする。0件・複数件を黙って成功扱いにしない。実DB確認前に未知の工程へ推測でcodeを割り当てない。

## 7. 工程終了との接続（主に6A-3）

必須ItemがすべてCOMPLETEDでなければ、6A対象Workを持つHistoryを終了できない方針。個別終了とbulk終了は現状別実装のため、両方から共通利用するService validationにする。専用画面だけに検証を置かず、ProcessService.endProcess()の遷移・日時・数量更新を画面へコピーしない。

既存終了入口:

- POST /processes/end
- POST /processes/bulk/end
- POST /api/process/end
- POST /api/process/bulk/end

**迂回経路候補**: PUT /api/guitars/{id}はcurrentProcessを直接変更し、History終了・Work完了検証を経由しない。6A-2では改修せずdesign debtとして残す。将来は専用画面・上記4入口・直接更新APIのいずれからも未完了状態を不正に抜けられない設計を検討する。

再実施時に新History→新Workを持てるDomainとするが、現行履歴表示・進捗は同一工程原則1回を前提とする。再実施の業務フローは6A-2で実装しない。

## 8. 未確定事項と後続の確認

以下は今回の決定事項へ混ぜず、未確定として残す。

- 分類不能Productの開始可否、分類ロジックの具体的な再利用方法。
- NG、RETEST_REQUIRED、comment、測定値、Item単位の作業者。
- 同じitemKeyの複数回実施モデル、再実施工程そのものの業務フロー。
- 詳細な弦巻き標準、Electronicsの将来的なposition単位検査。
- processCodeの全工程展開時期、Body / Neckの名前依存解消。
- currentProcess直接更新APIの具体的な改修方法。
- Work専用UI・Controller名、Item関連の具体的JPA設定。
- 各snapshot列の最終NULL制約、pickupLayoutの実DB長。
- Item順序の開始番号、日時初期値・precision等の物理定義、必要な実装詳細。

既存design debtとして、Spec初回補完の対象・期限、同時初回作成のエラー扱い、Spec更新と製造開始のrace、既存開始済み履歴との互換性、製品重複判定とパーツ差異の整合性も保持する。Guitarロック方針を決めたことだけでSpec側を含む競合が解消済みとはしない。

6A-1は完了済み。6A-2はDomain設計中で、今回の正式方針・候補を文書へ反映した段階。Java / SQL / test実装は未開始、6A-3も未実装。Markdown更新後に停止し、ChatGPTレビュー後に次の実装タスクを決める。
