# Jetpack Compose Heatmap Day-Number Labels

## TL;DR

> Add sparse "light marker" day numbers (1st, 15th, last day) to the `HeatMapSquare` composable in the `feature/record-heatmap` branch. Extend `HeatMapSquare` with a `dayNumber: Int` parameter, compute visibility inside the component, and give the "今" (today) indicator priority over day numbers. Follow up with Compose UI tests in a TDD style.
>
> **Deliverables**:
> - Updated `HeatMapSquare.kt` with `dayNumber` support and per-activity text colors
> - Updated `RecordHeatMap.kt` passing `dayNumber = day`
> - New `HeatMapSquareTest.kt` with Compose UI assertions for label visibility and today-priority
>
> **Estimated Effort**: Short (~15–25 min implementation + 15 min testing)
> **Parallel Execution**: YES — 2 waves + Final Verification
> **Critical Path**: T1 (Tests) + T2 (Square) → T3 (Grid) → Final Verification

---

## Context

### Original Request
User shared a screenshot and requested that heatmap cells for the **1st, 15th, and last day** of the month display a "light marker" — the day number rendered inside the cell, matching a teammate's UI style (`cqss-xx/ai`).

### Interview Summary
**Key Decisions**:
- `HeatMapSquare` will receive `dayNumber: Int` (determining whether to draw the label is internal to the square)
- Labels appear on day **1**, day **15**, and the **last day of the month**
- "今" (today) indicator **takes precedence** over day numbers
- Text styling follows Material Design 3 contrast guidance:
  - `None` (light gray): `Color.Black.copy(alpha = 0.45f)`
  - `ExpenseOnly` (light blue): `Color.Black.copy(alpha = 0.60f)`
  - `WithIncome` (orange): `Color.White.copy(alpha = 0.92f)` (+ subtle shadow if needed)
- Font size remains `10.sp` with `FontWeight.Medium` to match the existing "今" label

### Research Findings
- **Codebase**: `feature/home/src/main/java/com/aifinance/feature/home/component/HeatMapSquare.kt` and `RecordHeatMap.kt`
- **Design system**: `core/designsystem` provides `IcokieTypography` and semantic colors (`OnSurfaceTertiary`, `BrandPrimary`, etc.)
- **Tests**: No existing heatmap tests; project has JUnit4 + Compose UI Test infrastructure

### Metis Review
Metis consultation timed out; self-review applied the following guardrails:
- Lock scope to **two source files + one test file**; no grid-size or navigation changes
- Explicitly exclude full-month labeling to prevent visual clutter
- Require agent-executed screenshots or test assertions as evidence

---

## Work Objectives

### Core Objective
Enable sparse day-number labels inside the 3×10 heatmap grid for temporal orientation, without disrupting the existing compact visual density.

### Concrete Deliverables
- `feature/home/src/main/java/com/aifinance/feature/home/component/HeatMapSquare.kt` — extended API + label rendering
- `feature/home/src/main/java/com/aifinance/feature/home/component/RecordHeatMap.kt` — wired to pass day number
- `feature/home/src/test/java/com/aifinance/feature/home/component/HeatMapSquareTest.kt` — TDD-style Compose UI tests

### Definition of Done
- [ ] Compose UI tests pass (`./gradlew :feature:home:testDebugUnitTest`)
- [ ] Heatmap screenshots (Compose Preview or test capture) show correct labels on 1, 15, and last day
- [ ] No regressions in grid spacing, click handling, or "今" behavior

### Must Have
- `dayNumber` parameter on `HeatMapSquare`
- Labels on 1st, 15th, and last day of month
- "今" priority over day numbers
- Per-activity text color as specified

### Must NOT Have (Guardrails)
- Changes to grid column count, cell shape, or gap spacing
- Day numbers on every cell
- Alterations to `onDateClick` behavior or data layer
- New dependencies or theme-wide color changes

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: YES (project has JUnit4 + Compose UI Test)
- **Automated tests**: TDD-oriented
- **Framework**: JUnit4 + `createComposeRule`
- **If TDD**: Each implementation task follows RED → GREEN → REFACTOR

