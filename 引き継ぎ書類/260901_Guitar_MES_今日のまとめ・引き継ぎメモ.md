## 2026-09-01 Guitar MES 今日のまとめ・引き継ぎメモ

### 1. 本日の概要

本日は、前日のProductionSchedule（日産計画）実装を引き継ぎ、以下の3段階を進めた。

1. 確定済み日産計画からのBody・Neck一括発行
2. ProductionSchedule単位でのAssembly登録とGuitar生成
3. Phase 4AとしてGuitar工程のチェックボックス一括開始・一括終了の初期実装

本日の作業により、主要な製造フローは次の形まで接続された。

```text
ProductionOrder
↓
ProductionSchedule
↓
Body・Neck一括発行
↓
同じProductionScheduleに属するBody・Neckを選択
↓
Assembly登録
↓
Guitar自動生成
↓
Guitar工程の単体処理
↓
Guitar工程の一括開始・一括終了（Phase 4A初版）
```

Phase判定は次のとおり。

```text
Phase 1  主要UI・CRUD・自動テスト基盤              完了
Phase 2  月間計画・日産計画                        完了
Phase 3  Body・Neck個体一括発行                    完了
Phase 4  工程別一覧・チェックボックス一括処理      着手中
```

### 2. 本日完了した作業

#### 2.1 ProductionScheduleからのBody・Neck一括発行

確定済みの日産計画を起点として、計画数量分のBodyとNeckを一括発行する機能を完成させた。

主な対応内容。

- 発行対象はCONFIRMEDの日産計画のみ
- ProductにBodyMasterとNeckMasterが設定されていることを検証
- 日産計画の計画数量分を一括生成
- BodyとNeckへProductionOrderを関連付け
- BodyとNeckへProductionScheduleを関連付け
- 二重発行を拒否
- 発行処理を1トランザクションで実行
- 発行済み日産計画の取消を拒否
- Body発行数、Neck発行数、発行状態を生産計画詳細画面へ表示
- 発行済み行では部品発行ボタンと取消ボタンを非表示

生産計画詳細の日産計画一覧は、次の情報を表示する構成となった。

```text
計画日
計画数
状態
Body発行数
Neck発行数
発行状態
操作
```

#### 2.2 ProductionSchedule単位のAssembly登録

従来のProductionOrder単位のネック取付導線を、ProductionSchedule単位へ変更した。

変更後の導線。

```text
発行済み日産計画
↓
ネック取付
↓
対象日産計画に属するBody・Neckのみ表示
↓
Assembly登録
↓
Guitar生成
```

主な対応内容。

- Assembly登録リクエストへproductionScheduleIdを追加
- ProductionOrderとProductionScheduleの一致を検証
- Body候補をProductionOrder、ProductionSchedule、BodyMaster、AVAILABLEで絞り込み
- Neck候補をProductionOrder、ProductionSchedule、NeckMaster、AVAILABLEで絞り込み
- BodyとNeckが同じProductionScheduleに属することをServiceで検証
- 日産計画に紐付かない旧部材の使用を拒否
- assembly-form.htmlへ対象日産計画を表示
- production-order-detail.htmlの日産計画行へネック取付ボタンを追加
- 従来の生産計画全体に対するネック取付ボタンを廃止

現在のトレーサビリティ。

```text
ProductionOrder
└── ProductionSchedule
    ├── Body
    ├── Neck
    └── Assembly
        └── Guitar
```

#### 2.3 Assembly関連E2Eの更新

AssemblyCreateE2EをProductionSchedule対応へ更新した。

確認した内容。

- E2E用ProductionOrder作成
- E2E用ProductionSchedule作成
- 同じProductionScheduleへBodyとNeckを関連付け
- ネック取付画面に対象日産計画を表示
- 対象日産計画に属するBodyとNeckのみ選択可能
- Assembly登録
- Guitar生成
- BodyとNeckの状態をASSEMBLEDへ更新
- ProductionOrder.startedQuantityを更新
- ProductionOrderをIN_PROGRESSへ更新
- テストデータを削除

ステータスバッジが複数表示されるようになったため、Playwright Locatorを具体化した。

