# Guitar MES 新開発ロードマップ

- 作成日: 2026-08-31
- 対象プロジェクト: Guitar Manufacturing Execution System（Guitar MES）
- 対象期間: 2026-08-31以降
- 前回ロードマップ基準日: 2026-08-26
- 技術構成: Java 17 / Spring Boot / Thymeleaf / PostgreSQL / JUnit / Mockito / MockMvc / Playwright
- 開発環境: Mac OS / Eclipse（Pleiades日本語化版） / DBeaver / GitHub Desktop
- DBスキーマ管理方針: `spring.jpa.hibernate.ddl-auto=validate`
- 現在地: Phase 1完了。次はProductionScheduleによる月間・日産計画管理へ進む

---

## 1. このロードマップの目的

このロードマップは、2026-08-26に作成した旧ロードマップと、その後1週間の実装実績を踏まえて更新したものである。

旧ロードマップでは、主要UIの統一、CRUD整備、ProductionOrder編集・取消、日産計画、一括個体発行、一括工程操作、工程別ページ、認証・認可などを段階的に実装する方針を定めていた。

その後、当初の想定以上に以下が進展した。

- Phase 1の主要UI・CRUD整備を完了
- Product、BodyMaster、NeckMaster、ProductionOrderの編集・運用制御を整備
- Package by Featureへ移行
- ServiceテストとMockMvcテストを拡充
- Playwright E2E基盤を構築
- E2E専用DBを構築
- ProductionOrderからGuitar完成までの主要業務フローを実ブラウザで自動検証

今後は、現在の「管理者が個体や実績を管理するMES」から、以下を両立するシステムへ発展させる。

```text
生産管理者が月間・日産計画を管理できるMES
+
作業者が担当工程を効率よく処理できるMES
```

---

## 2. 現在の正式な製造フロー

現時点の正式フローは以下。

```text
ProductionOrder登録
↓
Product確定
↓
対応するBody・Neckを製造
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

GuitarはProductionOrder登録時には生成しない。BodyとNeckを選択してAssemblyを登録した時点で初めて生成する。

---

## 3. この1週間で完了した内容

### 3.1 主要UI・CRUD

- Product編集
- Product代表画像の登録、差し替え、削除、一覧サムネイル
- ProductSeriesMasterのDB化
- ProductSeriesMasterの編集、有効化、無効化
- InstrumentTypeMasterのDB化
- InstrumentTypeMasterの編集、有効化、無効化
- BodyMaster編集
- NeckMaster編集
- ProductionOrder発行前編集
- ProductionOrder取消
- ProductionOrder編集画面の日付初期値不具合修正

### 3.2 アーキテクチャ・保守性

- レイヤー単位からPackage by Featureへ移行
- 本番コードとテストコードのpackage構成を対応
- HibernateのDDL自動更新を停止
- DB変更をSQLで明示管理する方針を確立

### 3.3 自動テスト

- 通常テスト170件成功
- Serviceテスト整備
- MockMvcによる主要Controllerテスト整備
- Playwright Java導入
- E2E専用DB `guitar_mes_e2e` を構築
- E2E専用Springプロファイルを構築
- E2E実行後の自動クリーンアップを実装

### 3.4 完成したPlaywright E2E

```text
ProductionOrderSmokeE2E
ProductionOrderEditE2E
ProductionOrderCancelE2E
ProductImageUploadE2E
ProductSeriesMasterE2E
InstrumentTypeMasterE2E
AssemblyCreateE2E
GuitarProcessE2E
```

主要な画面操作と製造フローについて、以下まで自動検証できる。

```text
参照
登録
編集
取消
画像アップロード・表示・削除
マスタ有効化・無効化
ネック取付
Assembly生成
Guitar生成
工程開始・終了
Guitar完成
ProductionOrder完成
```

### 3.5 Phase 1判定

```text
Phase 1: 主要UI・CRUD・品質保証基盤
完了
```

---

## 4. 今後の基本設計方針

### 4.1 計画管理の二階層化

実際の工場では、月間約2,000本から2,500本という大枠の目標と、毎朝提示されるモデル別の日産目標が併存する。

作業者が直接認識するのは、月産全体よりも次のような当日投入数である。

```text
ST60  70本
TL50  50本
合計 120本
```

ただし、この日産数は「当日にすべての工程を完了する数量」とは限らない。組み込み工程へ投入されたGuitarが、パーツ取付や調音へ進むのは翌日以降になることがあり、工程間にはタイムラグが存在する。

したがって、計画を次の二階層で管理する。

```text
ProductionOrder
月間・モデル別計画

