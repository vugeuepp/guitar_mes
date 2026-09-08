# Guitar MES 今日のまとめ・引き継ぎメモ

- 作業日: 2026年9月4日
- 現在ブランチ: `feature/phase5b-search-filter`
- 現在Phase: Phase 5B
- 本日の終了地点: 主要一覧の表示改善と、ギター管理一覧の検索・フィルタ機能まで実装・対象E2E確認済み

## 1. 本日の概要

本日は、Guitar MESの主要一覧画面について、列幅、情報の優先順位、工程・状態表示、一括操作パネルを見直し、画面間の表示ルールを統一した。その後、Phase 5Bの検索・フィルタ機能に着手し、最初の対象としてギター管理一覧へサーバーサイド検索を実装した。

ギター、ネック、ボディ、ダッシュボード、生産計画詳細、ネック取付実績一覧で、シリアル番号、製品名、カラー、現在工程、状態を現場で識別しやすい形へ整理した。ネック取付実績一覧では、単なるシリアル番号だけでは製品を識別しにくいという手動確認結果を受け、ギター列に製品名とカラーも追加した。

## 2. 本日完了した作業

### 2.1 主要一覧画面の視認性改善

- ギター管理一覧
  - チェックボックス列を縮小
  - 製品名とカラーを2段表示
  - 製品名を主情報として強調
  - 現在工程をバッジ表示
  - 一括工程開始パネルの配置と幅を調整
- ネック管理一覧
  - チェックボックス列を縮小
  - モデル名列を拡張
  - 長いモデル名を複数行表示
  - 現在工程をバッジ表示
  - 一括操作パネルを共通化
- ボディ管理一覧
  - チェックボックス列を縮小
  - モデル名とカラーを2段表示
  - 現在工程をバッジ表示
  - 一括操作パネルを共通化

### 2.2 ダッシュボードの現在ギター一覧改善

- 内部ID列を削除
- シリアル番号を強調表示
- 製品名とカラーを2段表示
- 現在工程をバッジ表示
- 「ギター一覧へ」リンクを追加
- 狭い画面では横スクロールできる構成を維持

### 2.3 生産計画詳細の生成済みギター改善

- モデル番号、製品名、カラーのスラッシュ連結表示を廃止
- 製品名とカラーを2段表示
- 現在工程をバッジ表示
- 製品列と詳細列の幅を調整

### 2.4 ネック取付実績一覧改善

- 内部ID列を削除
- ギター、ネック、ボディ、作業者、組立日時、詳細の6列へ整理
- ギターシリアルを強調
- ギター列へ製品名とカラーを追加
- ネック、ボディはシリアル番号を維持
- 組立日時と詳細列の幅を調整
- 製品が関連付いていない過去データを考慮し、製品名とカラーは `-` を表示するフォールバックを追加

### 2.5 ギター管理一覧の検索・フィルタ機能

以下のサーバーサイド検索を実装した。

- シリアル番号の部分一致検索
- 製品フィルタ
- 現在工程フィルタ
- 状態フィルタ
  - 工程待ち
  - 作業中
  - 完成
- 複数条件のAND検索
- 検索後の条件保持
- 全件数または検索結果件数の表示
- クリアによる全条件解除
- 0件時の専用メッセージと検索条件クリア導線
- 検索後も既存の一括工程開始・終了が利用できることを確認

## 3. 実装または更新した機能

- 主要一覧の共通情報階層
  - 主情報: シリアル番号、製品名、モデル名
  - 補助情報: カラー
  - 業務状態: 工程バッジ、状態バッジ
- ギター、ネック、ボディ一括工程操作のレイアウト統一
- ダッシュボードの個体識別性向上
- 生産計画詳細の生成済みギター表示改善
- ネック取付実績の製品識別性向上
- ギター管理一覧のGETパラメーターによる検索・フィルタ

## 4. 新規作成・更新した主要ファイル

### src

