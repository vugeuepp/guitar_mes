# Guitar MES 開発 引き継ぎメモ

- 作成日: 2026-08-25
- 対象プロジェクト: Guitar Manufacturing Execution System (Guitar MES)
- 技術構成: Java / Spring Boot / Thymeleaf / PostgreSQL
- 次チャットの開始地点: **ネック管理一覧 `neck-list.html` の共通UI化**

---

## 1. 現在の正式な製造フロー

旧フローの「Guitarを直接登録してからAssemblyを登録」は廃止済み。

現在の正式フローは以下。

```text
ProductionOrder登録
→ Product（モデル・カラー・指板材）確定
→ 対応するBody / Neckを製造
→ 生産計画詳細からネック取付
→ Assembly実績保存
→ Guitar自動生成
→ ギターパーツ取付
→ 調整・調音
→ 最終検品
→ Guitar完成
→ ProductionOrder.completedQuantity更新
→ 計画数全数完成でProductionOrder = COMPLETED
```

---

## 2. ProductionOrder関連の実装状況

### 完了済み

- `ProductionOrder` Entity / Repository / Service作成
- `t_production_order`テーブル作成済み
- 生産計画の登録・一覧・詳細画面作成済み
- 生産指示番号は `PO + 年下2桁 + 4桁連番`
- ステータス:
  - `PLANNED`
  - `IN_PROGRESS`
  - `COMPLETED`
  - `CANCELLED`

### Product選択UI

生産計画登録画面はJavaScriptによる段階選択済み。

```text
シリーズ
→ モデル
→ カラー
→ 指板材
→ Product ID確定
```

仕様:

- 同一モデル・同一カラーで指板違いがある場合は、指板材をユーザーが選択
- 指板材が1種類だけの場合は自動選択
- 最終確認として、モデル番号・製品名・カラー・指板材を表示
- Controllerへ送信するのは最終的な`productId`

---

## 3. ProductとBodyMaster / NeckMasterの関連

`Product`へ以下を追加済み。

```java
@ManyToOne
@JoinColumn(name = "body_master_id")
private BodyMaster bodyMaster;

@ManyToOne
@JoinColumn(name = "neck_master_id")
private NeckMaster neckMaster;
```

BodyとNeckもそれぞれMasterを参照している。

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

画面だけでなく`AssemblyService`でもMaster ID一致を検証済み。

---

## 4. ネックマスタの指板表記問題

一時、Traditional 60sのRosewood仕様が`Maple Fingerboard`と表示されていた。

原因:

- `neck_master_id`は正しかった
- `m_neck.model_name`と`fingerboard_material`が不整合だった

対応:

- `m_neck.model_name`を`fingerboard_material`に合わせて修正
- `t_neck.model_name`もMaster側へ同期
- 現在は表示とMaster対応とも正常

今後、ネック表示は可能な限り次を使用する。

```html
${neck.neckMaster.modelName}
${neck.neckMaster.fingerboardMaterial}
```

旧`neck.modelName`は移行用フィールドとして残っている。

---

## 5. Assembly / Guitar自動生成

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

1. ProductionOrder取得
2. Neck取得
3. Body取得
4. ProductionOrderの計画上限チェック
5. Body / Neckが`AVAILABLE`か確認
6. ProductとBodyMaster / NeckMasterの適合確認
7. Guitar自動生成
8. Assembly保存
9. Body / Neckを`ASSEMBLED`へ変更
10. ProductionOrder.startedQuantityを1加算
11. ProductionOrder.statusを`IN_PROGRESS`へ変更

### Guitar

`Guitar`へ以下を追加済み。

```java
@ManyToOne
@JoinColumn(name = "production_order_id")
private ProductionOrder productionOrder;
```

Guitarはネック取付完了時に初めて生成される。

```text
シリアル: DY + 年下2桁 + 4桁連番
初期工程: ギターパーツ取付
```

---

## 6. Guitar工程

現行工程:

```text
1. ギターパーツ取付
2. 調整・調音
3. 最終検品
4. 完成
```

旧工程の「塗装検品」「ネック取付」等は現行GUITAR工程から外した。

ネック取付は`Assembly`そのものが実績なので、Guitarの`ProcessHistory`へ重複保存しない。

### 最終検品終了時

- `Guitar.currentProcess = 完成`
- `ProductionOrder.completedQuantity += 1`
- 完成数が計画数に達したら`ProductionOrder.status = COMPLETED`

### ProcessService

現行GUITAR工程のみを対象にするよう再設計済み。

主な内容:

- 工程開始順の検証
- 実施中工程の二重開始防止
- 最終工程終了時のGuitar完成処理
- ProductionOrder完成数更新
- LEGACY_GUITAR履歴を現行判定・平均時間から除外
- 完成数の二重加算防止

`startProcess()`には、ProductionOrder未関連Guitarを拒否する保険を残してもよい。

```java
if (guitar.getProductionOrder() == null) {
    throw new BusinessException(
        "生産計画に関連付けられていない"
        + "ギターでは工程を開始できません。");
}
```

