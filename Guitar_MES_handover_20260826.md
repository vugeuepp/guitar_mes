# Guitar MES 開発 引き継ぎメモ・今後の方針

- 更新日: 2026-08-26
- 対象プロジェクト: Guitar Manufacturing Execution System（Guitar MES）
- 技術構成: Java / Spring Boot / Thymeleaf / PostgreSQL
- 現在の開発段階: 主要一覧画面の共通UI化完了、フォーム・詳細画面整備および実運用向け設計の検討段階
- 次回の開始候補: `production-order-detail.html` の共通UI化

---

## 1. プロジェクトの目的

Guitar MESは、ギター製造における以下の情報を一元管理するためのシステムである。

- 月間・日別の生産計画
- Product、BodyMaster、NeckMasterなどの製品仕様
- Body、Neck、Guitarの個体管理
- ネック取付実績
- 工程開始・終了実績
- 工程内作業
- 検査、差し戻し、再作業
- 作業者と製造個体のトレーサビリティ
- 工程時間、進捗、品質情報の分析

現在は基本的な生産計画・個体・工程管理が動作しており、今後は実際の工場運用に近づけるため、日産計画、一括個体発行、工程内作業、差し戻し、ユーザー権限管理を追加していく。

---

## 2. 現在の正式な製造フロー

旧フローの「Guitarを直接登録してからAssemblyを登録」は廃止済み。

現在の正式フローは以下。

```text
ProductionOrder登録
↓
Product（モデル・カラー・指板材）確定
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

### 今後の想定フロー

実際の運用では月間計画と現場作業の間に日別製造指示が存在するため、将来的には次の構造を目指す。

```text
ProductionOrder（月間・モデル別計画）
↓
ProductionSchedule（日別製造指示）
↓
Body / Neck個体一括発行
↓
Body / Neck工程
↓
Assembly
↓
Guitar工程
↓
工程内作業
↓
検査・差し戻し・再作業
↓
完成
```

---

## 3. 生産規模と実運用上の前提

実際の生産規模は、おおよそ月産2,000〜2,500本。

一つのモデルにつき100本、200本などの単位で月間計画を立てる。

一方、現場では毎朝、次のような日産指示が配布される。

```text
ST60  10本
TL50  20本
JM60   3本
JG60   5本
```

したがって、次の二階層で管理する方針が適切である。

### 月間計画

```text
ST60 Black / Rosewood   200本
TL50 Blonde / Maple     300本
JM60 White / Rosewood    80本
```

### 日産計画

```text
2026-08-26

ST60 Black / Rosewood   10本
TL50 Blonde / Maple     20本
JM60 White / Rosewood    3本
JG60 Green / Rosewood    5本
```

日産計画を確定した時点で、必要数のBody・Neck個体を発行し、現場では当日分を対象に作業する構成を候補とする。

---

## 4. ProductionOrder関連の実装状況

### 完了済み

- ProductionOrder Entity / Repository / Service作成
- `t_production_order`テーブル作成
- 生産計画の登録・一覧・詳細画面作成
- 生産指示番号は `PO + 年下2桁 + 4桁連番`
- 計画数量、開始数量、完成数量を管理

### ステータス

```text
PLANNED      計画中
IN_PROGRESS  製造中
COMPLETED    完了
CANCELLED    中止
```

### Product選択UI

生産計画登録画面はJavaScriptによる段階選択済み。

```text
シリーズ
↓
モデル
↓
カラー
↓
指板材
↓
Product ID確定
```

仕様:

- 同一モデル・同一カラーで指板違いがある場合は指板材を選択
- 指板材が1種類だけの場合は自動選択
- 最終確認としてモデル番号・製品名・カラー・指板材を表示
- Controllerへ送信するのは最終的な`productId`
- 計画数量は数値入力方式を基本とする
- 将来的に50、100、150、200などの数量ショートカットを追加可能

---

## 5. ProductとBodyMaster / NeckMasterの関連

ProductはBodyMasterとNeckMasterを参照する。

```java
@ManyToOne
@JoinColumn(name = "body_master_id")
private BodyMaster bodyMaster;

