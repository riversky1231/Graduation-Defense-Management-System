# Findings

- Current branch already includes `ApiResponse`, controller tests, and `ScoreServiceImpl` tests, so the original “only one test file” claim is stale.
- Confirmed critical authz gap: `ScoreSubmissionController` exposes write endpoints that trust request body / params instead of binding operations to the authenticated teacher.
- Confirmed read-side boundary gap: `LargeGroupTeacherScoreController#getLargeGroupStudentScores` only checks login, not department ownership.
- Confirmed N+1 fallback in `ScoreGroupQueries#loadTeacherScoreMap` and `#loadFinalScores`.
- Confirmed full-table teacher-group load in `ScoreGroupSupport#buildDepartmentTeacherCountMap`.
- Confirmed `ScoreGroupSupport` is recreated on each call via `new` in `ScoreServiceImpl#groupSupport`.
- Confirmed `updateLargeGroupScore` uses awkward `scoreId` mismatch semantics instead of direct lookup by id.
- Confirmed `score` controllers still mix `String`, `Map`, entities, and exception-driven results.

## Resolved

- Teacher scoring endpoints now bind writes to the authenticated teacher session and reject cross-group scoring.
- Large-group teacher reads now enforce the same department boundary used for writes.
- `score` controllers now return `ApiResponse` consistently; frontend score views were updated to read wrapped payloads.
- Batch fallback queries were removed from `ScoreGroupQueries`.
- `ScoreGroupSupport` is Spring-managed and injected into `ScoreServiceImpl`.
- `updateLargeGroupScore` now resolves by `scoreId` directly and validates tuple consistency.
- Added focused tests for controller authz/contracts, `ScoreMathHelper`, and large-group score service behavior.