---

## 7. 旧フロー整理

### 削除・停止済み

- 旧Guitar直接登録リンク
- `guitar-form.html`
- `GuitarCreateRequest.java`
- Guitar直接作成POST API
- 旧Assembly独立登録導線
- `DataLoader.java`
- 旧Guitar生成メソッド

`/guitars/new`はProductionOrder一覧へリダイレクトする形で残してもよい。

### DB旧データ

`production_order_id IS NULL`の旧Guitarと関連Assemblyを削除済み。

外部キー制約があるため、削除順はAssembly → Guitarとした。

現在の確認SQL:

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

一度旧データ表示用に`legacyData`を追加したが、旧DBデータ削除後は不要となった。

削除対象:

- `GuitarProgressResponse.legacyData`
- `GuitarService`のlegacy判定
- `guitar-list.html`の「旧データ」「参照のみ」
- `.status-legacy`
- `.table-row-legacy`

`needAssembly`も新フローでは不要。

---

## 8. ステータス表示共通化

### StatusDisplayHelper

Body / Neckの内部ステータスを日本語化。

```text
WAITING             → 工程待ち
WORKING             → 作業中
WAITING_INSPECTION  → 検品待ち
REWORK              → 手直し待ち
AVAILABLE           → 組立可能
RETURNED            → 塗装前工程へ差し戻し
ASSEMBLED           → 組立済み
REJECTED            → 不合格
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
PLANNED      → 計画中
IN_PROGRESS  → 製造中
COMPLETED    → 完了
CANCELLED    → 中止
```

---

## 9. ヘッダー / ナビゲーションUI

### デザイン方針

- 白・黒・赤を基調
- 業務システム向け横ナビ
- 製品サイト風の高級感を参考にするが、既存ブランドロゴ等は複製しない

### header.html

- 独自`GM`マーク
- `Guitar MES`
- `Production Control System`
- ナビゲーションをヘッダー内に統合
- モバイル用ハンバーガーボタン

### menu.html

PC:

```text
ダッシュボード
生産計画
ギター管理
ネック管理
ボディ管理
ネック取付実績
工程時間分析
マスタ管理 ▼
```

900px以下:

- 横ナビを非表示
- `☰`ボタン表示
- クリックで縦メニュー開閉
- マスタ項目は縦に展開

### CSS重複

一度ナビCSSが重複・競合したが、Header / Navigationブロックを整理して解消済み。

旧横スクロール方式は廃止。

```css
@media (max-width: 1100px) {
    .top-nav {
        overflow-x: auto;
    }
}
```

のような古い指定は削除済み。

---

## 10. 共通UIクラス

現在、以下を共通部品として使用している。

### レイアウト

```text
page-container
page-toolbar
page-toolbar-description
page-toolbar-actions
table-container
empty-state
```

### テーブル

```text
data-table
```

### ボタン

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

### 本文レイアウト

```css
.page-container {
    width: 100%;
    max-width: 1600px;
    margin: 0 auto;
    padding: 0 24px 40px;
}
```

列数の多いテーブルは以下で囲む。

```html
<div class="table-container">
    <table class="data-table">
    </table>
</div>
```

### 注意

`style.css`冒頭には汎用の`table`, `th`, `td`も残っているため、最終整理では共通クラスへ寄せたい。

---

## 11. ダッシュボードUI

### 完了済み

- 上部5項目をカード化
  - 総ギター数
  - 製造中
  - 完成
  - 作業中工程
  - 完成率
- 組立可能ネック / ボディをカード化
- Guitar工程別平均作業時間をカード化
- Guitar工程別状況をカード化
- Body / Neck工程状況をカード化
- 現在のGuitar一覧を`data-table`化
- 詳細リンクを黒ボタン化
- 完成行を薄緑で表示
- 本文を`page-container`へ統一
- 多列テーブルのみ内部スクロール

### 旧工程表示問題

`GuitarService#getProcessCounts()`が全Guitarの`currentProcess`を集計していたため、旧`ネック取付`が表示されていた。

旧DBデータ削除後は、正式工程のみ表示される。

---

## 12. 生産計画一覧UI

完了済み。

- `page-container`
- `page-toolbar`
- 赤い「生産計画を登録」ボタン
- `data-table`
- Product情報を2段表示
- 指板材を独立列表示
- 日本語ステータスバッジ
- 詳細を黒ボタン化
- 完成行を薄緑表示
- 0件時は`empty-state`
- モバイル時はテーブル内部スクロール

---

## 13. ギター管理一覧UI

完了済み。

- `page-container`
- `page-toolbar`
- 生産計画一覧への赤ボタン
- `data-table`
- 列幅固定
- 工程開始: 赤
- 工程終了: オレンジ
- 詳細: 黒
- 作業中 / 工程待ち / 完成をバッジ表示
- 完成行を薄緑表示
- 0件時`empty-state`

旧`legacyData`対応は削除する。

### GuitarProgressResponseの最終形