### QA Policy
Every task includes agent-executed QA scenarios. Evidence is saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.png`.

- **Compose UI**: Use `composeTestRule` + `onNodeWithText` / `assertIsDisplayed`
- **Gradle**: Use `./gradlew :feature:home:testDebugUnitTest` for pass/fail verification

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Foundation — start immediately):
├── T1: Compose UI tests for HeatMapSquare label logic
└── T2: Extend HeatMapSquare with dayNumber + styling

Wave 2 (Integration — after T2 completes):
└── T3: Wire RecordHeatMap to pass dayNumber

Wave FINAL (After ALL implementation tasks):
├── F1: Plan Compliance Audit (oracle)
├── F2: Code Quality Review (unspecified-high)
├── F3: Real Manual QA via tests + screenshots (unspecified-high)
└── F4: Scope Fidelity Check (deep)
-> Present results -> Get explicit user okay
```

### Dependency Matrix
- **T1**: - - Wave 1
- **T2**: - - Wave 1, blocks T3
- **T3**: T2 - Wave 2

### Agent Dispatch Summary
- **Wave 1**: T1 → `quick` (+ `test-driven-development`), T2 → `quick` (+ `android-native-dev`)
- **Wave 2**: T3 → `quick`
- **FINAL**: F1 → `oracle`, F2 → `unspecified-high`, F3 → `unspecified-high`, F4 → `deep`

---

## TODOs

- [ ] T1. Add Compose UI tests for day-number visibility and today-priority

  **What to do**:
  - Create `feature/home/src/test/java/com/aifinance/feature/home/component/HeatMapSquareTest.kt`
  - Write test cases:
    1. Day 1 shows "1" when activity is `None`
    2. Day 15 shows "15" when activity is `ExpenseOnly`
    3. Day 31 (or last day) shows "31" (or appropriate last-day number) when activity is `WithIncome`
    4. Today (e.g., day 15) shows "今" and **not** "15"
    5. Day 2 shows **no** label text
  - Use `createComposeRule()` and `composeTestRule.setContent { ... }`
  - Assert with `composeTestRule.onNodeWithText("1").assertIsDisplayed()` etc.
  - Temporarily stub `dayNumber` in `HeatMapSquare` if it doesn't exist yet (TDD)

  **Must NOT do**:
  - Do not import `androidTest` dependencies — keep it in `src/test` as a unit test using `composeTestRule`
  - Do not test `RecordHeatMap` grid logic here; keep tests focused on `HeatMapSquare`

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `test-driven-development`, `android-native-dev`
  - **Reason**: Fast Compose UI test scaffolding; TDD discipline ensures assertions drive implementation

  **Parallelization**:
  - **Can Run In Parallel**: YES (with T2)
  - **Parallel Group**: Wave 1
  - **Blocks**: None
  - **Blocked By**: None

  **References**:
  - Current `HeatMapSquare.kt` uses `Box` + `Text` with `10.sp` / `FontWeight.Bold`
  - Compose UI testing docs: `androidx.compose.ui.test.junit4.createComposeRule`
  - Existing test structure: `feature/home/src/test/java/com/aifinance/feature/home/`

  **Acceptance Criteria**:
  - [ ] Test file created at the correct path
  - [ ] `./gradlew :feature:home:testDebugUnitTest` runs and FAILS (RED phase) because `dayNumber` parameter or label logic is not yet implemented
  - [ ] At least 5 test cases written covering happy path, edge cases (today priority), and non-labeled days

  **QA Scenarios**:
  ```
  Scenario: TDD Red phase verification
    Tool: Bash (gradle)
    Preconditions: Test file exists; HeatMapSquare unchanged
    Steps:
      1. Run `./gradlew :feature:home:testDebugUnitTest`
    Expected Result: Build succeeds; tests FAIL with assertion errors on missing labels
    Evidence: .sisyphus/evidence/task-t1-red-phase.txt
  ```

  **Commit**: YES
  - Message: `test(home): add HeatMapSquare label tests [RED]`
  - Files: `feature/home/src/test/java/com/aifinance/feature/home/component/HeatMapSquareTest.kt`

---

