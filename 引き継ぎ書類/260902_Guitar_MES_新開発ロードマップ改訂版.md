# Guitar MES 新開発ロードマップ 改訂版

- 基礎改訂日: 2026-09-02
- 方針追記日: 2026-09-10（Phase 6A）
- 対象プロジェクト: Guitar Manufacturing Execution System（Guitar MES）
- 技術構成: Java 17 / Spring Boot / Thymeleaf / PostgreSQL / JUnit / Mockito / MockMvc / Playwright
- 開発環境: macOS / Eclipse（Pleiades日本語化版） / DBeaver / GitHub Desktop
- DBスキーマ管理方針: `spring.jpa.hibernate.ddl-auto=validate`
- DB変更方針: 適用SQL・確認SQL・ロールバックSQLによる明示管理
- 現在地・branch・最新検証結果: [DEVELOPMENT_HANDOFF.md](../DEVELOPMENT_HANDOFF.md)を参照
- 第1〜10節および第16節Step 0〜6の進捗表現は、2026-09-02時点の計画・記録。現在の未完了タスク一覧として扱わない。

---

## 1. 改訂の目的

2026-08-31版ロードマップでは、ProductionSchedule、Body・Neck一括発行、工程一括処理、工程別専用ページ、認証・認可、品質管理、分析基盤の順に開発する方針を定めた。

その後、2026-09-02までに以下が進展した。

- ProductionScheduleによる日産計画管理を実装
- 日産計画からBody・Neckを一括発行
- Guitar、Body、Neckの工程一括処理を実装
- Body・Neck一覧に工程先行選択方式を導入
- 一括終了画面に工程別フィルターと工程別結果候補を導入
- ControllerテストとPlaywright E2Eを拡充
- 通常テスト230件、E2E 14件が成功
- テストログを`target`外の`logs/test-logs`へ移動

また、手動操作確認から、E2Eだけでは発見しづらい以下の課題が明確になった。

- Guitar一覧の一括操作方式がBody・Neckと統一されていない
- ネック取付を1本ずつ登録する操作が実運用に合わない
- 内部IDが利用者向け画面へ露出している
- 計画数に達した後もネック取付導線が表示される
- 製造中、組立待ち、組立済み、完成済みが同じ一覧に混在する
- 一覧の並び順が明示されていない
- 検索、フィルター、ソート、ページングが不足している

本改訂では、これらを既存ロードマップへ組み込み、現場操作性とデータ整合性を優先した順序へ引き直す。

---

## 2. 開発の基本原則

### 2.1 現場操作の原則

- 作業者画面では「今、処理可能な対象」を優先表示する
- 対象工程を先に選び、一致する対象だけを選択可能にする
- 操作不能な候補は、理由が明確な場合を除きプルダウンから除外する
- 内部IDではなく、シリアル番号、注文番号、工程名などの業務識別子を表示する
- 完了実績と現在の作業対象を分離する
- 一括操作は、操作対象と選択件数を常に明示する

### 2.2 業務ロジックの原則

- Controllerへ業務ロジックを書きすぎない
- 検証、計算、保存、トランザクション境界はServiceへ置く
- 画面上の非活性制御だけを信用せず、Serviceで再検証する
- 一括処理は全件検証後に実行する
- 部分成功は原則採用せず、1件でも不正なら全件ロールバックする
- 同時操作を考慮し、更新直前に状態を再確認する

### 2.3 DB変更の原則

- `ddl-auto=validate`を維持する
- DB変更はSQLで明示管理する
- 適用SQL、確認SQL、ロールバックSQLを揃える
- 既存データの移行方法をSQLへ含める
- 日付、状態、外部キー、検索条件に必要なインデックスを検討する

### 2.4 品質保証の原則

- 機能単位でServiceテストとMockMvcテストを実行する
- HTMLを新規作成または更新した場合はPlaywright E2Eを作成・更新する
- コミット前に通常テスト全件を実行する
- 主要業務フロー変更時はE2E全件を実行する
- E2E更新系テストは`guitar_mes_e2e`で実行する
- テストログは`logs/test-logs/<日時>/`へ保存する
- E2Eでは正常経路だけでなく、非活性、選択解除、候補絞り込み、リダイレクトも確認する
- 手動確認を併用し、意図しない挙動や操作上の違和感を確認する

---

## 3. 現在の正式な製造フロー