- `src/main/java/com/example/guitarmes/assembly/AssemblyResponse.java`
- `src/main/java/com/example/guitarmes/assembly/AssemblyService.java`
- `src/main/java/com/example/guitarmes/guitar/GuitarProgressResponse.java`
- `src/main/java/com/example/guitarmes/guitar/GuitarService.java`
- `src/main/java/com/example/guitarmes/guitar/GuitarViewController.java`

### test

- `src/test/java/com/example/guitarmes/e2e/AssemblyCreateE2E.java`
- `src/test/java/com/example/guitarmes/e2e/BodyBulkProcessE2E.java`
- `src/test/java/com/example/guitarmes/e2e/BulkGuitarProcessE2E.java`
- `src/test/java/com/example/guitarmes/e2e/NeckBulkProcessE2E.java`
- `src/test/java/com/example/guitarmes/guitar/GuitarViewControllerTest.java`

### HTML・CSS

- `src/main/resources/static/css/style.css`
- `src/main/resources/templates/assembly-list.html`
- `src/main/resources/templates/body-list.html`
- `src/main/resources/templates/guitar-list.html`
- `src/main/resources/templates/home.html`
- `src/main/resources/templates/neck-list.html`
- `src/main/resources/templates/production-order-detail.html`

## 5. 確定した業務ルールと設計方針

- 一覧画面では、業務上の識別価値が低い内部IDを安易に表示しない。
- 個体識別にはシリアル番号を使用する。
- 製品を識別する必要がある画面では、シリアル番号だけでなく製品名とカラーも表示する。
- 製品名を主情報、カラーを薄い補助情報として2段表示する。
- 現在工程と状態は通常テキストではなく、視認性の高いバッジで表示する。
- ネック取付実績一覧では、完成結果であるギター列の情報量を増やし、ネックとボディはシリアル中心の表示を維持する。
- 検索条件はGETパラメーターで受け取り、URL上で再現可能にする。
- 複数の検索条件はAND条件として扱う。
- 検索後も入力値、選択値、結果件数を画面へ保持する。
- 検索結果が0件の場合は、未登録と検索不一致を区別して案内する。
- Hibernateの `ddl-auto` による自動スキーマ更新は使用せず、DB変更とデータ移行はSQLで明示管理する。

## 6. テスト実績

### 通常テスト全件

- 実行件数: 246件
- Failures: 0
- Errors: 0
- Skipped: 0
- 結果: `BUILD SUCCESS`
- 実行日時: 2026年9月4日 16:15頃

この通常テスト全件は、主要一覧の表示改善、ダッシュボード、生産計画詳細、ネック取付実績一覧の改善後に実行した。ギター検索・フィルタ機能追加後の通常テスト全件は未実行。

### E2E全件

- 実行件数: 14件
- Failures: 0
- Errors: 0
- Skipped: 0
- 結果: `BUILD SUCCESS`
- 実行日時: 2026年9月4日 16:16頃

このE2E全件は、主要一覧の表示改善、ダッシュボード、生産計画詳細、ネック取付実績一覧の改善後に実行した。ギター検索・フィルタ機能追加後のE2E全件は未実行。

### 関連テスト

- `NeckBulkProcessE2E` と `BodyBulkProcessE2E`
  - 合計2件成功
  - Failures 0、Errors 0、Skipped 0
- `BulkGuitarProcessE2E`、`BodyBulkProcessE2E`、`NeckBulkProcessE2E`
  - 合計3件成功
  - Failures 0、Errors 0、Skipped 0
- `AssemblyCreateE2E`
  - 1件成功
  - Failures 0、Errors 0、Skipped 0
- ギター検索・フィルタ追加後の `BulkGuitarProcessE2E`
  - 1件成功
  - Failures 0、Errors 0、Skipped 0
  - 実行完了日時: 2026年9月4日 17:41:13 +09:00

## 7. 発生した問題、原因、対応内容

### 7.1 ダッシュボードE2Eの未定義変数

- 問題: `productName` と `productColor` をE2Eで参照したが、古いテストファイルを基準にした修正版ではフィールドが存在せずコンパイルエラーになった。
- 原因: コミット後の最新版ではなく、以前のBundleを基準に修正版を生成した。
- 対応: 変更を一度戻し、最新版Bundleを再取得した。最新版でフィールド定義とDB取得処理を確認してからダッシュボード単体の修正版を作り直した。

