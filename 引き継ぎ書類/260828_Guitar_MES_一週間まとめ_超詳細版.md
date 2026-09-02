# Guitar MES 開発 引き継ぎメモ（一週間まとめ・超詳細版）

- 更新日: 2026-08-28
- 対象期間: 2026-08-25 ～ 2026-08-28
- 対象プロジェクト: Guitar Manufacturing Execution System（Guitar MES）
- 技術構成: Java 17 / Spring Boot / Thymeleaf / PostgreSQL
- 開発環境: Eclipse系IDEを中心に利用
- DBスキーマ管理方針: `spring.jpa.hibernate.ddl-auto=validate`
- 現在地: Phase 1Bの主要項目であるProduct編集、製品シリーズDBマスタ化、楽器タイプDBマスタ化、Product代表画像対応まで完了
- 次回の第一候補: BodyMaster編集

---

## 1. このメモの目的

このファイルは、2026年8月25日から2026年8月28日までの引き継ぎメモ、2026年8月26日時点のロードマップ、2026年8月28日に実施した作業内容を統合したものである。

単なる日別作業記録ではなく、次回の開発で迷わず再開できるよう、以下をまとめている。

- 現在の正式な製造フロー
- Product、BodyMaster、NeckMaster、ProductionOrderの関係
- 旧設計から変更した内容
- Product編集の正式ルール
- 製品シリーズと楽器タイプのDBマスタ化
- Product画像の保存方式
- DBスキーマ変更履歴
- 自動テストの現在地
- 現在のファイル構成上の重要箇所
- 未完了項目
- 次回の推奨開始地点
- 今後のロードマップ
- 作業時に守るルール

---

## 2. 対象期間の大きな進捗

### 2026-08-25

主な到達点は以下。

- ProductionOrderを起点とする正式な製造フローを確立
- Body / Neck製造後にAssembly実績を登録し、Guitarを自動生成する流れへ移行
- ProductとBodyMaster / NeckMasterの外部キー関連を確立
- 生産計画に適合するBody / Neckだけをネック取付候補へ表示
- Guitar工程を「ギターパーツ取付 → 調整・調音 → 最終検品 → 完成」へ整理
- Header / Navigationを白・黒・赤の共通UIへ統一
- ダッシュボード、生産計画一覧、ギター管理一覧などのUI整備
- neck-list.htmlを含む主要画面の共通UI化を開始

### 2026-08-26

主な到達点は以下。

- 主要画面の共通UI整備を継続
- Product中心のマスタ構造を整理
- Productバリエーション一括登録を実装
- ProductSeries、InstrumentType、材質、指板材、フレット数、スケールをEnum化
- 製品シリーズと楽器タイプからMES内部モデルコードを自動生成
- ProductService側で画面値を再検証
- Product編集をPhase 1Bの最優先作業に設定
- Product画像対応、BodyMaster編集、NeckMaster編集、ProductionOrder編集・取消を後続候補として整理

### 2026-08-27

主な到達点は以下。

- スケール保存形式をmm表記からインチ数値へ統一
- Product編集機能を完成
- Product編集時のBusinessExceptionを画面内表示へ変更
- ProductionOrder開始済み、Guitar発行済みProductの仕様変更制限を実装
- BodyMaster / NeckMasterを製品ファミリー単位で分離
- HibernateのDDL自動更新を停止し、`ddl-auto=validate`へ移行
- 製品シリーズをDBマスタ化
- 次回最優先としてProduct登録・編集画面をDB製品シリーズへ接続する方針を確定

### 2026-08-28

主な到達点は以下。

- Product新規登録をDB製品シリーズへ完全接続
- Product編集をDB製品シリーズへ完全接続
- ProductSeries Enumを完全削除
- 楽器タイプをDBマスタ化
- Product新規登録・編集をDB楽器タイプへ接続
- InstrumentType Enumを完全削除
- 楽器タイプの編集、有効化、無効化を実装
- 製品シリーズの編集、有効化、無効化を実装
- Product代表画像の登録、差し替え、削除、一覧サムネイル表示を実装
- Product画像用DBカラムを明示SQLで追加
- 自動テストを合計89件まで拡充し、全件成功
- 実画面でProduct画像の登録、表示、差し替え、削除を確認
- `uploads/`をGit管理対象外に設定

---

## 3. 現在の正式な製造フロー

旧フローの「Guitarを直接登録してからAssemblyを登録」は廃止済み。

