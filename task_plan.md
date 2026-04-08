# Task Plan

## Goal

Fix the validated `score` module review findings on the current branch:
- close broken authorization / IDOR paths
- unify `score` controller responses on `ApiResponse`
- remove confirmed N+1 fallbacks and full-table counting
- refactor `ScoreGroupSupport` into a Spring-managed dependency
- add tests for the changed contracts and security boundaries

## Phases

| Phase | Status | Notes |
|---|---|---|
| 1. Baseline and targets | complete | Verified current findings against source and tests |
| 2. Authorization hardening | complete | Session-bound teacher writes and department read checks added |
| 3. Response contract cleanup | complete | `score` controllers now return `ApiResponse` consistently |
| 4. Performance and structure fixes | complete | Removed fallback N+1s, added scoped group-teacher loading, injected `ScoreGroupSupport` |
| 5. Tests and verification | complete | Added new unit/controller tests and passed `mvn test` |

## Errors Encountered

| Error | Attempt | Resolution |
|---|---|---|
| PowerShell parsed `-Dtest=a,b` as separate args | 1 | Re-ran Maven with quoted `-Dtest=...` |
