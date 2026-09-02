# Guitar MES 開発 引き継ぎメモ

- 更新日: 2026-08-29
- 対象日: 2026-08-29
- 対象プロジェクト: Guitar Manufacturing Execution System（Guitar MES）
- 技術構成: Java 17 / Spring Boot 4.1.0 / Thymeleaf / PostgreSQL / JUnit / Mockito / MockMvc / Playwright
- 開発環境: Mac OS / Eclipse / GitHub Desktop / ターミナル
- DBスキーマ管理方針: `spring.jpa.hibernate.ddl-auto=validate`
- 現在地: Phase 1BのBodyMaster編集、NeckMaster編集、ProductionOrder発行前編集・取消まで完了。Package by Feature移行、MockMvc拡充、Playwright E2E基盤の初期導入まで完了

## 1. 本日の総括

2026年8月29日は、機能追加だけでなく、今後の開発速度と品質を支える基盤整備を大きく進めた。

主な成果は以下。

- BodyMaster編集機能を完成
- NeckMaster編集機能を完成
- ProductionOrderの発行前編集・取消機能を完成
- ProductionOrder編集画面の日付初期表示不具合を修正
- MavenまたはJUnitによる全自動テスト一括実行を確認
- Javaパッケージをレイヤー単位から機能単位へ再編
- 本番コードとテストコードのパッケージ構成を対応させた
- MockMvcを使ったControllerテストを主要画面へ追加
- Playwrightを使ったE2Eテスト基盤を導入
- 生産計画一覧の実ブラウザ表示とスクリーンショット証跡保存を確認
- 自動テストは最終的に171件成功、エラー0、失敗0

## 2. BodyMaster編集

### 完成した仕様

- BodyMaster詳細画面から編集画面へ遷移可能
- 編集可能項目は`modelName`のみ
- 以下は識別情報・物理仕様として変更不可
  - `modelCode`
  - `productFamilyCode`
  - `bodyType`
  - `material`
  - `color`
- 編集画面では変更不可項目を読み取り専用表示
- モデル名未入力はBusinessExceptionで拒否
- エラー時は編集画面へ戻し、メッセージを画面内表示

### 追加・更新した主なファイル

```text
master/body/
├── BodyMaster.java
├── BodyMasterController.java
├── BodyMasterRepository.java
├── BodyMasterService.java
└── BodyMasterUpdateRequest.java
```

```text
templates/
├── body-master-detail.html
└── body-master-edit-form.html
```

### テスト

```text
BodyMasterServiceTest       9件
BodyMasterControllerTest    7件
```

## 3. NeckMaster編集

### 完成した仕様

- NeckMaster詳細画面から編集画面へ遷移可能
- 編集可能項目は`modelName`のみ
- 以下は識別情報・物理仕様として変更不可
  - `modelCode`
  - `productFamilyCode`
  - `neckType`
  - `neckMaterial`
  - `fingerboardMaterial`
  - `fretCount`
  - `scale`
- 編集画面では変更不可項目を読み取り専用表示
- モデル名未入力はBusinessExceptionで拒否
- エラー時は編集画面へ戻し、メッセージを画面内表示

### 追加・更新した主なファイル

```text
master/neck/
├── NeckMaster.java
├── NeckMasterController.java
├── NeckMasterRepository.java
├── NeckMasterService.java
└── NeckMasterUpdateRequest.java
```

```text
templates/
├── neck-master-detail.html
└── neck-master-edit-form.html
```

### テスト

```text
NeckMasterServiceTest       9件
NeckMasterControllerTest    7件
```

## 4. ProductionOrder発行前編集・取消

### 編集可能条件

以下をすべて満たすProductionOrderのみ編集可能。

```text
status = PLANNED
startedQuantity = 0
completedQuantity = 0
関連Guitarなし
```

### 編集可能項目

- Product
- 計画数
- 開始予定日
- 納期

### 編集不可項目