```text
ProductionOrder登録
↓
ProductionSchedule登録・確定
↓
日産計画からBody・Neckを一括発行
↓
Body・Neck工程を開始・終了
↓
Body・Neckが組立可能になる
↓
生産計画詳細からネック取付
↓
Assembly登録
↓
Guitar自動生成
↓
ギターパーツ取付
↓
調整・調音
↓
最終検品
↓
Guitar完成
↓
ProductionOrder.completedQuantity更新
↓
計画数全数完成でProductionOrder = COMPLETED
```

GuitarはProductionOrder登録時には生成しない。BodyとNeckを組み合わせてAssemblyを登録した時点で生成する。

---

## 4. フェーズ全体像

以下の状態表記は基礎改訂時点の記録。Phase 6の開発分割は第11節、実際の現在地はHandoffを参照する。

```text
Phase 1   主要UI・CRUD・自動テスト基盤                     完了
Phase 2   月間計画・日産計画                               完了
Phase 3   Body・Neck個体一括発行                           完了
Phase 4A  Guitar工程一括処理                               完了
Phase 4B  Body・Neck工程一括処理                           完了
Phase 4C  一括操作UI統一・表示改善                         次に着手
Phase 4D  ネック取付導線・数量上限の即時是正               4C後に着手
Phase 5A  ネック取付一括登録                               優先開発
Phase 5B  一覧分類・検索・ソート・完了品分離               5A後に着手
Phase 5C  日時列・並び順・ページング基盤                   5Bと連携
Phase 6   工程別専用ページ・工程内作業                     後続
Phase 7   ログイン・ユーザー・権限管理                     後続
Phase 8   差し戻し・再作業・品質管理                       後続
Phase 9   トレーサビリティ・分析・ダッシュボード           後続
Phase 10  運用強化・性能・保守性改善                       継続
```

---

## 5. 完了済みフェーズ

### 5.1 Phase 1: 主要UI・CRUD・品質保証基盤

主な完了内容:

- Product、BodyMaster、NeckMaster、各種マスタのCRUD
- Product画像登録、差し替え、削除
- ProductionOrder編集・取消
- Package by Featureへの移行
- Serviceテスト、MockMvcテスト、Playwright E2E基盤
- E2E専用DBと自動クリーンアップ

### 5.2 Phase 2: 月間計画・日産計画

主な完了内容:

- ProductionScheduleのDB・Entity・Repository・Service
- 日産計画登録、履歴、割当状況
- 計画数超過防止
- 発行後制御
- 関連するService、Controller、E2Eテスト

### 5.3 Phase 3: Body・Neck個体一括発行

主な完了内容:

- 日産計画からBody・Neckを一括発行
- ProductionOrder、ProductionSchedule、Masterとの外部キー関連
- 二重発行と数量超過の防止
- 発行後の計画変更制御

### 5.4 Phase 4A: Guitar工程一括処理

主な完了内容:

- Guitar複数選択
- 一括工程開始・終了
- 全件検証とトランザクション制御
- `BulkGuitarProcessE2E`

### 5.5 Phase 4B: Body・Neck工程一括処理

主な完了内容:

- Body・Neck複数選択
- 一括工程開始・終了
- 対象工程の先行選択
- 対象工程と一致する個体・履歴だけを活性化
- 対象が存在する工程だけプルダウンへ表示
- 工程変更時の選択解除
- 全件選択を対象工程内に限定
- 工程IDを工程名表示へ変更
- 工程別の終了結果候補
- NeckのNG時に備考必須
- 成功時は一覧へリダイレクト
- 失敗時は一括終了画面へ戻る
- 通常テスト230件成功
- E2E 14件成功

---

## 6. Phase 4C: 一括操作UI統一・表示改善

### 6.1 目的

Body・Neckで確立した操作方式をGuitarへ展開し、一括操作画面の挙動を統一する。また、利用者に不要な内部IDを整理する。

### 6.2 Guitar一覧の統一

Guitar一覧を次の操作順へ変更する。

```text
対象工程を選択
↓
選択工程と一致し、開始可能なGuitarだけ活性化
↓
複数選択または全件選択
↓
作業者を入力
↓
一括工程開始
```

実装条件:

- 一括開始可能なGuitarが存在する工程だけプルダウンへ表示
- 工程未選択時はチェックボックスを非活性
- 対象工程変更時は選択を解除
- 全件選択は活性行だけを対象とする
- 完成済み、作業中、不正状態のGuitarは選択不可
- 対象がない場合は一括開始欄を非活性
- 操作案内文を表示
- Service側の全件再検証を維持