```java
.status-badge.status-planned
.status-badge.status-working
```

完了表示については、存在しないCSSクラスを推測せず、生産計画基本情報の先頭セクションに限定して取得する方式へ変更した。

```java
page.locator(".detail-section")
        .first()
        .locator(".status-badge")
```

#### 2.4 既存E2Eの回帰修正

AssemblyとProductionScheduleの連携後、E2E全件実行で既存テストへの影響が4件発生した。

対応内容。

- GuitarProcessE2E
  - ProductionScheduleをテスト自身で作成
  - BodyとNeckをProductionScheduleへ関連付け
  - productionScheduleId付きでネック取付画面へ遷移
  - ステータスLocatorを具体化
  - CleanupへProductionSchedule削除を追加
- ProductionOrderEditE2E
  - 固定データPO260006への依存を廃止
  - テスト自身でProductionOrderを作成、更新、削除する自己完結型へ変更
- ProductionOrderSmokeE2E
  - 「生産計画を登録」を完全一致Locatorへ変更
- ProductionScheduleIssueE2E
  - 一時的なFlashメッセージではなく、発行数と発行済み状態を検証

最終的に既存E2E全11件が成功した。

```text
Tests run: 11
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

この11件成功はPhase 4A実装前の回帰確認結果である。

#### 2.5 Phase 4A 一括Guitar工程処理の初期実装

ロードマップのPhase 4へ着手した。

初期対象はGuitar工程とし、Body工程とNeck工程への展開は後続対応とする。

追加した機能。

- Guitar一覧での複数選択チェックボックス
- 選択可能個体のみチェック可能
- 全件選択と全件解除
- 選択件数のリアルタイム表示
- 対象工程の選択
- 共通作業者名の入力
- 複数Guitarの一括工程開始
- 実施中工程一覧からの複数履歴選択
- 複数工程履歴の一括終了
- 単体工程開始・終了機能の維持
- REST APIの一括開始・終了エンドポイント追加
- 成功・業務エラーのFlashメッセージ表示

新規作成したsrcファイル。

```text
BulkProcessEndRequest.java
BulkProcessStartRequest.java
```

新規作成したtestファイル。

```text
GuitarViewControllerTest.java
ProcessServiceTest.java
ProcessViewControllerTest.java
ProcessWorkControllerTest.java
```

更新した主なファイル。

```text
GuitarViewController.java
ProcessService.java
ProcessViewController.java
ProcessWorkController.java
guitar-list.html
process-end-form.html
style.css
```

#### 2.6 一括処理の業務ルール

一括処理は、保存前に全対象を検証する方式とした。

```text
全件取得
↓
全件検証
↓
1件でも不正
↓
何も保存しない
↓
トランザクションをロールバック
```

初版での主な検証。

- 対象IDが空でないこと
- 対象IDにnullが含まれないこと
- 重複IDを除外すること
- Guitarが存在すること
- ProductionOrderに紐付く現行フローのGuitarであること
- 完成済みでないこと
- 実施中工程を持たないこと
- 指定工程がGuitar工程であること
- 次に開始可能な工程と指定工程が一致すること
- ProcessHistoryが存在すること
- 終了済み履歴でないこと
- 一括終了対象が同一工程であること

#### 2.7 Phase 4A関連テスト

新規作成した4テストクラスを実行し、全8件成功した。

```text
Tests run: 8
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

現時点では、Phase 4AのService・Controller・MockMvcレベルまで確認済み。

次回はHTML更新方針に従い、画面確認とPlaywright E2Eの作成・実行が必須である。

### 3. 本日発生した問題と対応

#### 3.1 E2E LocatorのStrict Modeエラー

画面へ日産計画状態と発行状態を追加したことで、`.status-badge`が複数要素へ一致した。

エラー例。

```text
strict mode violation: locator(".status-badge") resolved to 3 elements
```

対応。

- 計画中は`.status-badge.status-planned`
- 製造中は`.status-badge.status-working`
- 完了は生産計画基本情報セクション内へ限定

今後も、汎用クラスだけを指定するLocatorは避け、対象セクション、行、固有クラス、ラベルを組み合わせる。

