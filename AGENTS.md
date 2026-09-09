# Guitar MES 開発ガイド

このファイルは、リポジトリ全体で常に守る恒久的な開発ルールです。特にCodexなど、リポジトリを直接参照するAIエージェントは毎回確認してください。

現在のPhase、branch、HEAD、最新テスト結果、次の作業、一時的なAI利用制約は `DEVELOPMENT_HANDOFF.md` を参照してください。このファイルには時点依存の進捗情報を原則として記録しません。

## プロジェクト

Guitar MESはギター工場向けの製造実行システムです。Java 17 / Spring Boot / Spring MVC / Spring Data JPA / Thymeleaf / PostgreSQLを使用します。ビルドはMaven Wrapper、テストはJUnit・Mockito・MockMvc・Playwrightです。

- Java: `src/main/java/com/example/guitarmes/`（機能別パッケージ）
- HTML: `src/main/resources/templates/`
- 静的ファイル: `src/main/resources/static/`
- テスト: `src/test/java/com/example/guitarmes/`
- 計画・作業記録: `引き継ぎ書類/`
- 実行コマンド例: `コマンド集.txt`

## 文書の責任

- `AGENTS.md`: 恒久ルール。頻繁に書き換えません。
- `DEVELOPMENT_HANDOFF.md`: 現在地、branch、Phase、latest verification、next candidate、Temporary Constraints。必要に応じて更新します。
- `COPILOT_WORKFLOW.md`: Copilot利用プロトコル。通常は安定した文書として扱います。

同じ時点情報を複数文書へ複製しません。ロードマップは計画、Handoffは時点ごとの記録として扱い、記載された次回候補をユーザーからの新しい実行指示と混同しないでください。

## 作業開始時

1. ユーザーが依頼した範囲と、対象コードに適用される指示を確認する。
2. `git branch --show-current`、`git status --short`、`git diff --check`、`git log --oneline --decorate -5`、`git branch -vv`で状態を確認する。
3. 関連するロードマップ、`DEVELOPMENT_HANDOFF.md`、最新の開発文書を読む。ファイル名だけでなく本文の作業日・改訂日を見る。
4. 文書の進捗と実コード・テスト・Git状態を照合する。古いbranch、HEAD、件数、commit状況を現在の事実として扱わない。ローカルのorigin参照と最新のリモート照会も区別する。
5. 既存の未commit変更を保全し、依頼と無関係な変更を混ぜない。

## AIとユーザーの役割

### ChatGPT

- 設計、仕様整理、計画
- push済みGitHubコードの確認
- diffレビュー
- テスト方針
- Codex / Copilotへの作業分割・指示
- 完了判定

### Codex

- local repositoryを直接参照する実装
- repository横断調査
- 大規模・複数ファイル変更
- 変更箇所に必要なtargeted testとtargeted validation

### Copilot

- task-specific Bundleを与えられた小規模から中規模の具体的作業
- Bundle内で `ROLE: EDITABLE` またはpromptで `ROLE: NEW / EDITABLE` と指定されたファイルだけの変更
- 変更箇所に必要なtargeted testとtargeted validation

Copilotの詳細な利用手順は `COPILOT_WORKFLOW.md` に従います。

### User / Mac

- 生成された変更の適用
- local diff確認
- full normal test
- full E2E
- manual UI verification
- commit
- push

## ChatGPT Delegation Gate

ChatGPTは実装担当を決める前に、次の4点を確認します。

1. **Who**: Codex / Copilot / Userの誰が担当するか。
2. **Source of Truth**: push済みGitHubか、local working tree / uncommitted changesか。
3. **Scope**: implementation / targeted test / full normal test / full E2E / manual UI / commit / pushの担当範囲。
4. **Availability**: 担当AIが現在利用可能か。`DEVELOPMENT_HANDOFF.md` のTemporary Constraintsを確認する。

利用できないAIを実装担当として提案しません。Copilotを使用する場合は、implementation promptより先にtask-specific Bundleを作成します。

## Codex Scope Control

