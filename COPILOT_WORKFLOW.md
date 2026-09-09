# Copilot Workflow

この文書は、Guitar MESでCopilotを利用する際のtask-specific Bundle前提の専用作業プロトコルです。

## Hard Rule

# NO BUNDLE -> NO IMPLEMENTATION PROMPT

Copilot implementation promptを先に作り、Bundleを後から作ることは禁止します。

## Context Boundary

Copilotが次へアクセスできることを前提にしません。

- local repository
- Git working tree
- GitHub repository
- Bundle外ファイル
- 過去のCopilotチャット

そのタスクのtask-specific Bundleだけを唯一の実装コンテキストとします。

## Source of Truth Rule

Bundle生成前に、何をsource of truthとするか確認します。

- push済み状態を作業基準にする場合: GitHub / latest target branchがsource of truth。
- localに未commit変更があり、その変更を引き継いで作業する場合: local working treeがsource of truth。

local working treeがsource of truthの場合、GitHubから作ったBundleではなく、現在のlocal working treeからBundleを作ります。古いBundleを無条件に再利用しません。source of truthが変わった場合はBundleを作り直します。

## Bundle Roles

Bundle内の各ファイルには、必ず役割を明示します。

```text
===== FILE: path/to/File.java =====
ROLE: EDITABLE
<source>

===== FILE: path/to/Reference.java =====
ROLE: REFERENCE ONLY
<source>
```

- `ROLE: EDITABLE`: 変更可能。
- `ROLE: REFERENCE ONLY`: 参照のみ。変更禁止。
- `ROLE: NEW / EDITABLE`: 新規ファイルについてprompt側で指定可能。

Copilotは `REFERENCE ONLY` のファイルを変更してはいけません。Bundleの役割とpromptの変更可能ファイル指定を一致させます。

## Missing Context Rule

必要情報がBundleに存在しない場合、Copilotは次を守ります。

- 推測しない。
- Bundle外を知っているふりをしない。
- 過去チャットの記憶で補わない。
- 必要なファイルを具体的に要求する。
- 必要な型・メソッド・仕様を具体的に要求する。
- 追加Bundleを受け取ってから続行する。

不足情報がある状態で、架空のAPI、型、メソッド、既存仕様を作りません。

## Required Order

次の順序をHard Ruleとします。

1. source of truth確認
2. task scope決定
3. Bundle作成
4. Bundle内容・ROLE確認
5. Copilot implementation prompt作成
6. Copilot実行
7. Macで適用・diff確認
8. targeted test / validation
9. 必要に応じてUserがfull normal / E2E / manual UI
10. commit / push
11. ChatGPTがGitHub差分レビュー
12. 完了判定

Copilotは実装と変更箇所に必要なtargeted test / targeted validationを担当します。full normal test、full E2E、manual UI verificationはUser / Macが担当します。明示的な理由またはユーザー指示がない限り、Copilotへfull suiteを実行させません。同じコード状態で必要なfull suiteがすでに成功済みなら、意味のない再実行を要求しません。

## Copilot Implementation Prompt Requirements

promptには少なくとも次を明記します。

- task-specific Bundleが唯一の実装コンテキストであること
- source of truth
- task scope
- `ROLE: EDITABLE`、`ROLE: REFERENCE ONLY`、必要なら `ROLE: NEW / EDITABLE`
- 変更可能ファイルと変更禁止ファイル
- Bundle外の型・メソッド・文書・仕様・現在状態を推測しないこと
- 不足時に要求すべき具体的なファイル・型・メソッド・仕様
- targeted test / validationの範囲
- full suite、manual UI、commit、pushの担当
- 作業終了時に報告すべき変更ファイル、変更内容、検証、未実行事項

## Apply, Review, and Completion

Copilotの完了報告だけで実装完了と判断しません。

- User / Macが生成された変更をlocal working treeへ適用する。
- `git diff`、`git status --short`、`git diff --check`相当で対象と不要な空白を確認する。
- 変更内容に応じたtargeted test / validationを確認する。
- 必要なfull normal test / full E2E / manual UIをUser / Macが実行する。
- User / Macがcommit / pushする。
- ChatGPTがGitHub上の実コード差分をレビューする。
- レビューと検証結果を合わせて完了判定する。

## Copilot Chat Session Reset

Copilotチャット履歴を永続的な作業コンテキストとして扱いません。

推奨運用:

```text
1 task = 1 Bundle = 1 Copilot chat
```

次の場合は、新しいCopilotチャットへの切り替えを推奨します。

- 新しい実装タスクへ移るとき
- Bundleを作り直したとき
- source of truthが変わったとき
- commit / push後に別の変更へ進むとき
- 会話が長くなったとき
- 古い指示と新しい指示の混同リスクが出たとき
- CopilotがBundle外の情報を前提にし始めたとき
- Copilotが古いコード状態を前提にし始めたとき

新しいCopilotチャットでは以前のチャット内容を前提にせず、次から作業を再構成します。

1. 現在のtask-specific Bundle
2. 現在のtask-specific prompt

同一タスク内の小さな追加修正は、source of truthとBundleが変わっていなければ同じチャットを継続できます。ただし、会話が長くなった場合は同一タスクでも新しいBundleと新しいチャットへ切り替えられます。

## Document Maintenance

この文書は通常、安定したCopilot利用プロトコルとして扱います。branch、HEAD、Phase、最新テスト件数、Temporary Constraintsなどの時点情報は記録せず、`DEVELOPMENT_HANDOFF.md` で管理します。
