### 2025-11-09

- Added tests for log analyzer processors:
  - `MapEntryLogProcessorTest`: verifies regex pattern matching, order (`15`), and that processing persists a new `Map` via `MapRepository`.
  - `MapExitLogProcessorTest`: verifies regex pattern matching, order (`15`), and that processing a matching line executes without errors.
- Added integration tests for `LogService` with real log tailing:
  - `LogServiceIntegrationTest`: starts tailing sample `.log` files and asserts that items and maps are persisted/updated correctly for
    a "happy path" (multiple `BagInit` → map entry → multiple `BagMgr@:Modfy` → map exit) and a multi‑scenario case.
  - Sample logs created under `src/test/resources/logs/`: `happy_path.log`, `multi_scenario.log`.

Notes:
- Tests follow the existing style (`@SpringBootTest`) and use H2 in-memory DB.
- No production code changes were required for these tests.

- Fixed failing `MapRepositoryTest` by correcting JPQL null check and test expectation:
  - In `MapRepository`, changed query from `m.endedAt = null` to `m.endedAt is null` so the active map is returned.
  - In `MapRepositoryTest`, assert that the active map has `endedAt == null`.
  - Verified related tests (`MapEntry/Exit/Bag*LogProcessorTest`, `LogServiceIntegrationTest`, `TaskServiceTest`) all pass.