#### 3.2 E2E DBの固定データ依存

ProductionOrderEditE2Eが`PO260006`を前提としており、E2E DBのデータ整理後に失敗した。

対応。

```text
固定データ前提
↓
テスト自身で作成
↓
画面操作
↓
DB確認
↓
finallyで削除
↓
残存0件確認
```

今後、更新系E2Eは原則として自己完結型にする。

#### 3.3 テストログ保存と色付き表示

`tee`だけを使用すると、Mavenがパイプ出力と判断し、ターミナルのカラー表示が無効になった。

`script`を使用すると色は維持できたが、次の問題があった。

- 保存ログへANSIカラー制御コードが混入
- `Saving session...`などの追加表示
- ターミナルによってはプロセス終了後に画面が終了待ちのように見える

本日の検討では最終的な標準コマンドを確定できていない。

次回は次の要件を満たすログ保存方式を確定する。

```text
ターミナルは色付き
console.logはANSI制御コードなし
Mavenの終了ステータスを正しく保持
実行後にシェルが通常状態へ戻る
Surefireレポートを日時別に退避
Playwright evidenceを日時別に退避
```

### 4. 本日確定した運用ルール

#### 4.1 作業開始時のファイル構成共有

作業日の最初に、現在のプロジェクトファイル構成を共有する。

標準ファイル名候補。

```text
guitar_mes_file_component.txt
```

Copilotはこの一覧を基準に、存在するファイルと新規作成対象を判定する。

#### 4.2 必要ファイルはBundleで共有

今後、必要ファイルを1件ずつ直接共有する方式を原則廃止し、Bundle形式を使用する。

標準フロー。

```text
1. 作業日の最初にファイル構成を共有
2. Copilotが必要ファイルをsrc・test・HTML別に指定
3. CopilotがBundle作成コマンドを提示
4. DownloadsへBundleファイルを生成
5. Bundleファイルを共有
6. CopilotがFILE、PATH、MISSINGを照合
7. 完全版確認後に設計と実装へ進む
```

Bundle内の区切り形式。

```text
================================================================================
FILE: ProcessService.java
PATH: /absolute/path/ProcessService.java
================================================================================

ファイル本文
```

存在しないファイルは次の形式で記録する。

```text
================================================================================
MISSING: ProcessServiceTest.java
PATH: /absolute/path/ProcessServiceTest.java
================================================================================
```

Bundleの保存先はDownloadsを標準とする。

```text
~/Downloads/phase4_src_bundle.txt
~/Downloads/phase4_test_bundle.txt
~/Downloads/phase4_html_bundle.txt
```

必要ファイルの指定順は従来どおり次を維持する。

```text
1. src
2. test
3. HTML
```

各区分内はアルファベット順とする。

#### 4.3 修正版ZIPの構成

Copilotが出力する修正版ZIPは、次の浅い構成を標準とする。

```text
feature_name.zip
├── src
│   └── Javaファイルを直接配置
├── test
│   └── テストJavaファイルを直接配置
└── templates
    ├── HTMLファイルを直接配置
    └── 必要に応じてstyle.cssを配置
```

`src/main/java/com/example/...`のような深いパッケージ階層はZIP内に作らない。

配置先は回答本文で明示する。

#### 4.4 HTML更新時のE2E

HTMLを新規作成または更新するたびに、対象画面のPlaywright E2Eを作成または更新し、実行する。

```text
HTML作成・更新
↓
Controller / MockMvcテスト
↓
対象テスト
↓
手動画面確認
↓
Playwright E2E
↓
通常テスト全件
↓
E2E全件
↓
コミット
```

#### 4.5 DBスキーマ管理

次の方針を継続する。

- `spring.jpa.hibernate.ddl-auto=validate`
- DB変更はSQLで明示管理
- 適用SQL、確認SQL、ロールバックSQLを用意
- 開発DBとE2E DBへ同じ変更を適用

#### 4.6 最終テスト証跡

最終的には、日時別フォルダへ3種類の証跡を保存する方針。

```text
target/test-logs/YYYYMMDD-HHMMSS/
├── console.log
├── surefire-reports/
└── evidence/
```

