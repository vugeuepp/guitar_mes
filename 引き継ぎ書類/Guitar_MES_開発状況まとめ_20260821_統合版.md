# Guitar MES 開発状況まとめ（統合版）

**更新日:** 2026/08/21  
**開発フェーズ:** Phase 2 完了／Phase 3 着手前

---

## 1. この文書の目的

この文書は、Guitar MESの以下を次回以降の開発へ引き継ぐための統合メモである。

- 現在までの実装内容
- 現在のソース構成
- 現行アーキテクチャ
- 旧設計と新設計の混在状況
- 既知の課題・技術的負債
- 次フェーズの設計方針
- 今後の実装順序

---

## 2. 開発フェーズ

```text
Phase 1：ギター中心MES
    ↓
Phase 2：Body・Neck独立工程管理 ← 現在ここまで完了
    ↓
Phase 3：ProductionOrder導入・Guitar生成タイミング変更 ← 次回着手
    ↓
Phase 4：Guitar工程再構築
    ↓
Phase 5：UI・用語・画面構成の統一
    ↓
Phase 6：実運用向け拡張
```

---

## 3. 実装済み機能

### 3.1 マスタ管理

- Productマスタ
- BodyMaster
- NeckMaster
- ManufacturingProcess
- Fenderスペック表を基にしたBodyMaster・NeckMasterデータ投入

### 3.2 個体管理と自動採番

| 対象 | 形式 | 例 |
|---|---|---|
| Guitar | `DY + 年下2桁 + 4桁連番` | `DY260001` |
| Body | `DB + 年下2桁 + 4桁連番` | `DB260001` |
| Neck | `DN + 年下2桁 + 4桁連番` | `DN260001` |

Body・Neckは登録画面からシリアル番号入力を廃止し、Serviceで自動採番している。

### 3.3 登録画面の段階選択

#### Body登録

```text
シリーズ
↓
モデル
↓
カラー
↓
BodyMaster特定
```

#### Neck登録

```text
シリーズ
↓
モデル
↓
指板材
↓
NeckMaster特定
```

JavaScriptで選択肢を段階的に絞り込み、最終的にhidden項目へMaster IDを設定する。

### 3.4 Body工程管理

```text
塗装後検品
├─ 合格
│  ↓
│  パーツ付け
│  ↓
│  組立可能
├─ 手直し
│  ↓
│  バフがけ
│  ↓
│  塗装後検品へ戻る
└─ 不合格
   ↓
   製造終了
```

実装内容：

- 工程開始・終了
- 実施中工程の二重開始防止
- 現在工程以外を開始できない制御
- 塗装後検品の合格・手直し・不合格分岐
- バフがけ完了後の再検品ループ
- パーツ付け完了後の組立可能化
- 作業者・開始日時・終了日時・結果・備考の履歴保存
- 状態に応じた開始／終了ボタンの出し分け
- BusinessExceptionを元画面へ戻して赤文字表示する仕組み

### 3.5 Neck工程管理

```text
PLEK
↓
フレット擦り合わせ・ナット手成形
↓
ネックパーツ付け
↓
組立可能
```

NG時：

```text
PLEK または フレット擦り合わせ・ナット手成形
↓
NG
↓
塗装前工程へ差し戻し
```

実装内容：

- 工程開始・終了
- 二重開始防止
- 現在工程以外を開始できない制御
- PLEKと擦り合わせ工程で完了／NGを選択
- NG時は備考への差し戻し理由入力を必須化
- ネックパーツ付け完了後の組立可能化
- 差し戻しネックの再開防止
- 状態に応じた開始／終了ボタンの出し分け

### 3.6 工程履歴

#### Body工程履歴

- 履歴ID
- 工程名
- 結果
- 作業者
- 開始日時
- 終了日時
- 作業時間
- 備考

#### Neck工程履歴