不要:

```text
needAssembly
legacyData
```

必要:

```text
id
serialNo
productName
currentProcess
progressRate
hasRunningProcess
hasNextProcess
```

---

## 14. 現在の作業地点: ネック管理一覧

ユーザーは`neck-list.html`を共通UIへ移行中。

最新のユーザー側HTMLは、途中まで以下が入っている。

- `header('ネック管理一覧')`
- `page-container`
- `page-toolbar`
- `新規ネック登録`を`btn btn-primary`化
- `table-container`
- `data-table neck-management-table`
- `colgroup`
- `thead`

ただし、HTMLの閉じタグが不足しており、完成版をチャットで返そうとした際にメッセージが途中で途切れた。

### 次チャットで最優先に行うこと

**`neck-list.html`完全版を、途中で切れない形で生成する。**

ユーザーは「なるべく全体で欲しい」ため、部分差分ではなく完全版を提供する。

推奨: 長いコードをチャット本文へ直接貼るのではなく、`.html`ファイルとして生成してダウンロード可能にすることも検討する。

### 維持すべきネック一覧のロジック

#### モデル名

```html
<td th:text="${neck.neckMaster != null
        ? neck.neckMaster.modelName
        : neck.modelName}">
</td>
```

#### Master情報

```html
${neck.neckMaster?.neckType}
${neck.neckMaster?.neckMaterial}
${neck.neckMaster?.fingerboardMaterial}
${neck.neckMaster?.fretCount}
${neck.neckMaster?.scale}
```

#### ステータス

```html
<span class="status-badge"
      th:classappend="${@statusDisplay.getCssClass(neck.status)}"
      th:text="${@statusDisplay.getLabel(neck.status)}">
</span>
```

#### 工程操作

```text
WORKING
→ /neck-processes/end/view
→ 工程終了

WAITING
→ /neck-processes/start/view
→ 工程開始

AVAILABLE / RETURNED / ASSEMBLED / REJECTED
→ 操作不可表示
```

#### 履歴

```text
/necks/{id}/process-history
```

### ネック一覧で使用する推奨クラス

```text
neck-management-table
neck-column-id
neck-column-serial
neck-column-model
neck-column-type
neck-column-material
neck-column-fingerboard
neck-column-fret
neck-column-scale
neck-column-process
neck-column-status
neck-column-action
neck-column-history
neck-current-process
status-cell
```

### ネック一覧用CSS案

```css
.neck-management-table {
    min-width: 1400px;
    table-layout: fixed;
}

.neck-column-id {
    width: 5%;
}

.neck-column-serial {
    width: 10%;
}

.neck-column-model {
    width: 22%;
}

.neck-column-type {
    width: 10%;
}

.neck-column-material {
    width: 10%;
}

.neck-column-fingerboard {
    width: 10%;
}

.neck-column-fret {
    width: 7%;
}

.neck-column-scale {
    width: 7%;
}

.neck-column-process {
    width: 10%;
}

.neck-column-status {
    width: 110px;
}

.neck-column-action {
    width: 120px;
}

.neck-column-history {
    width: 100px;
}

.neck-management-table th,
.neck-management-table td {
    overflow: hidden;
    text-overflow: ellipsis;
    vertical-align: middle;
    white-space: nowrap;
}

.neck-current-process {
    color: var(--app-black);
    font-weight: 700;
}

.status-cell {
    text-align: center;
}
```

工程開始ボタン:

```html
<button type="submit"
        class="btn btn-primary">
    工程開始
</button>
```

工程終了ボタン:

```html
<button type="submit"
        class="btn btn-process-end">
    工程終了
</button>
```

履歴ボタン:

```html
<button type="submit"
        class="btn btn-detail">
    工程履歴
</button>
```

### 完成版HTMLで必ず閉じるタグ

```html
</tbody>
</table>
</div>
</main>
</body>
</html>
```

---

## 15. 今後のUI整備順

ネック一覧完了後:

```text
1. ボディ管理一覧
2. ネック取付実績一覧
3. 製品マスタ一覧
4. BodyMaster一覧
5. NeckMaster一覧
6. 登録フォーム
7. 詳細画面
8. 工程履歴画面
9. 全体CSS整理
```

すべて以下の共通構造へ寄せる。

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

## 16. ユーザーの作業上の希望

- 差分よりも**ファイル全体の完成版**を好む
- コードはそのままコピーして使える形が望ましい
- 一つずつ動作確認しながら進めたい
- CSS重複には注意し、既存クラスを再利用したい
- UIは白・黒・赤を中心に統一
- PCと小画面の両方を確認する
- `zl`と入力された場合は`→`の意味として解釈する

---

## 17. 次チャット冒頭で伝えるとよいこと

```text
引き継ぎメモを確認しました。
現在はneck-list.htmlの共通UI化の途中です。
まず、ユーザーが貼った現行HTMLをベースに、
閉じタグ・Thymeleaf条件・工程操作を保持した
完全版neck-list.htmlをファイルで作成します。
```