@ManyToOne
@JoinColumn(name = "neck_master_id")
private NeckMaster neckMaster;
```

現在の関連は以下。

```text
ProductionOrder
└─ Product
   ├─ BodyMaster
   └─ NeckMaster

Body
└─ BodyMaster

Neck
└─ NeckMaster
```

ネック取付画面では、ProductionOrderのProductに対応するMasterを持つ部材だけを表示する。

```text
Body.status = AVAILABLE
かつ
Body.bodyMaster.id = Product.bodyMaster.id

Neck.status = AVAILABLE
かつ
Neck.neckMaster.id = Product.neckMaster.id
```

AssemblyService側でもMaster ID一致を検証済み。

### 今後追加する関連

Body・Neckがどの計画または日産指示から発行されたかを明確にする。

```text
Body
├─ ProductionOrder
├─ ProductionSchedule（導入後）
└─ BodyMaster

Neck
├─ ProductionOrder
├─ ProductionSchedule（導入後）
└─ NeckMaster
```

文字列のシリアル番号から所属計画を推測せず、DB上の外部キーで関連付ける。

---

## 6. 個体一括発行方針

現在はBody・Neckを一件ずつ登録する構成だが、実運用では非効率である。

### 目標

```text
日産計画を確定
↓
指定本数分のBodyを一括生成
↓
指定本数分のNeckを一括生成
↓
当日の作業対象一覧へ表示
```

例:

```text
ST60 10本
↓
Body 10件発行
Neck 10件発行
```

### 発行タイミング

月間計画登録時に200件を即時生成するのではなく、日産計画の内容確認後に「製造個体を発行」する方式を優先候補とする。

理由:

- 月間計画の入力間違いによる大量生成を防げる
- 日ごとの作業数量に合わせられる
- 個体番号の不要な先行確保を避けられる
- 日産計画と個体の対応が明確になる

### 二重発行防止

- 発行済みフラグまたは発行済み個体数を確認する
- 同じ日産計画から重複発行できないようServiceで検証する
- 発行後のProduct・Master変更を制限する

---

## 7. 一括作業機能の方針

発行されたBody・Neckは日産計画またはProductionOrder別に一覧表示する。

```text
☑ BD-PO260001-001  工程待ち
☑ BD-PO260001-002  工程待ち
☐ BD-PO260001-003  工程待ち
```

実施したい操作:

- 全件選択
- 選択解除
- 選択件数表示
- 選択個体の一括工程開始
- 選択個体の一括工程終了
- ステータス、モデル、日付、工程による絞り込み
- 必要に応じたページング

### Service側検証

一括処理時は選択された全個体について確認する。

- 対象が存在するか
- 同じProductionOrderまたはProductionScheduleに所属するか
- 現在ステータスが開始・終了可能か
- 工程順が正しいか
- すでに作業中ではないか

初期方針としては、1件でも不正な対象があれば全件を処理せず、トランザクションをロールバックする。

---

## 8. Assembly / Guitar自動生成

### AssemblyService

`createAssembly()`の引数は次へ変更済み。

```java
createAssembly(
    Long productionOrderId,
    Long neckId,
    Long bodyId,
    String workerName)
