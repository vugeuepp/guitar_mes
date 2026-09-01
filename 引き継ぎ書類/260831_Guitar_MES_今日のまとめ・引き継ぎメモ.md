# 2026-08-31 Guitar MES 今日のまとめ・引き継ぎメモ

## 1. 本日の概要

本日は、Guitar MESの新機能である **ProductionSchedule（日産計画）** の第一段階を実装した。

月単位のProductionOrderに対して、日付別の生産数量を割り当て、画面から登録、編集、確定、取消できるところまで完成した。また、HTMLを新規作成または更新したタイミングでPlaywright E2Eテストを追加・実行する新しい開発方針を、今回の実装から適用した。

本日の最終到達点は次のとおり。

```text
ProductionOrder（月間生産計画）
        ↓
ProductionSchedule（日産計画）
        ↓
今後の部品投入・Body/Neck発行
        ↓
Assembly
        ↓
Guitar
```

---

## 2. 本日完了した作業

### 2.1 ProductionOrderへの対象月対応

ProductionScheduleを月間生産計画の配下に持たせるため、ProductionOrderに`planMonth`を導入した。

主な対応内容は次のとおり。

- ProductionOrderへ対象月を追加
- 登録画面および編集画面へ対象月入力を追加
- 対象月と生産開始予定日、納期の整合性を検証
- DB変更をSQLで明示管理
- 開発DBおよびE2E DBへSQLを適用
- 既存テストを更新

### 2.2 ProductionScheduleのDBとコア機能

以下を実装した。

- `t_production_schedule`テーブル
- `ProductionSchedule` Entity
- `ProductionScheduleRepository`
- `ProductionScheduleStatusConstants`
- `ProductionScheduleService`
- `ProductionScheduleServiceTest`

ProductionScheduleの主な項目は次のとおり。

```text
id
production_order_id
schedule_date
planned_quantity
status
created_at
updated_at
```

状態は次の4種類。

```text
PLANNED    計画中
CONFIRMED  確定
COMPLETED  完了
CANCELLED  取消
```

### 2.3 Serviceの業務ルール

以下の業務ルールを実装済み。

- 日産計画日は必須
- 日産計画数は1以上
- 日産計画日は親ProductionOrderの対象月内
- 同一ProductionOrder内で同じ日付を重複登録できない
- 取消済み以外の日産計画合計が月間計画数を超えない
- 中止済みまたは完了済みProductionOrderには登録・変更できない
- 編集できるのは`PLANNED`のみ
- 確定できるのは`PLANNED`のみ
- `COMPLETED`または`CANCELLED`は取消不可
- `PLANNED`と`CONFIRMED`は取消可能
- 取消済み数量は割当済数から除外

編集処理では、自分自身の数量を合計から除外して月間上限を判定する。

### 2.4 ProductionSchedule登録画面

以下を実装した。

- `ProductionScheduleCreateRequest.java`
- `ProductionScheduleViewController.java`
- `production-schedule-form.html`
- `ProductionScheduleViewControllerTest.java`

登録URLは次のとおり。

```text
GET  /production-orders/{productionOrderId}/schedules/new
POST /production-orders/{productionOrderId}/schedules/create
```

登録画面では次の情報を表示する。

- 生産指示番号
- 対象月
- 製品
- 月間計画数
- 割当済数
- 未割当数

入力項目は次の2項目。

- 日産計画日
- 日産計画数

登録後は親ProductionOrderの詳細画面へ戻る。

### 2.5 ProductionOrder詳細への日産計画統合

`production-order-detail.html`へ日産計画セクションを追加した。

配置位置は次のとおり。

```text
生産計画基本情報
生産進捗
日産計画
製造開始
生成済みギター
```

日産計画セクションでは次を表示する。

- 割当済数
- 未割当数
- 日産計画一覧
- 日産計画登録ボタン
- 編集、確定、取消の操作ボタン

一覧項目は次のとおり。

```text
計画日
計画数
状態
操作
```

状態別の操作ボタンは次のとおり。

