# Guitar MES 開発 引き継ぎメモ（最新版）

- 更新日: 2026-08-30
- 対象プロジェクト: Guitar Manufacturing Execution System（Guitar MES）
- 技術構成: Java 17 / Spring Boot / Thymeleaf / PostgreSQL / JUnit / Mockito / MockMvc / Playwright
- 開発環境: Mac OS / Eclipse（Pleiades） / GitHub Desktop / ターミナル / DBeaver
- DBスキーマ管理方針: `spring.jpa.hibernate.ddl-auto=validate`
- 現在地: Phase 1Bの主要機能、Package by Feature移行、MockMvc主要画面テスト、Playwright主要業務シナリオ第1弾まで完了

---

## 1. 現在の総括

Guitar MESは、主要なCRUD・運用制御に加え、実ブラウザを使った主要業務シナリオの自動試験まで整備された。

現在の品質保証は、次の三層構成になっている。

```text
Service Test
↓
MockMvc Test
↓
Playwright E2E
```

主な到達点は以下。

- Product編集機能の完成
- Product代表画像の登録・差し替え・削除
- ProductSeriesMasterのDB化、編集、有効化、無効化
- InstrumentTypeMasterのDB化、編集、有効化、無効化
- BodyMaster編集機能の完成
- NeckMaster編集機能の完成
- ProductionOrderの発行前編集・取消機能の完成
- Package by Featureへの移行
- MockMvcによる主要画面テストの整備
- Playwright E2E基盤の導入
- E2E専用DBとE2E専用Springプロファイルの構築
- ProductionOrder、Product画像、マスタ管理、Assembly、Guitar全工程のE2E化
- E2E実行後のテストデータ自動クリーンアップ
- 通常テスト170件成功
- Playwright E2E 8シナリオ成功

---

## 2. 開発環境

```text
OS: Mac OS
IDE: Eclipse（Pleiades日本語化版）
DBクライアント: DBeaver
補助操作: psql
ビルド・テスト: Maven Wrapper
ブラウザ自動操作: Playwright Java / Chromium
```

Eclipseの操作説明は、今後もPleiades日本語表示を前提とする。

---

## 3. DBスキーマ管理方針

HibernateによるDDL自動更新は使用しない。

```properties
spring.jpa.hibernate.ddl-auto=validate
```

今後も以下を守る。

- Entity変更だけではDBスキーマを変更しない
- テーブル、カラム、制約、インデックスはSQLで明示管理する
- データ移行・補正もSQLで管理する
- 適用SQLを用意する
- 確認SQLを用意する
- ロールバックSQLを用意する
- SQL適用後にDB構造を検証する
- `ddl-auto=validate`でEntityとDBの整合を確認する
- 実績系データは原則として物理削除しない
- Masterは削除より無効化を優先する

---

## 4. Phase 1Bの主要完了機能

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
MockMvc主要画面テスト                    完了
```

### ProductionOrder編集可能条件

以下をすべて満たす場合だけ編集・取消を許可する。

```text
status = PLANNED
startedQuantity = 0
completedQuantity = 0
関連Guitarなし
```

### ProductionOrder編集可能項目

- Product
- 計画数
- 開始予定日
- 納期

### ProductionOrder取消

- 物理削除しない
- `status`を`CANCELLED`へ変更する
- 製造開始済み、完成済み、Guitar発行済みの場合は拒否する

### 日付初期表示の修正

`ProductionOrderUpdateRequest`のLocalDate項目へ次を設定済み。

```java
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
```

対象項目:

- `plannedStartDate`
- `dueDate`

---

## 5. Package by Feature

Javaパッケージは、レイヤー単位ではなく機能単位へ再編済み。

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

本番コードとテストコードは、同じpackage名に揃えつつソースフォルダを分離する。

```text
本番コード:
src/main/java/com/example/guitarmes/...