- 生産指示番号
- 着手数
- 完成数
- 状態

### 取消仕様

- 編集可能条件と同じ条件でのみ取消可能
- 物理削除は行わない
- `status`を`CANCELLED`へ変更
- 取消済み、製造中、完成済み、着手実績あり、完成実績あり、Guitar発行済みの場合は拒否
- 詳細画面で取消確認ダイアログを表示
- 取消後は既存のDisplayHelperにより「中止」と表示

### 日付初期表示不具合の修正

ProductionOrder編集画面でProductと計画数は表示される一方、開始予定日と納期が空になる問題が発生した。

`ProductionOrderUpdateRequest`のLocalDate項目へ以下を追加して解消した。

```java
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
```

対象項目:

- `plannedStartDate`
- `dueDate`

### 主なファイル

```text
productionorder/
├── ProductionOrder.java
├── ProductionOrderCreateRequest.java
├── ProductionOrderDisplayHelper.java
├── ProductionOrderRepository.java
├── ProductionOrderService.java
├── ProductionOrderStatusConstants.java
├── ProductionOrderUpdateRequest.java
└── ProductionOrderViewController.java
```

```text
templates/
├── production-order-detail.html
└── production-order-edit-form.html
```

### Serviceテスト

`ProductionOrderServiceTest`へ12件を整備。

主な対象:

- 未着手計画から編集DTO生成
- 未着手計画の更新
- 未着手計画の取消
- 製造中計画の編集拒否
- 着手実績ありの編集拒否
- 完成実績ありの取消拒否
- Guitar発行済みの編集拒否
- 取消済みの再取消拒否
- 計画数0の拒否
- 不正な日付範囲の拒否
- 存在しないProductの拒否
- nullリクエストの拒否

## 5. 全テスト一括実行

これまでServiceテストをクラス単位で実行していたが、すべてまとめて実行できることを確認した。

### ターミナル

```bash
./mvnw test
```

### Eclipse

```text
src/test/java
右クリック
→ Run As
→ JUnit Test
```

### 推奨運用

```text
開発中     → 変更対象のテストだけ実行
機能完成時 → 全件実行
コミット前 → 全件実行
```

## 6. Package by Featureへの移行

### 目的

従来は以下のレイヤー単位でファイルが分散していた。

```text
controller
service
repository
entity
dto
```

機能修正のたびに複数ディレクトリを移動する必要があったため、機能単位へ再編した。

### 現在の主要構成

```text
com.example.guitarmes
├── assembly
├── body
│   └── process
├── common
├── dashboard
├── dto
├── exception
├── guitar
├── master
│   ├── body
│   ├── instrumenttype
│   ├── neck
│   └── productseries
├── neck
│   └── process
├── process
│   ├── analysis
│   └── common
├── product
│   └── image
└── productionorder
```

### 移行済み機能

- ProductionOrder
- Product
- ProductImage
- ProductSeriesMaster
- InstrumentTypeMaster
- BodyMaster
- NeckMaster
- Body
- Neck
- Guitar
- Assembly
- Body工程
- Neck工程
- 共通工程
- 工程分析
- Dashboard

### テストの配置ルール

本番コードとテストコードは同じパッケージ名に揃えるが、ソースフォルダは分離する。

```text
本番コード:
src/main/java/com/example/guitarmes/master/body/

テストコード:
src/test/java/com/example/guitarmes/master/body/
```

テストを`src/main/java`へ置くとJUnit・Mockitoのテスト依存関係を利用できずエラーになるため注意。

### ブランチ

Package by Feature移行は専用ブランチで実施し、機能単位でテスト確認とコミットを行った。

```text
refactor/package-by-feature
```

移行完了後、mainへマージしてブランチを終了した。

## 7. MockMvcによるControllerテスト

ServiceテストだけではURL、Model、View名、フォームの型変換、画面遷移を確認できないため、MockMvcテストを追加した。

### 追加したControllerテスト

