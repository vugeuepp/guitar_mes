# Guitar MES 次回作業プロンプト

```text
Guitar MES開発を継続します。

【今回の作業目的】
Productを中心としたマスタ構造へ再設計するため、まず現状調査と実装計画を作成する。
現在はProduct、BodyMaster、NeckMasterを個別に登録しているが、今後は製品マスタ登録時にBodyMasterとNeckMasterを同時に自動生成し、関連付けまで一括で行う構成へ変更する。
カラーや指板材に複数の選択肢がある場合は、組み合わせごとの製品バリエーションを自動生成できるようにする。

今回は、既存コードを確認したうえで影響範囲を整理し、安全な実装順序を提示してください。
必要な関連ファイルを確認できた場合は、第一段階の実装まで進めてください。

【背景と解決したい課題】
・Product、BodyMaster、NeckMasterを別々に登録する現在の操作が非効率である
・Product登録時に、対応するBodyMasterとNeckMasterも自動生成したい
・カラー違い、指板材違いをバリエーションとして複数指定し、必要な組み合わせを自動生成したい
・Product詳細画面に、関連するBodyMasterとNeckMasterの情報をまとめて表示したい
・BodyMasterのモデルコードは「BM-モデル番号」に準拠しているが、NeckMasterは「NM-001」形式になっており、コード体系が統一されていない
・Productを中心に、BodyMasterとNeckMasterのモデルコードを一貫した規則へ統一したい
・将来的にProductへギター画像を登録し、一覧・詳細・生産計画・ネック取付画面などで表示したい

【今回の優先順位】
1. 現在のProduct、BodyMaster、NeckMasterのEntity・Service・Controller・DTO・Repository・HTML・DB関連を確認する
2. 既存データと既存業務ロジックへの影響範囲を整理する
3. Product中心のマスタ登録フローを設計する
4. モデルコード体系を決定する
5. カラー・指板材バリエーションの入力DTOと生成ルールを設計する
6. トランザクション境界、重複チェック、入力検証、部分登録防止を設計する
7. Product詳細画面へBodyMaster・NeckMaster情報を表示する方法を決める
8. 画像対応は上記のマスタ再設計後に実施できるよう、拡張方針のみ整理する

【想定する登録フロー】
製品マスタ登録画面
↓
共通仕様を入力
・モデル番号
・製品名
・Body仕様
・Neck仕様
・フレット数
・スケール
・PU構成など
↓
バリエーションを入力
・カラーを複数指定
・指板材を複数指定
↓
生成予定の組み合わせを確認
↓
登録実行
↓
BodyMaster生成または既存Master再利用
↓
NeckMaster生成または既存Master再利用
↓
カラー × 指板材のProductバリエーション生成
↓
各Productへ対応するBodyMaster・NeckMasterを関連付け

【バリエーション生成例】
モデル番号：ST60
カラー：Black、White
指板材：Maple、Rosewood

生成対象：
・ST60 / Black / Maple
・ST60 / Black / Rosewood
・ST60 / White / Maple
・ST60 / White / Rosewood

単純な直積生成が業務仕様として正しくない場合に備え、除外組み合わせまたは生成対象の確認方法も検討してください。
既存コードから判断できない業務仕様は推測で確定せず、実装前に論点として明示してください。

【モデルコード方針の検討】
以下を比較し、既存コード・DB・運用への影響を踏まえて推奨案を提示してください。

案1：プレフィックス付きで統一
・BodyMaster：BM-ST60
・NeckMaster：NM-ST60

案2：共通モデル番号を使用
・Product：ST60
・BodyMaster：ST60
・NeckMaster：ST60

カラーや指板材の違いによってMasterを複数持つ必要がある場合は、同一コード衝突を避ける採番規則も検討してください。
既存データの移行が必要な場合は、移行方針と確認SQLも提示してください。

【画像対応の将来方針】
マスタ再設計後、Productにギター画像を追加する。
初期段階ではProduct画像のみを対象とし、BodyMaster画像・NeckMaster画像は将来拡張とする。

検討対象：
・Productへ画像パスまたは画像キーを保持する方法
・ローカル開発環境と公開環境の保存先
・画像未登録時のプレースホルダー
・画像形式、最大サイズ、ファイル名衝突、削除・差し替え
・Product一覧、Product詳細、ProductionOrder詳細、ネック取付画面での表示

画像機能は今回のマスタ再設計と同時に無理に実装せず、マスタ構造確定後の独立工程として扱ってください。

【出力方針】
・最初に現状コードを確認し、必要なファイルが不足している場合は対象ファイル名を具体的に列挙してください
・既存コードを確認せずにEntity、DTO、URL、フィールド、メソッドを推測しないでください
・差分ではなく、修正後のファイル全体を出力してください
・そのままコピーして既存ファイルと置き換えられる状態にしてください
・省略記号（...）を使用せず、必要なコードをすべて記載してください
・package宣言、import、DOCTYPE、閉じタグを省略しないでください
・変更不要なファイルは出力しないでください
・複数ファイルの変更が必要な場合は、対象ファイルごとに完成版を出力してください
・一度に安全に実装できない規模の場合は、段階を分け、今回実装する範囲と次回範囲を明確にしてください
・解説よりも、設計根拠、影響範囲、完成版コード、動作確認項目を優先してください

【既存機能の維持】
・現在動作しているProduct、BodyMaster、NeckMasterの一覧・詳細・登録機能を無断で削除しないでください
・Controllerの既存URLを無断で変更しないでください
・Thymeleafのmodel属性名を無断で変更しないでください
・Entity、DTO、Serviceの既存フィールドやメソッドを無断で削除しないでください
・既存ProductとProductionOrder、Body、Neck、Assembly、Guitarの関連を壊さないでください
・既存のProduct選択UIとネック取付機能を壊さないでください
・既存データがある前提で、外部キー制約と移行順序を確認してください
・仕様変更が必要な場合は、コード出力前に理由と影響範囲を明示してください

【Service設計方針】
・Product、BodyMaster、NeckMasterの一括登録はServiceでトランザクション管理してください
・Controllerへ生成ロジックや重複判定を書かないでください
・一部だけ保存される状態を防止してください
・入力不正または重複時は全体をロールバックしてください
・既存Masterを再利用するのか、常に新規生成するのかを業務キーとともに明確化してください
・大文字小文字、前後空白、null、空文字を考慮してください
・同一モデル、カラー、指板材の重複Productを防止してください
・同一仕様のBodyMaster・NeckMaster重複生成を防止してください

【DTO方針】
今回は複数入力、入力検証、バリエーション、一括生成が必要なため、専用Request DTOの利用を優先してください。

想定項目例：
・modelNo
・productName
・bodyType
・bodyMaterial
・neckType
・neckMaterial
・fretCount
・scale
・pickupLayout
・colors
・fingerboardMaterials
・除外または選択された組み合わせ

ただし、実際のフィールド名は既存Entityと命名規則を確認して決めてください。
Bean Validationを導入する場合は、Controllerで検証エラーを登録画面へ戻し、入力値とエラーメッセージを維持してください。

【UI方針】
・白、黒、赤を基調とした現在のGuitar MES共通UIを維持してください
・既存の共通CSSクラスを優先して再利用してください
・PCと小画面の両方を考慮してください
・カラー、指板材の追加・削除操作が分かりやすいUIにしてください
・生成予定のProduct組み合わせを登録前に確認できる構成を検討してください
・0件時はempty-stateを使用してください
・ボタンは共通btnクラスを使用してください

使用可能な主な共通クラス：
・page-container
・page-toolbar
・page-toolbar-description
・page-toolbar-actions
・form-container
・form-section
・form-section-heading
・form-section-title
・form-grid
・form-group
・form-label
・form-input
・form-select
・form-actions
・detail-section
・detail-grid
・detail-item
・detail-label
・detail-value
・table-container
・data-table
・empty-state
・btn
・btn-primary
・btn-secondary
・btn-outline
・btn-detail
・status-badge

【CSSの出力方針】
・既存クラスで実現できる場合は、新しいCSSを追加しないでください
・追加CSSが必要な場合は、style.cssへ追加するコードを別ブロックで出力してください
・既存CSSと重複する定義を作らないでください
・追加位置の目安を記載してください
・style.css全体は明示的に依頼した場合のみ出力してください

【Javaの出力方針】
・package宣言とimportを含むクラス全体を出力してください
・未使用importを残さないでください
・ControllerはHTTP受付、検証結果処理、Service呼出、Model設定、画面遷移に限定してください
・業務ルール、重複判定、一括生成、トランザクション処理はServiceへ置いてください
・RepositoryはEntity取得、保存、重複確認に必要な検索へ限定してください
・例外処理とnull対策を確認してください
・既存の命名規則に合わせてください

【HTMLの出力方針】
・DOCTYPEからhtml終了タグまで、ファイル全体を出力してください
・既存のThymeleaf式、フォームaction、method、hidden項目を維持してください
・tbody、table、div、section、main、body、htmlの閉じ忘れがないか確認してください
・JavaScriptを変更する場合はscript全体を省略せずに出力してください
・動的入力欄では、削除後のname属性と添字が正しく送信されることを確認してください

【DB・移行方針】
・Entity変更が必要な場合は、既存テーブルと外部キーへの影響を説明してください
・既存のProduct、BodyMaster、NeckMasterデータを保持してください
・モデルコード変更時は、更新順序、重複確認、ロールバック方法を整理してください
・必要なDDLまたは移行SQLは、実行順序付きで全文を提示してください
・本番相当データへ適用する前に件数確認SQLと不整合確認SQLを提示してください
・物理削除を前提にせず、参照中データを安全に扱ってください

【今回確認したい設計論点】
・Product、BodyMaster、NeckMasterの業務上の親子関係
・BodyMasterはカラー単位で分けるべきか
・NeckMasterは指板材単位で分けるべきか
・同じBodyMasterまたはNeckMasterを複数Productで共有する条件
・モデルコードを識別子として使うか、表示用コードとして使うか
・カラー × 指板材の全組み合わせを作るか、選択した組み合わせだけ作るか
・登録済みの同一仕様が存在する場合に再利用、スキップ、エラーのどれにするか
・登録後にProduct仕様を編集した場合、関連Masterへ変更を伝播させるか
・製造実績から参照済みのMasterをどこまで編集可能にするか

【出力順】
1. 現状構造の整理
2. 問題点と影響範囲
3. 推奨するマスタ構造
4. 未確定の業務仕様と確認事項
5. 段階的な実装計画
6. 今回変更するファイル一覧
7. 必要な場合はDB移行手順
8. 対象ファイルの完成版
9. 必要な場合のみstyle.css追加コード
10. 変更したポイント
11. 動作確認項目
12. 次回作業への引き継ぎ事項

【実施タイミング】
このマスタ再設計は、ProductionSchedule、Body・Neck個体一括発行、一括工程操作へ進む前に実施してください。
画像対応は、Product中心のマスタ構造とコード体系を確定した後に実施してください。
```

## 最初に添付する関連ファイル候補

```text
Product.java
BodyMaster.java
NeckMaster.java
ProductService.java
BodyMasterService.java
NeckMasterService.java
ProductController.java または ProductViewController.java
BodyMasterController.java
NeckMasterController.java
ProductRepository.java
BodyMasterRepository.java
NeckMasterRepository.java
product-form.html
product-detail.html
product-list.html
body-master-form.html
body-master-detail.html
neck-master-form.html
neck-master-detail.html
ProductionOrder.java
Body.java
Neck.java
style.css
application.properties
現在使用しているDDLまたはテーブル定義
```

## 作業開始時の補足

最初から全ファイルを一括変更せず、現状コードを確認してから以下の単位に分割してください。

```text
Step 1：現状調査と業務キー決定
Step 2：Request DTOと重複チェック設計
Step 3：Serviceの一括登録処理
Step 4：Product登録画面のバリエーション対応
Step 5：Product詳細へのMaster情報表示
Step 6：既存データのコード統一・移行
Step 7：単独Master登録導線の停止または管理者用への変更
Step 8：Product画像対応
```
