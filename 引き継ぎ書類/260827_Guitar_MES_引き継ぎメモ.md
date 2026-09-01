## Guitar MES 開発 引き継ぎメモ

- 更新日: 2026-08-27
- 対象: Guitar Manufacturing Execution System
- 技術構成: Java 17 / Spring Boot / Thymeleaf / PostgreSQL
- 現在地: Phase 1BのProduct編集と参照制限が完了。製品ファミリー単位のMaster分離と製品シリーズDBマスタ化の基盤・管理画面まで完了。次回はProduct登録画面をDBの製品シリーズマスタへ接続する。

### 1. 本日完了した作業

#### 1-1. スケール保存形式をインチへ統一

従来のmm形式から、インチ数値のみを保存する方式へ変更した。

変更前:

```text
648mm
629mm
610mm
```

変更後:

```text
25.5
24.75
24
```

実施内容:

- `ProductService.toScaleStorageValue()`を`ScaleLengthType.getValue()`へ変更
- `m_product.scale`をインチ値へ移行
- `m_neck.scale`をインチ値へ移行
- `neck-master-list.html`で「25.5インチ」と表示
- `neck-master-detail.html`で「25.5インチ」と表示
- `product-detail.html`で「25.5インチ」と表示

#### 1-2. Product編集機能を完成

追加・変更した主なファイル:

```text
ProductUpdateRequest.java
ProductRepository.java
BodyMasterRepository.java
NeckMasterRepository.java
ProductService.java
ProductViewController.java
product-edit-form.html
product-detail.html
GlobalExceptionHandler.java
style.css
```

実装内容:

- Product編集専用DTOを新設
- `GET /products/{id}/edit`を追加
- `POST /products/{id}/edit`を追加
- 既存Productから編集DTOへの初期値変換
- `internalModelCode`からProductSeriesとInstrumentTypeを復元
- DBラベル値から各Enum名を復元
- インチ値からScaleLengthTypeを復元
- Product固有項目の更新
- 自分自身を除外した公式モデル番号重複検証
- 自分自身を除外した「内部モデルコード + カラー + 指板材」重複検証
- BodyMasterの検索・再利用・新規生成・付け替え
- NeckMasterの検索・再利用・新規生成・付け替え
- 共有中のBodyMaster / NeckMasterを直接更新しない
- 更新処理をServiceのトランザクション内で実行
- Product詳細画面へ編集ボタンを追加

#### 1-3. Product編集のエラー表示を改善

従来はView Controllerの`BusinessException`も`GlobalExceptionHandler`でJSONへ変換されていた。

改善内容:

- `GlobalExceptionHandler`をAPI Controller向けに限定
- Product編集の`BusinessException`は`ProductViewController`で処理
- 入力内容を保持したまま`product-edit-form.html`を再表示
- フォーム上部へエラーメッセージを表示
- 共通エラー表示CSSを追加

#### 1-4. Product編集の動作確認

確認済み:

- 変更なし保存
- PU構成などProduct固有項目の変更
- BodyMaster新規生成
- BodyMaster既存再利用
- NeckMaster新規生成
- NeckMaster既存再利用
- 元のMasterを直接変更しない
- 公式モデル番号重複の拒否
- 仕様組み合わせ重複の拒否
- エラー時の入力保持
- 詳細画面から編集画面への導線

#### 1-5. ProductionOrder / Guitar参照済みProductの変更制限

Repositoryへ追加:

```text
GuitarRepository.existsByProductId(...)
ProductionOrderRepository.findByProductId(...)
```

採用した制限ルール:

```text
未参照Product
→ 全項目編集可能

ProductionOrderあり・未開始
startedQuantity = 0
completedQuantity = 0
Guitarなし
→ 全項目編集可能

ProductionOrder開始済み
startedQuantity > 0
または completedQuantity > 0
→ 製品名のみ変更可能

Guitar発行済み
→ 製品名のみ変更可能
```

製造開始後またはGuitar発行後に変更禁止とした項目:

```text
製品シリーズ
楽器タイプ
MES内部モデルコード
ボディタイプ
ボディ材
ネックタイプ
ネック材
指板材
PU構成
フレット数
スケール
公式モデル番号
カラー
```

確認済み:

- Guitar発行済みProductの仕様変更拒否
- Guitar発行済みProductの製品名変更許可
- ProductionOrder未開始・Guitarなしの仕様変更許可
- ProductionOrder開始済み・Guitarなしの仕様変更拒否
- 変更なし保存を許可
- 制限エラー時に不要なMasterを生成しない