```text
PLANNED
├── 編集
├── 確定
└── 取消

CONFIRMED
└── 取消

COMPLETED
└── 操作なし

CANCELLED
└── 操作なし
```

### 2.6 ProductionSchedule編集・確定・取消

以下を実装した。

- `ProductionScheduleUpdateRequest.java`
- `production-schedule-edit-form.html`
- `ProductionScheduleViewController.java`の編集・確定・取消処理
- `ProductionScheduleViewControllerTest.java`の追加テスト

URLは次のとおり。

```text
GET  /production-schedules/{id}/edit
POST /production-schedules/{id}/edit
POST /production-schedules/{id}/confirm
POST /production-schedules/{id}/cancel
```

編集画面では現在の日付と数量を初期表示し、保存後は親ProductionOrder詳細へ戻る。

確定と取消はProductionOrder詳細画面の一覧からPOSTする。ブラウザの確認ダイアログを表示する。

---

## 3. テスト実績

### 3.1 Service・Controller対象テスト

最終確認結果は次のとおり。

```text
ProductionScheduleViewControllerTest: 7件成功
ProductionScheduleServiceTest: 17件成功

合計: 24件
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### 3.2 ProductionSchedule登録E2E

`ProductionScheduleCreateE2E.java`を作成し、次を確認した。

```text
E2E用ProductionOrderを作成
↓
生産計画詳細を表示
↓
日産計画未登録を確認
↓
登録画面へ遷移
↓
日付と数量を入力
↓
登録
↓
詳細画面へ戻る
↓
一覧表示を確認
↓
割当済数と未割当数を確認
↓
DB登録内容を確認
↓
テストデータを削除
↓
残存データ0件を確認
```

登録例は次のとおり。

```text
月間計画数: 10台
登録前割当済数: 0台
登録前未割当数: 10台
登録数量: 4台
登録後割当済数: 4台
登録後未割当数: 6台
状態: PLANNED
```

### 3.3 ProductionSchedule操作E2E

`ProductionScheduleOperationE2E.java`を作成し、次を確認した。

```text
PLANNEDの日産計画をDBへ準備
↓
詳細画面から編集画面へ遷移
↓
日付と数量を変更
↓
割当済数と未割当数を確認
↓
日産計画を確定
↓
編集ボタンが消えることを確認
↓
確定済み日産計画を取消
↓
割当済数が0へ戻ることを確認
↓
DBの最終状態がCANCELLEDであることを確認
↓
テストデータを削除
```

最終結果は次のとおり。

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### 3.4 E2E全件

ProductionSchedule登録E2E追加時点で、既存E2Eを含む全9件が成功した。

```text
Tests run: 9
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

その後、ProductionSchedule操作E2Eを1件追加したため、次回の全件実行時は10件が想定される。

```text
想定件数: 10件
```

---

## 4. 証跡ファイル

### 4.1 登録E2E

保存先。

```text
target/playwright/evidence/production-schedule-create/
```

主な証跡。

```text
01-production-order-detail-before.png
02-production-schedule-form.png
03-production-schedule-input.png
04-production-order-detail-after.png
05-database-verified.png
99-final-*.png
```

### 4.2 編集・確定・取消E2E

保存先。

```text
target/playwright/evidence/production-schedule-operation/
```

想定される主な証跡。

```text
01-planned-before-edit.png
02-edit-form-before.png
03-edit-form-input.png
04-detail-after-edit.png
05-detail-after-confirm.png
06-detail-after-cancel.png
99-final-*.png
```

---

## 5. 本日発生した問題と対応

### 5.1 `validateEditable()`の定義漏れ

`ProductionScheduleService`で次の呼び出しがある一方、メソッド定義が不足していた。

```java
validateEditable(productionSchedule);
```

次の検証を追加して解決した。

```java
private void validateEditable(
        ProductionSchedule productionSchedule) {

    if (!PLANNED.equals(
            productionSchedule.getStatus())) {

        throw new BusinessException(
                "計画中の日産計画のみ編集できます。");
    }
}
```

