# Guitar MES 開発 引き継ぎメモ

- 更新日: 2026-08-26
- 対象: Guitar Manufacturing Execution System
- 技術構成: Java 17 / Spring Boot / Thymeleaf / PostgreSQL
- 現在地: Phase 1Aの主要UI整備とProduct一括登録のEnum対応が完了。次はPhase 1Bの編集・取消・無効化へ進む。

## 1. 本日完了した作業

### 主要画面の共通UI整備

白・黒・赤を基調としたGuitar MES共通UIへ、主要な一覧・詳細・フォーム・工程画面を段階的に統一した。

主な共通クラス:

- page-container
- page-toolbar
- page-toolbar-description
- page-toolbar-actions
- form-container
- form-section
- form-grid
- form-group
- form-label
- form-input
- form-select
- form-help
- form-actions
- table-container
- data-table
- empty-state
- btn
- btn-primary
- btn-secondary
- btn-outline
- status-badge

### Product中心のマスタ構造

- Product登録時のBodyMaster自動生成
- Product登録時のNeckMaster自動生成
- Productバリエーション一括登録
- 同一カラー・同一ボディ仕様のBodyMaster共有
- 同一指板材・同一ネック仕様のNeckMaster共有
- Product.modelNoとProduct.internalModelCodeの役割分離
- ProductとBodyMaster / NeckMasterの外部キー関連
- 既存データの診断・補正
- Product詳細で関連Masterを表示

### Product一括登録画面のEnum対応

追加済みEnum:

- ProductSeries
- InstrumentType
- BodyMaterialType
- NeckMaterialType
- FingerboardMaterialType
- ScaleLengthType
- FretCountType

実装内容:

- 製品シリーズをプルダウン化
- 楽器タイプをプルダウン化
- 製品シリーズと楽器タイプからMES内部モデルコードを自動生成
- 製品シリーズと楽器タイプから製品名候補を自動補填
- 楽器タイプからボディタイプを自動設定
- 楽器タイプからネックタイプを自動設定
- ボディ材をEnumプルダウン化
- ネック材をEnumプルダウン化
- 指板材をEnumプルダウン化
- フレット数をEnumプルダウン化
- スケールをインチ・mm併記のEnumプルダウン化
- 動的バリエーション追加時も指板材プルダウンを維持
- バリエーション削除後の添字再設定を維持
- 登録内容プレビューを維持
- 入力内のモデル番号重複検証を維持
- 入力内のカラー・指板材重複検証を維持

### Service側の再検証

画面上の自動生成値を信用せず、ProductService側でも次を検証する構成にした。

- ProductSeriesとInstrumentTypeをEnumへ変換
- OTHERでは内部モデルコードを生成しない
- InternalModelCodeServiceで内部モデルコードを再生成
- 送信された内部モデルコードとの一致確認
- 楽器タイプとボディタイプの一致確認
- 楽器タイプとネックタイプの一致確認
- ボディ材、ネック材、指板材がEnum候補内か確認
- フレット数がFretCountType候補内か確認
- スケールがScaleLengthType候補内か確認
- スケールは既存DB互換のmm形式で保存
- トランザクション内の一括保存を維持
- BodyMaster / NeckMasterの検索・再利用・自動生成を維持

### 動作確認

Product一括登録画面から実際のデータ登録に成功した。

確認済みの主な動作:

- シリーズと楽器タイプの選択
- 内部モデルコードの自動生成
- 製品名候補の自動補填
- ボディタイプとネックタイプの自動設定
- 各Enum候補の選択
- バリエーション入力
- Product / BodyMaster / NeckMaster登録

## 2. 現在の正式なProduct登録フロー

```text
製品シリーズ選択
+
楽器タイプ選択
↓
MES内部モデルコード自動生成
↓
製品名候補自動補填
↓
ボディタイプ・ネックタイプ自動設定
↓
材質・フレット数・スケール選択
↓
販売バリエーション入力
  ・公式モデル番号
  ・カラー
  ・指板材
↓
Service側再検証
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

## 3. 現在のコード体系

```text
Product.modelNo
・販売バリエーション単位の公式モデル番号