- 履歴ID
- 工程名
- 結果
- 作業者
- 開始日時
- 終了日時
- 作業時間
- 備考
- NG差し戻し理由

### 3.7 Assembly候補制御

ネック取付登録画面では次の部材だけを候補表示する。

```text
Body.status = AVAILABLE
Neck.status = AVAILABLE
```

以下は候補に表示しない。

```text
WAITING
WAITING_INSPECTION
WORKING
REWORK
RETURNED
REJECTED
ASSEMBLED
```

### 3.8 工程時間分析

独立した工程時間分析画面を実装済み。

- Body工程別平均時間・完了件数
- Neck工程別平均時間・完了件数
- Guitar工程別平均時間
- 実施中工程は平均計算から除外
- 実績0件の工程も表示

### 3.9 ダッシュボード

実装済み表示：

- 総ギター数
- 仕掛中
- 完成数
- 実施中工程数
- 完成率
- 使用可能ネック数
- 使用可能ボディ数
- Guitar工程別平均作業時間
- Guitar工程別状況
- Body状態別件数カード
- Neck状態別件数カード
- 現在のギター一覧

---

## 4. 現在のステータスコード

### 4.1 Body

```text
WAITING_INSPECTION
WAITING
WORKING
REWORK
AVAILABLE
ASSEMBLED
REJECTED
```

### 4.2 Neck

```text
WAITING
WORKING
AVAILABLE
RETURNED
ASSEMBLED
REJECTED
```

### 4.3 工程結果

```text
COMPLETED
PASSED
REWORK
REJECTED
NG
```

内部コードは英語でDBへ保存し、画面側では日本語へ変換する方針。

---

## 5. 現在の主要ファイル構成

### 5.1 Entity

```text
entity/
├─ Product.java
├─ BodyMaster.java
├─ NeckMaster.java
├─ Body.java
├─ Neck.java
├─ Guitar.java
├─ Assembly.java
├─ ManufacturingProcess.java
├─ ProcessHistory.java
├─ BodyProcessHistory.java
└─ NeckProcessHistory.java
```

### 5.2 Repository

```text
repository/
├─ ProductRepository.java
├─ BodyMasterRepository.java
├─ NeckMasterRepository.java
├─ BodyRepository.java
├─ NeckRepository.java
├─ GuitarRepository.java
├─ AssemblyRepository.java
├─ ManufacturingProcessRepository.java
├─ ProcessHistoryRepository.java
├─ BodyProcessHistoryRepository.java
└─ NeckProcessHistoryRepository.java
```

### 5.3 Service

```text
service/
├─ ProductService.java
├─ BodyMasterService.java
├─ NeckMasterService.java
├─ BodyService.java
├─ NeckService.java
├─ GuitarService.java
├─ AssemblyService.java
├─ ProcessService.java
├─ BodyProcessService.java
└─ NeckProcessService.java
```

### 5.4 View Controller

```text
controller/view/
├─ HomeController.java
├─ ProductViewController.java
├─ BodyMasterController.java
├─ NeckMasterController.java
├─ BodyViewController.java
├─ NeckViewController.java
├─ GuitarViewController.java
├─ AssemblyViewController.java
├─ ProcessViewController.java
├─ BodyProcessViewController.java
├─ NeckProcessViewController.java
└─ ProcessAnalysisViewController.java
```

### 5.5 DTO

```text
dto/
├─ GuitarCreateRequest.java
├─ GuitarUpdateRequest.java
├─ GuitarProgressResponse.java
├─ BodyCreateRequest.java
├─ NeckCreateRequest.java
├─ AssemblyCreateRequest.java
├─ AssemblyResponse.java
├─ ProcessStartRequest.java
├─ ProcessEndRequest.java
├─ ProcessHistoryResponse.java
├─ ProcessStatusResponse.java
├─ ProcessCountResponse.java
├─ ProcessAverageTimeResponse.java
├─ BodyProcessHistoryResponse.java
├─ NeckProcessHistoryResponse.java
├─ ComponentProcessAverageTimeResponse.java
├─ ComponentStatusCountResponse.java
└─ ErrorResponse.java
```