ProductionSchedule
ProductionOrder配下の日付別投入計画
```

### 4.2 利用者別の表示範囲

#### 生産管理者

- 月産計画
- 日産計画
- 日産割当済数
- 未割当数
- 着手数
- 完成数
- 完成残数
- 遅延状況
- モデル別達成率

#### 現場リーダー

- 当日のモデル別投入予定
- 担当工程の予定数
- 実績数
- 残数
- 工程別滞留数
- 異常・差し戻し状況

#### 作業者

- 担当工程で現在処理可能な個体
- 本日の目標
- 本日の完了数
- 残数
- 複数個体のチェックボックス選択
- 一括工程開始
- 一括工程終了
- 工程固有の作業内容

作業者画面では「今、何を処理すべきか」を中心にし、月間進捗は補助情報として表示する。

---

## 5. 新ロードマップ全体像

```text
Phase 1  主要UI・CRUD・自動テスト基盤              完了
Phase 2  月間計画・日産計画                        次に着手
Phase 3  Body・Neck個体一括発行
Phase 4  工程別一覧・チェックボックス一括処理
Phase 5  工程別専用ページ・工程内作業
Phase 6  ログイン・ユーザー・権限管理
Phase 7  差し戻し・再作業・品質管理
Phase 8  トレーサビリティ・分析・ダッシュボード
Phase 9  運用強化・性能・保守性改善
```

---

# Phase 2: 月間計画・日産計画

## 6. Phase 2の目的

- 月産2,000本から2,500本規模をモデル別に管理する
- 毎朝配布される日産指示をシステム化する
- 月間目標と日々の投入数を関連付ける
- 月間計画に対する割当状況を可視化する
- 将来の個体一括発行と工程別作業画面の基盤を作る

## 7. ProductionOrderの位置付け

ProductionOrderは、月間・モデル別の大枠の生産計画として扱う。

例:

```text
対象月: 2026年9月
Product: ST60 Black / Rosewood
月間計画数: 200本
```

既存のProductionOrderには日付項目があるため、導入初期は既存項目との役割を整理する。

検討事項:

- `plannedStartDate`を対象月の開始目安として残すか
- `dueDate`を月間計画の最終期限として残すか
- 対象月を表す`planMonth`を追加するか
- 既存データとの互換性をどう保つか

初期方針として、既存項目を即座に削除せず、ProductionSchedule導入後の利用状況を見て整理する。

## 8. ProductionScheduleの位置付け

ProductionScheduleは、ProductionOrder配下の日付別投入計画とする。

```text
ProductionOrder
ST60 Black / Rosewood 200本

├── 2026-09-01  20本
├── 2026-09-02  25本
├── 2026-09-03  20本
└── 以降の日産計画
```

## 9. ProductionSchedule初期項目案

```text
id
productionOrder
scheduleDate
plannedQuantity
status
createdAt
updatedAt
```

### 状態候補

```text
DRAFT        下書き
CONFIRMED    確定
IN_PROGRESS  製造中
COMPLETED    完了
CANCELLED    取消
```

### 初期段階では直接保持しない候補

```text
startedQuantity
completedQuantity
```

これらをProductionScheduleへ直接保存すると、ProductionOrder・ProductionSchedule・Guitar間で数量不整合が起こる可能性がある。

初期方針:

```text
plannedQuantity
→ ProductionScheduleへ保存

startedQuantity
→ 関連個体・Guitarから集計