### 5.2 編集画面テンプレートの配置漏れ

E2E実行時に次のエラーが発生した。

```text
Error resolving template [production-schedule-edit-form]
```

原因は、次のHTMLをテンプレートフォルダへ配置し忘れていたこと。

```text
src/main/resources/templates/
└── production-schedule-edit-form.html
```

配置後にアプリケーションを再起動し、`ProductionScheduleOperationE2E`は成功した。

今後、新規HTMLを作成した際は次を確認する。

```text
1. src/main/resources/templatesへ配置
2. ファイル名がControllerの戻り値と完全一致
3. target/classes/templatesへコピー済み
4. アプリケーションを再起動
5. 手動画面確認
6. Playwright E2E実行
```

### 5.3 DevToolsによる自動再起動

HTML・class変更時にSpring Boot DevToolsが複数回再起動した。

ログ例。

```text
Restarting due to class path changes
Graceful shutdown complete
Started GuitarMesApplication
```

これは異常ではない。ただし、E2E実行中に再起動が重ならないよう、ファイル配置完了後にアプリケーションが完全起動したことを確認してからE2Eを開始する。

---

## 6. 本日確定した開発方針

### 6.1 HTML変更時のE2E必須化

今後はHTMLを新規作成または更新するたびに、該当画面のPlaywright E2Eを都度作成または更新し、実行する。

標準フローは次のとおり。

```text
HTML作成・更新
↓
Controller / MockMvcテスト作成・更新
↓
対象テスト実行
↓
手動画面確認
↓
Playwright E2E作成・更新
↓
対象E2E実行
↓
通常テスト全件
↓
E2E全件
↓
コミット
```

### 6.2 必要ファイルの依頼形式

今後、実装に必要なファイルを依頼するときは、次の区分順で記載する。

```text
1. src
2. test
3. HTML
```

各区分内ではファイル名をアルファベット順にする。

例。

### `src`

```text
ProductionScheduleService.java
ProductionScheduleViewController.java
```

### `test`

```text
ProductionScheduleOperationE2E.java
ProductionScheduleServiceTest.java
ProductionScheduleViewControllerTest.java
```

### `HTML`

```text
production-order-detail.html
production-schedule-edit-form.html
```

### 6.3 DBスキーマ変更の管理

Hibernateの`ddl-auto`による自動更新には依存せず、DBスキーマ変更とデータ移行はSQLで明示管理する。

```text
開発DB
E2E DB
将来の本番DB
```

すべて同一のSQL履歴から再現できるようにする。

### 6.4 テストデータの後処理

E2Eではテスト専用データを作成し、テスト終了時に削除する。

```text
try
↓
画面操作・DB確認
↓
finally
↓
日産計画削除
↓
ProductionOrder削除
↓
残存データ0件確認
```

---

## 7. 現在の主要ファイル構成

### `src`

```text
src/main/java/com/example/guitarmes/productionschedule/
├── ProductionSchedule.java
├── ProductionScheduleCreateRequest.java
├── ProductionScheduleRepository.java
├── ProductionScheduleService.java
├── ProductionScheduleStatusConstants.java
├── ProductionScheduleUpdateRequest.java
└── ProductionScheduleViewController.java
```

### `test`

```text
src/test/java/com/example/guitarmes/productionschedule/
├── ProductionScheduleServiceTest.java
└── ProductionScheduleViewControllerTest.java

src/test/java/com/example/guitarmes/e2e/
├── ProductionScheduleCreateE2E.java
└── ProductionScheduleOperationE2E.java
```

### `HTML`

```text
src/main/resources/templates/
├── production-order-detail.html
├── production-schedule-edit-form.html
└── production-schedule-form.html
```

---

## 8. 次回開始時に確認すること

### 8.1 Git状態

次回開始時に未コミット差分を確認する。

```bash
git status
```

必要に応じて差分を確認する。

```bash
git diff
```

### 8.2 通常テスト

まず通常テスト全件を実行する。