- [ ] T2. Extend HeatMapSquare with dayNumber parameter and label styling

  **What to do**:
  - Add `dayNumber: Int` to `HeatMapSquare` signature
  - Compute `shouldShowDayNumber = dayNumber in setOf(1, 15, daysInMonth)`
    - Wait — `HeatMapSquare` doesn't know `daysInMonth`. Actually, the caller (`RecordHeatMap`) knows it. However, `dayNumber == currentMonth.lengthOfMonth()` can be computed at the call site or inside `RecordHeatMap`. Better approach: pass `isLastDayOfMonth: Boolean = false` OR compute the set in `RecordHeatMap`.
    - **Decision**: Keep `HeatMapSquare` simple. Pass a boolean-like enum or just compute in `RecordHeatMap` and pass `showDayNumber: Boolean`. But earlier we decided on `dayNumber: Int`. Let's refine: `HeatMapSquare` receives `dayNumber: Int` and `daysInMonth: Int`. Then it computes `(dayNumber == 1 || dayNumber == 15 || dayNumber == daysInMonth)`.
    - Actually, even simpler: `RecordHeatMap` computes a `Set<Int>` of labeled days and passes it down... no, that leaks logic.
    - **Final decision**: Add `dayNumber: Int` only. Inside `RecordHeatMap`, calculate `val daysInMonth = currentMonth.lengthOfMonth()` and pass it... wait, `HeatMapSquare` needs to know whether this day is the last day. Let's add `daysInMonth: Int` to `HeatMapSquare` as well. It's cheap and keeps logic inside the component.
  - Inside `HeatMapSquare`:
    ```kotlin
    val shouldShowDayNumber = !isToday &&
        (dayNumber == 1 || dayNumber == 15 || dayNumber == daysInMonth)
    ```
  - Add text color mapping:
    ```kotlin
    val labelColor = when (activity) {
        DayActivity.WithIncome -> Color.White.copy(alpha = 0.92f)
        DayActivity.ExpenseOnly -> Color.Black.copy(alpha = 0.60f)
        DayActivity.None -> Color.Black.copy(alpha = 0.45f)
    }
    ```
  - Render number with `Text(text = dayNumber.toString(), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = labelColor)`
  - Keep the existing "今" `Text` conditional. Ensure they are mutually exclusive (`if (isToday) ... else if (shouldShowDayNumber) ...`)

  **Must NOT do**:
  - Do not change `DayActivity` enum values or meanings
  - Do not alter `RoundedCornerShape(6.dp)`, `aspectRatio(1f)`, or click behavior
  - Do not import theme-wide colors if they create coupling issues; use local `Color` constants as the existing code does

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `android-native-dev`
  - **Reason**: Small scoped Compose change; no heavy logic

  **Parallelization**:
  - **Can Run In Parallel**: YES (with T1)
  - **Parallel Group**: Wave 1
  - **Blocks**: T3
  - **Blocked By**: None

  **References**:
  - `feature/home/src/main/java/com/aifinance/feature/home/component/HeatMapSquare.kt` — existing "今" label pattern
  - `core/designsystem/src/main/java/com/aifinance/core/designsystem/theme/Color.kt` — `OnSurfaceTertiary` (`0xFF94A3B8`) for reference only
  - `core/designsystem/src/main/java/com/aifinance/core/designsystem/theme/Type.kt` — `labelSmall` = 10sp Medium

  **Acceptance Criteria**:
  - [ ] `HeatMapSquare` compiles with new `dayNumber: Int` and `daysInMonth: Int` parameters
  - [ ] `./gradlew :feature:home:testDebugUnitTest` passes (GREEN phase)
  - [ ] `./gradlew :feature:home:compileDebugKotlin` succeeds with zero warnings

  **QA Scenarios**:
  ```
  Scenario: Green phase verification
    Tool: Bash (gradle)
    Preconditions: T1 tests exist; T2 implementation applied
    Steps:
      1. Run `./gradlew :feature:home:testDebugUnitTest`
    Expected Result: All tests PASS
    Evidence: .sisyphus/evidence/task-t2-green-phase.txt

  Scenario: Today priority verification
    Tool: Bash (gradle)
    Preconditions: Tests covering today-priority exist
    Steps:
      1. Run `./gradlew :feature:home:testDebugUnitTest --tests "*today*"`
    Expected Result: Test asserting "今" over day number passes
    Evidence: .sisyphus/evidence/task-t2-today-priority.txt
  ```

  **Commit**: YES
  - Message: `feat(home): add day-number labels to HeatMapSquare [GREEN]`
  - Files: `feature/home/src/main/java/com/aifinance/feature/home/component/HeatMapSquare.kt`

---