```

処理内容:

- ProductionOrder取得
- Neck取得
- Body取得
- ProductionOrderの計画上限チェック
- Body / NeckがAVAILABLEか確認
- ProductとBodyMaster / NeckMasterの適合確認
- Guitar自動生成
- Assembly保存
- Body / NeckをASSEMBLEDへ変更
- ProductionOrder.startedQuantityを1加算
- ProductionOrder.statusをIN_PROGRESSへ変更

### Guitar

GuitarはProductionOrderを参照する。

```java
@ManyToOne
@JoinColumn(name = "production_order_id")
private ProductionOrder productionOrder;
```

Guitarはネック取付完了時に初めて生成される。

- シリアル: `DY + 年下2桁 + 4桁連番`
- 初期工程: ギターパーツ取付

---

## 9. Guitar工程と工程内作業

### 現行Guitar工程

```text
1. ギターパーツ取付
2. 調整・調音
3. 最終検品
4. 完成
```

旧工程の「塗装検品」「ネック取付」などは現行Guitar工程から外した。

ネック取付はAssemblyそのものが実績であるため、GuitarのProcessHistoryへ重複保存しない。

### 現実の工程

実際には「ギターパーツ取付」だけでも作業が細分化される。

例:

```text
ギターパーツ取付
├─ ペグ取付
├─ 穴あけ
├─ ブリッジ取付
├─ ピックガード取付
├─ 結線
├─ 弦巻き
└─ 通電確認
```

そのため将来的には、工程の下に工程内作業を持たせる。

```text
Guitar
└─ ProcessHistory
   └─ WorkTaskHistory