テストコード:
src/test/java/com/example/guitarmes/...
```

---

## 6. MockMvcテスト

主要な画面Controllerについて、URL、Model、View、フォームバインド、リダイレクト、エラー表示を検証済み。

```text
ProductionOrderViewControllerTest
ProductViewControllerTest
ProductSeriesMasterViewControllerTest
InstrumentTypeMasterViewControllerTest
BodyMasterControllerTest
NeckMasterControllerTest
```

特にProductionOrderでは、編集画面の日付初期値と`yyyy-MM-dd`形式のLocalDateバインドを保護している。

---

## 7. E2E専用環境

### DB構成

```text
guitar_mes
→ 通常開発用DB

guitar_mes_e2e
→ Playwright E2E専用DB
```

PostgreSQL 18の`pg_dump`を使用して、開発DBのスキーマとデータをE2E専用DBへ複製済み。

### E2Eプロファイル

配置場所:

```text
src/main/resources/application-e2e.properties
```

主な設定:

```properties
spring.application.name=guitar_mes

spring.datasource.url=jdbc:postgresql://localhost:5432/guitar_mes_e2e
spring.datasource.username=naokiyamada
spring.datasource.password=
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

app.product-image.storage-path=./uploads/product-images
```

### Eclipseでの起動

```text
実行
↓
実行構成...
↓
Spring Boot アプリケーション
↓
GuitarMesApplication
↓
引数
```

プログラム引数:

```text
--spring.profiles.active=e2e
```

起動ログでは、必ず次を確認する。

```text
jdbc:postgresql://localhost:5432/guitar_mes_e2e
```

---

## 8. Playwright E2E基盤

配置場所:

```text
src/test/java/com/example/guitarmes/e2e/
```

基底クラス:

```text
PlaywrightTestBase.java
```

主な機能:

- Chromium起動・終了
- ヘッドレス／画面表示の切替
- ベースURL管理
- 画面サイズ設定
- スクリーンショット保存
- テスト終了時の最終証跡保存

証跡保存先:

```text
target/playwright/evidence/
```

---

## 9. 完成したPlaywright E2E

### 9.1 ProductionOrderSmokeE2E

```text
生産計画一覧を表示
↓
タイトル確認
↓
登録リンク確認
↓
スクリーンショット保存
```

### 9.2 ProductionOrderEditE2E

```text
一覧
↓
詳細
↓
編集
↓
計画数・開始予定日・納期の初期値確認
↓
計画数変更
↓
保存
↓
反映確認
↓
元の値へ復元
```

### 9.3 ProductionOrderCancelE2E

```text
生産計画を新規登録
↓
詳細画面
↓
中止確認ダイアログ
↓
中止
↓
状態=CANCELLEDを確認
↓
E2Eで作成したProductionOrderを削除
```

### 9.4 ProductImageUploadE2E

```text
画像未登録Productを取得
↓
テスト用PNGを生成
↓
画像アップロード
↓
画像表示とDB反映を確認
↓
画像削除
↓
画像未登録表示とDB復元を確認
↓
一時ファイル削除
```

既存画像を持つProductは対象にしない。

### 9.5 ProductSeriesMasterE2E

```text
製品シリーズ登録
↓
コード正規化確認
↓
シリーズ名編集
↓
無効化
↓
有効化
↓
DB確認
↓
E2E-SERIESを削除
```

### 9.6 InstrumentTypeMasterE2E

```text
楽器タイプ登録
↓
コード・内部モデルコードプレビュー確認
↓
名称・BodyType・NeckType編集
↓
無効化
↓
有効化
↓
DB確認
↓
E2E-INSTを削除
```

### 9.7 AssemblyCreateE2E

```text
E2E専用ProductionOrderを準備
↓
適合するE2E専用Body・Neckを準備
↓
生産計画詳細
↓
ネック取付
↓
Assembly登録
↓
Guitar生成
↓
Body.status = ASSEMBLED
↓
Neck.status = ASSEMBLED
↓
ProductionOrder.startedQuantity = 1
↓
ProductionOrder.status = IN_PROGRESS
↓
関連データ削除
```

### 9.8 GuitarProcessE2E

Guitar MESの主要製造フローを完成まで通すE2E。

```text
E2E専用ProductionOrder・Body・Neckを準備
↓
ネック取付
↓
Assembly登録
↓
Guitar生成
↓
ギターパーツ取付 開始・終了
↓
調整・調音 開始・終了
↓
最終検品 開始・終了
↓
Guitar完成
↓
工程履歴3件を確認
↓
ProductionOrder.completedQuantity = 1
↓
ProductionOrder.status = COMPLETED
↓
関連データ削除
```

最終確認状態:

```text
Guitar.currentProcess = 完成