completedQuantity
→ 完成個体から集計
```

性能上の問題が明確になった場合のみ、集計値のキャッシュを検討する。

## 10. ProductionSchedule業務ルール

- 計画数は1以上
- 同一ProductionOrder・同一日付は重複不可
- 日産計画合計はProductionOrderの月間計画数を超過不可
- CANCELLEDのProductionOrderには追加不可
- COMPLETEDのProductionOrderには追加不可
- 確定済み日産計画の変更には制限を設ける
- 個体発行後はProduct・数量・日付変更を制限する
- 取消は物理削除ではなく`CANCELLED`を使用する

### 数量例

```text
月間計画数: 200本
日産割当済数: 170本
未割当数: 30本
```

30本を超える日産計画を追加しようとした場合はServiceで拒否する。

## 11. Phase 2実装順序

### Phase 2A: DB・ドメイン基盤

```text
1. ProductionSchedule正式仕様の確定
2. 適用SQL作成
3. 確認SQL作成
4. ロールバックSQL作成
5. ProductionSchedule Entity作成
6. ProductionScheduleStatusConstants作成
7. ProductionScheduleRepository作成
8. ProductionScheduleService作成
9. Serviceテスト作成
```

初回SQLファイル名候補:

```text
260831_01_create_t_production_schedule.sql
260831_02_verify_t_production_schedule.sql
260831_03_rollback_t_production_schedule.sql
```

### Phase 2B: 日産計画画面

```text
10. ProductionOrder詳細へ日産計画サマリー追加
11. 日産計画一覧
12. 日産計画登録
13. 日産計画編集
14. 日産計画詳細
15. 日産計画確定
16. 日産計画取消
17. 月間計画超過エラー表示
18. MockMvcテスト
19. 必要なE2Eテスト
```

### Phase 2C: 進捗可視化

```text
20. 日産割当済数
21. 未割当数
22. 製造開始数
23. 完成数
24. 完成残数
25. 日別・モデル別表示
26. 当日計画サマリー
```

## 12. Phase 2完了条件

- ProductionOrder配下に複数の日産計画を登録できる
- 同一日付の重複を防止できる
- 月間計画超過を防止できる
- 日産計画を確定・取消できる
- 割当済数と未割当数を確認できる
- ServiceテストとMockMvcテストが成功する
- DDL、確認SQL、ロールバックSQLが揃っている

---

# Phase 3: Body・Neck個体一括発行

## 13. Phase 3の目的

現在のBody・Neck一件ずつの登録を、実運用向けの一括発行へ変更する。

```text
日産計画を確定
↓
「製造個体を発行」
↓
計画数分のBodyを生成
↓
計画数分のNeckを生成
↓
当日の作業対象へ表示
```

## 14. 個体と計画の関連

将来的に以下の外部キー関連を追加する。

```text
Body
├── ProductionOrder
├── ProductionSchedule
└── BodyMaster

Neck
├── ProductionOrder
├── ProductionSchedule
└── NeckMaster

Guitar
├── ProductionOrder
└── ProductionSchedule
```

文字列シリアルから所属計画を推測せず、DB外部キーで追跡する。

## 15. 一括発行ルール

- 日産計画が確定済みであること
- ProductionOrderが有効であること
- ProductにBodyMaster・NeckMasterが設定されていること
- 計画数を超えて発行しないこと
- 二重発行を防止すること
- 発行済数を確認できること
- 一部発行を許可するか、全数一括のみとするかを決定すること
- 発行後のProductionSchedule変更を制限すること
- 一括処理は1トランザクションで行うこと

## 16. Phase 3完了条件

- 日産計画からBody・Neckを一括発行できる
- Body・NeckがProductionScheduleを参照する
- 二重発行を防止できる
- 発行数と未発行数を確認できる
- Product・Masterとの対応が保証される
- Serviceテスト、MockMvcテスト、主要E2Eが成功する

---

# Phase 4: 工程別一覧・チェックボックス一括処理

## 17. Phase 4の目的

実際の現場では、1本ずつ開始・終了を登録するのではなく、10本、20本、50本などの作業単位で複数個体を処理する。

作業者が複数のGuitar、Body、Neckへチェックを入れ、一括してステータスまたは工程実績を登録できる画面を作る。

## 18. 画面イメージ

```text
ギターパーツ取付

本日の予定: 120本
本日の完了: 84本
残り: 36本

[ ] 全件選択

[ ] DY260001  ST60 Black  未実施
[ ] DY260002  ST60 Black  未実施
[ ] DY260003  TL50 Blue   未実施
[ ] DY260004  ST60 Red    未実施

選択中: 0件

作業者
[山田                     ]

[一括工程開始]
```

工程終了画面:

```text
[ ] 全件選択

[ ] DY260001  実施中
[ ] DY260002  実施中
[ ] DY260003  実施中

選択中: 3件

[一括工程終了]
```

## 19. 一括処理Request DTO候補

```text
BulkProcessStartRequest
- guitarIds
- processId
- workerName

