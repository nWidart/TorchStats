### 2025-11-09

- Added scenario runner script `scenario.sh` to orchestrate maps and item drops using `append.sh`. See usage in the script header. Example:
  - `./scenario.sh -f ./tmp/test.log --maps 2 --drops 3 --itemIds 3001,3002 --sleepAction 1 --sleepMap 2`
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

### 2025-11-10

- CI: Added GitHub Actions workflow `prod-build.yml` to build and export a production JAR.
  - Uses Temurin JDK 21 via `actions/setup-java@v4` with Maven cache.
  - Runs `./mvnw -B clean package -Pproduction -DskipTests` to build with the production profile (Vaadin `build-frontend`).
  - Uploads `target/*.jar` as artifact `torchstats-jar` (retention 7 days).

- Data: Added in-memory loading of `full_table.json` on application startup.
  - New DTO record `com.nwidart.fulltable.FullTableItem` mapping fields: `from`, `last_time`, `last_update`, `name`, `price`, `type`.
  - New service `com.nwidart.fulltable.FullTableService` reads the JSON from classpath at startup and exposes `findById`, `all`, and `size`.
  - Added test `FullTableServiceTest` to verify data is loaded and key `10001` exists with expected name.


### 2025-11-11

- Tests: Added `StatsServiceQuantityTest` to thoroughly validate revenue calculations with item quantities:
  - `currentMapRevenue_includes_quantity_multiplier`
  - `sessionRevenue_sums_price_times_quantity_across_all_maps`
  - `currentMapRevenue_zero_when_no_active_map`
  - `items_with_zero_quantity_do_not_increase_revenue`
- Fix: Corrected `StatsService#currentMapRevenue()` to multiply unit price by `item.getNum()` (was summing unit prices only). This aligns it with `getSessionRevenue()` and the domain expectation.
- Verification: New tests pass, and existing `StatsServiceIntegrationTest` still passes. No changes needed in `FullTableService`.

### 2025-11-13

- Feature: Real-time UI updates for item drops using Vaadin Push.
  - Added `DropEventBroadcaster` component to broadcast `ItemWasDroppedEvent` updates to all active UIs and keep a small in-memory backlog for newly opened views.
  - Enhanced `ItemUpdateListener` to listen for `ItemWasDroppedEvent`, transform it to a lightweight DTO, and broadcast it.
  - Updated `LogFileView` to display a live-updating table (Grid) of the most recent item drop events. The view subscribes on attach and unsubscribes on detach, using `UI.access()` to safely push updates.
  - Push is globally enabled via `@Push` on `Application`.
  - Added unit test `DropEventBroadcasterTest` validating backlog sizing and listener notifications.

Manual verification:
1. Start the app: `./mvnw spring-boot:run`.
2. Open http://localhost:8080/log-file.
3. Click "Start Tailing" to begin tailing your log (ensure the path points to a valid UE log).
4. Trigger some `BagMgr@:Modfy` lines (use `scenario.sh`/`append.sh` helpers). The "Last item drops" table should update live without refreshing.