### 7.2 `style.css`とE2Eを戻しすぎた

- 問題: ダッシュボード変更を戻す際、共通CSSとギター一覧E2Eに含まれていた正常な既存改善まで `git restore` で消えた。
- 原因: 複数画面で共有するファイルを、変更単位を分けずに復元した。
- 対応: 成功済みZIPから `style.css` と `BulkGuitarProcessE2E.java` を復旧し、期待する変更ファイルが揃っていることを `git status --short` で確認した。

### 7.3 PlaywrightのGetByTextOptions型不一致

- 問題: `Locator#getByText` に `Page.GetByTextOptions` を渡し、コンパイルエラーになった。
- 原因: Playwright Java APIのオプションクラスを取り違えた。
- 対応: `Locator.GetByTextOptions` へ修正した。

### 7.4 生産計画詳細へ工程バッジが反映されなかった

- 問題: E2Eが `.production-order-guitar-table .process-badge` を検出できなかった。
- 原因: 修正版作成時の文字列置換が実際のHTML改行・空白構造と一致せず、置換が成立していなかった。
- 対応: 現在適用中の実ファイルを再取得し、実構造に合わせて置換した。HTML内に `process-badge` が存在すること、旧連結表示が消えていることを機械的に確認した。

### 7.5 ネック取付実績一覧の列ずれ

- 問題: IDヘッダーを削除した後も、明細行の `assemblyId` セルが残っていた。
- 原因: ヘッダーと本文の変更が一致していなかった。
- 対応: 明細行の内部IDセルも削除し、詳細リンクのURL生成に必要な `assemblyId` のみ維持した。

### 7.6 ギター検索E2Eの固定件数依存

- 問題: 工程と状態の複合検索結果を2件固定で検証したところ、既存データを含め7件が該当しE2Eが失敗した。
- 原因: E2E用データ以外の既存データ件数に依存するアサーションだった。
- 対応: 固定件数検証を廃止し、E2Eで作成した対象2件が表示され、異なる工程のE2Eデータが除外されることを検証する方式へ変更した。

## 8. 本日確定した開発運用ルール

- 作業日の最初に必要ファイル構成一覧を共有する。
- 必要ファイルは `src`、`test`、`HTML・CSS` の区分に分け、区分内をアルファベット順で指定する。
- 最新状態を取得したBundleのみを修正版作成の基準にする。
- 修正版ZIPは `templates`、`src`、`test` に分け、その配下へパッケージ階層を作らず対象ファイルを直接配置する。
- HTMLを新規作成または更新した場合、その画面のPlaywright E2Eを作成または更新して都度実行する。
- E2Eでは既存DB件数に依存する固定件数アサーションを避け、テストで作成した対象データの包含・除外を検証する。
- 共有CSSを戻す場合、他画面の成功済み変更まで消えないか確認する。
- 修正版生成時は、置換が1件成立したことと、期待するクラス・項目が出力ファイルに存在することを機械的に確認する。
- 通常テスト全件およびE2E全件の最終実行は、日時付きログを保存する `tee` 形式を標準とする。
- ブランチは作業単位で区切り、完了した作業と次の機能を別ブランチで管理する。

## 9. 未完了事項と注意事項

- ギター検索・フィルタ機能追加後の通常テスト全件は未実行。
- ギター検索・フィルタ機能追加後のE2E全件は未実行。
- ネック管理一覧への検索・フィルタ横展開は未着手。
- ボディ管理一覧への検索・フィルタ横展開は未着手。
- 生産計画一覧への検索・フィルタ追加は未着手。
- ダッシュボードを製造中ギターだけに絞る、または表示件数を制限する改善案は未実装。
- 検索は現在、全件取得後にJava側で絞り込む構成。データ件数が増えた場合はRepository/DB検索やページングを検討する。
- `spring.jpa.open-in-view` の警告は残っている。今回の作業範囲では変更していない。
- Mockitoの動的Java Agent読み込みに関する将来互換性警告が通常テストで出ている。今回のテスト結果には影響していないが、将来のJDK更新に備えて対応検討が必要。