ProductionOrder
plannedQuantity   = 1
startedQuantity   = 1
completedQuantity = 1
status            = COMPLETED

ProcessHistory
3件すべて終了済み
```

---

## 10. E2E証跡

主な証跡フォルダー:

```text
target/playwright/evidence/
├── production-order-smoke
├── production-order-edit
├── production-order-cancel
├── product-image
├── product-series-master
├── instrument-type-master
├── assembly-create
└── guitar-process
```

`@DisplayName`の日本語は、最終スクリーンショット名でアンダースコアへ置換される。動作上の問題はない。

将来的には最終証跡名を次へ固定してよい。

```text
99-final.png
```

---

## 11. E2Eの自動クリーンアップ

更新系E2Eでは、テストで作成したデータだけを削除する。

GuitarProcessE2Eの削除順:

```text
t_process_history
↓
t_assembly
↓
t_guitar
↓
t_body
↓
t_neck
↓
t_production_order
```

全E2E実行後、DBeaverで以下がすべて0件であることを確認済み。

```text
ProductionOrder   0件
Body              0件
Neck              0件
ProcessHistory    0件
ProductSeries     0件
InstrumentType    0件
```

Product画像E2Eも、対象Productの`image_file_name`を実行前の状態へ戻している。

PostgreSQLのシーケンス値はDELETEでは戻らない。E2E用のGuitarシリアルやIDに欠番が発生しても正常である。

---

## 12. テスト実行方法

### 通常テスト全件

```bash
./mvnw test
```

確認済み結果:

```text
Tests run: 170
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### E2E単体実行

```bash
./mvnw \
  -Dtest=GuitarProcessE2E \
  test
```

### ブラウザ表示あり

```bash
./mvnw \
  -Dtest=GuitarProcessE2E \
  -De2e.headless=false \
  test
```

### E2E全件実行

Macのシェルによるワイルドカード展開を避けるため、引用符を付ける。

```bash
./mvnw \
  '-Dtest=*E2E' \
  test
```

現在のE2E対象:

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

### Mavenテストレポート

```text
target/surefire-reports/
```

Finderで開く場合:

```bash
open target/surefire-reports
```

### Playwright証跡

```text
target/playwright/evidence/
```

Finderで開く場合:

```bash
open target/playwright/evidence
```

---

## 13. 現在のテスト状況

```text
通常テスト:        170件成功
Playwright E2E:      8シナリオ成功
通常テスト失敗:     0
E2E失敗:             0
E2E残存データ:       0件
```

通常テストとE2Eは別コマンドで実行しているため、正式な記録は「通常テスト170件成功、E2E 8シナリオ成功」とする。

---

## 14. 画面テストの評価

主要画面・主要業務フローのE2E第1弾は完了扱いでよい。

```text
画面E2E Phase 1
完了
```

現在、以下を実ブラウザで保護できている。

```text
参照
登録
編集
取消
ファイルアップロード
画像表示
画像削除
Master有効・無効切替
ネック取付
Assembly生成
Guitar生成
工程開始
工程終了
Guitar完成
ProductionOrder完成
```

追加候補はあるが、主要業務フローに比べて優先度は低い。

```text
DashboardE2E
ProcessAnalysisE2E
Product登録・編集E2E
BodyMasterE2E
NeckMasterE2E
```

---

## 15. 明日以降の推奨方針

### 第一候補: Phase 2へ進む

次の正式フェーズとして、月間計画・日産計画の設計を開始する。