現在の正式フローは以下。

```text
ProductionOrder登録
↓
Product確定
  ・製品シリーズ
  ・楽器タイプ
  ・公式モデル番号
  ・カラー
  ・指板材
  ・BodyMaster
  ・NeckMaster
↓
対応するBody / Neckを製造
↓
生産計画詳細からネック取付
↓
Assembly実績保存
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

### ProductionOrderの状態

```text
PLANNED
IN_PROGRESS
COMPLETED
CANCELLED
```

### Guitarの生成タイミング

GuitarはProductionOrder登録時やProduct登録時には生成しない。

BodyとNeckが完成し、ネック取付実績としてAssemblyを登録した時点で初めて生成する。

### AssemblyServiceの主な責務

```text
ProductionOrder取得
Neck取得
Body取得
生産計画上限チェック
Body / NeckのAVAILABLE確認
ProductとBodyMaster / NeckMasterの適合確認
Guitar自動生成
Assembly保存
Body / NeckをASSEMBLEDへ変更
ProductionOrder.startedQuantityを1加算
ProductionOrder.statusをIN_PROGRESSへ変更
```

---

## 4. 現在の正式なEntity関連

```text
ProductionOrder
└─ Product
   ├─ BodyMaster
   └─ NeckMaster
```

```text
Body
└─ BodyMaster
```

```text
Neck
└─ NeckMaster
```

```text
Assembly
├─ ProductionOrder
├─ Body
├─ Neck
└─ Guitar
```

```text
Guitar
└─ ProductionOrder
```

Entity間の正式な関連は、文字列コードではなくDB上のIDと外部キーを使用する。

文字列コードは表示、識別、検索、採番ルールのために使用する。

---

## 5. Productの正式な識別ルール

### Product.modelNo

販売バリエーション単位の公式モデル番号。

例:

```text
5660102318
5371502303
TEST000001
```

### Product.internalModelCode

MES内部で製品ファミリーを識別するコード。

現在はDBマスタの以下を組み合わせて生成する。

```text
ProductSeriesMaster.seriesCode
+
InstrumentTypeMaster.instrumentCode
```

例:

```text
MIJ-HER50-ST
MIJ-TR50-ST
MIJ-H2-TL
```

### BodyMaster / NeckMaster

```text
productFamilyCode = Product.internalModelCode
```

異なる製品ファミリーでは、画面上の仕様が同じでもMasterを共有しない。

同一製品ファミリー内で、現在管理している仕様も一致する場合のみ共有する。

### BodyMaster共有条件

```text
productFamilyCode
+
bodyType
+
material
+
color
```

### NeckMaster共有条件

```text
productFamilyCode
+
neckType
+
neckMaterial
+
fingerboardMaterial
+
fretCount
+
scale
```

---

## 6. Product登録の現在の正式フロー

```text
有効な製品シリーズをDBから取得
↓
有効な楽器タイプをDBから取得
↓
製品シリーズ選択
↓
楽器タイプ選択
↓
MES内部モデルコードを画面で自動生成
↓
製品名候補を自動補填
↓
BodyType / NeckTypeを自動設定
↓
材質、PU構成、フレット数、スケールを選択
↓
販売バリエーション入力
  ・公式モデル番号
  ・カラー
  ・指板材