Product.internalModelCode
・MES内部で製品ファミリーを識別するコード
・ProductSeries + InstrumentTypeから生成

BodyMaster.modelCode
・BodyMasterの表示・検索用内部コード

NeckMaster.modelCode
・NeckMasterの表示・検索用内部コード
```

Entity間の関連は文字列コードではなく、DB上のIDと外部キーを使用する。

## 4. スケール保存方針

Product.scaleとNeckMaster.scaleは現在String型で、既存値は648mmなどのmm形式である。

現在の方針:

- 画面: 25.5インチ（約648mm）のように表示
- POST値: ScaleLengthTypeのEnum名
- Service: Enumを検証し、648mmなどへ変換
- DB: 従来どおりmm形式で保存

現段階ではDB保存形式をインチへ変更しない。

## 5. 現在のフォルダ構成上の重要箇所

現在はレイヤー単位の構成を維持している。

```text
com.example.guitarmes
├─ common
├─ controller
│  ├─ api
│  └─ view
├─ dto
├─ entity
├─ exception
├─ master
├─ repository
└─ service
```

Enumは次に配置済み。

```text
src/main/java/com/example/guitarmes/master
├─ BodyMaterialType.java
├─ FingerboardMaterialType.java
├─ FretCountType.java
├─ InstrumentType.java
├─ NeckMaterialType.java
├─ ProductSeries.java
└─ ScaleLengthType.java
```

Product登録関連の主要ファイル:

```text
src/main/java/com/example/guitarmes/dto/ProductVariationCreateRequest.java
src/main/java/com/example/guitarmes/dto/ProductVariationRequest.java
src/main/java/com/example/guitarmes/service/InternalModelCodeService.java
src/main/java/com/example/guitarmes/service/ProductService.java
src/main/java/com/example/guitarmes/controller/view/ProductViewController.java
src/main/resources/templates/product-form.html
src/main/resources/templates/product-list.html
src/main/resources/templates/product-detail.html
src/main/resources/static/css/style.css
```

現在のプロジェクトは26ディレクトリ、135ファイル。

## 6. ロードマップの現在地

### Phase 1A: 主要UI整備

状態: ほぼ完了

残確認:

- 未整備画面の最終確認
- 小画面表示確認
- 工程履歴画面の確認
- 共通CSSの重複確認
- 戻る導線とボタン配置の確認

### Phase 1B: CRUDと運用制御

次の本命フェーズ。

推奨順:

1. Product編集
2. Product画像対応
3. BodyMaster編集
4. NeckMaster編集
5. マスタ削除または無効化
6. ProductionOrderの発行前編集
7. ProductionOrder取消

### Phase 2: 月間計画・日産計画

- ProductionOrderを月間・モデル別計画として整理
- ProductionSchedule Entity設計
- 日産計画登録・一覧・詳細
- 月間計画からの日産割当
- 割当済数、未割当数、開始数、完成数、残数の表示
- 日産計画確定・取消
- 月間計画超過チェック

### Phase 3: Body / Neck個体一括発行

- 日産計画の数量からBody / Neckを一括生成
- ProductionOrder / ProductionScheduleとの外部キー関連
- 採番ルール整理
- 二重発行防止
- 発行後のProduct変更制限

### Phase 4: 一括工程操作

- チェックボックス選択
- 全件選択・解除
- 選択件数表示
- 一括工程開始・終了
- 絞り込み・ページング
- 不正対象が1件でもあれば全件ロールバック

### Phase 5以降

- 工程別ページ・工程内作業
- 差し戻し・再作業・品質管理
- ユーザー・認証・認可
- トレーサビリティ・分析
- パッケージ・CSS・テスト整理

## 7. Product編集で注意する設計

ProductはBodyMasterとNeckMasterを共有するため、Product編集時に共有Masterを直接更新しない。

推奨ルール:

- Product固有項目だけの変更はProductを更新
- ボディ仕様変更時は一致するBodyMasterを検索し、なければ新規生成して関連を付け替える
- ネック仕様変更時は一致するNeckMasterを検索し、なければ新規生成して関連を付け替える
- 共有中のBodyMaster / NeckMasterそのものをProduct編集処理から直接書き換えない
- 公式モデル番号の重複検証を維持
- 内部モデルコード・カラー・指板材の重複検証を維持
- 参照実績があるProductの重要仕様変更は制限を検討する

## 8. Product画像機能の追加方針

Product編集と合わせてProduct画像を追加する方針。

初期対象:

- Productごとに代表画像を1枚
- Product登録または編集画面で画像選択
- Product一覧へサムネイル表示
- Product詳細へ大きめの画像表示
- 画像変更
- 画像削除
- 画像未登録時のプレースホルダー

Entity候補:

```java
private String imagePath;
```

ただし、実装前に保存方式を決める。

### 開発環境の候補

```text
アプリ外部ディレクトリ
uploads/products/
```

`src/main/resources/static`配下へ実行時アップロードしない。ビルドや再デプロイで消える可能性があるため。

### 将来の公開環境

- Azure Blob Storage
- Amazon S3
- Cloudinaryなど

DBには画像本体ではなく、原則としてファイル名、相対パス、またはオブジェクトキーを保存する。

### セキュリティ・検証

- JPEG / PNG / WebPなど許可形式を限定
- Content-Typeだけでなく拡張子も検証
- 最大ファイルサイズを設定
- 元ファイル名をそのまま保存しない
- UUIDなどで一意なファイル名を生成
- パストラバーサルを防止
- 画像変更時の旧ファイル削除方針を決める
- DB保存失敗時に孤立ファイルを残さない方針を検討する

## 9. 明日の推奨開始地点

最初にProduct編集の仕様と影響範囲を確定する。

確認対象:

- Product.java
- ProductRepository.java
- ProductService.java
- ProductViewController.java
- ProductVariationCreateRequest.java
- ProductVariationRequest.java
- product-form.html
- product-list.html
- product-detail.html
- BodyMaster.java
- NeckMaster.java
- BodyMasterRepository.java
- NeckMasterRepository.java
- ProductionOrder.java
- ProductionOrderRepository.java
- style.css

最初の作業候補:

1. Productの編集可能項目を分類
2. Product登録フォームを編集でも再利用するか、新しいproduct-edit-form.htmlを作るか判断
3. 共有Masterの付け替えルールをServiceへ実装
4. Product画像の保存方式を決定
5. 画像なしでProduct編集を先に完成
6. Product画像アップロードを追加
7. 一覧・詳細へ画像表示を追加

## 10. 作業ルール

- 最初に添付された最新版ファイルを確認する
- 過去チャットだけを根拠にコードを推測しない
- 差分より修正後ファイル全体を優先する
- コピーして置換できる完成版を出力する
- 既存URL、Thymeleaf属性名、業務ロジックを維持する
- 変更不要なファイルは出力しない
- 一つずつ動作確認しながら進める
- CSSは既存クラスを優先し、重複を作らない
- 実績系データは原則物理削除しない
- マスタは参照状況に応じて無効化を優先する
- Controllerに業務ロジックを書かず、Serviceへ置く
- トランザクション境界をServiceへ置く
- 長いコードはファイル全体で提示する

## 11. 明日の終了条件候補

最低ライン:

- Product編集の仕様が確定
- 編集画面を表示できる
- 既存Productの値がフォームへ初期表示される
- 共有Masterを直接破壊しない更新方針がコードへ反映される

理想ライン:

- Product編集が完了
- 重複検証が動く
- Product画像アップロードの保存方式が確定
- Product画像の登録または編集まで着手
