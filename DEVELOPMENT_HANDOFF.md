# Guitar MES Development Handoff

Updated: 2026-09-10

## Repository State

- Repository: `vugeuepp/guitar_mes`
- Current working branch: `chore/ai-development-workflow`
- Current base: `main`
- Main HEAD at branch creation: `f4fa0fe9cdc35edb3808b6c56939a40811ff26bd`

## Current State

Phase 5C completed and merged into main.

Latest verified test results:

- Normal tests: 339 passed, 0 failures, 0 errors, 0 skipped
- E2E tests: 26 passed, 0 failures, 0 errors, 0 skipped

Recent completion summary for Phase 5C:

- timestamps
- sorting
- pagination
- performance improvements
- concurrency control
- current-page bulk selection
- datetime display

## Next Major Roadmap Candidate

Phase 6 - 工程別専用ページ・工程内作業

参照ロードマップは2026-09-02時点の文書です。Phase 6を無条件に次の確定タスクとして扱わず、実装開始前に次を照合します。

- actual Git state
- latest roadmap / development documents
- user intent

## AI Responsibilities

- ChatGPT: planning / design / GitHub review / delegation / completion judgment
- Codex: repo-aware implementation / investigation / targeted tests
- Copilot: Bundle-only implementation / targeted validation
- User / Mac: full normal test / full E2E / manual UI / commit / push

## Temporary Constraints

- Codex is unavailable until 04:55 JST on 2026-09-10.
- Until that time, do not delegate implementation to Codex.
- Copilot may be used only with a task-specific Bundle.
- ChatGPT may continue planning, GitHub review, document preparation, Bundle preparation, and Copilot delegation.

このセクションは恒久情報ではありません。制約が解除または変更されたら、更新または削除します。

## New Chat Startup

新しいChatGPTチャットでは、次を順に確認して照合してから次の作業を提案します。

1. `DEVELOPMENT_HANDOFF.md`
2. `AGENTS.md`
3. actual GitHub / current branch HEAD

このHandoffに書かれたbranch、HEAD、テスト件数、Temporary Constraintsを永続的な真実として扱いません。actual Git状態と矛盾する場合は、現在の実状態を優先します。

ChatGPTチャットが長くなった場合や、Phase・大きな作業単位の区切りでは、新しいチャットへ切り替えられます。Phase途中でも会話が重くなった場合は、このHandoffを更新して移行できます。新しいチャットは過去チャットの完全な記憶を前提にしません。