BulkProcessEndRequest
- historyIds
```

Body・Neck工程にも同様のRequest DTOを用意するか、共通化可能かを実装時に判断する。

## 20. 一括処理の業務ルール

一括処理は、選択された全件をServiceで事前検証してから更新する。

確認項目:

- 対象IDが存在すること
- 対象が空でないこと
- 同じ対象種別であること
- 現在工程が一致すること
- 現在の状態が開始・終了可能であること
- 実施中工程が重複していないこと
- ProductionScheduleや対象日の条件に一致すること
- 完成・取消・不合格個体が混在していないこと

初期方針:

```text
1件でも不正
↓
全件処理しない
↓
トランザクションをロールバック
↓
不正対象と理由を画面表示
```

部分成功は原則として採用しない。

## 21. 同時操作への対応

複数作業者が同じ個体を同時選択する可能性がある。

初期実装:

- Serviceのトランザクション内で状態を再検証
- 画面表示時点の状態だけを信用しない

将来候補:

```text
楽観ロック: @Version
悲観ロック: PESSIMISTIC_WRITE
```

## 22. Phase 4実装順序

```text
1. 工程別処理対象一覧
2. ProductionSchedule・日付・モデル・工程で絞り込み
3. チェックボックス
4. 全件選択・解除
5. 選択件数表示
6. 一括工程開始Request DTO
7. 一括工程開始Service
8. 一括工程終了Request DTO
9. 一括工程終了Service
10. 全件検証・ロールバック
11. エラー対象表示
12. Serviceテスト
13. MockMvcテスト
14. Playwright E2E
15. 大量件数での動作確認
```

## 23. Phase 4完了条件

- 複数個体をチェックボックスで選択できる
- 全件選択・解除ができる
- 選択件数が表示される
- 共通作業者で一括開始できる
- 実施中の複数工程を一括終了できる
- 不正対象が含まれる場合は全件ロールバックされる
- 画面とDBの状態が一致する
- 主要シナリオをPlaywrightで検証できる

---

# Phase 5: 工程別専用ページ・工程内作業

## 24. Phase 5の目的

現在の工程開始・終了だけではなく、工程ごとに異なる作業内容、チェック項目、測定値、進捗を管理する。

作業者がログイン後に担当工程ページを開き、必要な作業をその画面内で完結できる構成を目指す。

## 25. 工程別ページ候補

```text
ギターパーツ取付ページ
調整・調音ページ
最終検品ページ
Body工程別ページ
Neck工程別ページ
```

### ギターパーツ取付の工程内作業例

```text
ペグ取付
穴あけ
ブリッジ取付
ピックガード取付
結線
弦巻き
通電確認
```

### 調整・調音の工程内作業例

```text
ネック調整
弦高調整
オクターブ調整
ピックアップ高さ調整
電装確認
調音
```

### 最終検品の工程内作業例

```text
外観検査
演奏確認
電装検査
寸法・調整値確認
付属品確認
合否判定
```

## 26. データモデル候補

```text
ProcessTaskDefinition
工程内作業マスタ

ProcessTaskHistory
工程内作業実績
```

候補項目:

```text
ProcessTaskDefinition
- id
- manufacturingProcess
- taskCode
- taskName
- taskOrder
- inputType
- required
- active

ProcessTaskHistory
- id
- processHistory
- taskDefinition
- checked
- inputValue
- result
- comment
- worker
- completedAt
```

## 27. Phase 5完了条件

- 工程ごとに専用ページがある
- 工程固有の作業項目を表示できる
- 必須作業が未完了の場合は工程終了できない
- チェック、数値、文字、判定を保存できる
- 工程内作業履歴を追跡できる
- 作業者が現在の進捗を確認できる

---

# Phase 6: ログイン・ユーザー・権限管理

## 28. Phase 6の目的

- 誰がどの工程・個体・作業を担当したかを記録する
- 管理者向け画面と作業者向け画面を分離する
- マスタ・計画の変更権限を制限する
- `workerName`文字列入力をログインユーザー参照へ移行する

## 29. 初期ロール案

```text
ADMIN
システム・マスタ・ユーザー管理

MANAGER
月間計画・日産計画・全体進捗

LEADER
担当ライン・担当工程の計画と進捗

WORKER
担当工程の作業実績入力