#### 1-6. BodyMaster / NeckMasterを製品ファミリー単位で分離

問題:

```text
HeritageとTraditionalは、現在管理中の材質・指板材・フレット数・スケール等が同一でも、
付属パーツ、塗装、加工など未管理仕様が異なる。
```

従来は現在管理している物理仕様だけでMasterを共有していたため、Heritage ProductへTraditional由来のNeckMasterが関連付いていた。

採用した正式ルール:

```text
製品ファミリーコードが一致
+
現在管理している部品仕様が一致
↓
同じMasterを再利用
```

BodyMasterの共有条件:

```text
productFamilyCode
+ bodyType
+ material
+ color
```

NeckMasterの共有条件:

```text
productFamilyCode
+ neckType
+ neckMaterial
+ fingerboardMaterial
+ fretCount
+ scale
```

実施内容:

- `BodyMaster.productFamilyCode`を追加
- `NeckMaster.productFamilyCode`を追加
- `m_body.product_family_code`を追加
- `m_neck.product_family_code`を追加
- Repositoryへ製品ファミリーを含む検索メソッドを追加
- Product登録・編集時にMasterへ`internalModelCode`を製品ファミリーコードとして保存
- 既存Masterの全件診断
- ファミリー横断共有の診断
- Heritage Product ID 57をHeritage専用NeckMaster ID 20へ付け替え
- Traditional Product ID 1・2はNeckMaster ID 1を維持
- 参照中Masterへ`product_family_code`を設定
- 製品ファミリーをまたぐMaster共有が0件であることを確認
- Productと関連Masterのファミリーコード不一致が0件であることを確認

未参照Masterは物理削除していない。

#### 1-7. HibernateのDB自動更新を停止

正式方針:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

従来の`update`を停止した。

今後のDB変更手順:

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
アプリケーション起動・動作確認
```

注意事項:

- Entity変更だけではDBスキーマは変わらない
- テーブル、カラム、制約、インデックスはSQLで管理する
- データ補正もSQLで管理する
- 本番適用時はバックアップとロールバック手順を用意する

今回のスキーマ監査結果:

- `m_product`から`m_body`・`m_neck`への外部キー正常
- MasterとProductのファミリーコード整合性正常
- ファミリー横断共有0件
- `product_family_code`をEntity・DBとも`VARCHAR(50)`へ統一
- `ddl-auto=validate`で正常起動

#### 1-8. 製品シリーズをDBマスタ化

新規追加:

```text
ProductSeriesMaster.java
ProductSeriesMasterRepository.java
ProductSeriesMasterService.java
ProductSeriesMasterViewController.java
product-series-list.html
product-series-form.html
```

DBテーブル:

```text
m_product_series
```

主な項目:

```text
id
series_code
series_name
active
```

既存の6シリーズを初期登録:

```text
MIJ-HER50
MIJ-TR50
MIJ-TR60
MIJ-TR70
MIJ-TR50-ORIGINAL
MIJ-H2
```

実装内容:

- シリーズ一覧表示
- 新規シリーズ登録
- シリーズコードの大文字正規化
- 空白をハイフンへ正規化
- 半角英数字・ハイフン形式の検証
- 先頭・末尾ハイフン、連続ハイフンを拒否
- シリーズコードの大小文字無視重複検証
- DBの一意制約
- `UPPER(series_code)`の一意インデックス
- 新規登録時は`active = true`
- 入力エラー時の画面内表示
- 入力内容保持
- 内部モデルコード生成例のプレビュー
- マスタ管理メニューへ「製品シリーズマスタ一覧」を追加

### 2. 現在の正式なProduct / Master識別ルール

#### Product

```text
Product.modelNo
→ 販売バリエーション単位の公式モデル番号

Product.internalModelCode
→ 製品ファミリーを識別するMES内部コード
```

例:

```text
MIJ-HER50-ST
MIJ-TR50-ST
MIJ-H2-TL
```

#### BodyMaster / NeckMaster

```text
productFamilyCode = Product.internalModelCode
```

異なる製品ファミリーでは、現在表示される仕様が同じでもMasterを共有しない。

同一製品ファミリー内では、現在管理している仕様も同じ場合に限り共有する。

### 3. 現在のスケール保存方針

```text
画面POST値:
ScaleLengthTypeのEnum名

DB保存値:
インチ数値のみ
例 25.5

