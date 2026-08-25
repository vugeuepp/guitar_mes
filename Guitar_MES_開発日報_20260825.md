# Guitar MES 開発日報

- 日付: 2026-08-25
- プロジェクト: Guitar Manufacturing Execution System (Guitar MES)
- 技術スタック: Java / Spring Boot / Thymeleaf / PostgreSQL

---

# 本日の成果

本日は主に一覧画面のUI共通化を実施した。

共通ヘッダー・ナビゲーション導入後の画面統一作業を進め、主要な一覧画面について共通デザインへの移行が完了した。

---

# 実施内容

## 1. ネック管理一覧

対象ファイル

```text
neck-list.html
style.css
```

実施内容

- page-container導入
- page-toolbar導入
- 新規ネック登録ボタン共通化
- data-table化
- table-container化
- status-badge対応
- 工程開始ボタン共通化
- 工程終了ボタン共通化
- 工程履歴ボタン共通化
- empty-state対応
- レスポンシブ対応
- 閉じタグ不足修正

表示ポリシー

```html
${neck.neckMaster.modelName}
```

を優先表示。

旧

```java
neck.modelName
```

は移行用として維持。

---

## 2. ボディ管理一覧

対象ファイル

```text
body-list.html
style.css
```

実施内容

- page-container導入
- page-toolbar導入
- 新規ボディ登録ボタン追加
- data-table化
- BodyMaster表示対応
- status-badge対応
- 工程開始ボタン共通化
- 工程終了ボタン共通化
- 工程履歴ボタン共通化
- empty-state対応
- 「操作不可」表示追加

---

## 3. ネック取付実績一覧

対象ファイル

```text
assembly-list.html
style.css
```

実施内容

- 共通一覧UIへ移行
- 生産計画一覧への導線追加
- 詳細ボタン追加
- empty-state対応
- data-table化
- toolbar追加

表示項目

```text
組立ID
ギターシリアル
ネックシリアル
ボディシリアル
作業者
組立日時
詳細
```

---

## 4. 製品マスタ一覧

対象ファイル

```text
product-list.html
style.css
```

実施内容

- toolbar化
- 製品登録ボタン追加
- 検索機能UI整理
- 詳細ボタン追加
- BodyMaster表示追加
- NeckMaster表示追加

製品情報を2段表示化

例

```text
Made in Japan Traditional '50s Stratocaster
SSS
```

---

## 5. ボディマスタ一覧

対象ファイル

```text
body-master-list.html
style.css
```

実施内容

- page-toolbar導入
- ボディマスタ登録ボタン追加
- data-table化
- 詳細ボタン追加
- レスポンシブ対応

表示項目

```text
ID
モデルコード
モデル名
ボディタイプ
材質
カラー
```

---

## 6. ネックマスタ一覧

対象ファイル

```text
neck-master-list.html
style.css
```

実施内容

- page-toolbar導入
- ネックマスタ登録ボタン追加
- data-table化
- 詳細ボタン追加
- レスポンシブ対応

表示項目

```text
ID
モデルコード
モデル名
ネックタイプ
ネック材
指板材
フレット数
スケール
```

---

# 現在の正式製造フロー

```text
ProductionOrder作成
↓
Product選択
↓
Body製造
↓
Neck製造
↓
Assembly実績登録
↓
Guitar自動生成
↓
ギターパーツ取付
↓
調整・調音
↓
最終検品
↓
完成
↓
ProductionOrder.completedQuantity更新
```

---

# 本日時点のUI進捗

## 完了

```text
✅ ダッシュボード

✅ production-order-list.html

✅ guitar-list.html

✅ neck-list.html

✅ body-list.html

✅ assembly-list.html

✅ product-list.html

✅ body-master-list.html

✅ neck-master-list.html
```

## 未着手

### フォーム系

```text
production-order-form.html

product-form.html

body-master-form.html

neck-master-form.html

body-form.html

neck-form.html

assembly-form.html

process-start-form.html

process-end-form.html

body-process-start-form.html

body-process-end-form.html

neck-process-start-form.html

neck-process-end-form.html
```

### 詳細画面系

```text
production-order-detail.html

product-detail.html

body-master-detail.html

neck-master-detail.html

assembly-detail.html

guitar-detail.html
```

### 履歴画面系

```text
history-list.html

body-process-history.html

neck-process-history.html
```

### 最終整理

```text
style.css整理

不要CSS削除

旧UI削除

フォーム共通部品作成

詳細画面共通部品作成
```

---

# 現在のソース構成状況

主要構成

```text
src/main/java/com/example/guitarmes
├─ common
├─ controller
│  ├─ api
│  └─ view
├─ dto
├─ entity
├─ exception
├─ repository
└─ service

src/main/resources
├─ static/css/style.css
└─ templates
    ├─ fragments
    ├─ 一覧画面
    ├─ 詳細画面
    ├─ フォーム画面
    └─ 履歴画面
```

一覧画面系は一通り揃い、今後はフォーム・詳細画面中心の開発フェーズへ移行できる状態となった。

---

# 次回作業開始地点

優先順位

```text
1. production-order-detail.html

2. production-order-form.html

3. product-form.html

4. body-master-form.html

5. neck-master-form.html

6. フォーム共通CSS作成

7. 詳細画面共通化

8. 履歴画面共通化

9. style.css最終整理
```

---

# 次回チャット開始用メモ

Guitar MES開発を継続。

一覧画面の共通UI化は完了済み。

完了画面

- Dashboard
- ProductionOrder一覧
- Guitar一覧
- Neck一覧
- Body一覧
- Assembly一覧
- Product一覧
- BodyMaster一覧
- NeckMaster一覧

現在の正式フローは ProductionOrder 中心。

```text
ProductionOrder
↓
Product
↓
Body / Neck
↓
Assembly
↓
Guitar自動生成
↓
完成
```

次回は

```text
production-order-detail.html
```

の共通UI化から開始する。