## 10. 現在のPhaseと到達点

- Phase 5B
- 主要一覧画面のレイアウト・表示統一: 完了
- ダッシュボード、生産計画詳細、ネック取付実績一覧への表示ルール横展開: 完了
- ギター管理一覧の検索・フィルタ: 実装および対象E2E成功
- ネック、ボディ、生産計画の検索・フィルタ: 未着手

## 11. Gitブランチ、コミット状況

- 現在ブランチ: `feature/phase5b-search-filter`
- `feature/phase5b-table-layout` はローカルブランチ一覧に存在していた。
- 本日途中までの一覧UI改善についてはコミット・Push済みと確認した時点がある。
- その後、`feature/phase5b-search-filter` 上で実施したダッシュボード、生産計画詳細、ネック取付実績一覧、ギター検索・フィルタの最終変更について、コミット完了を示すログは本日の会話内で未確認。
- したがって、次回開始時に `git status --short`、`git log --oneline --decorate -5`、`git branch --show-current` で必ず確認する。

## 12. 次回開始時に確認すること

1. 現在ブランチが `feature/phase5b-search-filter` であること。
2. 作業ツリーとステージ状況。
3. 本日の最終変更がコミット・Push済みかどうか。
4. `git diff --check` に問題がないこと。
5. ギター検索・フィルタの画面が全件表示、複合検索、クリア、0件表示で正常に動くこと。
6. 検索後も一括工程開始・終了が正常に動くこと。
7. 通常テスト全件とE2E全件をどの時点で再実行するか。

確認コマンド:

```bash
git branch --show-current
git status --short
git diff --check
git log --oneline --decorate -5
```

## 13. 次回の推奨作業順序

1. Git状態と本日のコミット状況を確認する。
2. 未コミットなら、本日の最終変更をステージ、コミット、Pushする。
3. ネック管理一覧の検索・フィルタ要件を整理する。
4. ネック管理一覧へ検索・フィルタを実装する。
5. `NeckBulkProcessE2E` とController/Serviceテストを更新して実行する。
6. ボディ管理一覧へ同じ検索UIと操作感を横展開する。
7. `BodyBulkProcessE2E` とController/Serviceテストを更新して実行する。
8. 通常テスト全件を日時付きログ保存形式で実行する。
9. E2E全件を日時付きログ保存形式で実行する。
10. Phase 5B検索・フィルタのまとまりとしてコミットする。

## 14. 次回必要になる可能性が高いファイル

### src

- `BodyService.java`
- `BodyViewController.java`
- `NeckService.java`
- `NeckViewController.java`
- ボディ一覧・ネック一覧で使用しているResponseまたはDTOクラス

### test

- `BodyBulkProcessE2E.java`
- `BodyViewControllerTest.java` または対応するControllerテスト
- `NeckBulkProcessE2E.java`
- `NeckViewControllerTest.java` または対応するControllerテスト
- 必要に応じてBody/Neck Serviceテスト

### HTML・CSS

- `body-list.html`
- `neck-list.html`
- `style.css`

## 15. コミットメッセージ候補

本日の最終変更をまだコミットしていない場合:

```text
機能追加: ギター一覧に検索・フィルタ機能を追加
```

ダッシュボード、生産計画詳細、実績一覧の改善も同じ未コミット範囲に含まれる場合:

```text
UI改善: 関連画面の表示統一とギター検索機能を追加
```

## 16. 次回チャット開始用ショートメモ

```text
前回はPhase 5Bで、主要一覧の表示統一とギター管理一覧の検索・フィルタを実装しました。ギター検索はシリアル部分一致、製品、現在工程、状態のAND検索、条件保持、件数表示、クリアに対応し、BulkGuitarProcessE2Eは1件成功しています。通常テスト246件とE2E 14件の全件成功は検索機能追加前の結果なので、検索追加後の全件テストは未実行です。まずfeature/phase5b-search-filterのGit状態とコミット状況を確認し、その後ネック一覧、ボディ一覧の順で検索・フィルタを横展開してください。
```
