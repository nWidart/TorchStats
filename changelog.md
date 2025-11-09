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

- Test stability: LogServiceIntegrationTest minimal fix to avoid H2 lock timeouts and lazy-load failures:
  - Removed class-level `@Transactional` to prevent concurrent transactions with tailer thread.
  - Ensured tailer is stopped before/after each test.
  - Cleanup now deletes join table first (`map_item`), then entities (`Item`, `Map`), wrapped in a dedicated transaction.
  - Increased await timeouts to 10s and added small settling delay after tailing.
  - Used fetch-join query in assertions to load `items` eagerly and avoid LazyInitializationException.
  - Verified `LogServiceIntegrationTest` passes locally; no production code changes required.

- Repository enhancement: moved fetch-join helper into `MapRepository`:
  - Added `MapRepository#fetchMapsWithItemsSortedByEndedAtDesc()` with JPQL `select distinct m from Map m left join fetch m.items order by m.endedAt desc`.
  - Updated `LogServiceIntegrationTest` to use the repository method and removed the private helper.
