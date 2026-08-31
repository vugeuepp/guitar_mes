# Guitar MES 次回作業プロンプト

```text
Guitar MES開発を継続します。

【今回の作業目的】
Product一括登録画面を、追加済みのEnumとInternalModelCodeServiceへ接続する。
現在はMES内部モデルコード、ボディタイプ、ネックタイプ、各材質、フレット数、スケールを手入力しているため、入力ミスや表記揺れが起きる可能性がある。

以下を実現してください。

・製品シリーズをプルダウン選択にする
・楽器タイプをプルダウン選択にする
・製品シリーズと楽器タイプからMES内部モデルコードを自動生成する
・楽器タイプからボディタイプとネックタイプを自動設定する
・ボディ材、ネック材、指板材を既存Enumから選択する
・フレット数を既存Enumから選択する
・スケールを既存Enumからインチ表記で選択する
・画面だけでなくService側でも送信値と内部モデルコードを再検証する
・既存のProductバリエーション一括登録とMaster自動生成機能を維持する

今回は、既存コードを確認してから以下の4ファイルをまとめて改修してください。

・ProductVariationCreateRequest.java
・ProductService.java
・ProductViewController.java
・product-form.html

必要な場合のみstyle.cssへ追加するコードを提示してください。

【現在までに完了している作業】
・Guitar MES主要画面の共通UI化
・Product中心のマスタ構造へ移行
・Product登録時のBodyMaster自動生成
・Product登録時のNeckMaster自動生成
・複数Productバリエーションの一括登録
・同一カラーのBodyMaster共有
・同一指板材・同一ネック仕様のNeckMaster共有
・公式モデル番号とMES内部モデルコードの分離
・Product.internalModelCode追加
・既存Productへのinternal_model_code移行
・Product詳細へのBodyMaster情報表示
・Product詳細へのNeckMaster情報表示
・マスタ管理メニューをProduct中心の登録導線へ整理
・既存データの診断と補正
・Forest BlueのNeckMaster関連付け修正
・Traditional 60s Stratocasterの指板材をRosewoodへ修正
・NeckMasterのスケール表記を648mmへ統一
・公式モデル番号の重複なしを確認
・MES内部モデル・カラー・指板材の重複なしを確認
・BodyMaster未関連Productなしを確認
・NeckMaster未関連Productなしを確認
・ProductとBodyMasterの仕様不一致なしを確認
・ProductとNeckMasterの仕様不一致なしを確認

【現在のコード体系】
Product.modelNo
・Fender公式サイトなどで使用される販売バリエーション単位の公式モデル番号
・例：5660100300

Product.internalModelCode
・MES内部で製品ファミリーを識別するコード
・例：MIJ-H2-TL

BodyMaster.modelCode
・BodyMasterの表示・検索用内部コード
・新規自動生成例：BM-TEST-TL-0001

NeckMaster.modelCode
・NeckMasterの表示・検索用内部コード
・新規自動生成例：NM-TEST-TL-0001

Entity間の関連は文字列コードではなく、DB上のIDと外部キーを使用する。

【現在のProduct一括登録構造】
共通仕様
・MES内部モデルコード
・製品名
・ボディタイプ
・ボディ材
・ネックタイプ
・ネック材
・PU構成
・フレット数
・スケール

バリエーション行
・公式モデル番号
・カラー
・指板材

登録処理
共通仕様とバリエーション行を受信
↓
入力検証
↓
公式モデル番号の重複確認
↓
MES内部モデル・カラー・指板材の重複確認
↓
BodyMasterを仕様単位で検索または生成
↓
NeckMasterを仕様単位で検索または生成
↓
ProductへBodyMasterとNeckMasterを関連付け
↓
トランザクション内で一括保存

【Master共有ルール】
BodyMasterの共有単位
・internalModelCode
・bodyType
・bodyMaterial
・color

NeckMasterの共有単位
・internalModelCode
・neckType
・neckMaterial
・fingerboardMaterial
・fretCount
・scale

Productの識別
・公式モデル番号 modelNo

Productの補助的な重複確認
・internalModelCode
・color
・fingerboardMaterial

【追加済みEnum】
以下のEnumはすでに実装済みです。
実際のpackage、定数名、getter名は添付された最新版コードを確認し、推測で変更しないでください。

・ProductSeries
・InstrumentType
・BodyMaterialType
・NeckMaterialType
・FingerboardMaterialType
・ScaleLengthType
・FretCountType

想定する責務：

ProductSeries
・製品シリーズの表示名
・MES内部コード用のシリーズコード

InstrumentType
・楽器タイプの表示名
・MES内部コード用のタイプコード
・ボディタイプ
・ネックタイプ

BodyMaterialType
・ボディ材の選択候補

NeckMaterialType
・ネック材の選択候補

FingerboardMaterialType
・指板材の選択候補

ScaleLengthType
・スケール名称
・インチ値
・ミリ換算表示
・画面表示名

FretCountType
・フレット数
・画面表示名

【追加済みService】
InternalModelCodeServiceは実装済みです。
実際のメソッド名と引数は添付コードを確認してください。

想定する責務：
・ProductSeriesとInstrumentTypeからMES内部モデルコードを生成
・製品名からProductSeries候補を判定
・製品名からInstrumentType候補を判定
・製品名からボディタイプ候補を判定
・製品名からネックタイプ候補を判定
・判定できない場合は誤ったコードを生成しない

【今回の入力・自動補完方針】
MES内部モデルコード
・原則としてProductSeriesとInstrumentTypeから自動生成する
・毎回の自由入力は避ける
・画面上では生成結果を確認できるようにする
・手動変更を許可する場合も、Service側で生成値との不一致を検証する
・既存内部コードと異なる製品名が衝突する場合はエラーにする

ボディタイプ
・InstrumentTypeから自動設定する
・必要に応じてプルダウンで修正可能にする
・完全な自由入力にはしない

ネックタイプ
・InstrumentTypeから自動設定する
・必要に応じてプルダウンで修正可能にする
・完全な自由入力にはしない

ボディ材
・BodyMaterialTypeから選択する

ネック材
・NeckMaterialTypeから選択する

指板材
・バリエーション行ごとにFingerboardMaterialTypeから選択する

フレット数
・FretCountTypeから選択する
・既存データには20、21、22フレットが存在する
・19、20、21、22、24など、Enumに定義済みの候補を使用する

スケール
・ScaleLengthTypeから選択する
・画面はインチを主表示とし、ミリ換算値を併記する
・例：25.5インチ（約648mm）
・現在の既存DB値は648mm形式である
・今回の画面接続時に保存値をインチへ変更する場合は、既存ProductとNeckMasterの移行が必要になるため、勝手に変更しない
・既存のString scaleを維持する場合は、保存形式と既存値との互換性を明示する

【スケールに関する重要事項】
現在のProduct.scaleとNeckMaster.scaleはString型で、既存データは648mmへ統一済みです。
追加済みScaleLengthTypeはインチ値とミリ換算を持っています。

今回、次のどちらを採用するかは既存コードと影響範囲を確認して決めてください。

案A：保存値は従来どおりmm形式を維持
・画面では25.5インチ（約648mm）と表示
・POST時は648mmを送信
・既存DB移行が不要

案B：保存値をインチ形式へ変更
・DBには25.5などを保存
・既存ProductとNeckMasterを同時移行
・将来的にはBigDecimal化を検討

今回の推奨は案Aです。
理由は、画面改善とDB移行を同時に行わず、既存データとMaster再利用検索の互換性を維持できるためです。

【製品名からの自動判定について】
製品名だけの文字列解析を唯一の判定根拠にしないでください。
基本はProductSeriesとInstrumentTypeの選択値を使用してください。

製品名入力後に候補を補完する機能を付ける場合：
・候補として設定する
・ユーザーが最終確認できる
・判定不能の場合は無理に補完しない
・Telecaster CustomをTelecasterより先に判定する
・Precision BassとJazz Bassを単なるBassより先に判定する
・アポストロフィ、スマートクォート、登録商標記号、連続空白を考慮する

【将来構想として保持する内容】
将来的にAI APIを利用し、以下からProduct登録候補を生成する案がある。

・製品名
・公式モデル番号
・Fender公式URL

AIの候補対象：
・製品シリーズ
・楽器タイプ
・MES内部モデルコード
・ボディタイプ
・ネックタイプ
・ボディ材
・ネック材
・指板材
・フレット数
・スケール

ただし、今回の実装対象ではない。
今回の段階ではルールベースとEnumによる安定した入力補助を完成させる。
AIは将来の入力支援機能として扱い、AI出力を未確認のまま保存しない。

【今回変更するファイル】
・ProductVariationCreateRequest.java
・ProductService.java
・ProductViewController.java
・product-form.html

追加CSSが必要な場合のみ、style.cssへ追加するコードを別ブロックで出力してください。
既存のProductVariationRequest.javaは、指板材の型変更が必要な場合のみ変更してください。
変更する場合はファイル全体を出力してください。

【ファイル確認ルール】
・最初に添付された最新版ファイルを確認してください
・以前のチャット内容だけを根拠にコードを作らないでください
・追加済みEnumとInternalModelCodeServiceの実際のpackage、定数、getter、メソッドを確認してください
・必要なファイルが不足している場合は、具体的なファイル名だけを列挙してください
・確認できない内容を推測して実装しないでください

【出力方針】
・差分ではなく、修正後のファイル全体を出力してください
・そのままコピーして既存ファイルと置き換えられる状態にしてください
・既存機能、URL、Thymeleaf条件、フォーム送信先を維持してください
・省略記号を使用せず、必要なコードをすべて記載してください
・package宣言、import、DOCTYPE、閉じタグなどを省略しないでください
・コンパイルまたは画面表示できる状態を前提にしてください
・変更箇所の解説は必要最小限にしてください
・解説よりも完成版コードの出力を優先してください
・複数ファイルの変更が必要な場合は、変更対象ごとに完成版を出力してください
・変更不要なファイルは出力しないでください
・前段階の確認だけで回答を止めず、必要なファイルが揃っていれば完成版コードまで進めてください

【既存機能の維持】
・現在動作しているProductバリエーション一括登録を削除しないでください
・Product登録時のBodyMaster自動生成を維持してください
・Product登録時のNeckMaster自動生成を維持してください
・同一仕様Masterの再利用を維持してください
・公式モデル番号の重複検証を維持してください
・内部モデル・カラー・指板材の重複検証を維持してください
・入力内の同一公式モデル番号重複検証を維持してください
・入力内の同一カラー・指板材重複検証を維持してください
・ControllerのURLを無断で変更しないでください
・Product登録画面URL /products/new を維持してください
・Product登録先 /products/create を維持してください
・Product一覧URL /products/view を維持してください
・Thymeleafのmodel属性名を無断で変更しないでください
・既存Entity、DTO、Serviceのフィールドやメソッドを無断で削除しないでください
・仕様変更が必要な場合は、コード出力前に理由と影響範囲を短く明示してください

【Service設計方針】
・一括登録処理はProductServiceでトランザクション管理してください
・Controllerへ業務ロジックや重複判定を書かないでください
・画面で自動生成したMES内部モデルコードを信用せず、Service側でも再生成または検証してください
・ProductSeriesとInstrumentTypeが不正または未指定の場合は登録しないでください
・Enumに存在しない材質、指板材、フレット数、スケールを登録しないでください
・一部だけ保存される状態を防止してください
・入力不正または重複時は全体をロールバックしてください
・既存Masterの検索・再利用条件を維持してください
・大文字小文字、前後空白、null、空文字を考慮してください
・既存のBusinessExceptionとNotFoundExceptionの使い方に合わせてください

【DTO方針】
ProductVariationCreateRequestへ、少なくとも以下の入力を保持できるようにしてください。

・ProductSeries
・InstrumentType
・productName
・internalModelCodeまたは自動生成結果
・bodyType
・bodyMaterial
・neckType
・neckMaterial
・pickupLayout
・fretCount
・scale
・variations

実際にEnum型を直接バインドするか、Stringで受けてServiceで変換するかは、Thymeleafとの相性とエラー処理を考慮して決めてください。
不正な値を受けた場合に500エラーにせず、BusinessExceptionまたは入力エラーとして扱える方式を優先してください。

【Controller方針】
GET /products/new で以下の候補をModelへ渡してください。

・ProductSeriesの候補
・InstrumentTypeの候補
・BodyMaterialTypeの候補
・NeckMaterialTypeの候補
・FingerboardMaterialTypeの候補
・ScaleLengthTypeの候補
・FretCountTypeの候補

初期バリエーション1行を維持してください。
POST /products/create はDTOを受け取り、ProductServiceを呼び出してください。
業務ロジックはControllerへ書かないでください。

【HTML方針】
・DOCTYPEからhtml終了タグまでファイル全体を出力してください
・現在の白、黒、赤を基調としたGuitar MES共通UIを維持してください
・既存の共通CSSクラスを優先して使用してください
・PC画面と小画面の両方を考慮してください
・動的バリエーション行の追加・削除を維持してください
・削除後のvariations添字を正しく再設定してください
・登録予定件数とプレビュー表示を維持してください
・公式モデル番号、カラー、指板材の入力を維持してください
・指板材はEnum候補のselectへ変更してください
・製品シリーズと楽器タイプのselectを追加してください
・MES内部モデルコードは自動更新してください
・ボディタイプとネックタイプは楽器タイプから自動更新してください
・材質、フレット数、スケールをselectへ変更してください
・JavaScriptを変更する場合はscript全体を省略せずに出力してください
・tbody、table、div、section、main、body、htmlの閉じ忘れがないか確認してください

【UIの推奨動作】
製品シリーズ選択
＋
楽器タイプ選択
↓
MES内部モデルコード自動生成

楽器タイプ選択
↓
ボディタイプ自動設定
↓
ネックタイプ自動設定

製品名入力
↓
判定可能ならシリーズ・楽器タイプの候補を補完
↓
ユーザーが最終確認

自動生成項目は、なぜその値になったか分かる補足文を表示してください。
判定不能な場合は、自動でOTHERを確定せず、選択を促してください。

【CSSの出力方針】
・既存クラスで実現できる場合は、新しいCSSを追加しないでください
・追加CSSが必要な場合は、style.cssへ追加するコードを別ブロックで出力してください
・既存CSSと重複する定義を作らないでください
・追加位置の目安を記載してください
・style.css全体は、明示的に依頼した場合のみ出力してください

【Javaの出力方針】
・package宣言とimportを含むクラス全体を出力してください
・未使用importを残さないでください
・Java 17でコンパイルできるコードにしてください
・既存のSpring Boot構成と命名規則に合わせてください
・ControllerはHTTP受付、Model設定、Service呼出、画面遷移に限定してください
・業務ルール、入力検証、コード生成、Master再利用、一括保存はServiceへ置いてください
・例外処理とnull対策を確認してください

【DBと既存データに関する制約】
・既存Product、BodyMaster、NeckMasterを削除しないでください
・既存Product IDとMaster IDを変更しないでください
・既存外部キーを変更しないでください
・Product.modelNoは公式モデル番号として維持してください
・Product.internalModelCodeはMES内部コードとして維持してください
・現在の既存scale値は648mm形式で統一済みです
・今回scaleの保存形式を変える場合は、コード出力前に移行が必要な理由を明示してください
・ddl-auto=updateに名前変更やデータ移行を任せないでください

【使用可能な主な共通CSSクラス】
・page-container
・page-toolbar
・page-toolbar-description
・page-toolbar-actions
・form-container
・form-section
・form-section-heading
・form-section-caption
・form-section-title
・form-section-description
・form-grid
・form-group
・form-label
・form-required
・form-input
・form-select
・form-help
・form-actions
・information-message
・empty-state
・table-container
・data-table
・btn
・btn-primary
・btn-secondary
・btn-outline
・variation-section-heading
・variation-heading-actions
・variation-count
・variation-list
・variation-card
・variation-card-heading
・variation-card-title
・variation-number
・variation-field-grid
・variation-remove-button
・variation-preview-table

【最初に添付するファイル】
以下の最新版を添付します。

必須：
・ProductVariationCreateRequest.java
・ProductVariationRequest.java
・ProductService.java
・ProductViewController.java
・product-form.html
・InternalModelCodeService.java
・ProductSeries.java
・InstrumentType.java
・BodyMaterialType.java
・NeckMaterialType.java
・FingerboardMaterialType.java
・ScaleLengthType.java
・FretCountType.java

必要に応じて：
・Product.java
・BodyMaster.java
・NeckMaster.java
・BodyMasterRepository.java
・NeckMasterRepository.java
・ProductRepository.java
・style.css

【出力順】
1. 修正方針の短い説明
2. 変更対象ファイル一覧
3. ProductVariationCreateRequest.java完成版
4. 必要な場合のみProductVariationRequest.java完成版
5. ProductService.java完成版
6. ProductViewController.java完成版
7. product-form.html完成版
8. 必要な場合のみstyle.css追加コード
9. 変更したポイント
10. 動作確認項目
11. 次の作業

【動作確認の重点】
・アプリケーションが正常起動する
・Product登録画面が表示される
・製品シリーズを選択できる
・楽器タイプを選択できる
・MES内部モデルコードが自動生成される
・ボディタイプとネックタイプが自動設定される
・ボディ材、ネック材、指板材を選択できる
・フレット数を選択できる
・スケールがインチとミリ併記で表示される
・保存値が既存NeckMaster検索と一致する
・バリエーション行を追加・削除できる
・削除後もDTOへ正しくバインドされる
・公式モデル番号重複時に全件保存されない
・同一カラー・指板材重複時に全件保存されない
・同じカラーのProductがBodyMasterを共有する
・同じ指板材・同じネック仕様のProductがNeckMasterを共有する
・登録成功後にProduct一覧へ戻る
・Product詳細にMES内部モデルコードと関連Masterが表示される

【今回の完了条件】
Product登録画面における自由入力を必要な項目だけに限定し、シリーズ、楽器タイプ、内部モデルコード、タイプ、材質、指板材、フレット数、スケールの入力揺れを防止できること。
既存のProduct一括登録、BodyMaster自動生成、NeckMaster自動生成、Master共有、重複防止がすべて維持されること。
```

## 新しいチャットでの進め方

1. このファイルのコードブロック内を新しいチャットへ貼り付けます。
2. 「最初に添付するファイル」の必須ファイルを添付します。
3. 添付が多すぎる場合は、次の2回に分けます。

### 1回目

```text
ProductVariationCreateRequest.java
ProductVariationRequest.java
ProductService.java
ProductViewController.java
product-form.html
InternalModelCodeService.java
```

### 2回目

```text
ProductSeries.java
InstrumentType.java
BodyMaterialType.java
NeckMaterialType.java
FingerboardMaterialType.java
ScaleLengthType.java
FretCountType.java
```

4. ファイル不足の確認だけで処理を止めず、必要ファイルが揃った時点で完成版コードの出力へ進めます。
