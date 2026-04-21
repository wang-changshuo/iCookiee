# Draft: Heatmap Day-Number Labels Plan

## Requirements (confirmed)
- Add "light markers" (day numbers) inside specific heatmap cells: 1st, 15th, and last day of month
- Target branch: `feature/record-heatmap`
- Reference UI style: teammate `cqss-xx/ai` design
- Components to modify: `HeatMapSquare.kt` and `RecordHeatMap.kt`

## Research Findings

### Codebase Structure
- `feature/home/src/main/java/com/aifinance/feature/home/component/HeatMapSquare.kt` — individual square composable
- `feature/home/src/main/java/com/aifinance/feature/home/component/RecordHeatMap.kt` — 3x10 grid composable
- Design system in `core/designsystem/src/main/java/com/aifinance/core/designsystem/theme/`
  - `Type.kt`: `IcokieTypography`, `labelSmall` = 10sp Medium, `labelMedium` = 12sp Medium
  - `Color.kt`: `OnSurfaceTertiary` = `0xFF94A3B8`, `OnSurfaceSecondary` = `0xFF475569`, `BrandPrimary` = `0xFF4F46E5`
- **No existing tests** for HeatMapSquare or RecordHeatMap specifically

### UI/UX Research (Material Design 3 + Heatmap Patterns)
- Text on colored backgrounds: use white on dark/saturated, black on light; apply opacity to black/white rather than using gray
- Font size for 30–40dp cells: 10sp–12sp, `FontWeight.Medium` minimum
- "Today" state indicator should take priority over day number information
- Sparse in-cell labels (1, 15, 30) are acceptable for monthly mini-calendars; avoid labeling every cell

## Design Decisions

### Q1: Parameter extension for HeatMapSquare
**Decision**: Add `dayNumber: Int` parameter (non-nullable). `RecordHeatMap` already knows the day and can always pass it. The decision of *whether* to render the number belongs inside `HeatMapSquare` based on the day value and `isToday`.
- Alternative `dayLabel: String?` was rejected: days are numeric, no need for String flexibility.
- Alternative `showDayNumber: Boolean` was rejected: it pushes logic to the caller; keeping logic inside `HeatMapSquare` is cleaner.

### Q2: Display rules
**Decision**: Show day number on day 1, day 15, and the **last day of the month** (which may be 28, 29, 30, or 31).
- Today indicator takes precedence: if today is 1/15/last-day, show "今" instead of the number.
- Rationale: "今" is a state indicator (more actionable) while day number is informational.

### Q3: Number styling (colors)
**Decision**:
- `DayActivity.None` (light gray `0xFFF0F1F4`): `Color.Black.copy(alpha = 0.45f)` — soft watermark effect
- `DayActivity.ExpenseOnly` (light blue `0xFFA1B9F8`): `Color.Black.copy(alpha = 0.60f)` — readable dark gray
- `DayActivity.WithIncome` (orange `0xFFFF9800`): `Color.White.copy(alpha = 0.92f)` — crisp white with `FontWeight.Medium`

**Cross-concern for orange**: If orange fails WCAG contrast at 10sp with white, add a subtle `Shadow` (black, blur 2f, alpha 0.3f).

### Q4: Priority when today overlaps milestone day
**Decision**: "今" wins. If the user needs both, future enhancement could add a dot/ring indicator.

### Q5: Grid sizing / readability
**Decision**: Keep `10.sp` (matches existing "今" text). The cell is `aspectRatio(1f)` within a 10-column grid that fills width. On typical phones this yields ~30–36dp cells. `10.sp` with `FontWeight.Medium` is legible. No grid changes needed.

### Q6: Files to touch
1. `HeatMapSquare.kt` — add `dayNumber: Int`, add label logic and styling
2. `RecordHeatMap.kt` — pass `day = day` into `HeatMapSquare`
3. **New test file** (TDD): `feature/home/src/test/java/com/aifinance/feature/home/component/HeatMapSquareTest.kt` — Compose UI unit test via `createComposeRule`

## Scope Boundaries
- INCLUDE: Day number labels, color styling, priority logic, Compose UI test
- EXCLUDE: Changes to grid dimensions, interactions, navigation, or data layer

## Test Strategy Decision
- **Infrastructure exists**: YES (project has JUnit + Compose Test dependencies based on existing test structure)
- **Automated tests**: TDD-oriented (test first for label display logic)
- **Framework**: JUnit4 + Compose UI Test (`createComposeRule`)
- **Agent-Executed QA**: Screenshot-based via Compose testing or preview verification