画面表示:
25.5インチ
または
レギュラースケール / 25.5インチ（約648mm）
```

### 4. DBスキーマ管理方針

```text
Hibernate ddl-auto=updateは使用しない
Hibernate ddl-auto=validateを使用する
```

今後、Entity追加や変更を行う場合は、対応するSQLを必ず先に用意する。

### 5. 現在の製品シリーズマスタ機能

完成済み:

```text
一覧
新規登録
コード正規化
重複検証
エラー表示
共通メニュー導線
```

未実装:

```text
編集
有効・無効化
Product登録画面との接続
ProductSeries Enumの廃止
新規楽器タイプのDBマスタ化
```

### 6. 次回の最優先作業

## Product登録画面をDBの製品シリーズマスタへ接続する

現在のProduct登録画面は、まだJava Enumの`ProductSeries.values()`を使用している。

次回は、Product登録画面の製品シリーズ選択肢を`m_product_series`から取得し、新しく登録したシリーズをProduct登録で選べるようにする。

主な検討対象:

```text
ProductVariationCreateRequest.java
ProductViewController.java
ProductService.java
InternalModelCodeService.java
product-form.html
product-edit-form.html
ProductSeriesMasterService.java
ProductSeriesMasterRepository.java
```

想定方針:

- Product登録画面のシリーズ選択値は`seriesCode`を使用
- 表示名は`seriesName`
- 有効なシリーズだけを選択肢に表示
- ProductServiceでDB上のシリーズコードを再取得・検証
- 内部モデルコードは`seriesCode + '-' + instrumentType.code`で生成
- 画面上の自動生成値を信用せずServiceで再生成
- 新規シリーズでも既存のInstrumentTypeと組み合わせてProduct登録可能にする
- `ProductSeries` Enumはすぐ削除せず、移行完了まで残す
- Product編集画面への接続は、登録画面の動作確認後に行う

### 7. 次回の推奨実装順

#### Step 1

最新版ファイルを確認し、Enum依存箇所を洗い出す。

#### Step 2

Product登録画面だけをDBシリーズマスタへ接続する。

#### Step 3

既存6シリーズと新規追加シリーズで内部モデルコードが生成できることを確認する。

#### Step 4

ProductServiceのDBシリーズ検証と重複検証を確認する。

#### Step 5

Product編集画面もDBシリーズマスタへ接続する。

#### Step 6

`ProductSeries` Enumの利用箇所がなくなったことを確認してから、廃止可否を判断する。

#### Step 7

新規楽器タイプ・新規モデル登録をDBマスタ化する設計へ進む。

### 8. 次回に確認するファイル

必須:

```text
ProductVariationCreateRequest.java
ProductUpdateRequest.java
ProductViewController.java
ProductService.java
InternalModelCodeService.java
ProductSeriesMaster.java
ProductSeriesMasterRepository.java
ProductSeriesMasterService.java
ProductSeries.java
InstrumentType.java
product-form.html
product-edit-form.html
menu.html
application.properties
```

必要に応じて:

```text
Product.java
BodyMaster.java
NeckMaster.java
BodyMasterRepository.java
NeckMasterRepository.java
GlobalExceptionHandler.java
style.css
```

### 9. 現在のフォルダ構成上の重要追加

```text
controller/view/ProductSeriesMasterViewController.java
entity/ProductSeriesMaster.java
repository/ProductSeriesMasterRepository.java
service/ProductSeriesMasterService.java
templates/product-series-list.html
templates/product-series-form.html
```

現在のプロジェクト構成は26ディレクトリ、143ファイル。

### 10. 作業ルール

- 添付された最新版ファイルを最初に確認する
- 過去チャットだけを根拠にコードを推測しない
- 必要なファイルが不足している場合は無理に進めない
- 2〜3ファイル程度の依存関係が明確なまとまりで進める
- 修正コードは差分ではなくファイル全体を提示する
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
- SQL実行後は確認SQLを提示する
- 一つのまとまりごとに起動・画面・DBを確認する
- 実績系データは原則物理削除しない
- Masterの削除より無効化を優先する

### 11. 次回の完了条件候補

最低ライン:

```text
Product登録画面のシリーズ候補がDB取得になる
既存6シリーズが選択できる
新規追加シリーズが選択できる
内部モデルコードを生成できる
Service側でDBシリーズを再検証できる
```

理想ライン:

```text
Product登録画面のDBシリーズ移行完了
Product編集画面のDBシリーズ移行完了
ProductSeries Enumの廃止可否を判断
新規楽器タイプ・新規モデル登録の設計開始
```

### 12. 後続ロードマップ

シリーズDB接続後の候補:

```text
新規楽器タイプ・新規モデルのDBマスタ化
ProductSeries編集・有効無効化
Product画像対応
BodyMaster編集
NeckMaster編集
ProductionOrder編集・取消
```