### 6.3 内部ID表示の整理

原則として、一般利用者向け画面ではDB主キーを表示しない。

置換方針:

```text
Body ID          → Bodyシリアル番号
Neck ID          → Neckシリアル番号
Guitar ID        → Guitarシリアル番号
工程ID           → 工程名
履歴ID           → 非表示
ProductionOrder ID → 注文番号
ProductionSchedule ID → 計画日・注文番号・モデル
```

内部処理では、hidden入力や`data-*`属性、URLパラメーターとしてIDを保持する。

管理者向けの障害調査機能やログでは、IDを引き続き利用できる。

### 6.4 対象画面

- Guitar管理一覧
- Body管理一覧
- Neck管理一覧
- 工程一括終了画面
- 工程履歴画面
- 生産計画詳細
- 日産計画詳細
- ネック取付画面

### 6.5 テスト

- Guitar一覧の工程先行選択
- 対象のない工程が表示されない
- 工程変更時の選択解除
- 全件選択の対象範囲
- 完成済みGuitarが選択されない
- 画面上に不要なID列が残っていない
- 既存`BulkGuitarProcessE2E`を新方式へ更新
- 通常テスト全件、E2E全件

### 6.6 完了条件

- Guitar、Body、Neckの一括開始UIが統一されている
- 一括操作画面で利用者が工程を誤選択しにくい
- 一般利用者向け画面から不要な内部IDが除かれている
- 既存の単体工程操作が維持されている
- 通常テストとE2Eが成功する

---

## 7. Phase 4D: ネック取付導線・数量上限の即時是正

### 7.1 目的

計画数に達したProductionOrderでもネック取付ボタンが表示され、空の取付画面へ進める問題を解消する。

### 7.2 表示条件

ネック取付ボタンは、次のすべてを満たす場合だけ有効にする。

- ProductionOrderが取消済み・完了済みではない
- `plannedQuantity > startedQuantity`
- 対象ProductionScheduleに未着手数がある
- 対応する組立可能Bodyが存在する
- 対応する組立可能Neckが存在する
- ProductとBodyMaster・NeckMasterの対応が成立する

### 7.3 表示方法

操作できない場合は、可能な限り理由を表示する。

候補:

- 計画数に達しています
- 取付可能なBodyがありません
- 取付可能なNeckがありません
- 対象日産計画は完了しています
- 生産計画が取消済みです

### 7.4 Service側の防御

画面ボタンが非表示でも、直接URLや不正リクエストから登録されないようにする。

- 数量上限の再検証
- Body・Neck状態の再検証
- Product・Master適合性の再検証
- 二重使用の防止
- ProductionOrder・ProductionSchedule状態の再検証

### 7.5 完了条件

- 計画数到達後にネック取付画面へ誘導されない
- 利用不可理由が画面で分かる
- URL直接アクセスでも不正登録できない
- 境界値テストとE2Eが成功する

---

## 8. Phase 5A: ネック取付一括登録

### 8.1 目的

1本ずつ行っているネック取付を、実運用の作業単位に合わせて複数本まとめて登録できるようにする。

### 8.2 初期UI方針

初期実装では複雑なドラッグ＆ドロップを避け、安定した組み合わせ保持方式を採用する。

```text
Bodyを選択
Neckを選択
↓
「組み合わせへ追加」
↓
登録予定リストへ保持
↓
複数ペアを確認
↓
一括登録
```

画面構成:

```text
取付可能なBody      取付可能なNeck
[DB260011 ▼]        [DN260011 ▼]
[組み合わせへ追加]

登録予定
Body          Neck          操作
DB260011      DN260011      解除
DB260012      DN260012      解除
DB260013      DN260013      解除

作業者 [                ]
[3件を一括登録]
```

将来候補:

- Body・Neckのチェックボックス選択
- 自動ペアリング
- モデル・仕様一致候補の自動提示
- ドラッグ＆ドロップ
- バーコード・QRコード入力

### 8.3 一括登録の業務ルール

登録前に全ペアを検証する。

- BodyとNeckが空でない
- 同じBodyが重複していない
- 同じNeckが重複していない
- BodyとNeckが組立可能状態
- Body・Neckが未使用
- Product、BodyMaster、NeckMasterが適合
- ProductionOrderとProductionScheduleが一致
- 計画数・日産計画数を超えない
- ProductionOrderが有効
- ProductionScheduleが取付可能状態
- 作業者が入力されている
- 既存Assemblyと重複しない

