# Guitar MES 次回作業プロンプト

Guitar MES開発を継続します。

## 今回の開始地点

2026-08-26時点で、主要画面の共通UI整備とProduct一括登録画面のEnum対応が完了しています。

完了済み:

- Product中心のマスタ構造
- Productバリエーション一括登録
- Product登録時のBodyMaster自動生成
- Product登録時のNeckMaster自動生成
- 同一仕様Masterの検索・再利用
- Product.modelNoとProduct.internalModelCodeの分離
- ProductSeries / InstrumentTypeから内部モデルコードを自動生成
- ProductSeries / InstrumentTypeから製品名候補を自動補填
- InstrumentTypeからボディタイプ・ネックタイプを自動設定
- ボディ材・ネック材・指板材・フレット数・スケールのEnum選択化
- 画面送信値のService側再検証
- スケールの既存mm保存形式維持
- 実際のProductデータ登録成功

次のフェーズはPhase 1BのCRUD・運用制御です。

## 今回の作業目的

Product編集機能の設計と実装を開始してください。

ProductはBodyMasterとNeckMasterを共有しているため、Product編集から共有Masterを直接書き換えず、変更後の仕様に一致するMasterを検索し、存在しなければ新規生成してProductの関連を付け替える方式を優先してください。

あわせて、Productへ代表画像を1枚登録できる機能をProduct編集と近いタイミングで追加したいです。

ただし、一度に推測で実装せず、まず添付ファイルから既存構造、参照関係、URL、フォーム再利用可否、画像保存方式の影響範囲を確認してください。

## Product編集の希望仕様

編集候補:

- 製品シリーズ
- 楽器タイプ
- MES内部モデルコード
- 製品名
- ボディタイプ
- ボディ材
- ネックタイプ
- ネック材
- PU構成
- フレット数
- スケール
- 公式モデル番号
- カラー
- 指板材

編集ルール:

- 製品シリーズと楽器タイプから内部モデルコードを再生成する
- 楽器タイプからボディタイプとネックタイプを再設定する
- 製品名候補の自動補填を維持する
- 各仕様のEnum選択を維持する
- Service側でもEnum値と自動生成値を再検証する
- 公式モデル番号の重複検証を維持する
- 内部モデルコード・カラー・指板材の重複検証を維持する
- 自分自身のProductは重複判定から除外する
- 共有中のBodyMasterを直接更新しない
- 共有中のNeckMasterを直接更新しない
- 変更後の仕様に一致するMasterがあれば再利用する
- 一致するMasterがなければ新規生成する
- ProductのMaster関連を安全に付け替える
- ProductionOrderや実績から参照済みの場合の変更制限を検討する
- 更新処理はトランザクション内で行う
- 一部だけ更新される状態を防止する

## Product画像機能の希望仕様

初期機能:

- Productごとに代表画像を1枚
- Product登録または編集画面で画像選択
- Product一覧へサムネイル表示
- Product詳細へ画像表示
- 画像変更
- 画像削除
- 未登録時のプレースホルダー

保存方針:

- DBへ画像バイナリを直接保存せず、ファイル名・相対パス・オブジェクトキーのいずれかを保存する
- 開発環境ではアプリ外部のuploads/productsディレクトリを候補とする
- src/main/resources/static配下へ実行時アップロードしない
- 将来はAzure Blob Storage、Amazon S3、Cloudinary等へ移行可能な構造を意識する

検証:

- 許可する画像形式を限定する
- 最大ファイルサイズを設定する
- 元のファイル名をそのまま保存しない
- UUID等で一意なファイル名を生成する
- パストラバーサルを防止する
- 画像変更時の旧ファイル削除方針を決める
- DB更新失敗時に孤立ファイルが残らないよう検討する

## 最初に確認するファイル

必須:

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
- application.properties
- style.css

必要に応じて:

- InternalModelCodeService.java
- ProductSeries.java
- InstrumentType.java
- BodyMaterialType.java
- NeckMaterialType.java
- FingerboardMaterialType.java
- ScaleLengthType.java
- FretCountType.java
- GlobalExceptionHandler.java

## 最初に判断してほしい事項

1. 現在のproduct-form.htmlを登録・編集で共用できるか
2. product-edit-form.htmlを分ける方が安全か
3. Product編集用DTOを新設すべきか
4. ProductionOrderやGuitar等から参照済みProductの変更制限
5. Product仕様変更時のBodyMaster / NeckMaster再利用・新規生成ルール
6. Product画像の保存先と配信方法
7. 画像アップロードをProduct編集と同時に行うか、編集完成後に分けるか
8. DBカラム追加方法と既存データへの影響

## 進め方

一度にすべて実装せず、次の順で進めてください。

### Step 1

既存ファイルを確認し、Product編集の影響範囲と設計方針を短く提示する。

### Step 2

必要なDTO、Controller、Service、Repository、HTMLの変更対象を確定する。

### Step 3

まず画像なしのProduct編集機能を完成させる。

### Step 4

編集機能の動作確認後、Product画像機能を追加する。

### Step 5

Product一覧とProduct詳細へ画像を表示する。

## 出力方針

- 差分ではなく、修正後のファイル全体を出力する
- そのままコピーして既存ファイルと置き換えられる状態にする
- package宣言、import、DOCTYPE、閉じタグを省略しない
- 省略記号を使用しない
- 既存URL、Thymeleaf属性名、フォーム送信先を無断で変更しない
- 既存業務ロジックを削除しない
- 変更不要なファイルは出力しない
- Controllerに業務ロジックを書きすぎない
- 検証、Master再利用、更新、ファイル保存ルールはServiceへ置く
- トランザクション境界はServiceへ置く
- CSSは既存共通クラスを優先する
- style.css追加が必要な場合のみ追加コードを出力する
- 確認できない内容を推測で実装しない
- 必要なファイルが不足している場合は、具体的なファイル名だけを列挙する
- ファイルが揃っていれば、確認だけで止めず完成版コードまで進める

## 出力順

1. 現状確認
2. 設計方針
3. 影響範囲
4. 変更対象ファイル一覧
5. 各ファイルの完成版
6. 必要な場合のみstyle.css追加コード
7. DB変更または設定変更
8. 変更したポイント
9. 動作確認項目
10. 次の作業

## 動作確認の重点

- アプリケーションが正常起動する
- Product編集画面を開ける
- 既存値がフォームへ初期表示される
- Enumプルダウンが正しく選択済みになる
- シリーズと楽器タイプの変更で内部コード等が更新される
- 製品名候補の自動補填後に手動修正できる
- 公式モデル番号の重複を防止できる
- 自分自身を重複判定から除外できる
- 異なるProductと重複する場合は更新されない
- BodyMaster / NeckMasterの共有データを破壊しない
- 一致するMasterがあれば再利用する
- 一致するMasterがなければ新規生成する
- エラー時に一部だけ更新されない
- 既存のProduct登録機能が引き続き動作する
- 画像未登録Productも正常表示できる
- 画像登録・変更・削除ができる
- 不正な画像形式や過大ファイルを拒否できる
- Product一覧にサムネイルが表示される
- Product詳細に画像が表示される

## 今回の完了条件

最低条件:

- Product編集の設計方針が確定している
- 共有Masterを破壊しない更新ルールが明確である
- 編集画面を表示できる
- 既存Productの値を初期表示できる

理想条件:

- Product編集が完了している
- 重複検証が維持されている
- Masterの再利用・生成・付け替えが動作する
- 画像保存方式が確定している
- Product画像の登録・表示まで実装できている
