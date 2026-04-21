# Plan: 合并队友分支 feat/theme-dark-and-clear-history

## TL;DR
> 将队友仓库 `echenglight/ai_finance_app` 的 `feat/theme-dark-and-clear-history` 分支安全合并到本地。先创建备份分支，再在 `integration/merge-theme-dark-and-clear-history` 分支上执行 merge、冲突解决、构建验证和静态引用检查，最后生成合并报告供用户确认是否推入 `main`。
> 
> **Deliverables**:
> - 备份分支 `backup/pre-merge-feat-theme-dark-YYYYMMDD`
> - Integration 分支，已完成 merge
> - 构建通过报告（`:app:assembleDebug`）
> - 静态引用检查报告
> - 合并总结（含功能删除清单）
>
> **Estimated Effort**: Medium
> **Parallel Execution**: NO - 顺序执行（合并 → 构建 → 检查）
> **Critical Path**: T1 → T2 → T3 → T4 → T5 → F1-F4

---

## Context

### Original Request
用户希望将队友开发的新功能分支 `feat/theme-dark-and-clear-history` 合并到自己的仓库中。需要创建一个专属分支进行合并与测试。

### Interview Summary
**Key Discussions**:
- 用户已授权自主决定分支名、合并策略、冲突处理和测试范围。
- 已在本地添加 remote `echenglight` 并 fetch 分支。

**Research Findings**:
- 项目类型：Android Gradle 项目（Kotlin + Jetpack Compose）
- 当前本地在 `main`，origin 为 `Karlineal/ai_finance_app`
- 队友分支领先 `main` 两个提交，涉及 55 个文件的变更
- **大范围功能删除**：`feature/budget/` 整个模块、HeatMap 组件、`CalendarTransactionsScreen` 被移除
- **新增功能**：Settings 页面增加暗色主题切换（theme dark mode）和清空历史记录（clear history）

### Metis Review
**Identified Gaps** (addressed):
- **功能降级风险**：合并后会永久删除 budget、heatmap、calendar transactions 功能——计划将明确列出删除清单并向用户报告。
- **运行时崩溃风险**：删除的模块可能仍被导航图或 DI 引用——计划包含静态引用检查任务。
- **本地配置保护**：冲突解决时不应覆盖本地配置（如 API key、 signing config）——任务 T3 中会特别保护 `local.properties` 和 `build.gradle.kts` 中的本地配置。

---

## Work Objectives

### Core Objective
将 `echenglight/feat/theme-dark-and-clear-history` 安全合并到一个隔离的 integration 分支中，验证构建通过并检查运行时引用完整性。

### Concrete Deliverables
1. 备份分支 `backup/pre-merge-feat-theme-dark-<date>`
2. Integration 分支 `integration/merge-theme-dark-and-clear-history`
3. 构建成功日志（`:app:assembleDebug BUILD SUCCESSFUL`）
4. 静态引用检查报告（grep 结果截图/日志）
5. 合并总结 Markdown 报告

### Definition of Done
- `integration/merge-theme-dark-and-clear-history` 分支上无未解决的合并冲突
- `./gradlew :app:assembleDebug` 成功退出
- 静态引用检查无危险残留（或已记录）
- 最终报告已呈现给用户

### Must Have
- 备份分支在 merge 前创建
- Integration 分支从当前 `main` 切出
- `echenglight/feat/theme-dark-and-clear-history` 成功 merge 到 integration 分支
- 构建验证通过
- 删除的功能清单在报告中明确列出

### Must NOT Have (Guardrails)
- 不允许直接 push/merge 到 `main` 分支（必须等用户最终确认）
- 不允许在 merge 过程中对代码进行非必要的重构或 cleanup
- 不允许丢失本地已有的 `local.properties` 和签名配置
- 不允许删除未备份的代码后无法恢复

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: YES (Gradle + Android build system)
- **Automated tests**: None required (项目本身未确认有持续运行的单元测试)
- **Agent-Executed QA**: 构建命令 + 静态 grep 检查 + 文件结构验证

### QA Policy
每个任务包含 agent 可执行的 QA 场景，通过 Bash 命令验证。

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Merge Preparation):
├── T1: 创建备份分支
├── T2: 创建 integration 分支并执行 merge
└── T3: 解决合并冲突（如存在）

Wave 2 (Validation):
├── T4: Gradle 构建验证
└── T5: 静态引用检查与报告生成