初期方針:

```text
1件でも不正
↓
全件登録しない
↓
トランザクションをロールバック
↓
不正な組み合わせと理由を表示
```

### 8.4 登録結果

一括登録時にペアごとに以下を生成・更新する。

- Assembly
- Guitar
- Body.status = ASSEMBLED
- Neck.status = ASSEMBLED
- ProductionOrder.startedQuantity
- 必要な工程初期状態
- 作業者と取付日時

### 8.5 Request DTO候補

```text
BulkAssemblyCreateRequest
- productionOrderId
- productionScheduleId
- workerName
- pairs

AssemblyPairRequest
- bodyId
- neckId
```

### 8.6 テスト

Serviceテスト:

- 2件以上の正常一括登録
- Body重複拒否
- Neck重複拒否
- Master不一致拒否
- 計画数量超過拒否
- 使用済みBody・Neck拒否
- 全件ロールバック

Controllerテスト:

- 登録予定リストの受信
- 成功時リダイレクト
- エラー時の入力保持
- エラーメッセージ

E2E:

- 複数ペアを登録予定へ追加
- ペア解除
- 重複候補防止
- 一括登録
- Guitar複数生成
- Body・Neck状態更新
- ProductionOrder数量更新
- E2Eデータのクリーンアップ

### 8.7 完了条件

- 複数のBody・Neckペアを保持できる
- 登録予定リストを確認・解除できる
- 複数Assembly・Guitarを1回で登録できる
- 数量超過と重複を防止できる
- エラー時に部分登録されない
- 通常テストと主要E2Eが成功する

---

## 9. Phase 5B: 一覧分類・完了品分離・検索

### 9.1 目的

現在処理すべき対象と、組立待ち・完了実績を分離し、一覧から対象を探しやすくする。

### 9.2 一覧の分類

Body:

```text
製造中
組立待ち
組立済み
不合格・手直し
```

Neck:

```text
製造中
組立待ち
組立済み
差し戻し・不合格
```

Guitar:

```text
製造中
完成済み
```

ProductionOrder:

```text
計画中
製造中
完了
取消
遅延
```

### 9.3 表示方式

初期案はタブ方式とする。

```text
[製造中] [組立待ち] [完了・組立済み] [異常]
```

デフォルトタブは未完了・作業対象とする。完了品は別タブまたは別ページに移し、現在一覧を占有しないようにする。

### 9.4 検索とフィルター

第1段階:

- シリアル番号検索
- 状態フィルター
- 工程フィルター
- Product・モデルフィルター
- ProductionOrderフィルター
- ProductionSchedule・日付フィルター

第2段階:

- 作業者
- 作成日・更新日
- 納期範囲
- 昇順・降順
- 複合条件

### 9.5 完了条件

- 作業対象と完了実績が分離されている
- デフォルトでは未完了対象が表示される
- シリアル番号、状態、工程で絞り込める
- フィルター適用時の件数が分かる
- URLクエリまたはフォームで条件を再現できる
- 主要一覧のE2Eが更新されている

---

## 10. Phase 5C: 日時列・並び順・ページング基盤

### 10.1 目的

一覧の表示順を明確にし、数千件・数万件規模のデータに対応する基盤を整える。

### 10.2 追加候補列

対象Entity:

- Body
- Neck
- Guitar
- Assembly
- ProcessHistory
- 必要なMaster・計画Entity

候補列:

```text
createdAt
updatedAt
completedAt
```

すでに同等列があるEntityは重複追加しない。

### 10.3 デフォルト並び順

ProductionOrder:

```text
未完了を先頭
↓
納期が近い順
↓
更新日時の新しい順
```

Body・Neck・Guitarの作業対象:

```text
作業中
↓
検品待ち・工程待ち
↓
手直し・差し戻し
↓
更新日時の新しい順
```

組立待ち:

```text
組立可能になった日時が古い順
```

完了品:

```text
完成日時の新しい順
```

工程履歴:

```text
初期表示は開始日時の新しい順
必要に応じて古い順へ変更可能
```

### 10.4 DB移行

- 日時列追加SQL
- 既存データの初期値設定
- NOT NULL適用可否の判断
- 検索・ソート用インデックス
- 確認SQL
- ロールバックSQL

### 10.5 ページング