↓
ProductServiceでDBマスタを再取得
↓
MES内部モデルコードをServiceで再生成
↓
画面送信値との一致確認
↓
BodyType / NeckTypeの一致確認
↓
材質・指板材・フレット数・スケール検証
↓
重複確認
↓
BodyMaster検索または生成
↓
NeckMaster検索または生成
↓
ProductへMasterを関連付け
↓
トランザクション内で一括保存
```

### 重要

画面上で自動生成された値は信用しない。

以下は必ずServiceで再検証する。

- 製品シリーズがDBに存在し、有効であること
- 楽器タイプがDBに存在し、有効であること
- MES内部モデルコードが正しいこと
- BodyTypeが楽器タイプマスタと一致すること
- NeckTypeが楽器タイプマスタと一致すること
- 材質、指板材、フレット数、スケールが正式候補であること
- 公式モデル番号が重複していないこと
- 内部モデルコード、カラー、指板材の組み合わせが重複していないこと

---

## 7. Product編集機能

2026-08-27に基本機能を完成し、2026-08-28にDB製品シリーズ・DB楽器タイプへ完全移行した。

### 完成済み

- 編集専用DTO
- 編集画面表示
- 編集内容保存
- 既存Productから編集DTOへの変換
- internalModelCodeから現在のシリーズと楽器タイプを復元
- DBラベル値から材質・指板材・フレット数・スケールを復元
- 自分自身を除外した重複検証
- BodyMaster検索、再利用、新規生成、付け替え
- NeckMaster検索、再利用、新規生成、付け替え
- 共有Masterを直接更新しない
- BusinessException時に入力値を保持して再表示
- 詳細画面から編集画面への導線

### 参照状況による変更制限

#### 未参照Product

```text
全項目編集可能
```

#### ProductionOrderあり、未開始、Guitarなし

条件:

```text
startedQuantity = 0
completedQuantity = 0
Guitarなし
```

結果:

```text
全項目編集可能
```

#### ProductionOrder開始済み

条件:

```text
startedQuantity > 0
または
completedQuantity > 0
```

結果:

```text
製品名のみ変更可能
```

#### Guitar発行済み

```text
製品名のみ変更可能
```

### 製造開始後またはGuitar発行後に変更禁止の項目

- 製品シリーズ
- 楽器タイプ
- MES内部モデルコード
- ボディタイプ
- ボディ材
- ネックタイプ
- ネック材
- 指板材
- PU構成
- フレット数
- スケール
- 公式モデル番号
- カラー

---

## 8. スケール保存方針

2026-08-27にmm形式からインチ数値のみへ統一した。

### 変更前

```text
648mm
629mm
610mm
```

### 変更後

```text
25.5
24.75
24
```

### 現在の正式方針

```text
画面POST値
→ ScaleLengthTypeのEnum名

DB保存値
→ インチ数値のみ

画面表示
→ 25.5インチ
```

Product.scaleとNeckMaster.scaleは現在String型を維持している。

---

## 9. ProductSeries DBマスタ

### テーブル

```text
m_product_series
```

### 主な項目

```text
id
series_code
series_name
active
```

### 初期登録したシリーズ

```text
MIJ-HER50
MIJ-TR50
MIJ-TR60
MIJ-TR70
MIJ-TR50-ORIGINAL
MIJ-H2
```

### 完成済み機能

- 一覧表示
- 新規登録
- 編集
- 有効化
- 無効化
- コード大文字正規化
- 空白のハイフン変換
- 半角英数字・ハイフン形式検証
- 先頭・末尾・連続ハイフン拒否
- 大文字小文字を無視した重複検証
- DB一意制約
- Product登録画面との接続
- Product編集画面との接続
- 現在使用中の無効シリーズの維持
- 別の無効シリーズへの変更拒否
- ProductSeries Enumの削除

### 編集ルール

```text
seriesCode
→ 登録後変更不可
→ 編集画面では読み取り専用

seriesName
→ 編集可能

active
→ 一覧画面から切替
```

### 無効シリーズのルール

```text
Product新規登録候補
→ 表示しない

別シリーズからの変更先
→ 使用不可

現在使用中のProduct
→ 維持可能

Product編集候補
→ 現在使用中の場合のみ「無効」として表示
```

---

## 10. InstrumentType DBマスタ

### テーブル

```text
m_instrument_type
```

### 主な項目

```text
id
instrument_code
instrument_name
body_type
neck_type
active
```

### 完成済み機能

- 一覧表示
- 新規登録
- 編集
- 有効化
- 無効化
- コード大文字正規化
- 空白のハイフン変換
- 半角英数字・ハイフン形式検証
- 先頭・末尾・連続ハイフン拒否
- 大文字小文字を無視した重複検証
- Product登録画面との接続
- Product編集画面との接続
- 内部モデルコード生成への利用
- BodyType / NeckType自動設定
- 現在使用中の無効タイプの維持
- 別の無効タイプへの変更拒否
- InstrumentType Enumの削除

### 編集ルール

```text
instrumentCode
→ 登録後変更不可

instrumentName
→ 編集可能

bodyType / neckType
→ 未使用タイプなら変更可能
→ Productで使用中なら変更不可

active
→ 一覧画面から切替
```

### 使用中判定

Product.internalModelCodeの末尾が以下に一致するかで判定する。

```text
-ST
-TL
-DUO
```

### 無効楽器タイプのルール

```text
Product新規登録候補
→ 表示しない

別タイプからの変更先
→ 使用不可

現在使用中のProduct
→ 維持可能