```text
ProductionOrder
↓
ProductionSchedule
↓
日産計画
↓
Body・Neck個体一括発行
↓
一括工程操作
```

検討候補:

- `ProductionSchedule` Entity
- 月間計画から日産計画への割当
- 割当済数
- 未割当数
- 開始数
- 完成数
- 残数
- 日産計画確定
- 日産計画取消
- 月間計画超過チェック
- Body・Neck個体の一括発行

推奨ブランチ:

```text
feature/production-schedule
```

### 第二候補: E2E基盤の共通化

現在、各E2EクラスにDB接続・テストデータ作成・削除処理が重複している。

将来的な候補:

```text
E2EDatabaseSupport.java
E2ETestDataFactory.java
E2ECleanupSupport.java
```

推奨ブランチ:

```text
refactor/e2e-test-support
```

ただし、8本が安定して動いている現状を保存したうえで、専用ブランチで行う。

---

## 16. 次回開始時の確認事項

```text
1. 最新の引き継ぎメモを確認
2. 現在のGitブランチを確認
3. E2E関連変更がコミット済みか確認
4. 必要ならfeature/playwright-e2eをmainへマージ
5. ./mvnw testで通常テスト170件を確認
6. 必要に応じて./mvnw '-Dtest=*E2E' testを実行
7. Phase 2のProductionSchedule設計を開始
```

---

## 17. 次回チャット冒頭で伝える内容

```text
2026-08-30のGuitar MES引き継ぎメモ最新版を確認してください。

現在は以下まで完了しています。

・Product編集、画像管理
・ProductSeriesMaster DB化、編集、有効無効
・InstrumentTypeMaster DB化、編集、有効無効
・BodyMaster編集
・NeckMaster編集
・ProductionOrder発行前編集、取消
・Package by Feature移行
・MockMvc主要画面テスト
・E2E専用DB guitar_mes_e2e
・Playwright E2E 8シナリオ
・AssemblyからGuitar完成までの実ブラウザ試験
・通常テスト170件成功
・E2E全件成功
・E2E残存データ0件

画面E2E Phase 1は完了扱いです。
次はPhase 2のProductionSchedule、月間計画・日産計画の設計から開始してください。
```

---

## 18. 継続する作業ルール

- 最新版ファイルを最初に確認する
- 過去チャットだけを根拠にコードを推測しない
- 新規・修正コードは省略せず完成版ファイルで出力する
- package、import、DOCTYPE、閉じタグを省略しない
- Eclipseの案内はPleiades日本語表示を優先する
- 既存URL、Thymeleaf属性名、業務ロジックを無断変更しない
- Controllerへ業務ロジックを書きすぎない
- 検証、正規化、重複確認、保存はServiceへ置く
- トランザクション境界はServiceへ置く
- `ddl-auto=validate`を維持する
- DB変更はSQLで明示管理する
- SQL適用後は確認SQLを実行する
- 必要な場合はロールバックSQLを用意する
- 機能単位で対象テストを実行する
- コミット前に通常テスト全件を実行する
- 主要変更時はE2Eも全件実行する
- E2E更新系テストは`guitar_mes_e2e`で実行する
- E2Eで作成したデータは実行後に削除する
- スクリーンショットを証跡として保存する
- コミットメッセージは日本語にする
- 必要なファイルを指定するときはアルファベット順に依頼する。
- ファイルが欠けた状態で共有されたときは無理に進めず完全版を依頼する。

---

## 19. 現在の完了状況

```text
Phase 1B主要機能                         完了
Package by Feature                       完了
Serviceテスト                            整備済み
MockMvc主要画面テスト                    整備済み
E2E専用DB                                完了
Playwright E2E基盤                       完了
ProductionOrder E2E                      完了
Product画像 E2E                          完了
ProductSeriesMaster E2E                  完了
InstrumentTypeMaster E2E                 完了
Assembly E2E                             完了
Guitar全工程 E2E                         完了
E2E自動クリーンアップ                    完了
画面E2E Phase 1                          完了
ProductionSchedule設計                   次回候補
E2E共通基盤リファクタリング              将来候補
```