- Spring Dataの`Pageable`を利用
- デフォルト表示件数を決定
- URLにページ・件数・ソート条件を保持
- フィルターとページングを併用
- 完了実績や履歴を全件ロードしない

### 10.6 完了条件

- 主要Entityに必要な日時が保持される
- 一覧のデフォルト順が仕様として明示される
- Repositoryが明示的な並び順を持つ
- 数千件以上でも一覧を全件取得しない
- フィルター、ソート、ページングを併用できる
- SQL一式とテストが揃っている

---

## 11. Phase 6: 工程別専用ページ・工程内作業

### 11.1 目的と初期対象

工程ごとに異なる作業内容・チェック・検査結果を記録し、作業者が担当工程ページ内で作業を完結できるようにする。

Phase 6AはStratocaster系の「ギターパーツ取付工程」を最初の対象とする。調整・調音、最終検品、Body・Neck工程別ページは後続の展開候補とし、6Aへ一括で含めない。

### 11.2 Phase 6Aの開発分割

| 段階 | 開発内容 |
| --- | --- |
| 6A-1 製品仕様拡張 | Bridge、Tuner、Electronicsの不足仕様、Stringを対象候補に、作業判断に必要な仕様を製品マスタへ保存する。製品詳細の電子仕様書化を目指す |
| 6A-2 工程内作業記録基盤 | 汎用ProcessHistoryを維持し、ProcessWork / ProcessWorkItem等の別層で、確定済み作業項目と実績・検査結果を記録する |
| 6A-3 専用画面・工程連携 | 作業チェック、OK / NG検査、NGコメント、途中保存、完了条件判定、工程終了を専用画面で扱う。終了処理は既存ProcessService.endProcess()を再利用する |

### 11.3 設計原則

- 製品仕様（何を作るか）、作業指示（何をするか）、作業実績（何を行ったか）、検査結果（OK / NG）を分離する。
- 製品仕様はDBへ保存する。工程固有のチェック・検査列をProcessHistoryに直接追加しない。
- Phase 6Aでは作業項目定義そのものを直ちにDBマスタ化せず、仕様から必要作業を決めるルールはService側に持たせる方向で検討する。
- 作業開始時点で必要項目を確定して保存し、後の製品マスタ変更が開始済み個体の作業内容を勝手に変えない構造とする。
- 旧候補ProcessTaskDefinition / ProcessTaskHistoryは採用確定ではない。ProcessWork / ProcessWorkItemも名称候補で、物理モデルは6A-2で確定する。

### 11.4 対象作業と完了判定

初期対象はトレモロユニット・スプリングハンガー取付、ピックガード・舟形ジャック取付、ペグ取付、指定弦による弦巻。

仕様ごとの分岐、詳細作業、段階ごとの完了判定、未確定の設計論点は[Phase 6A設計方針](260910_Guitar_MES_Phase6A_設計方針.md)にまとめる。

必須作業が未完了なら工程終了できないことを維持する。NGの扱い、具体的な必須項目・検査基準、既存の個別・一括終了経路との接続は詳細設計で決め、画面だけでなくServiceで検証する。

---

## 12. Phase 7: ログイン・ユーザー・権限管理

目的:

- 作業実績をログインユーザーへ関連付ける
- 管理者、生産管理者、現場リーダー、作業者、品質担当の画面を分離する
- `workerName`文字列入力をUser参照へ段階移行する

初期ロール候補:

```text
ADMIN
WORKER
```

拡張候補:

```text
MANAGER
LEADER
QUALITY
```

完了条件:

- ログイン・ログアウト
- パスワード安全保存
- URLアクセス制御
- ロール別メニュー
- 工程実績とUserの関連
- Securityテスト

---

## 13. Phase 8: 差し戻し・再作業・品質管理

目的:

- 差し戻し、再作業、不良、再検査を履歴として管理する
- 状態の単純上書きではなく、理由と経路を残す

主な項目:

- 差し戻し元・先工程
- 理由コード
- 詳細コメント
- 登録者・承認者
- 再作業開始・完了
- 再検査結果
- 画像

完了条件:

- 差し戻しを履歴登録できる
- 差し戻し先を制御できる
- 再作業を開始・完了できる
- 再検査結果を保存できる
- 不合格・廃棄を履歴付きで管理できる

---

## 14. Phase 9: トレーサビリティ・分析・ダッシュボード

トレーサビリティ:

```text
ProductionOrder
↓
ProductionSchedule
↓
Body / Neck
↓
Assembly
↓
Guitar
↓
ProcessHistory
↓
工程内作業記録（ProcessWork / ProcessWorkItem等の候補）
↓
Return / Rework / Inspection
↓
User
```

分析候補:

- 月間・日産計画達成率
- 工程別仕掛数
- 工程別滞留時間
- 工程平均時間
- 作業者別実績
- 差し戻し率・再作業率
- モデル別不良率
- 完成リードタイム
- 計画と実績の差

---

## 15. Phase 10: 運用強化・性能・保守性改善

### 15.1 E2E基盤共通化

候補:

```text
E2EDatabaseSupport.java
E2ETestDataFactory.java
E2ECleanupSupport.java
```

### 15.2 大量データ対応

- ページング
- 日付・モデル・工程・状態による絞り込み
- インデックス
- 集計クエリ
- N+1問題
- 一括INSERT・一括UPDATE
- 長期履歴のアーカイブ

### 15.3 保守性

- 未使用コード・import整理
- DTO責務整理
- Service分割
- JavaScript共通化
- CSS共通化
- View Component共通化
- エラーメッセージ統一
- 監査ログ
- CIによる通常テスト・E2E実行

### 15.4 現在確認されている技術的負債

- Mockitoの動的Agent読み込み警告
- 一部テストのunchecked警告
- `spring.jpa.open-in-view`警告
- Body・Neckの一括画面JavaScript重複
- E2EのDB接続・データ準備・削除処理の重複
- 一部Serviceの肥大化

主要機能開発を妨げない単位で解消する。

---

## 16. 推奨実装順（Step 0〜6は基礎改訂時点の計画）

### Step 0: Phase 4Bを確定・コミット

- 変更ファイル確認
- 通常テスト230件成功ログを保存
- E2E 14件成功ログを保存
- コミットメッセージは日本語

候補:

```text
Body・Neck工程の一括開始・終了機能を追加
```

### Step 1: Phase 4C Guitar一覧統一

- Guitar一覧を工程先行選択方式へ変更
- 対象のない工程をプルダウンから除外
- 工程変更時の選択解除
- 完成済み・作業中の選択防止
- `BulkGuitarProcessE2E`更新

### Step 2: Phase 4C 内部ID表示整理

- 主要一覧のID列を棚卸し
- シリアル番号、工程名、注文番号へ置換
- 履歴IDを非表示化
- 管理者向け調査用途との分離

### Step 3: Phase 4D ネック取付導線修正

- 計画数達成時のボタン非表示または非活性
- Body・Neck不足時の理由表示
- URL直接アクセス時のService検証
- 境界値テストとE2E

### Step 4: Phase 5A ネック取付一括登録

- 画面仕様確定
- Request DTO
- Service一括登録
- 登録予定リスト
- 全件検証・ロールバック
- Controllerテスト
- Playwright E2E

### Step 5: Phase 5B 一覧分類・検索

- 製造中、組立待ち、完了、異常の分類
- デフォルト表示を作業対象へ限定
- シリアル番号、状態、工程フィルター

### Step 6: Phase 5C 日時・並び順・ページング

- SQL設計
- `createdAt`、`updatedAt`、`completedAt`
- 明示的なデフォルト順
- ページングとインデックス

### Step 7: Phase 6A

製品仕様拡張（6A-1）→ 工程内作業記録基盤（6A-2）→ ギターパーツ取付専用画面・工程連携（6A-3）の順に進める。詳細は第11節とPhase 6A設計方針を参照する。

---

## 17. 作業開始時の確認

現在地と次の作業は[DEVELOPMENT_HANDOFF.md](../DEVELOPMENT_HANDOFF.md)、恒久ルールは[AGENTS.md](../AGENTS.md)を確認し、実際のGit状態・ユーザー指示と照合する。

Phase 6Aの実装前には、既存製品仕様を棚卸しし、不足項目と各段階の未確定事項を決める。ロードマップの計画記述だけで実装開始や仕様確定とみなさない。

---

## 18. 次回チャット開始時の参照先

1. DEVELOPMENT_HANDOFF.mdで現在のPhase・branch・検証状況・依頼範囲を確認する。
2. AGENTS.mdと実際のGit状態を確認する。
3. 本文書第11節とPhase 6A設計方針を読み、ユーザーが確認済みの仕様と未確定事項を区別する。

進捗・テスト件数をこのショートメモへ複製せず、Handoffを参照する。