```text
ProductionOrderViewControllerTest           9件
ProductViewControllerTest                  11件
ProductSeriesMasterViewControllerTest       8件
InstrumentTypeMasterViewControllerTest      8件
BodyMasterControllerTest                    7件
NeckMasterControllerTest                    7件
-----------------------------------------------
MockMvcテスト合計                          50件
```

### ProductionOrder画面テスト

主な対象:

- 一覧画面
- 登録画面
- 登録後リダイレクト
- 詳細画面
- 編集画面の初期値
- LocalDateのISO形式バインド
- 更新後リダイレクト
- 更新エラー表示
- 取消後リダイレクト
- 取消エラー表示

特に、編集画面の日付問題に対し、以下をテストで保護した。

```text
GET編集画面
→ plannedStartDateとdueDateがModelに存在

POST更新
→ yyyy-MM-dd形式をLocalDateへ変換
```

### Product画面テスト

主な対象:

- 一覧
- キーワード検索
- 登録画面
- 初期バリエーション生成
- 登録
- 詳細
- 編集画面
- 更新
- 更新エラー
- 画像登録
- 画像登録エラー
- 画像削除

## 8. Playwright E2Eテスト基盤

### 導入目的

MockMvcでは実ブラウザ描画、JavaScript、ボタン操作、レイアウト、実際の入力欄表示を確認できないため、Playwrightを導入した。

### 環境

```text
Mac OS
Eclipse
Playwright Java 1.62.0
Chromium
```

### 追加ファイル

```text
src/test/java/com/example/guitarmes/e2e/
├── PlaywrightTestBase.java
└── ProductionOrderSmokeE2E.java
```

`pom.xml`へPlaywright依存関係を追加した。

### 初回実行で確認した内容

- Chromiumの起動
- `http://localhost:8080/production-orders/view`へのアクセス
- ページタイトル「生産計画一覧」
- 「生産計画を登録」リンクの表示
- メイン領域の表示
- フルページスクリーンショット保存

### E2Eテスト結果

```text
ProductionOrderSmokeE2E
実行: 1件
失敗: 0件
エラー: 0件
スキップ: 0件
BUILD SUCCESS
```

### 証跡保存先

```text
target/playwright/evidence/
└── production-order-smoke/
    ├── 01-production-order-list.png
    └── 99-final-___________________.png
```

2枚目のファイル名にアンダースコアが多い理由は、日本語の`@DisplayName`を安全なファイル名へ変換しているため。動作上の問題はない。将来的には`99-final.png`の固定名へ整理してよい。

### 実行コマンド

E2Eのみ:

```bash
./mvnw -Dtest=ProductionOrderSmokeE2E test
```

ブラウザ表示あり:

```bash
./mvnw   -Dtest=ProductionOrderSmokeE2E   -De2e.headless=false   test
```

### 最終テスト結果

Eclipseの全件実行ではE2Eを含めて以下を確認した。

```text
実行: 171/171
エラー: 0
失敗: 0
```

## 9. Git運用

### 本日の主なブランチ

```text
refactor/package-by-feature
feature/mockmvc-test
feature/playwright-e2e
```

### 方針

- GitHub Desktopを普段のブランチ作成、コミット、マージに使用して問題なし
- 状態確認やテスト実行にはターミナルも併用
- コミットメッセージは今後日本語で統一

### コミットメッセージ例

```text
リファクタリング: ProductionOrder機能を機能別パッケージへ移行
テスト追加: 生産計画画面のMockMvcテストを追加
テスト追加: 製品画像操作のMockMvcテストを追加
テスト追加: PlaywrightによるE2Eテスト基盤を導入
```

## 10. 現在のDB方針

従来どおり以下を維持する。

```properties
spring.jpa.hibernate.ddl-auto=validate
```

- HibernateによるDDL自動更新は行わない
- DB変更はSQLで明示管理する
- 適用SQL、確認SQL、ロールバックSQLを用意する
- 実績系データは原則物理削除しない
- Masterは削除より無効化を優先する