### 5.6 Common

```text
common/
├─ DateTimeFormatterUtil.java
├─ ProcessConstants.java
├─ ProcessStatusConstants.java
├─ StatusConstants.java
├─ ProcessTargetConstants.java
├─ ProcessResultConstants.java
├─ BodyProcessConstants.java
└─ NeckProcessConstants.java
```

### 5.7 Templates

```text
templates/
├─ home.html
├─ product-form.html
├─ product-list.html
├─ product-detail.html
├─ body-master-form.html
├─ body-master-list.html
├─ body-master-detail.html
├─ neck-master-form.html
├─ neck-master-list.html
├─ neck-master-detail.html
├─ body-form.html
├─ body-list.html
├─ neck-form.html
├─ neck-list.html
├─ guitar-form.html
├─ guitar-list.html
├─ guitar-detail.html
├─ assembly-form.html
├─ assembly-list.html
├─ assembly-detail.html
├─ process-start-form.html
├─ process-end-form.html
├─ history-list.html
├─ body-process-start-form.html
├─ body-process-end-form.html
├─ body-process-history.html
├─ neck-process-start-form.html
├─ neck-process-end-form.html
├─ neck-process-history.html
├─ process-analysis.html
└─ fragments/
   ├─ header.html
   └─ menu.html
```

### 5.8 Static

```text
static/css/style.css
```

---

## 6. 現在のアーキテクチャ評価

現在は次の2世代が混在している。

### 第1世代：Guitar中心MES

```text
Guitar
↓
ProcessHistory
↓
ProcessService
↓
ProcessViewController
```

関連ファイル：

```text
ProcessHistory.java
ProcessHistoryRepository.java
ProcessService.java
ProcessViewController.java
process-start-form.html
process-end-form.html
history-list.html
```

### 第2世代：Body・Neck独立工程管理MES

```text
Body
├─ BodyProcessHistory
├─ BodyProcessService
└─ BodyProcessViewController

Neck
├─ NeckProcessHistory
├─ NeckProcessService
└─ NeckProcessViewController
```

Body・Neck側は、新しい製造フローに沿った独立工程管理として成立している。

---

## 7. 現在判明している課題

### 7.1 表記揺れ

```text
使用可能／組立待ち／組立可能
製造終了／不合格
Home／ダッシュボード
組立実績／組込実績／ネック取付実績
製品管理／製品マスタ管理
```

### 7.2 Guitar工程が旧設計

現在のGuitar工程：

```text
塗装検品
↓
ネック取付
↓
パーツ取付
↓
調音
↓
最終検品
```

問題点：

- 塗装検品はBody工程へ移行済み
- ネック取付前にGuitar個体が存在している
- Body・Neckが未装着でもGuitar工程を開始できる
- Guitar詳細に「まだネック取付登録されていません」が存在する

### 7.3 Guitar生成タイミング

現在：

```text
Guitarを先に生成
↓
後からAssemblyを登録
```

目標：

```text
Body完成
＋
Neck完成
↓
ネック取付
↓
Guitar生成
＋
Assembly保存
```

### 7.4 Assembly登録導線

現在：

```text
組立実績一覧
↓
ネック取付登録
```

目標：

```text
ProductionOrder
↓
ネック取付
↓
Assemblyを自動保存
```

Assembly一覧・詳細はトレーサビリティ用の閲覧画面として残す。

### 7.5 旧Guitar工程関連の技術的負債

今後の整理対象：

```text
ProcessHistory.java
ProcessHistoryRepository.java
ProcessService.java
ProcessViewController.java
process-start-form.html
process-end-form.html
history-list.html
```

ただし、Phase 3・4完了までは削除しない。新しいGuitar工程履歴へ移行後に整理する。