```

### 追加候補

```text
ProcessDefinition       工程マスタ
ProcessTaskDefinition   工程内作業マスタ
ProcessHistory          工程実績
ProcessTaskHistory      工程内作業実績
```

工程ごとに専用ページを用意し、必要な作業項目、入力項目、判定方法を切り替える方向で検討する。

---

## 10. 差し戻し・再作業の方針

実際の工程では出戻りが頻繁に発生する。

例:

- 塗装が厚くパーツが取り付けられない
- 作業中に傷を付けてしまう
- 塗装検品へ戻す
- 再塗装後に再検品する
- パーツ取付を再開する

想定履歴:

```text
塗装検品 PASS
↓
パーツ取付開始
↓
傷発生
↓
塗装検品へ差し戻し
↓
再塗装
↓
塗装検品 PASS
↓
パーツ取付再開
```

### 保持したい情報

- 対象個体
- 差し戻し元工程
- 差し戻し先工程
- 差し戻し理由
- 詳細コメント
- 登録者
- 登録日時
- 再作業担当者
- 再開日時

### 差し戻し理由例

```text
傷
打痕
塗装過多
色ムラ
組付不良
配線不良
部品不良
作業ミス
```

単純なステータス上書きではなく、差し戻し・再作業を履歴として残す。

---

## 11. ProcessServiceの現状と今後

現行Guitar工程のみを対象に再設計済み。

主な処理:

- 工程開始順の検証
- 実施中工程の二重開始防止
- 最終工程終了時のGuitar完成処理
- ProductionOrder完成数更新
- LEGACY_GUITAR履歴を現行判定・平均時間から除外
- 完成数の二重加算防止

ProductionOrder未関連Guitarを拒否する保険を残してよい。

```java
if (guitar.getProductionOrder() == null) {
    throw new BusinessException(
        "生産計画に関連付けられていない" +
        "ギターでは工程を開始できません。");
}
```

### 将来的なService分割候補

機能追加によりProcessServiceが肥大化した場合のみ分割する。

```text
GuitarProcessService
ProcessTaskService
ProcessReturnService
QualityInspectionService
ProcessAnalysisService
```

まだ存在しない機能のために、先に空Serviceを作らない。

---

## 12. ユーザー管理・ログイン・権限

最終的にはログイン機能を導入し、誰がどの個体・工程・工程内作業に関わったかを記録する。

### 初期ロール

```text
ADMIN   管理者
WORKER  作業者
```

### 管理者の主な権限

- Product管理
- BodyMaster管理
- NeckMaster管理
- ProductionOrder管理
- 日産計画管理
- ユーザー管理
- 工程マスタ管理
- 品質分析閲覧
- 全データ参照

### 作業者の主な権限

- 本日の担当作業参照
- 工程開始
- 工程終了
- 工程内作業登録
- 差し戻し申請または登録
- 作業履歴参照
- 自分が関わった個体の参照

### 作業者に許可しない操作

- Product変更
- BodyMaster変更
- NeckMaster変更
- ProductionOrder変更
- ユーザー管理
- 工程マスタ変更

### 将来追加を検討するロール

```text
QUALITY  品質担当
LEADER   班長・工程責任者
MANAGER  生産管理者
```

### User Entity候補

```text
id
employeeNo
loginId
passwordHash
name
department
role
enabled
createdAt
updatedAt
```

現在の`workerName`文字列は将来的にUser参照へ変更する。

```text
ProcessHistory.workerName
↓
ProcessHistory.worker(User)
```

Spring Securityを使い、URL制御・メソッド制御・メニュー表示制御を実装する。

---

## 13. CRUD方針

すべてのデータへ機械的に編集・削除を付けない。

### マスタ系

対象:

- Product
- BodyMaster
- NeckMaster
- 将来の工程マスタ
- 将来の工程内作業マスタ

方針:

- 登録: 必要
- 参照: 必要
- 編集: 必要
- 削除: 条件付きで必要

すでにProductionOrderや実績から参照されているマスタは物理削除せず、`enabled`または`active`による無効化を優先する。

### ProductionOrder

発行前:

- Product変更可
- 計画数量変更可
- 削除または取消可

個体発行後・製造開始後:

- Product変更禁止
- 計画数量変更を制限
- 物理削除禁止
- `CANCELLED`による中止を使用

### 実績系

対象:

- Body
- Neck
- Assembly
- Guitar
- ProcessHistory
- 将来のProcessTaskHistory
- 将来のReturnHistory

方針:

- 原則として物理削除しない
- 個体のモデルや所属計画を安易に編集しない
- 不良・廃棄・中止はステータスと履歴で表現する
- ProcessHistoryは原則Read Only
- 誤登録訂正が必要な場合は、管理者限定の訂正履歴方式を検討する

---

## 14. DTOの利用方針

ControllerとServiceを行き来するたびにDTOを機械的に作らない。

### DTOを作る場面

- 複数の入力項目をまとめる
- Bean Validationが必要
- 一括処理対象を渡す
- 複数Entityをまとめて表示する
- 集計結果・計算結果を表示する
- APIの外部契約としてEntityを直接公開したくない

### Request DTO例

```text
ProductionOrderCreateRequest
DailyScheduleCreateRequest
BulkProcessStartRequest
BulkProcessEndRequest
ProcessReturnRequest
```

### Response DTO / View Model例

```text
ProductionOrderDetailResponse
DailyScheduleDetailResponse
GuitarTraceabilityResponse
QualitySummaryResponse
```

### DTOが不要な場面

```java
getBodyById(Long id)
cancelProductionOrder(Long id)
getBodiesByProductionOrderId(Long id)
```

単純なID受け渡しやRepositoryからServiceへのEntity受け渡しには、専用DTOを作らなくてよい。

### Controllerの責務

- HTTPリクエスト受付
- Request DTO受取
- Service呼出
- Modelへの表示データ設定
- 画面遷移

### Serviceの責務

- 業務ルール
- トランザクション
- Entity状態変更
- Repository呼出
- 必要に応じてResponse DTOへ変換

### Repositoryの責務

- Entityの取得・保存
- DB検索
- 集計クエリ

---

## 15. パッケージ構成の方針

現在はレイヤー単位の構成。

```text
com.example.guitarmes
├─ common
├─ controller
│  ├─ api
│  └─ view
├─ dto
├─ entity
├─ exception
├─ repository
└─ service
```

現段階ではこの構成を維持する。

### 将来の候補

規模が大きくなったら、機能単位へ段階的に移行する。

```text
com.example.guitarmes
├─ production
│  ├─ order
│  └─ schedule
├─ component
│  ├─ body
│  └─ neck
├─ assembly
├─ guitar
├─ process
├─ quality
├─ master
│  ├─ product
│  ├─ body
│  └─ neck
├─ user
├─ analysis
└─ shared
```

### 移行方針

- 既存ファイルを今すぐ全面移動しない
- 日産計画などの新機能から機能単位パッケージを試す
- クラスを探しにくくなった時に分割する
- Serviceの責務が複数業務へまたがった時に分割する
- 一括変更によるimport修正やコンパイルエラーを避ける

---

## 16. 旧フロー整理

### 削除・停止済み

- 旧Guitar直接登録リンク
- `guitar-form.html`
- `GuitarCreateRequest.java`
- Guitar直接作成POST API
- 旧Assembly独立登録導線
- `DataLoader.java`
- 旧Guitar生成メソッド

`/guitars/new`はProductionOrder一覧へリダイレクトする形で残してよい。

### DB旧データ

`production_order_id IS NULL`の旧Guitarと関連Assemblyは削除済み。

外部キー制約があるため削除順はAssembly → Guitar。

確認SQL:

```sql
SELECT COUNT(*)
FROM t_guitar
WHERE production_order_id IS NULL;
```

期待値:

```text
0
```

### legacyData対応

旧DBデータ削除後は不要。

削除対象:

- `GuitarProgressResponse.legacyData`
- GuitarServiceのlegacy判定
- guitar-list.htmlの「旧データ」「参照のみ」
- `.status-legacy`
- `.table-row-legacy`
- `needAssembly`

---

## 17. ステータス表示共通化

### StatusDisplayHelper

```text
WAITING             工程待ち
WORKING             作業中
WAITING_INSPECTION  検品待ち
REWORK              手直し待ち
AVAILABLE           組立可能
RETURNED            塗装前工程へ差し戻し
ASSEMBLED           組立済み
REJECTED            不合格
```

Thymeleaf例:

```html
<span class="status-badge"
      th:classappend="${@statusDisplay.getCssClass(neck.status)}"
      th:text="${@statusDisplay.getLabel(neck.status)}">