Codexは実装開始前に次を行います。

1. 現在のrepository状態を確認する。
2. ユーザーから依頼されたscopeを特定する。
3. 変更予定範囲を決める。
4. そのscope内で実装する。

調査中に関連問題を見つけても、重大なデータ整合性・安全性問題など直ちに扱う必要がある場合を除き、勝手にscopeを大きく広げません。scope拡張が必要な場合は、理由・対象・影響範囲を報告してから進めることを基本とします。

既存コードの命名、import形式、整形、記述スタイルに合わせます。不要な完全修飾名や独自の整形を持ち込みません。

## 業務ルール

- 正式な流れは生産計画 → 日産計画 → Body・Neck発行 → 部材工程 → Assembly → Guitar工程 → 完成です。
- Guitarは生産計画登録時ではなく、Body・Neckを組み合わせたAssembly登録時に生成します。
- 検証・計算・保存・トランザクション境界はServiceに置きます。画面の非活性制御だけに頼らずServiceで再検証します。
- 一括処理は全件検証後に実行し、1件でも不正なら全件ロールバックします。数量超過、二重使用、仕様不一致、無効な計画や状態を防ぎます。
- 同時操作を考慮し、更新直前にも状態を再検証します。

## 画面・検索

- Guitar・Body・Neckで操作感を統一し、一括開始では工程を先に選びます。工程変更時に選択を解除し、全件選択は処理可能な対象だけに限定します。
- 利用者向け表示は内部IDではなく、シリアル番号・注文番号・工程名などを使います。
- 検索はGETパラメーターで再現可能にし、複数条件はAND検索とします。条件保持、件数表示、クリア、検索0件と未登録の区別を維持します。

## DB

- `spring.jpa.hibernate.ddl-auto=validate`を維持し、自動スキーマ更新を使いません。
- DB変更時は適用SQL・確認SQL・ロールバックSQLを揃え、既存データの移行も明示します。
- 更新系E2Eは専用DB `guitar_mes_e2e`で実行します。起動先アプリとテストのDB接続先を実行前に確認します。

## テスト・検証

### 責任分担

「何を検証するか」と「誰がfull suiteを実行するか」を区別します。

- Codex / Copilot: 実装、変更箇所に必要なtargeted test、targeted validation。
- User / Mac: full normal test、full E2E、manual UI verification。

明示的な理由またはユーザー指示がない限り、CodexまたはCopilotへfull suiteを実行させません。これはcommit前に必要なfull testを省略する意味ではなく、必要なfull normal test / full E2EはUser / Macが実行します。

### Mac側テストコマンドの提示形式

ChatGPT・Copilot・Codexがユーザーへ提示するMac側テストコマンドは、以下の標準形式に統一します。Maven Wrapperと `-f "$PROJECT/pom.xml"` を使用し、`-Dstyle.color=always` でターミナルのカラー表示を維持します。

通常テスト全件の例:

```bash
(
  set +e
  set -o pipefail
  PROJECT="/Users/naokiyamada/git/guitar-mes/guitar_mes"
  RUN_ID=$(date +%Y%m%d-%H%M%S)
  DAY=${RUN_ID%-*}
  LOGDIR="$PROJECT/logs/test-logs/$DAY"
  LOGFILE="$LOGDIR/console-$RUN_ID.log"
  cd "$PROJECT" || exit 1
  mkdir -p "$LOGDIR" || exit 1
  "$PROJECT/mvnw" -f "$PROJECT/pom.xml" -Dstyle.color=always test 2>&1 | tee "$LOGFILE"
  STATUS=$?
  printf '終了コード: %s\nログ: %s\n' "$STATUS" "$LOGFILE"
  if [ "$STATUS" -eq 0 ]; then
    printf 'テスト成功\n'
  else
    printf 'テスト失敗（ログ保存の失敗も含む）。ログを確認してください。\n'
  fi
  exit "$STATUS"
)
```