---

## 8. 次フェーズ：ProductionOrder

### 8.1 目的

ProductionOrderは「どの製品を何台生産するか」を管理し、Guitar個体とは分離する。

```text
ProductionOrder
= 生産計画・生産指示

Guitar
= ネックとボディの取付後に成立した製品個体
```

### 8.2 追加予定Entity

```text
ProductionOrder
```

予定フィールド：

```text
id
orderNo
product
plannedQuantity
startedQuantity
completedQuantity
plannedStartDate
dueDate
status
```

### 8.3 追加予定ファイル

```text
entity/ProductionOrder.java
repository/ProductionOrderRepository.java
service/ProductionOrderService.java
controller/view/ProductionOrderViewController.java
dto/ProductionOrderCreateRequest.java
dto/ProductionOrderResponse.java
templates/production-order-list.html
templates/production-order-form.html
templates/production-order-detail.html
```

---

## 9. 新しい製造フロー

```text
ProductionOrder
├─ Body製造
│  └─ AVAILABLE（組立可能）
├─ Neck製造
│  └─ AVAILABLE（組立可能）
└─ ネック取付
   ├─ Guitar生成
   ├─ DYシリアル採番
   ├─ Assembly保存
   ├─ Body → ASSEMBLED
   ├─ Neck → ASSEMBLED
   └─ ProductionOrder.startedQuantity更新
      ↓
   Guitar工程
      ↓
   完成
      ↓
   ProductionOrder.completedQuantity更新
```

---

## 10. ネック取付時のトランザクション

ネック取付処理は一つのServiceメソッド内で同時に行う。

```text
1. ProductionOrder取得
2. Product取得・整合性確認
3. AVAILABLEのBody取得
4. AVAILABLEのNeck取得
5. Body・NeckとProductの適合性確認
6. Guitar新規生成
7. DYシリアル自動採番
8. Assembly実績保存
9. Body.statusをASSEMBLEDへ更新
10. Neck.statusをASSEMBLEDへ更新
11. Guitarの最初の工程を設定
12. ProductionOrder.startedQuantityを加算
```

`@Transactional`で処理し、途中失敗時は全体をロールバックする。

---

## 11. Guitar工程の再設計

### 現在

```text
塗装検品
↓
ネック取付
↓
パーツ取付
↓
調音
↓
最終検品
```

### 推奨

```text
【Guitar生成イベント】
ネック取付
↓
Guitar生成・Assembly保存

【Guitar工程】
ギターパーツ取付
↓
調整・調音
↓
最終検品
↓
完成
```

変更内容：

- 塗装検品をGuitar工程から削除
- ネック取付をGuitar工程から外し、Guitar生成イベントに変更
- パーツ取付を「ギターパーツ取付」へ明確化
- 調音を「調整・調音」へ統一
- 完成時にProductionOrder.completedQuantityを更新

---

## 12. Assemblyの最終的な位置付け

```text
Assembly
= ネック取付実績
```

記録内容：

```text
Guitar
Body
Neck
作業者
取付日時
ProductionOrder（将来追加候補）
```

画面方針：

- 一覧・詳細は残す
- 独立した新規登録導線は削除または非表示
- 登録はProductionOrder経由のネック取付処理のみ
- 画面名称は「ネック取付実績一覧」「ネック取付実績詳細」へ統一予定

---

## 13. UI・用語統一フェーズ

ProductionOrder導入とGuitar工程再設計後に実施する。

### 13.1 ステータス表示統一案

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

### 13.2 メニュー・画面名称統一案

```text
Home               → ダッシュボード
製品管理           → 製品マスタ管理
組立実績           → ネック取付実績
組込実績一覧       → ネック取付実績一覧
Assembly詳細       → ネック取付実績詳細
工程別状況         → ギター工程別状況
工程別平均作業時間 → ギター工程別平均作業時間
```