</span>
```

### ProductionOrderDisplayHelper

```text
PLANNED      計画中
IN_PROGRESS  製造中
COMPLETED    完了
CANCELLED    中止
```

---

## 18. UIデザイン方針

- 白・黒・赤を基調
- 業務システム向け横ナビ
- 製品サイト風の高級感を参考にする
- 既存ブランドロゴ等は複製しない
- PCと小画面の両方を確認する
- 多列テーブルは内部横スクロールにする

### 共通UIクラス

```text
page-container
page-toolbar
page-toolbar-description
page-toolbar-actions
table-container
empty-state
data-table
btn
btn-primary
btn-secondary
btn-outline
btn-detail
btn-process-end
status-badge
```

### 共通画面構造

```html
<div th:replace="~{fragments/header :: header('画面名')}">
</div>

<main class="page-container">
    <div class="page-toolbar">
        ...
    </div>

    <div class="table-container">
        <table class="data-table">
            ...
        </table>
    </div>
</main>
```

---

## 19. UI整備の完了状況

### 完了済み

- ダッシュボード
- 生産計画一覧
- ギター管理一覧
- ネック管理一覧
- ボディ管理一覧
- ネック取付実績一覧
- 製品マスタ一覧
- BodyMaster一覧
- NeckMaster一覧

対象ファイル:

```text
home.html
production-order-list.html
guitar-list.html
neck-list.html
body-list.html
assembly-list.html
product-list.html
body-master-list.html
neck-master-list.html
```

### 次に整備する画面

#### 主要詳細画面

```text
production-order-detail.html
product-detail.html
body-master-detail.html
neck-master-detail.html
assembly-detail.html
guitar-detail.html
```

#### 登録・編集フォーム

```text
production-order-form.html
product-form.html
body-master-form.html
neck-master-form.html
body-form.html
neck-form.html
assembly-form.html
```

#### 工程フォーム

```text
process-start-form.html
process-end-form.html
body-process-start-form.html
body-process-end-form.html
neck-process-start-form.html
neck-process-end-form.html
```

#### 履歴画面

```text
history-list.html
body-process-history.html
neck-process-history.html
```

### フォーム用共通CSS候補

```text
form-container
form-section
form-group
form-label
form-input
form-select
form-help
form-error
form-actions
detail-card
detail-grid
detail-label
detail-value
```

---

## 20. 今後のロードマップ

### Phase 1: 主要UI・CRUD基盤の完成

目的:

- 現在の機能をテストしやすくする
- 一覧・詳細・フォームの見た目を統一する
- マスタの誤登録を修正可能にする

実施内容:

1. `production-order-detail.html`共通UI化
2. `production-order-form.html`共通UI化
3. Product / BodyMaster / NeckMaster登録フォーム共通化
4. Product / BodyMaster / NeckMaster編集機能
5. マスタ削除または無効化方針の実装
6. ProductionOrderの発行前編集
7. ProductionOrderの取消
8. 主要詳細画面共通化
9. 工程履歴画面共通化

### Phase 2: 月間計画・日産計画

目的:

- 月産2,000〜2,500本規模をモデル別に管理する
- 毎朝配布される日産指示をシステム化する

実施内容:

1. ProductionOrderを月間・モデル別計画として整理
2. `ProductionSchedule` Entity設計
3. 日産計画登録・一覧・詳細
4. 月間計画から日産割当数を集計
5. 月間残数表示
6. 日産計画の確定・取消
7. 月間計画超過チェック

月間残数は次の複数指標を区別する。

```text
計画数
日産割当済数
未割当数
製造開始数
完成数
完成残数
```

### Phase 3: Body / Neck個体一括発行

目的:

- 一件ずつの個体登録を廃止する
- 日産指示数量に応じて個体を生成する

実施内容:

1. Body / NeckへProductionOrder関連追加
2. 必要に応じてProductionSchedule関連追加
3. DBマイグレーション
4. 日産計画詳細へ「製造個体を発行」追加
5. Body / Neck一括生成
6. 採番ルール整理
7. 二重発行防止
8. 発行後のProduct変更禁止
9. 発行数・工程進捗表示

### Phase 4: 一括工程操作

目的:

- 10本、20本、50本などの作業単位で処理する

実施内容:

1. ProductionSchedule別Body一覧
2. ProductionSchedule別Neck一覧
3. チェックボックス選択
4. 全件選択・解除
5. 一括工程開始
6. 一括工程終了
7. ページング・絞り込み
8. 一括処理Service検証
9. エラー時の全件ロールバック

### Phase 5: 工程別ページ・工程内作業

目的:

- 工程特有の作業手順と実績を管理する

実施内容:

1. 全工程のヒアリング・リストアップ
2. 工程マスタ設計
3. 工程内作業マスタ設計
4. 工程別専用ページ
5. 作業項目チェック
6. 作業開始・終了時刻
7. コメント・測定値入力
8. 作業未完了時の工程終了禁止
9. 工程内作業履歴の保存

### Phase 6: 差し戻し・再作業・品質管理

目的:

- 実際に多発する工程間の出戻りを正確に記録する

実施内容:

1. 差し戻し先工程選択
2. 理由マスタ
3. コメント・画像添付検討
4. 差し戻し履歴
5. 再作業開始・完了
6. 再検査
7. 廃棄・不合格処理
8. 管理者・品質担当による承認

### Phase 7: ユーザー・認証・認可

目的:

- 誰がどのギター・部品・工程に関わったか記録する
- マスタや計画の変更権限を制限する

実施内容:

1. User Entity
2. ログイン画面
3. Spring Security導入
4. ADMIN / WORKERロール
5. URLアクセス制御
6. Serviceメソッド権限制御
7. ロール別メニュー表示
8. 工程実績とUserの関連付け
9. `workerName`からUser参照への移行
10. QUALITY / LEADER / MANAGER追加検討

### Phase 8: トレーサビリティ・分析

目的:

- 個体ごとの製造履歴と品質傾向を確認する

実施内容:

1. Guitarトレーサビリティ画面
2. Body / Neck / Assembly / Guitarの連結履歴
3. 作業者別履歴
4. 工程時間分析
5. 工程別差し戻し率
6. モデル別不良率
7. 原因別不良件数
8. 再作業回数
9. 日別・月別の生産達成率
10. 品質ダッシュボード

### Phase 9: パッケージ・CSS・テスト整理

目的:

- 機能拡大後も保守しやすい状態にする

実施内容:

1. 未使用Controller / import整理
2. 不要DTO整理
3. `style.css`の重複削除
4. 旧汎用`table`, `th`, `td`指定の見直し
5. 未使用ボタンクラス削除
6. 機能単位パッケージへの段階移行
7. Service責務分割
8. 単体テスト追加
9. Service業務ルールテスト
10. Controller / Securityテスト
11. 大量データでの性能確認

---

## 21. 現時点の優先順位

直近は、機能追加に入る前に主要画面を短期間で整える。

```text
1. production-order-detail.html
2. production-order-form.html
3. Product / BodyMaster / NeckMasterフォーム
4. Product / BodyMaster / NeckMaster編集
5. ProductionOrder編集・取消
6. その他の主要詳細画面
7. ProductionSchedule設計
8. 個体一括発行
9. 一括工程操作
10. 工程別ページ・工程内作業
11. 差し戻し・再作業
12. ログイン・権限管理
13. トレーサビリティ・品質分析
14. パッケージ・CSS最終整理
```

認証・認可の実装はPhase 7としているが、マスタ編集機能を実運用に近づける段階では前倒しを検討する。認証導入前は、編集画面を開発・試験用途として扱う。

---

## 22. 次回作業開始地点

まずは予定どおり、次のファイルを共通UIへ移行する。

```text
production-order-detail.html
```

確認したい関連ファイル:

```text
production-order-detail.html
ProductionOrderViewController.java
ProductionOrder.java
ProductionOrderService.java
AssemblyService.java
style.css
```

ProductionOrder詳細画面では、現在の情報に加えて将来次を表示できる構造を意識する。

```text
生産指示番号
Product仕様
計画数量
開始数量
完成数量
日産割当済数
未割当数
Body発行数
Neck発行数
Assembly数
完成率
ステータス
編集・取消操作
日産計画一覧
個体発行操作
```

現時点で未実装の値は表示しないが、後からセクションを追加しやすいカード・グリッド構造にする。

---

## 23. 作業上のルール・希望

- 差分よりファイル全体の完成版を優先する
- コードはそのままコピーして使える形にする
- 一つずつ動作確認しながら進める
- CSS重複に注意し、既存共通クラスを再利用する
- UIは白・黒・赤を中心に統一する
- PCと小画面の両方を確認する
- 長いコードは可能ならファイルとして出力する
- 毎日の終了時にMarkdown形式の引き継ぎメモを作成する
- 翌日は新しいチャットに引き継ぎメモを添付して再開する
- `zl`と入力された場合は`→`の意味として解釈する

---

## 24. 次回チャット開始用ショートメモ

```text
Guitar MES開発を継続します。

2026-08-26時点で主要一覧画面の共通UI化は完了しています。

完了:
- Dashboard
- ProductionOrder一覧
- Guitar一覧
- Neck一覧
- Body一覧
- Assembly一覧
- Product一覧
- BodyMaster一覧
- NeckMaster一覧

現在の正式フローはProductionOrder中心で、Assembly時にGuitarを自動生成します。

今後は次を追加する方針です。
- 月間計画と日産計画
- Body / Neck個体一括発行
- チェックボックスによる一括工程操作
- 工程別ページと工程内作業
- 差し戻し・再作業履歴
- ADMIN / WORKERのログイン・権限管理
- 作業者トレーサビリティ
- 品質分析

CRUDはマスタ編集を優先し、実績データは原則物理削除しません。
ProductionOrderは発行前のみ編集可、発行後は変更制限・取消運用とします。

パッケージは当面レイヤー別構成を維持し、新規の日産計画機能から機能単位構成を試します。
DTOは入力検証、一括処理、複数Entity表示、API契約に必要な場合だけ作ります。

次の作業はproduction-order-detail.htmlの共通UI化から開始します。
```