- `DAY`・`RUN_ID`・`LOGDIR`・`LOGFILE`を使用し、ログは `logs/test-logs/YYYYMMDD/console-YYYYMMDD-HHMMSS.log` に保存します。各実行で日時を取り直し、同名ログを上書きしません。
- `tee`でターミナル表示とログ保存を両立し、`set -o pipefail`でMavenの失敗を検出します。パイプライン直後に `STATUS=$?` を取得し、終了コード・ログパス・日本語の成功／失敗メッセージを表示します。
- targeted testは標準形式のMaven呼び出しに `-Dtest=対象クラス名` を追加します。複数クラスはカンマ区切りにします。
- E2Eは `-Dplaywright.headless=true` を追加し、E2E全件はさらに `-Dtest='*E2E'` を指定します。対象E2Eだけの場合は対象クラスを `-Dtest=...` で指定します。ブラウザ対象アプリのURLが既定と異なる場合は `-De2e.base.url=...` も明示します。
- 通常テスト全件とE2E全件は別々の実行・ログにします。共通部分を省略したMaven呼び出しだけを実行用の標準コマンドとして提示しません。

### E2Eの実行環境

- ブラウザ対象アプリをe2e profileで起動し、アプリとテストセットアップの両方が同じ専用DB `guitar_mes_e2e` を使用することを確認します。Dev profileのアプリを起動したままE2Eを実行しません。
- Eclipse運用では「Devを停止 → GuitarMES - E2Eを起動 → E2E実行 → E2E停止 → 必要ならDevへ戻す」を基本とします。

### 検証範囲と結果の扱い

- 実行前に既存のPlaywrightTestBase、E2E設定、`コマンド集.txt`で起動方法・前提条件を確認します。
- 機能変更に応じてService・MockMvcテストを更新・実行します。HTML変更時は対応するPlaywright E2Eも更新・実行します。
- E2Eはテスト自身が作成したデータを識別して検証・削除します。一覧先頭行、固定件数、一覧順に依存しません。
- 機能変更のcommit前には通常テスト全件、主要業務フロー変更時にはE2E全件を実行します。検索横展開では対象検索E2Eに加えて既存の一括処理E2Eも確認します。
- 同じコード状態で必要なfull suiteがUser / Macですでに成功済みなら、その結果を確認して扱い、意味のない再実行を要求しません。コード変更、失敗、未確認の影響範囲など再実行が必要な場合は理由を示します。
- 誰がどの環境で実行した検証かを明確に区別して報告します。
- 文書のみの変更は参照先・内容・差分を確認します。コードテストを実行したことにはしません。
- 未実行、失敗、環境上実行できない検証は理由とともに報告します。過去の成功を今回の結果として報告しません。

## 変更の提供と終了時

- local作業では最新の実ファイルを修正基準にします。Copilot利用時は `COPILOT_WORKFLOW.md` に従います。
- パッチを配布する場合は `git apply` 対応の標準unified diffを使い、`git apply --check`で確認します。`*** Begin Patch`形式を配布用 `.patch` にしません。
- ZIP提供が必要な大規模変更では、既存運用に合わせて `templates`・`src`・`test` に分け、各区分へ対象ファイルを直接配置します。
- 変更後は `git diff --check`、`git status --short`、対象差分を確認します。
- commitする場合は日本語で変更目的を記述し、対象ファイルを確認します。
- Handoffには現在地、Phase、変更内容、検証結果、未実行事項、残課題、確認時点のGit状態、次の候補作業、Temporary Constraintsを簡潔に残します。

## ChatGPT Chat Session Reset

ChatGPTも長大なチャット履歴へ依存しません。チャットが長くなった場合、Phaseや大きな作業単位の区切り、または会話が重くなった場合は、Phase途中でも `DEVELOPMENT_HANDOFF.md` を更新して新しいチャットへ移行できます。

新しいチャットでは過去チャットの完全な記憶を前提にせず、次を照合して現在地を再構築します。

1. `DEVELOPMENT_HANDOFF.md`
2. `AGENTS.md`
3. actual GitHub / current branch state