Wave FINAL (Review & Report):
├── F1: Plan Compliance Audit (oracle)
├── F2: Code Quality Review (unspecified-high)
├── F3: Real Manual QA (unspecified-high)
└── F4: Scope Fidelity Check (deep)
-> Present results -> Get explicit user okay
```

### Dependency Matrix
- **T1**: — → T2
- **T2**: T1 → T3
- **T3**: T2 → T4
- **T4**: T3 → T5
- **T5**: T4 → F1-F4
- **F1-F4**: T5 → 用户确认

### Agent Dispatch Summary
- **Wave 1**: T1-T3 → `git-master` / `unspecified-high`
- **Wave 2**: T4-T5 → `unspecified-high`
- **FINAL**: F1-F4 → `oracle` / `unspecified-high` / `deep`

---

## TODOs

- [x] T1. **创建备份分支**

  **What to do**:
  - 从当前 `main` 分支创建备份分支 `backup/pre-merge-feat-theme-dark-$(Get-Date -Format yyyyMMdd)`（Windows PowerShell 下使用 `Get-Date`，Bash 下使用 `date +%Y%m%d`）。
  - 备份分支用于在需要时恢复原始 `main` 状态。

  **Must NOT do**:
  - 不要切换当前工作分支到备份分支（保持当前在 `main`）。
  - 不要删除或修改任何现有文件。

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`git-master`]
    - `git-master`: 用于安全的分支创建和验证。

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 (T1 → T2 → T3)
  - **Blocks**: T2
  - **Blocked By**: None

  **References**:
  - `git branch` 输出（当前在 `main`）

  **Acceptance Criteria**:
  - [ ] 备份分支存在于本地分支列表中。

  **QA Scenarios**:
  ```
  Scenario: 备份分支创建成功
    Tool: Bash
    Preconditions: 当前在 main 分支
    Steps:
      1. 运行 `git branch backup/pre-merge-feat-theme-dark-<date> main`
      2. 运行 `git branch --list backup/pre-merge-feat-theme-dark-*`
    Expected Result: 输出包含 `backup/pre-merge-feat-theme-dark-` 前缀的分支名
    Evidence: .sisyphus/evidence/t1-backup-branch.txt
  ```

  **Evidence to Capture**:
  - [ ] `task(t1-backup-branch.txt)` - 备份分支列表输出

- [x] T2. **创建 integration 分支并执行 merge**

  **What to do**:
  - 从 `main` 切出 integration 分支：`integration/merge-theme-dark-and-clear-history`
  - 执行：`git merge echenglight/feat/theme-dark-and-clear-history --no-ff -m "Merge branch 'feat/theme-dark-and-clear-history' from echenglight"`
  - 如果 merge 过程中出现冲突，记录所有冲突文件列表，但不要在此任务中解决（交给 T3）。

  **Must NOT do**:
  - 不要直接 push 到 origin/main。
  - 不要使用 fast-forward merge（保留 merge commit 以便 revert）。

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`git-master`]
    - `git-master`: 负责 merge 操作和冲突文件清单提取。

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1
  - **Blocks**: T3
  - **Blocked By**: T1

  **References**:
  - `echenglight/feat/theme-dark-and-clear-history` — 已 fetch 到本地的队友分支

  **Acceptance Criteria**:
  - [ ] `integration/merge-theme-dark-and-clear-history` 分支已创建
  - [ ] merge 已执行（可能处于冲突中或已完成）

  **QA Scenarios**:
  ```
  Scenario: Integration 分支创建并包含 merge 提交
    Tool: Bash
    Preconditions: T1 已完成
    Steps:
      1. `git checkout -b integration/merge-theme-dark-and-clear-history main`
      2. `git merge echenglight/feat/theme-dark-and-clear-history --no-ff -m "Merge branch 'feat/theme-dark-and-clear-history' from echenglight"`
      3. `git log --oneline -1`
    Expected Result: HEAD 为 merge commit（即使因冲突未最终提交，也应有 merge 状态）
    Failure Indicators: 分支未创建或 HEAD 不在 integration 分支上
    Evidence: .sisyphus/evidence/t2-merge-status.txt
  ```

  **Evidence to Capture**:
  - [ ] `task(t2-merge-status.txt)` - 最近的 log 和 git status 输出

- [x] T3. **解决合并冲突（如存在）**

  **What to do**:
  - 如果 T2 产生冲突，读取每个冲突文件，使用合理的策略解决：
    - **代码文件（.kt）**：优先保留队友的 dark mode / clear history 改动，但如果涉及到本地特有的配置（如 `local.properties` 引用、特定 API endpoint），保留本地值。
    - **构建文件（build.gradle.kts / settings.gradle.kts）**：优先采用队友的模块删减，但确保本地依赖版本和签名配置不被覆盖。
    - **README.md**：合并两者内容，保留队友新增的功能说明，也保留本地已有的说明。
  - 解决后执行 `git add .` 和 `git commit` 完成 merge commit。
  - 如果没有冲突，此任务为 no-op（标记完成即可）。

  **Must NOT do**:
  - 不要删除本地 `local.properties` 中的 API key 或签名信息。
  - 不要对解决冲突后的代码进行重构（只解决冲突）。

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: [`git-master`, `android-native-dev`]
    - `git-master`: 冲突解决
    - `android-native-dev`: 理解 Android Gradle 构建文件和 Kotlin 代码的冲突取舍

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1
  - **Blocks**: T4
  - **Blocked By**: T2

  **References**:
  - `app/build.gradle.kts` — 本地/队友的构建配置
  - `settings.gradle.kts` — 模块列表（队友删除了 budget）
  - `local.properties` — 本地独有配置，必须保留

  **Acceptance Criteria**:
  - [ ] `git status --short` 不包含 `UU` (unmerged) 状态
  - [ ] `git log --oneline -1` 显示 merge commit
  - [ ] `local.properties` 未被覆盖或删除

  **QA Scenarios**:
  ```
  Scenario: 所有冲突已解决且无残留
    Tool: Bash
    Preconditions: T2 已完成，可能处于冲突状态
    Steps:
      1. `git status --short`
      2. 检查是否有 UU 前缀的文件
    Expected Result: 零个 UU 文件
    Failure Indicators: 存在 UU 文件意味着冲突未解决
    Evidence: .sisyphus/evidence/t3-conflict-resolution.txt

  Scenario: 本地配置文件完整性
    Tool: Bash
    Preconditions: 冲突处理后
    Steps:
      1. `cat local.properties`
    Expected Result: 文件存在且包含原有的键值（如 `sdk.dir`）
    Failure Indicators: 文件丢失或被覆盖为空
    Evidence: .sisyphus/evidence/t3-local-properties-preserved.txt
  ```

  **Evidence to Capture**:
  - [ ] `task(t3-conflict-resolution.txt)` - git status 输出
  - [ ] `task(t3-local-properties-preserved.txt)` - local.properties 内容

- [x] T4. **Gradle 构建验证**

  **What to do**:
  - 在 integration 分支上运行 `./gradlew :app:assembleDebug`（Windows 下为 `gradlew.bat :app:assembleDebug`）。
  - 捕获完整输出，确认 `BUILD SUCCESSFUL`。
  - 如果构建失败，读取错误日志，判断是合并引入的问题还是环境问题：
    - 如果是合并引入的符号缺失（如删除了 budget 模块但某处仍引用），记录问题但暂不深究修复（因为超出 merge 范围），只报告给用户。
    - 如果是环境问题（如 JDK 版本、Android SDK 缺失），尝试根据 `local.properties` 调整。

  **Must NOT do**:
  - 不要在构建失败时随意重构代码来“让构建通过”。
  - 不要在此任务中修复未请求的 bug。

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: [`android-native-dev`]
    - `android-native-dev`: 理解 Gradle 构建错误和 Android 环境配置。

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2
  - **Blocks**: T5
  - **Blocked By**: T3

  **References**:
  - `app/build.gradle.kts` — App 模块构建配置
  - `build.gradle.kts` (root) — 项目级构建配置
  - `gradle.properties` — Gradle 属性

  **Acceptance Criteria**:
  - [ ] `./gradlew :app:assembleDebug` 返回退出码 0
  - [ ] 输出中包含 `BUILD SUCCESSFUL`

  **QA Scenarios**:
  ```
  Scenario: Debug APK 构建成功
    Tool: Bash
    Preconditions: T3 已完成，在 integration 分支上
    Steps:
      1. `gradlew.bat :app:assembleDebug`
      2. `echo $?` (或 `$LASTEXITCODE`)
    Expected Result: 退出码为 0，输出包含 `BUILD SUCCESSFUL`
    Failure Indicators: 非零退出码或 `BUILD FAILED` 字样
    Evidence: .sisyphus/evidence/t4-build-log.txt
  ```

  **Evidence to Capture**:
  - [ ] `task(t4-build-log.txt)` - 构建日志最后 100 行（含成功/失败标志）

- [x] T5. **静态引用检查与报告生成**

  **What to do**:
  - 使用 `grep` 在 `.kt` 和 `.kts` 文件中搜索已被删除模块的关键符号：
    - `Budget` (budget 模块)
    - `HeatMap` / `heatmap` (热力图组件)
    - `CalendarTransactions` (日历交易页)
  - 如果搜索结果中发现 `import` 或函数调用引用到已删除的类/模块，记录为潜在运行时崩溃风险。
  - 生成一份 Markdown 总结报告，包含：
    1. 合并摘要（提交数、变更文件数）
    2. 功能删除清单（budget、heatmap、calendar transactions）
    3. 构建验证结果（通过/失败）
    4. 静态引用检查结果（安全/存在残留引用）
    5. 冲突解决记录（如有）
    6. 最终建议（是否推荐合并到 main）
  - 将报告保存为 `.sisyphus/evidence/merge-report.md`。

  **Must NOT do**:
  - 不要修改报告内容来掩盖构建失败或引用残留问题。
  - 不要在此任务中修复发现的引用残留（超出 merge 范围）。

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: [`git-master`]

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2
  - **Blocks**: F1-F4
  - **Blocked By**: T4

  **References**:
  - 队友分支的 diff 统计（55 files changed，大量删除）

  **Acceptance Criteria**:
  - [ ] grep 结果已保存
  - [ ] `.sisyphus/evidence/merge-report.md` 已生成

  **QA Scenarios**:
  ```
  Scenario: 危险引用扫描
    Tool: Bash
    Preconditions: T4 已完成
    Steps:
      1. `grep -ri "Budget\|HeatMap\|CalendarTransactions" --include="*.kt" --include="*.kts" app/src/ feature/ core/ | tee .sisyphus/evidence/t5-stale-references.txt`
    Expected Result: 输出文件已生成（即使为空也正常）
    Failure Indicators: grep 命令本身执行失败
    Evidence: .sisyphus/evidence/t5-stale-references.txt

  Scenario: 合并报告存在且结构完整
    Tool: Bash
    Preconditions: 扫描完成后
    Steps:
      1. `cat .sisyphus/evidence/merge-report.md`
    Expected Result: 文件存在且包含构建结果和删除清单
    Evidence: .sisyphus/evidence/merge-report.md
  ```

  **Evidence to Capture**:
  - [ ] `task(t5-stale-references.txt)` - grep 输出
  - [ ] `task(merge-report.md)` - 合并总结报告

---

## Final Verification Wave

- [x] F1. **Plan Compliance Audit** — `oracle`
  逐条检查 Must Have 是否完成：备份分支存在、integration 分支 merge 成功、构建通过、报告存在。检查 Must NOT Have：没有直接修改 main。
  Output: `Must Have [5/5] | Must NOT Have [4/4] | VERDICT: APPROVED`

- [x] F2. **Code Quality Review** — `unspecified-high`
  检查 integration 分支的 diff：是否有 `<<<<<<< HEAD` 残留、是否有意外的本地配置文件被覆盖、build.gradle 是否语法完整。
  Output: `Conflict residue [CLEAN/0] | Config preserved [YES] | VERDICT: APPROVED`

- [x] F3. **Real Manual QA** — `unspecified-high`
  重新运行 `./gradlew :app:assembleDebug`，验证构建仍然通过。运行 grep 检查确认引用情况。保存证据到 `.sisyphus/evidence/final-qa/`。
  Output: `Build [PASS] | Grep [CLEAN - 0 issues] | VERDICT: APPROVED`

- [x] F4. **Scope Fidelity Check** — `deep`
  对比计划中的任务和实际执行结果，确认没有超范围操作（如未请求的代码重构）。
  Output: `Scope [COMPLIANT/0 issues] | VERDICT: APPROVED`

---

## Commit Strategy
- 合并本身产生 merge commit，无需额外 commit。
- 冲突解决后的修改作为 merge commit 的一部分提交。

## Success Criteria

### Verification Commands
```bash
# 备份分支存在
git branch --list backup/pre-merge-feat-theme-dark-*

# Integration 分支存在且包含 merge 提交
git log --oneline integration/merge-theme-dark-and-clear-history --grep="Merge branch" -1

# 构建成功
./gradlew :app:assembleDebug
# Expected: BUILD SUCCESSFUL

# 无未解决冲突
git diff --check integration/merge-theme-dark-and-clear-history
```

### Final Checklist
- [x] 备份分支已创建
- [x] Integration 分支 merge 成功，无冲突残留
- [x] 构建通过
- [x] 静态引用检查完成
- [x] 最终报告已生成并呈现给用户
- [x] 已按用户要求合并到 `main` 并推送到 GitHub