ただし、色付き表示、ANSI除去、終了ステータス保持を両立する実行方法は次回確定する。

### 5. 現在の主なファイル構成

本日の追加後、ファイル数は205、ディレクトリ数は52となった。

#### src

```text
src/main/java/com/example/guitarmes/process/
├── BulkProcessEndRequest.java
├── BulkProcessStartRequest.java
├── ManufacturingProcess.java
├── ManufacturingProcessController.java
├── ManufacturingProcessRepository.java
├── ProcessEndRequest.java
├── ProcessHistory.java
├── ProcessHistoryRepository.java
├── ProcessHistoryResponse.java
├── ProcessService.java
├── ProcessStartRequest.java
├── ProcessStatusResponse.java
├── ProcessViewController.java
└── ProcessWorkController.java
```

#### test

```text
src/test/java/com/example/guitarmes/guitar/
└── GuitarViewControllerTest.java

src/test/java/com/example/guitarmes/process/
├── ProcessServiceTest.java
├── ProcessViewControllerTest.java
└── ProcessWorkControllerTest.java
```

#### HTML・CSS

```text
src/main/resources/templates/
├── guitar-list.html
├── process-end-form.html
└── process-start-form.html

src/main/resources/static/css/
└── style.css
```

### 6. Git・ブランチ状態

Phase 4Aの推奨ブランチ名は次のとおり。

```text
feature/bulk-guitar-process
```

ただし、このチャット内ではブランチ作成完了の明示確認が取れていない。

次回開始時に必ず確認する。

```bash
git -C /Users/naokiyamada/git/guitar-mes/guitar_mes branch --show-current

git -C /Users/naokiyamada/git/guitar-mes/guitar_mes status
```

期待するブランチ。

```text
feature/bulk-guitar-process
```

異なる場合は、差分を失わないよう状態を確認してからブランチを作成または切り替える。

### 7. 次回開始時の作業

#### 7.1 Phase 4Aの手動画面確認

E2Eプロファイルでアプリケーションを起動し、Guitarを複数用意して次を確認する。

##### Guitar一覧

- チェックボックスが表示される
- 工程待ちのGuitarのみ選択できる
- 作業中と完成済みは選択不可
- 全件選択と全件解除が動作する
- 選択件数がリアルタイムに更新される
- 対象工程を選択できる
- 作業者を入力できる
- 一括工程開始後に成功メッセージが表示される
- 対象Guitarが作業中になる

##### 工程終了画面

- 実施中工程が一覧表示される
- 複数履歴を選択できる
- 全件選択と全件解除が動作する
- 選択件数が更新される
- 一括工程終了後に成功メッセージが表示される
- 対象Guitarが次工程へ進む

#### 7.2 Phase 4A Playwright E2E

HTMLを更新済みのため、新しいE2Eを必ず作成する。

新規ファイル候補。

```text
BulkGuitarProcessE2E.java
```

最低限のシナリオ。

```text
E2E用ProductionOrder作成
↓
ProductionSchedule作成
↓
複数のBody・Neckを準備
↓
複数AssemblyとGuitarを生成
↓
Guitar一覧を表示
↓
複数Guitarをチェック
↓
選択件数を確認
↓
共通作業者で一括工程開始
↓
DBのProcessHistoryとGuitar状態を確認
↓
工程終了一覧を表示
↓
複数履歴をチェック
↓
一括工程終了
↓
次工程への更新を確認
↓
テストデータを削除
↓
残存0件を確認
```

追加で検証したい異常系。

- 0件選択
- 異なる開始可能工程の混在
- 実施中Guitarの混在
- 異なる工程履歴の一括終了
- 終了済み履歴の混在
- 不正対象を含む場合に全件更新されないこと

#### 7.3 テスト拡充

現在の`ProcessServiceTest`は初期確認レベルであるため、次回は正常系とロールバック条件を増やす。

優先テスト。

- 複数Guitarの一括開始成功
- 重複Guitar IDの排除
- 作業者空文字の拒否
- Guitar不存在の拒否
- 旧フローGuitar混在時に保存0件
- 完成済みGuitar混在時に保存0件
- 実施中Guitar混在時に保存0件
- 開始可能工程不一致時に保存0件
- 複数ProcessHistoryの一括終了成功
- 異なる工程混在時に保存0件
- 終了済み履歴混在時に保存0件
- 最終工程終了時のGuitar完成とProductionOrder完成数更新