QUALITY
検査・品質・差し戻し・承認
```

最初からすべて実装せず、初期導入は以下でもよい。

```text
ADMIN
WORKER
```

その後、運用に応じて`MANAGER`、`LEADER`、`QUALITY`を追加する。

## 30. User Entity候補

```text
id
employeeNo
loginId
passwordHash
name
department
role
enabled
createdAt
updatedAt
```

将来的な関連変更:

```text
ProcessHistory.workerName
↓
ProcessHistory.worker(User)
```

既存履歴との互換性を保つため、移行期間は`workerName`を残す可能性がある。

## 31. ロール別画面

### ADMIN

- 全機能
- Product・Master管理
- ユーザー管理
- 工程マスタ管理
- 全履歴・分析

### MANAGER

- ProductionOrder
- ProductionSchedule
- 月間・日産進捗
- 全工程状況

### LEADER

- 担当工程の本日計画
- 担当工程の実績
- 一括作業
- 異常・滞留確認

### WORKER

- 担当工程ページ
- 作業対象一覧
- 一括開始・一括終了
- 工程内作業入力
- 自分の作業履歴

### QUALITY

- 検査
- 不良判定
- 差し戻し
- 再検査
- 品質分析

## 32. ログイン後の遷移候補

```text
ADMIN
→ 管理ダッシュボード

MANAGER
→ 月間・日産計画ダッシュボード

LEADER
→ 担当工程進捗画面

WORKER
→ 担当工程専用ページ

QUALITY
→ 検査待ち一覧
```

## 33. Phase 6完了条件

- ログイン・ログアウトができる
- パスワードを安全に保存できる
- ロール別にURLアクセスを制御できる
- ロール別にメニュー表示を切り替えられる
- 工程実績へUserを関連付けられる
- 管理者以外がマスタ・計画を変更できない
- Securityテストが整備されている

---

# Phase 7: 差し戻し・再作業・品質管理

## 34. Phase 7の目的

実際の工場で頻繁に発生する工程間の出戻り、再作業、不良、再検査を履歴として記録する。

単純なステータス上書きではなく、変更理由と経路を残す。

## 35. 想定フロー

```text
塗装検品 PASS
↓
パーツ取付開始
↓
傷発生
↓
塗装工程へ差し戻し
↓
再塗装
↓
再検品 PASS
↓
パーツ取付再開
```

## 36. 保持する情報候補

```text
対象個体
差し戻し元工程
差し戻し先工程
理由コード
詳細コメント
登録者
登録日時
承認者
承認日時
再作業担当者
再作業開始日時
再作業完了日時
再検査結果
画像
```

## 37. 理由マスタ候補

```text
傷
打痕
塗装過多
色ムラ
組付不良
配線不良
部品不良
作業ミス
寸法不良
音質・演奏性不良
```

## 38. Phase 7完了条件

- 差し戻しを履歴として登録できる
- 差し戻し先工程を制御できる
- 再作業を開始・完了できる
- 再検査結果を保存できる
- 不合格・廃棄を履歴付きで管理できる
- 品質担当または管理者の承認を設定できる

---

# Phase 8: トレーサビリティ・分析・ダッシュボード

## 39. Phase 8の目的

個体、計画、部材、作業者、工程、品質を横断して追跡・分析できるようにする。

## 40. トレーサビリティ

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
ProcessTaskHistory
↓
Return / Rework / Inspection
↓
User
```

## 41. 分析候補

- 月間計画達成率
- 日産計画達成率
- モデル別完成数
- 工程別仕掛数
- 工程別滞留時間
- 工程平均時間
- 作業者別実績
- 差し戻し率
- 再作業率
- モデル別不良率
- 理由別不良件数
- 完成リードタイム
- 計画と実績の差

## 42. ダッシュボードの役割

### 管理者・生産管理者

```text
月間計画 2,000本
完成 1,420本
残 580本
日産割当済 1,800本
未割当 200本
```

### 現場リーダー

```text
本日の予定 120本
完了 84本
残 36本
工程待ち 22本
実施中 5本
異常 3本
```

### 作業者

```text
担当工程の待ち 18本
自分の実施中 4本
本日の完了 32本
```

---

# Phase 9: 運用強化・性能・保守性改善

## 43. E2E基盤共通化

現在、各E2EクラスにDB接続・データ準備・削除処理が重複している。

将来候補:

```text
E2EDatabaseSupport.java
E2ETestDataFactory.java
E2ECleanupSupport.java
```

主要機能開発を妨げないタイミングで、専用ブランチにより共通化する。

## 44. 大量データ対応

実運用では月2,000本から2,500本、複数年運用では数万件以上になる。

確認項目:

- ページング
- 日付・モデル・工程・状態による絞り込み
- インデックス
- 集計クエリ
- N+1問題
- 一括INSERT・一括UPDATE
- E2Eテストの実行時間
- 長期履歴のアーカイブ方針

## 45. 保守性改善

