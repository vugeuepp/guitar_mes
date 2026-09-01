## Guitar MES 次回作業プロンプト

Guitar MES開発を継続します。

### 今回の開始地点

2026-08-27時点で、次の作業が完了しています。

#### Product編集

- Product編集専用DTO
- 編集画面の初期値表示
- Product更新
- 自己除外付き重複検証
- BodyMaster / NeckMasterの検索・再利用・新規生成・付け替え
- 共有Masterを直接更新しない処理
- エラー時の入力保持と画面内表示
- Product詳細から編集画面への導線

#### 参照済みProductの変更制限

```text
未参照Product
→ 全項目編集可能

ProductionOrderあり・未開始・Guitarなし
→ 全項目編集可能

ProductionOrder開始済み
→ 製品名のみ変更可能

Guitar発行済み
→ 製品名のみ変更可能
```

#### Masterの製品ファミリー分離

BodyMasterとNeckMasterへ`productFamilyCode`を追加済みです。

正式な共有条件:

```text
BodyMaster:
productFamilyCode
+ bodyType
+ material
+ color

NeckMaster:
productFamilyCode
+ neckType
+ neckMaterial
+ fingerboardMaterial
+ fretCount
+ scale
```

異なる製品ファミリーでは、現在画面に表示している仕様が同一でもMasterを共有しません。

Heritage Product ID 57とTraditional Product ID 1・2で誤共有されていたNeckMasterは分離済みです。

#### スケール

DB保存形式をmm表記からインチ数値へ変更済みです。

```text
例: 25.5
```

画面では「25.5インチ」またはインチとmm換算値を併記します。

#### DBスキーマ管理

Hibernateの自動更新は停止済みです。

```properties
spring.jpa.hibernate.ddl-auto=validate
```

今後、DB変更は必ずSQLで明示します。

```text
Entity設計・修正
↓
DDL SQL作成
↓
必要ならデータ移行SQL作成
↓
SQL実行
↓
確認SQL
↓
ddl-auto=validateで起動確認
```

#### 製品シリーズマスタ

次の機能が完成しています。

```text
ProductSeriesMaster Entity
ProductSeriesMasterRepository
ProductSeriesMasterService
ProductSeriesMasterViewController
製品シリーズ一覧画面
製品シリーズ新規登録画面
重複検証
コード正規化
エラー表示
マスタ管理メニュー導線
```

DBテーブル:

```text
m_product_series
```

登録済みシリーズ:

```text
MIJ-HER50
MIJ-TR50
MIJ-TR60
MIJ-TR70
MIJ-TR50-ORIGINAL
MIJ-H2
```

### 今回の最優先目的

Product登録画面を、Java Enumの`ProductSeries`ではなく、DBの`m_product_series`へ接続してください。

新しく製品シリーズマスタ画面から登録したシリーズを、Product登録画面で選択し、MES内部モデルコードを生成できる状態にします。

### 希望する仕様

#### 製品シリーズ選択

- 有効な`ProductSeriesMaster`だけをProduct登録画面へ表示する
- optionの値は`seriesCode`を使用する
- optionの表示名は`seriesName`を使用する
- `active = false`のシリーズは新規Product登録画面へ表示しない

#### 内部モデルコード生成

```text
seriesCode
+
InstrumentType.code
↓
internalModelCode
```

例:

```text
MIJ-HER70 + ST
↓
MIJ-HER70-ST
```

#### Service側検証

画面で生成した値を信用せず、Service側で次を再検証してください。

- 送信されたシリーズコードがDBに存在する
- シリーズが有効である
- InstrumentTypeが有効なEnum値である
- `seriesCode + '-' + instrumentType.code`で内部モデルコードを再生成する
- 送信された内部モデルコードと一致する
- BodyType / NeckTypeがInstrumentTypeと一致する
- 材質、フレット数、スケール、指板材の既存検証を維持する
- Product重複検証を維持する
- BodyMaster / NeckMasterへ`productFamilyCode = internalModelCode`を設定する

### 段階的な移行方針

- `ProductSeries` Enumは最初から削除しない
- まずProduct新規登録だけをDBシリーズへ接続する
- 動作確認後、Product編集画面もDBシリーズへ接続する
- Enum利用箇所がなくなったことを確認してから廃止可否を判断する
- 新規楽器タイプ・新規モデル登録は、シリーズ接続完了後に別工程で設計する