- [ ] T3. Wire RecordHeatMap to pass dayNumber and daysInMonth

  **What to do**:
  - In `RecordHeatMap.kt`, inside the `items(daysInMonth)` block:
    - Pass `dayNumber = day`
    - Pass `daysInMonth = daysInMonth`
  - Example:
    ```kotlin
    HeatMapSquare(
        activity = activity,
        isToday = isToday,
        dayNumber = day,
        daysInMonth = daysInMonth,
        onClick = { onDateClick(day) },
        modifier = Modifier.fillMaxWidth()
    )
    ```
  - Verify no other call sites for `HeatMapSquare` exist (explore agent confirmed only `RecordHeatMap.kt` uses it)

  **Must NOT do**:
  - Do not change grid layout, column count, or spacing
  - Do not modify `monthActivity` handling or `onDateClick` lambda

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `android-native-dev`
  - **Reason**: Trivial wiring change

  **Parallelization**:
  - **Can Run In Parallel**: NO (depends on T2)
  - **Parallel Group**: Wave 2
  - **Blocks**: None
  - **Blocked By**: T2

  **References**:
  - `feature/home/src/main/java/com/aifinance/feature/home/component/RecordHeatMap.kt` — `items(daysInMonth)` block
  - `HeatMapSquare.kt` (post-T2) — new parameter signature

  **Acceptance Criteria**:
  - [ ] `RecordHeatMap.kt` compiles and passes `./gradlew :feature:home:compileDebugKotlin`
  - [ ] `./gradlew :feature:home:testDebugUnitTest` still passes after wiring

  **QA Scenarios**:
  ```
  Scenario: Compile check after wiring
    Tool: Bash (gradle)
    Preconditions: T2 merged
    Steps:
      1. Run `./gradlew :feature:home:compileDebugKotlin`
    Expected Result: BUILD SUCCESSFUL
    Evidence: .sisyphus/evidence/task-t3-compile.txt
  ```

  **Commit**: YES
  - Message: `feat(home): wire dayNumber into RecordHeatMap grid`
  - Files: `feature/home/src/main/java/com/aifinance/feature/home/component/RecordHeatMap.kt`
  - Pre-commit: `./gradlew :feature:home:testDebugUnitTest`

---

## Final Verification Wave

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, check for `dayNumber`, label logic, priority rule). For each "Must NOT Have": search codebase for forbidden patterns (grid changes, data layer edits). Check evidence files exist.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `./gradlew :feature:home:ktlintCheck` (if available) and `./gradlew :feature:home:compileDebugKotlin`. Review changed files for: `@Suppress` without reason, magic numbers without context, unused imports, commented-out code.
  Output: `Build [PASS/FAIL] | Lint [PASS/FAIL] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high`
  Run `./gradlew :feature:home:testDebugUnitTest`. Verify all 5 test assertions pass. Capture terminal output.
  Output: `Scenarios [N/N pass] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  Read diffs of all changed files. Verify: only `HeatMapSquare.kt`, `RecordHeatMap.kt`, and `HeatMapSquareTest.kt` were modified. No scope creep (grid size, colors, new dependencies). Check that `daysInMonth` calculation uses `currentMonth.lengthOfMonth()` correctly.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

| Commit | Scope | Files | Pre-commit command |
|---|---|---|---|
| 1 | `test(home): add HeatMapSquare label tests [RED]` | `HeatMapSquareTest.kt` | `./gradlew :feature:home:testDebugUnitTest` (expect failures) |
| 2 | `feat(home): add day-number labels to HeatMapSquare [GREEN]` | `HeatMapSquare.kt` | `./gradlew :feature:home:testDebugUnitTest` (must pass) |
| 3 | `feat(home): wire dayNumber into RecordHeatMap grid` | `RecordHeatMap.kt` | `./gradlew :feature:home:testDebugUnitTest` (must pass) |

---

## Success Criteria

### Verification Commands
```bash
# Compile check
./gradlew :feature:home:compileDebugKotlin

# Unit tests (Compose UI tests)
./gradlew :feature:home:testDebugUnitTest
```

### Final Checklist
- [ ] All "Must Have" present (`dayNumber`, 1/15/last-day labels, today priority, per-activity colors)
- [ ] All "Must NOT Have" absent (no grid changes, no data layer changes, no full-month labeling)
- [ ] All tests pass
- [ ] Zero unaccounted file changes outside the 3 planned files