本日のProductionOrder編集・取消は既存カラムのみを使用したため、DDL変更は不要だった。

## 11. 次回の最優先作業

Playwrightで更新系シナリオを実行するとDBを書き換えるため、開発用DBとは分離する。

### 推奨方針

```text
guitar_mes
→ 通常開発用DB

guitar_mes_e2e
→ Playwright専用DB
```

### 次回の最初の1ステップ

一気に編集シナリオへ進まず、以下だけを行う。

```text
1. PostgreSQLにguitar_mes_e2eを作成
2. 既存スキーマと必要なテストデータを複製
3. src/test/resources/application-e2e.propertiesを追加
4. E2Eプロファイルでアプリを起動
5. 接続先がguitar_mes_e2eであることを確認
6. 既存の参照系Smokeテストを再実行
```

### その次のシナリオ

E2E専用DBの接続確認後、ProductionOrder編集シナリオへ進む。

```text
生産計画一覧
↓
計画中の生産計画を選択
↓
詳細画面
↓
編集画面
↓
計画数・開始予定日・納期の初期値確認
↓
値を変更
↓
保存
↓
詳細画面への反映確認
↓
各段階のスクリーンショット保存
```

想定証跡:

```text
target/playwright/evidence/production-order-edit/
├── 01-list.png
├── 02-detail-before.png
├── 03-edit-before.png
├── 04-edit-input.png
├── 05-detail-after.png
└── 99-final.png
```

## 12. 次回開始時の確認事項

- 現在のブランチが`feature/playwright-e2e`であること
- Playwright基盤のコミット有無
- `./mvnw test`の通常テスト結果
- `ProductionOrderSmokeE2E`が成功すること
- PostgreSQLが起動していること
- 開発用DBを更新系E2Eから保護すること

## 13. 次回チャット冒頭で伝える内容

```text
2026-08-29のGuitar MES引き継ぎメモを確認してください。
本日は以下まで完了しています。

・BodyMaster編集
・NeckMaster編集
・ProductionOrder発行前編集・取消
・Package by Featureへの移行
・MockMvcテスト50件の追加
・Playwright E2E基盤の導入
・生産計画一覧の実ブラウザ確認とスクリーンショット保存
・全テスト171/171成功

次は開発用DBを保護するため、guitar_mes_e2eとapplication-e2e.propertiesを準備し、E2E専用プロファイルの接続確認から開始してください。
```

## 14. 作業ルール

- 最新版ファイルを最初に確認する
- 新規コード・修正コードは省略せず完成版ファイルで出力する
- コード以外の操作は日本語で手順を案内する
- package、import、DOCTYPE、閉じタグを省略しない
- 既存URL、Thymeleaf属性名、業務ロジックを無断変更しない
- Controllerへ業務ロジックを書きすぎない
- 検証、正規化、重複確認、保存はServiceへ置く
- トランザクション境界はServiceへ置く
- `ddl-auto=validate`を維持する
- DB変更はSQLで明示管理する
- 一つのまとまりごとに対象テストと全件テストを実行する
- コミットメッセージは日本語にする
- E2Eの更新系テストは専用DBで実行する
- スクリーンショットを証跡として保存する

## 15. 現時点の完了状況

```text
Product編集                              完了
ProductSeries DB化・編集・有効無効       完了
InstrumentType DB化・編集・有効無効      完了
Product画像管理                          完了
BodyMaster編集                           完了
NeckMaster編集                           完了
ProductionOrder発行前編集                完了
ProductionOrder取消                      完了
Package by Feature移行                   完了
Serviceテスト一括実行                    完了
MockMvc主要画面テスト                    完了
Playwright E2E基盤                       初期導入完了
E2Eスクリーンショット証跡                確認済み
E2E専用DB・プロファイル                  次回着手
ProductionOrder更新系E2E                 次々回候補
```