```bash
./mvnw test
```

### 8.3 E2Eアプリ起動

E2Eプロファイルで起動し、次を確認する。

```text
The following 1 profile is active: "e2e"
Database JDBC URL: jdbc:postgresql://localhost:5432/guitar_mes_e2e
Started GuitarMesApplication
```

### 8.4 E2E全件

今回追加した操作E2Eを含めて全件実行する。

```bash
./mvnw '-Dtest=*E2E' test
```

期待値。

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

件数が10件と異なる場合は、実際に検出されたE2Eクラス数と照合する。

---

## 9. 明日以降の推奨作業

ProductionScheduleの登録、編集、確定、取消が揃ったため、次は **ProductionScheduleを起点とした部品投入** へ進む。

推奨順序は次のとおり。

```text
ProductionScheduleと部品投入の要件整理
↓
Body発行方式の設計
↓
Neck発行方式の設計
↓
日産計画数との数量整合性
↓
一括発行Service
↓
Controller / MockMvc
↓
画面
↓
Playwright E2E
↓
通常テスト全件
↓
E2E全件
```

### 9.1 次に検討する業務ルール

以下を決める必要がある。

- 部品発行できる日産計画の状態
- `PLANNED`で発行可能か、`CONFIRMED`のみか
- BodyとNeckを同時発行するか、別々に発行するか
- 日産計画数と発行数を一致させるか
- 部分発行を許可するか
- 再発行を許可するか
- 取消済み日産計画に紐づく部品をどう扱うか
- 発行済み数量をどこで管理するか
- シリアル番号の採番規則
- 発行後の部品初期状態
- Assembly開始時の日産計画との紐付け方法

### 9.2 推奨する状態制御

現時点の推奨案。

```text
PLANNED
├── 編集可能
├── 確定可能
└── 部品発行不可

CONFIRMED
├── 編集不可
├── 部品発行可能
└── 条件付き取消可能

COMPLETED
└── 操作不可

CANCELLED
└── 操作不可
```

ただし、部品発行後の取消可否は先に設計する必要がある。

### 9.3 次回依頼する可能性が高いファイル

正式な実装範囲を決めた後、必要ファイルは次の形式で依頼する。

#### `src`

```text
Body.java
BodyRepository.java
BodyService.java
Neck.java
NeckRepository.java
NeckService.java
ProductionSchedule.java
ProductionScheduleService.java
```

#### `test`

```text
BodyServiceTest.java
NeckServiceTest.java
ProductionScheduleServiceTest.java
```

#### `HTML`

```text
production-order-detail.html
```

実際には要件確認後、必要なファイルだけに絞る。

---

## 10. コミット候補

本日の変更が未コミットの場合、役割ごとに分けるなら次の候補。

```text
DB追加: 日産計画テーブルを追加
```

```text
機能追加: 日産計画の登録機能と一覧表示を追加
```

```text
テスト追加: 日産計画登録のE2Eテストを追加
```

```text
機能追加: 日産計画の編集・確定・取消機能を追加
```

```text
テスト追加: 日産計画操作のE2Eテストを追加
```

コミット前は必ず次を実行する。

```bash
./mvnw test
./mvnw '-Dtest=*E2E' test
```

---

## 11. 現在の到達点

ProductionScheduleの第一段階は、次の範囲まで完成した。

```text
ProductionOrder
├── 対象月
├── 月間計画数
└── ProductionSchedule一覧

ProductionSchedule
├── 登録
├── 編集
├── 確定
├── 取消
├── 月間数量上限チェック
├── 重複日付チェック
├── 状態制御
├── MockMvcテスト
├── Serviceテスト
├── 登録E2E
└── 操作E2E
```

次回は、日産計画を実際の製造投入へ接続する段階に入る。

```text
ProductionOrder
↓
ProductionSchedule
↓
Body / Neck発行
↓
Assembly
↓
Guitar
```

本日の作業により、Guitar MESは月間生産計画だけでなく、日別の投入計画を管理できる構造へ進んだ。