#### 7.4 全件回帰確認

Phase 4Aの画面E2E完成後に行う。

```text
対象Service・Controllerテスト
↓
通常テスト全件
↓
BulkGuitarProcessE2E
↓
E2E全件
↓
証跡保存
↓
コミット
```

テストコマンドは、次回確定する新しい証跡保存方式で案内する。

### 8. 未完了・注意事項

- Phase 4Aは初版実装と関連テスト8件成功まで
- 手動画面確認は未完了
- BulkGuitarProcessE2Eは未作成
- Phase 4A追加後の通常テスト全件は未確認
- Phase 4A追加後のE2E全件は未確認
- ログ保存方式の最終コマンドは未確定
- `script`方式は採用しない方向
- Body・Neck工程の一括処理は未着手
- ProductionSchedule、日付、モデル、工程による絞り込みは未実装
- 同時更新対策の`@Version`やロックは未実装
- Phase 4Aのコミットは未確認

### 9. 次回の必要ファイル共有

次回も作業開始時に最新版のファイル構成を共有する。

```text
guitar_mes_file_component.txt
```

その後、必要ファイルはBundle作成コマンドで指定する。

BulkGuitarProcessE2E作成時に必要となる可能性が高いファイル。

#### src

```text
AssemblyService.java
GuitarService.java
ProcessService.java
ProductionScheduleService.java
```

#### test

```text
AssemblyCreateE2E.java
GuitarProcessE2E.java
PlaywrightTestBase.java
ProcessServiceTest.java
```

#### HTML

```text
guitar-list.html
process-end-form.html
```

実際の依頼時は、朝一のファイル構成を確認したうえで、必要最小限に絞り、各区分内をアルファベット順で指定する。

### 10. コミット候補

Phase 4Aが手動画面確認、E2E、全件テストまで完了した後の候補。

```text
機能追加: Guitar工程の一括開始・一括終了機能を追加
```

テストだけを分ける場合。

```text
テスト追加: Guitar工程一括処理の自動テストを追加
```

現時点ではE2Eと全件回帰確認が未完了のため、コミット前に次回作業を完了する。

### 11. 次回チャット開始用ショートメモ

```text
Guitar MES開発を継続します。
2026-09-01版の引き継ぎメモと最新版ファイル構成を確認してください。

完了済み:
・Phase 1 主要UI・CRUD・自動テスト基盤
・Phase 2 ProductionScheduleによる月間・日産計画
・Phase 3 日産計画からのBody・Neck一括発行
・ProductionSchedule単位のAssembly登録
・既存E2E全11件成功（Phase 4A実装前）

現在:
・Phase 4A Guitar工程の一括開始・一括終了を初期実装済み
・関連Service / Controller / MockMvcテスト8件成功

次に行うこと:
1. Gitブランチと差分確認
2. Phase 4Aの手動画面確認
3. ProcessServiceTest拡充
4. BulkGuitarProcessE2E作成
5. 通常テスト全件
6. E2E全件
7. 日時別証跡保存
8. コミット

継続ルール:
・作業日の最初にファイル構成を共有
・必要ファイルはsrc、test、HTML別Bundleで共有
・BundleはDownloadsへ作成
・ZIPはsrc、test、templatesの浅い構成
・HTML変更時はPlaywright E2E必須
・DB変更はSQLで明示管理
・一括処理は全件検証後に実行し、部分成功を認めない
```

### 12. 本日の到達点

本日の作業により、Guitar MESは日産計画を実際の部品発行とAssemblyへ接続し、さらに現場向けの複数個体一括処理へ進み始めた。

```text
月間計画
↓
日産計画
↓
Body・Neck発行
↓
Assembly
↓
Guitar
↓
複数選択による工程開始・終了
```

次回はPhase 4Aを画面・E2E・全件回帰確認まで完成させ、Guitar工程の一括処理を正式にコミット可能な状態へ仕上げる。