### 最初に確認するファイル

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
application.properties
```

必要に応じて:

```text
Product.java
BodyMaster.java
NeckMaster.java
BodyMasterRepository.java
NeckMasterRepository.java
ProductRepository.java
ProductSeriesMasterViewController.java
menu.html
GlobalExceptionHandler.java
style.css
```

### 最初に判断してほしい事項

- `ProductVariationCreateRequest.productSeries`をシリーズコード文字列としてそのまま利用できるか
- `ProductViewController.addProductFormOptions()`のシリーズ候補だけDB取得へ置き換えられるか
- `InternalModelCodeService`をDBシリーズ対応へ変更するか、ProductService内で生成するか
- Product登録とProduct編集で共通利用できるシリーズコード検証処理をどこへ置くか
- 無効シリーズを既存Product編集画面でどう表示するか
- `ProductSeries` Enumを残した移行期間の責務をどう整理するか

### 推奨する実装順

#### Step 1

最新版ファイルを確認し、Product登録のEnum依存箇所を整理する。

#### Step 2

Product登録画面へ有効な`ProductSeriesMaster`一覧を渡す。

#### Step 3

`product-form.html`のシリーズselectをDBマスタ対応へ変更する。

#### Step 4

ProductServiceでシリーズコードをDBから再取得し、内部モデルコードを再生成する。

#### Step 5

既存6シリーズで登録テストを行う。

#### Step 6

新規追加したシリーズでProduct登録テストを行う。

#### Step 7

Product編集画面をDBシリーズへ接続する。

#### Step 8

`ProductSeries` Enumの残存利用箇所を確認する。

### 出力方針

- 最初に添付された最新版ファイルを確認する
- 過去チャットだけを根拠にコードを推測しない
- 必要なファイルが不足している場合は無理に進めない
- 不足ファイルは具体的なファイル名で示す
- 2〜3ファイル程度の依存関係が明確なまとまりで進める
- 差分ではなく、修正後ファイル全体を出力する
- そのままコピーして置換できる状態にする
- package宣言、import、DOCTYPE、閉じタグを省略しない
- 省略記号を使用しない
- 既存URL、Thymeleaf属性名、フォーム送信先を無断で変更しない
- 既存業務ロジックを削除しない
- Controllerに業務ロジックを書きすぎない
- 検証、正規化、重複確認、Master再利用、保存はServiceへ置く
- トランザクション境界はServiceへ置く
- CSSは既存共通クラスを優先する
- `ddl-auto=validate`を維持する
- DB変更が必要な場合は、EntityコードだけでなくDDL SQLを必ず提示する
- データ移行が必要な場合はDML SQLと確認SQLを提示する
- 変更不要なファイルは出力しない

### DB変更ルール

今回は既存の`m_product_series`を利用するため、Product登録画面接続だけであれば原則DB変更は不要です。

新しいEntity・カラム・制約が必要になった場合は、次の順で進めてください。

```text
DDL SQL提示
↓
SQL実行
↓
確認SQL
↓
Entity反映
↓
ddl-auto=validateで起動確認
```

### 動作確認の重点

- アプリケーションが`ddl-auto=validate`で正常起動する
- Product登録画面に既存6シリーズが表示される
- 新規追加シリーズが表示される
- 無効シリーズは表示されない
- シリーズ選択で内部モデルコードが生成される
- Product名候補が補填される
- 楽器タイプからBodyType / NeckTypeが設定される
- Service側でDBシリーズを再検証できる
- 存在しないシリーズコードを拒否できる
- 無効シリーズを新規登録に使用できない
- 送信内部モデルコードの改ざんを拒否できる
- 既存Product登録の重複検証が維持される
- BodyMaster / NeckMasterの製品ファミリー分離が維持される
- 既存Product編集機能が壊れていない
- ProductionOrder / Guitar参照制限が壊れていない

### 今回の完了条件

最低ライン:

```text
Product登録画面のシリーズ候補がDB取得になる
既存6シリーズが選択できる
新規追加シリーズが選択できる
内部モデルコードを生成できる
Service側でシリーズ存在・有効状態を再検証できる
```

理想ライン:

```text
Product登録画面のDBシリーズ移行完了
Product編集画面のDBシリーズ移行完了
ProductSeries Enumの廃止可否を判断
新規楽器タイプ・新規モデル登録の設計開始
```