### 13.3 表示Helper

追加予定：

```text
StatusDisplayHelper
```

役割：

- 内部ステータスから日本語表示名を取得
- ステータスに対応するCSSクラスを取得
- Body・Neck・履歴・ダッシュボードの表記を一元化

Thymeleaf利用イメージ：

```html
<span class="status-badge"
      th:classappend="${@statusDisplay.getCssClass(body.status)}"
      th:text="${@statusDisplay.getLabel(body.status)}">
</span>
```

---

## 14. 優先順位

### 優先度A：ProductionOrder

1. ProductionOrder Entity
2. Repository
3. Service
4. 生産計画登録画面
5. 生産計画一覧画面
6. 生産計画詳細画面
7. 生産指示番号の自動採番

### 優先度A：ネック取付フロー再構築

1. ProductionOrder詳細からネック取付画面へ遷移
2. Productに適合するAVAILABLEのBody・Neckを候補表示
3. ネック取付時にGuitar生成
4. DYシリアル採番
5. Assembly保存
6. Body・NeckをASSEMBLEDへ更新
7. ProductionOrder.startedQuantity更新
8. Assembly独立登録リンクを除去

### 優先度B：Guitar工程再構築

1. Guitar工程マスタ整理
2. Guitar生成直後の最初の工程を設定
3. Guitar工程履歴を新設計へ移行
4. 完成時にProductionOrder.completedQuantity更新
5. 旧Guitar工程関連処理の整理

### 優先度C：UI・用語統一

1. StatusDisplayHelper
2. 一覧・詳細・履歴の表示統一
3. ダッシュボード整理
4. メニュー名統一
5. Assembly名称統一
6. ボタンとカードのデザイン統一
7. 空データ・エラー・成功メッセージの統一

---

## 15. 次回開始時の確認事項

次回は以下から開始する。

```text
ProductionOrder Entity設計
```

実装前に確認するファイル：

```text
Product.java
Guitar.java
Assembly.java
GuitarService.java
AssemblyService.java
GuitarRepository.java
AssemblyRepository.java
GuitarViewController.java
AssemblyViewController.java
DataLoader.java
application.properties
```

確認する設計事項：

- ProductionOrderとProductの関連
- GuitarとProductionOrderの関連
- AssemblyにProductionOrderを保持するか
- ProductとBodyMaster・NeckMasterの適合関係
- 生産指示番号の採番形式
- startedQuantity／completedQuantityの更新タイミング
- 既存Guitar・Assemblyテストデータの扱い

---

## 16. 現在地

```text
【完了】
マスタ管理
Body個体管理
Neck個体管理
Body工程管理
Neck工程管理
Body工程履歴
Neck工程履歴
工程時間分析
状態別ダッシュボード
AVAILABLEのみAssembly候補
ネック取付実績

━━━━━━━━━━━━━━━━━━━━
【次フェーズ】
ProductionOrder
↓
ネック取付
↓
Guitar生成
↓
Assembly自動保存
↓
Guitar工程再構築

━━━━━━━━━━━━━━━━━━━━
【その後】
UI統一
用語統一
ダッシュボード再構成
旧Guitar工程関連の整理
```

---

## 17. 要約

現在のGuitar MESは、Body・Neckの独立工程管理、履歴、分析、状態別ダッシュボードまで完成している。

一方、Guitar側はPhase 1の設計が残っており、Guitar個体がネック取付前から存在する点と、Assembly登録が独立導線になっている点が現在の最大課題である。

次フェーズではProductionOrderを導入し、ネック取付時にGuitar生成・DY採番・Assembly保存・Body／NeckのASSEMBLED更新を同一トランザクションで実行する。これにより、生産計画・部材製造・ネック取付・Guitar工程の責務を明確に分離する。

その後、Guitar工程をネック取付後の工程だけに再構築し、最後にUI・用語・画面名称を全体統一する。