- 未使用コード・import整理
- DTOの責務整理
- Service肥大化時の分割
- CSS重複整理
- 共通View Component整理
- エラーメッセージ統一
- 監査ログ
- 設定値の外部化
- CIでの通常テスト・E2E実行

---

## 46. 今週の推奨作業計画

今週はPhase 2の基礎に集中する。

### Step 1: ProductionSchedule仕様確定

- ProductionOrderとの関係
- 日産計画の定義
- 状態
- 数量制約
- 日付制約
- 編集・取消条件
- 個体発行前後の変更制限

### Step 2: DB設計

```text
260831_01_create_t_production_schedule.sql
260831_02_verify_t_production_schedule.sql
260831_03_rollback_t_production_schedule.sql
```

### Step 3: Java基盤

```text
productionschedule/
├── ProductionSchedule.java
├── ProductionScheduleRepository.java
├── ProductionScheduleService.java
└── ProductionScheduleStatusConstants.java
```

### Step 4: Serviceテスト

優先テスト:

- 正常登録
- 計画数0以下の拒否
- ProductionOrder不存在
- 取消済みProductionOrderへの登録拒否
- 完了済みProductionOrderへの登録拒否
- 同一日付重複拒否
- 月間計画超過拒否
- 割当済数計算
- 未割当数計算
- 更新
- 取消

### Step 5: 最小画面

```text
ProductionOrder詳細
↓
日産計画一覧
↓
日産計画登録
```

今週中に最低限、ProductionScheduleのDB・Entity・Repository・Service・Serviceテストまで到達できればよい。

---

## 47. 今週はまだ実装しないもの

ProductionScheduleの基盤が固まる前に、以下へ同時着手しない。

```text
Body・Neck一括発行
チェックボックス一括工程処理
Spring Security
工程内作業
差し戻し
品質管理
```

これらは重要だが、ProductionScheduleの設計と関連付けが確定してから進める。

---

## 48. 開発時の継続ルール

- 最新版ファイルを最初に確認する
- 過去チャットだけを根拠にコードを推測しない
- 新規・修正コードは省略せず完成版ファイルで出力する
- Eclipseの案内はPleiades日本語表示を優先する
- Controllerへ業務ロジックを書きすぎない
- 検証・計算・保存はServiceへ置く
- トランザクション境界はServiceへ置く
- `ddl-auto=validate`を維持する
- DB変更はSQLで明示管理する
- 適用SQL・確認SQL・ロールバックSQLを用意する
- 画面値を信用せずServiceで再検証する
- 一括処理は全件検証後に実行する
- 一部成功を避け、エラー時は全件ロールバックする
- 実績系データは原則物理削除しない
- Masterは削除より無効化を優先する
- 機能単位で対象テストを実行する
- コミット前に通常テスト全件を実行する
- 主要業務変更時はE2Eも実行する
- E2E更新系テストは`guitar_mes_e2e`で実行する
- コミットメッセージは日本語にする

---

## 49. 次回開始地点

次回は、ProductionScheduleの正式仕様を確定してからSQLへ進む。

最初に決める項目:

```text
1. ProductionScheduleの状態
2. 同一ProductionOrder・同一日付を1件に限定するか
3. 対象日付をProductionOrderの期間内へ制限するか
4. DRAFTとCONFIRMEDの編集可能範囲
5. CANCELLEDの再有効化を許可するか
6. 個体発行後の数量・日付変更を禁止するか
7. 一部発行を許可するか
8. 日産計画完了をどの実績から判定するか
```

推奨ブランチ:

```text
feature/production-schedule
```

---

## 50. 次回チャット開始用ショートメモ

```text
Guitar MES開発を継続します。
2026-08-31版の新ロードマップを確認してください。

Phase 1は完了しています。

完了済み:
・主要UIとCRUD
・Product、Master、ProductionOrder編集・取消
・Package by Feature
・通常テスト170件
・MockMvc主要画面テスト
・Playwright E2E 8シナリオ
・E2E専用DB
・ProductionOrderからGuitar完成までの自動シナリオ

今後の方向:
ProductionOrder = 月間・モデル別計画
ProductionSchedule = 日付別投入計画

その後:
・Body / Neck一括発行
・チェックボックスによる一括工程開始・終了
・工程別専用ページと工程内作業
・ログインとADMIN / WORKER等の権限管理
・差し戻し、再作業、品質管理
・トレーサビリティと分析

次はProductionScheduleの正式仕様確定から開始します。
```