Product編集候補
→ 現在使用中の場合のみ「無効」として表示
```

---

## 11. Product画像対応

2026-08-28に完成。

### 初期仕様

- Productごとに代表画像1枚
- 画像未登録を許可
- Product詳細画面から登録
- Product詳細画面から差し替え
- Product詳細画面から削除
- Product一覧へサムネイル表示
- Product詳細へ大きめのプレビュー表示

### 対応形式

```text
JPEG
PNG
WebP
```

### 容量制限

```text
最大5MB
```

### DB保存内容

画像本体はDBへ保存しない。

```text
m_product.image_file_name
```

に生成後のファイル名だけを保存する。

### 実ファイル保存先

```text
./uploads/product-images
```

### 画像参照URL

```text
/product-images/{fileName}
```

### ファイル名規則

利用者がアップロードした元ファイル名は保存に使わない。

```text
product-{Product ID}-{UUID}.{extension}
```

例:

```text
product-49-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.jpg
```

### ProductImageServiceの責務

- Productの存在確認
- 空ファイル拒否
- 5MB超過拒否
- JPEG / PNG / WebP以外を拒否
- UUID付きファイル名生成
- 保存ディレクトリ作成
- 新画像保存
- Product.imageFileName更新
- 差し替え成功後の旧画像削除
- DB保存失敗時の新画像削除
- 画像削除時のDB更新
- パストラバーサル防止

### ProductImageWebConfigの責務

```text
/product-images/**
↓
./uploads/product-images
```

を外部リソースとして公開する。

### application.properties追加内容

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
app.product-image.storage-path=./uploads/product-images
```

### Git管理

アップロード画像はGit管理しない。

`.gitignore`へ追加済みまたは追加対象:

```gitignore
/uploads/
```

---

## 12. Product画像用DB変更

### 適用SQL

```text
260828_05_add_product_image_file_name.sql
```

内容:

```sql
BEGIN;

ALTER TABLE m_product
ADD COLUMN image_file_name VARCHAR(255);

COMMENT ON COLUMN m_product.image_file_name IS
'Product代表画像ファイル名';

COMMIT;
```

### 確認SQL

```text
260828_06_verify_product_image_file_name.sql
```

確認済み結果:

```text
column_name: image_file_name
data_type: character varying
character_maximum_length: 255
is_nullable: YES
```

追加直後の登録件数:

```text
image_registered_count = 0
```

### ロールバックSQL

```text
260828_07_rollback_product_image_file_name.sql
```

内容:

```sql
BEGIN;

ALTER TABLE m_product
DROP COLUMN image_file_name;

COMMIT;
```

---

## 13. DBスキーマ管理方針

Hibernateによる自動更新は使用しない。

```properties
spring.jpa.hibernate.ddl-auto=validate
```

### 今後のDB変更手順

```text
Entityを設計・修正
↓
DDL SQLを明示作成
↓
必要ならデータ移行SQLを作成
↓
DBへSQLを実行
↓
information_schema、pg_indexes、確認SQLで検証
↓
ddl-auto=validateでEntityとDBの整合を確認
↓
アプリケーション起動
↓
自動テスト
↓
画面確認
```

### 必須ルール

- Entity変更だけではDBスキーマは変わらない
- テーブル、カラム、制約、インデックスはSQLで管理する
- データ補正もSQLで管理する
- 適用SQLを作成する
- 確認SQLを作成する
- ロールバックSQLを作成する
- 本番適用時はバックアップを取得する
- 既存の実績系データは原則物理削除しない
- Masterは削除より無効化を優先する

---

## 14. 自動テストの現在地

### ProductServiceTest

```text
26/26成功
```

主な対象:

- 有効DBシリーズでProduct登録
- 存在しないシリーズ拒否
- 無効シリーズ拒否
- 改ざんされた内部モデルコード拒否
- BodyType不一致拒否
- NeckType不一致拒否
- 公式モデル番号重複拒否
- Master再利用
- Master新規生成
- 製品ファミリーを含むMaster検索
- 変更なし更新
- 製品名のみ更新
- ProductionOrder未開始時の仕様変更
- ProductionOrder開始済み時の仕様変更拒否
- Guitar発行済み時の仕様変更拒否
- 現在使用中の無効シリーズ維持
- 別の無効シリーズへの変更拒否
- 存在しないDB楽器タイプ拒否
- 無効DB楽器タイプ拒否
- 現在使用中の無効楽器タイプ維持
- 別の無効楽器タイプへの変更拒否

### InstrumentTypeMasterServiceTest

```text
33/33成功
```

主な対象:

- 一覧取得
- 有効一覧取得
- 正常登録
- コード正規化
- 空白のハイフン変換
- 前後空白除去
- 重複拒否
- 不正文字拒否
- 不正ハイフン位置拒否
- 文字数制限
- 必須入力
- 有効タイプ取得
- 存在しないタイプ拒否
- 無効タイプ拒否
- 編集候補取得
- 現在使用中の無効タイプ追加
- 有効タイプ重複防止
- 現在使用中の無効タイプ維持
- 別の無効タイプ拒否
- 楽器タイプ名更新
- 未使用タイプのBodyType / NeckType更新
- 使用中タイプのBodyType変更拒否
- 使用中タイプのNeckType変更拒否
- 有効状態切替
- 存在しないID拒否

### ProductSeriesMasterServiceTest

```text
20/20成功
```

主な対象:

- 一覧取得
- 有効一覧取得
- 編集候補取得
- 現在使用中の無効シリーズ追加
- 有効シリーズ重複防止
- シリーズ取得
- 存在しないシリーズ拒否
- 無効シリーズの新規利用拒否
- 現在使用中の無効シリーズ維持
- 別の無効シリーズへの変更拒否
- 正常登録
- コード大文字化
- 空白のハイフン変換
- コード重複拒否
- 不正文字拒否
- 不正ハイフン位置拒否
- コード文字数制限
- シリーズ名必須
- シリーズ名文字数制限
- シリーズ名更新
- シリーズコード変更防止
- 有効状態切替
- 存在しないID拒否

### ProductImageServiceTest

```text
10/10成功
```

主な対象:

- JPEG保存
- PNG保存
- WebP保存
- 空ファイル拒否
- 5MB超過拒否
- 未許可形式拒否
- 差し替え後の旧画像削除
- 画像削除
- 存在しないProduct拒否
- Product IDとUUIDを使った安全なファイル名生成

### 総テスト数

```text
ProductServiceTest                   26
InstrumentTypeMasterServiceTest      33
ProductSeriesMasterServiceTest       20
ProductImageServiceTest              10
---------------------------------------
合計                                 89
```

結果:

```text
89/89成功
エラー 0
失敗 0
```

---

## 15. 実画面で確認済みの内容

### ProductSeries

- 一覧表示
- 登録
- 編集
- シリーズコード読み取り専用
- シリーズ名変更
- 無効化
- 有効化
- 無効化後にProduct新規登録候補から除外
- 有効化後に候補へ復帰

### InstrumentType

- 一覧表示
- 登録
- 編集
- 楽器タイプコード読み取り専用
- 楽器タイプ名変更
- 未使用タイプのBodyType / NeckType変更
- 使用中タイプのBodyType / NeckType変更拒否
- 無効化
- 有効化
- Product登録・編集候補への反映

### Product画像

- 画像未登録表示
- JPEG画像登録
- Product詳細画面で画像表示
- Product一覧画面でサムネイル表示
- 画像差し替え
- 画像削除
- 削除後に「画像未登録」へ復帰
- 一覧画面で「未登録」へ復帰
- レイアウト崩れなし

---

## 16. UI設計と共通クラス

白・黒・赤を基調とした業務システム向けUIを採用している。

### 共通レイアウト

```text
page-container
page-toolbar
page-toolbar-description
page-toolbar-actions
table-container
empty-state
```

### 共通フォーム

```text
form-container
form-section
form-section-heading
form-section-caption
form-section-title
form-section-description
form-grid
form-group
form-label
form-input
form-select
form-help
form-actions
```

### 共通テーブル

```text
data-table
```

### 共通ボタン

```text
btn
btn-primary
btn-secondary
btn-outline
btn-detail
btn-process-end
```

### ステータス

```text
status-badge
status-waiting
status-working
status-available
status-returned
status-assembled
status-rejected
status-inspection
status-rework
status-planned
status-unknown
```

### Product画像用CSS

```text
product-image-layout
product-image-preview
product-detail-image
product-image-placeholder
product-image-actions
product-column-image
product-thumbnail-cell
product-thumbnail
product-thumbnail-placeholder
```

### 注意

style.css冒頭には古い汎用`table`、`th`、`td`指定も残っている。

将来のCSS整理では、共通クラスへ寄せて重複を減らす。

---

## 17. 現在の重要ファイル構成

```text
src/main/java/com/example/guitarmes
├─ common
├─ config
│  └─ ProductImageWebConfig.java
├─ controller
│  ├─ api
│  └─ view
│     ├─ InstrumentTypeMasterViewController.java
│     ├─ ProductSeriesMasterViewController.java
│     └─ ProductViewController.java
├─ dto
│  ├─ ProductUpdateRequest.java
│  ├─ ProductVariationCreateRequest.java
│  └─ ProductVariationRequest.java
├─ entity
│  ├─ InstrumentTypeMaster.java
│  ├─ Product.java
│  └─ ProductSeriesMaster.java
├─ exception
├─ master
│  ├─ BodyMaterialType.java
│  ├─ FingerboardMaterialType.java
│  ├─ FretCountType.java
│  ├─ NeckMaterialType.java
│  └─ ScaleLengthType.java
├─ repository
│  ├─ InstrumentTypeMasterRepository.java
│  ├─ ProductRepository.java
│  └─ ProductSeriesMasterRepository.java
└─ service
   ├─ InstrumentTypeMasterService.java
   ├─ InternalModelCodeService.java
   ├─ ProductImageService.java
   ├─ ProductSeriesMasterService.java
   └─ ProductService.java
```

### templates

```text
src/main/resources/templates
├─ instrument-type-edit-form.html
├─ instrument-type-form.html
├─ instrument-type-list.html
├─ product-detail.html
├─ product-edit-form.html
├─ product-form.html
├─ product-list.html
├─ product-series-edit-form.html
├─ product-series-form.html
└─ product-series-list.html
```

### tests

```text
src/test/java/com/example/guitarmes/service
├─ InstrumentTypeMasterServiceTest.java
├─ ProductImageServiceTest.java
├─ ProductSeriesMasterServiceTest.java
└─ ProductServiceTest.java
```

---

## 18. 廃止済み・変更済み設計

### 廃止済み

- ProductSeries Enum
- InstrumentType Enum
- Guitar直接登録フロー
- GuitarCreateRequest
- guitar-form.html
- Guitar直接作成POST API
- 旧Assembly独立登録導線
- DataLoader
- 旧Guitar生成メソッド
- Hibernate `ddl-auto=update`
- 製品ファミリーをまたぐBodyMaster / NeckMaster共有

### 変更済み

```text
製品シリーズ
Enum管理
↓
DBマスタ管理
```

```text
楽器タイプ
Enum管理
↓
DBマスタ管理
```

```text
スケール
648mm
↓
25.5
```

```text
Product画像
未対応
↓
外部ファイル保存 + DBファイル名管理
```

---

## 19. 現在のロードマップ状況

### Phase 1A: 主要UI整備

状態: 主要画面は概ね完了。

残確認候補:

- 小画面表示の最終確認
- 工程履歴画面の統一確認
- 共通CSSの重複整理
- 戻る導線とボタン配置の最終確認

### Phase 1B: CRUDと運用制御

```text
Product編集
✅ 完了

Product画像対応
✅ 完了

ProductSeries DB化
✅ 完了

ProductSeries編集・有効無効
✅ 完了

InstrumentType DB化
✅ 完了

InstrumentType編集・有効無効
✅ 完了

BodyMaster編集
未実装

NeckMaster編集
未実装

ProductionOrder発行前編集
未実装

ProductionOrder取消
未実装
```

### Phase 2: 月間計画・日産計画

候補:

- ProductionOrderを月間・モデル別計画として整理
- ProductionSchedule Entity設計
- 日産計画登録・一覧・詳細
- 月間計画からの日産割当
- 割当済数、未割当数、開始数、完成数、残数表示
- 日産計画確定・取消
- 月間計画超過チェック

### Phase 3: Body / Neck個体一括発行

候補:

- 日産計画数量からBody / Neckを一括生成
- ProductionOrder / ProductionScheduleとの外部キー関連
- 採番ルール整理
- 二重発行防止
- 発行後のProduct変更制限

### Phase 4: 一括工程操作

候補:

- チェックボックス選択
- 全件選択・解除
- 選択件数表示
- 一括工程開始
- 一括工程終了
- 絞り込み
- ページング
- 不正対象が1件でもあれば全件ロールバック

### Phase 5以降

- 工程別ページ・工程内作業
- 差し戻し・再作業
- 品質管理
- ユーザー・認証・認可
- トレーサビリティ
- 分析強化
- パッケージ整理
- CSS整理
- テスト拡充

---

## 20. 次回の最優先候補: BodyMaster編集

### 推奨理由

- BodyMaster一覧・詳細画面は存在する
- Product編集時にはBodyMasterの検索・再利用・新規生成が動いている
- 一方、BodyMasterそのものを管理者が修正する専用機能が未実装
- 次にNeckMaster編集へ同じパターンを展開しやすい

### 最初に確認するファイル

```text
BodyMaster.java
BodyMasterRepository.java
BodyMasterService.java
BodyMasterController.java
body-master-list.html
body-master-detail.html
body-master-form.html
Product.java
ProductService.java
ProductRepository.java
Body.java
BodyRepository.java
```

### 先に決めるべき仕様

- BodyMaster.modelCodeは変更可能か
- BodyMaster.modelNameは変更可能か
- productFamilyCodeは変更可能か
- bodyType、material、colorは変更可能か
- Productから参照中の場合の制限
- Body個体から参照中の場合の制限
- 共有中Masterを直接変更してよい項目
- 同一仕様Masterとの重複時の扱い
- 削除ではなく無効化が必要か
- Entityへactiveを追加するか
- DB変更が必要な場合のSQL

### 推奨する安全な初期方針

```text
modelCode
→ 登録後変更不可

productFamilyCode
→ 登録後変更不可

modelName
→ 参照中でも変更可能候補

bodyType / material / color
→ ProductまたはBodyで使用中なら変更制限を検討
```

ただし、実装前に最新版ファイルを確認して正式決定する。

---

## 21. 次回以降の推奨順序

```text
1. BodyMaster編集仕様の確定
2. BodyMasterServiceTest作成
3. BodyMaster編集画面実装
4. BodyMasterの使用中制限確認
5. 自動テスト・画面確認
6. Gitコミット
7. NeckMaster編集へ展開
8. ProductionOrder発行前編集
9. ProductionOrder取消
10. Phase 2のProductionSchedule設計
```

---

## 22. 作業ルール

- 添付された最新版ファイルを最初に確認する
- 過去チャットだけを根拠にコードを推測しない
- 必要ファイルが不足している場合は無理に進めない
- 依存関係が明確な2～3ファイル程度のまとまりで進める
- 修正コードは差分ではなくファイル全体を優先する
- package、import、DOCTYPE、閉じタグを省略しない
- 省略記号を使わない
- 既存URL、Thymeleaf属性名、業務ロジックを無断で変更しない
- Controllerに業務ロジックを書きすぎない
- 検証、正規化、重複確認、保存はServiceへ置く
- トランザクション境界はServiceへ置く
- CSSは既存共通クラスを優先する
- `ddl-auto=validate`を維持する
- DB変更は必ずSQLを明示する
- データ移行・補正もSQLを明示する
- SQL実行後は確認SQLを実行する
- ロールバックSQLを用意する
- 一つのまとまりごとに自動テスト・起動・画面・DBを確認する
- 実績系データは原則物理削除しない
- Master削除より無効化を優先する
- アップロード画像などの実運用ファイルはGit管理しない

---

## 23. 次回チャット冒頭で伝える内容

```text
2026-08-28の一週間まとめ・超詳細版引き継ぎメモを確認してください。

現在は以下が完了しています。
・Product編集
・ProductSeries DBマスタ化、編集、有効無効
・InstrumentType DBマスタ化、編集、有効無効
・Product画像登録、差し替え、削除、一覧サムネイル
・自動テスト89件

次はBodyMaster編集の仕様整理から開始します。
まず最新版のBodyMaster関連ファイルを確認し、参照中Masterの変更制限を決めてください。
```

---

## 24. 現在の完了条件

```text
Product編集
✅

ProductSeries DBマスタ化
✅

ProductSeries編集・有効無効
✅

ProductSeries Enum削除
✅

InstrumentType DBマスタ化
✅

InstrumentType編集・有効無効
✅

InstrumentType Enum削除
✅

Product画像登録
✅

Product画像差し替え
✅

Product画像削除
✅

Product一覧サムネイル
✅

DDL明示管理
✅

自動テスト89件
✅

実画面確認
✅
```

次の正式なフェーズ:

```text
BodyMaster編集
↓
NeckMaster編集
↓
ProductionOrder編集・取消
↓
月間計画・日産計画
```
