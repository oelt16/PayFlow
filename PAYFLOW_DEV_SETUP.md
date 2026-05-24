# PayFlow — Development Setup Guide

> **Target**: New machine, first time setting up PayFlow for development.
> **OS**: Windows or macOS.

---

## 1. Prerequisites

| Dependency | Version | Check command |
|---|---|---|
| Git | Any recent | `git --version` |
| Node.js | 20+ | `node --version` |
| Java | 21+ (Temurin) | `java --version` |
| Maven | 3.9+ | `mvn --version` |
| Docker Desktop | Latest | `docker version` |
| opencode / gentle-ai | Latest | `opencode --version` |

Also install:
- **VS Code** (recommended editor)
- **Docker Desktop** (required for TestContainers + local Kafka/PostgreSQL)

> **macOS note**: Use `brew install opencode` or follow the official opencode install docs. Engram is bundled with opencode.

---

## 2. Clone & Open

```bash
# Windows (PowerShell)
cd C:\Users\<YOUR_USER>\Documents\Projects
git clone https://github.com/<YOUR_USER>/PayFlow.git
cd PayFlow

# macOS
cd ~/Projects
git clone https://github.com/<YOUR_USER>/PayFlow.git
cd PayFlow
```

Open the folder in VS Code:

```bash
code .
```

gentle-ai (via opencode) will auto-detect the project by reading the git remote.

---

## 3. Initialize SDD

This is the **first thing** you should do in a new environment. It detects the project stack, testing capabilities, and boots up the SDD persistence backend.

In the opencode chat, run:

```
/sdd-init
```

The first time you do this, you'll get a **Session Preflight** prompt. Choose:

```
A1 — Interactive
B1 — OpenSpec (or B3 — Both, if you want Engram too)
C1 — Ask me
D1 — 400 lines
```

Or simply type `use recommended` to accept all defaults.

What `/sdd-init` does:
- Scans the project for frontend/backend stack
- Detects test runners (Vitest, JUnit 5)
- Caches everything in Engram + writes `openspec/config.yaml` if using OpenSpec
- Activates **Strict TDD Mode** when tests are detected

---

## 4. Verify Everything Is There

Check that the OpenSpec artifacts are visible:

```bash
ls openspec/
```

You should see:
```
openspec/
├── config.yaml
├── baseline/spec.md
└── changes/    (6 phase directories)
```

If the baseline spec is empty or OpenSpec wasn't initialized, run `/sdd-ff baseline` to regenerate it.

---

## 5. Workflow for New Features (The SDD Way)

This is **not mandatory** but **strongly recommended** — it's how PayFlow was designed to be developed.

| Step | Command | What it does |
|---|---|---|
| **Start a change** | `/sdd-new "my-feature"` | Creates proposal → spec → design → tasks files in `openspec/changes/my-feature/`. Stops at each phase in Interactive mode. |
| **Fast-forward** | `/sdd-ff "my-feature"` | Same as `/sdd-new` but runs all planning phases back-to-back without stopping. Use when you already know exactly what to build. |
| **Implement** | `/sdd-apply "my-feature"` | Executes the tasks from the tasks spec. Writes code, runs tests. Updates `apply-progress.md` as it goes. |
| **Verify** | `/sdd-verify "my-feature"` | Validates implementation against the spec. Reports CRITICAL / WARNING / SUGGESTION per requirement. |
| **Archive** | `/sdd-archive "my-feature"` | Finalizes the change. Syncs the delta spec into baseline and writes `archive-report.md`. |

### The rule of thumb

```
/sdd-new  → when you have an idea but need to think through the approach
/sdd-ff   → when you already know what to build (proposal → tasks in one shot)
/sdd-apply → when tasks are ready (NEVER skip planning)
```

**Do not skip the planning phases** (`/sdd-new` or `/sdd-ff`). They produce the spec, design, and tasks that make the implementation predictable and reviewable. If you skip straight to `/sdd-apply`, SDD will reject it.

---

## 6. Other Useful Commands

| Command | When to use |
|---|---|
| `/sdd-explore <topic>` | Investigate the codebase without committing to a change. Reads files, compares approaches. |
| `/sdd-continue "my-feature"` | Resume a paused change (e.g., after completing design, run this to start tasks). |
| `/sdd-onboard` | Full guided walkthrough of SDD on the real codebase. Good for new contributors. |

### Meta-commands (type directly, no `/sdd` prefix)

| Command | What it does |
|---|---|
| `/remember` | Ask the AI about past decisions. "What was the rate limiting approach?" |
| `@PayFlow_Specification_v2.md` | Reference a file in conversation. |

---

## 7. Keeping Engram in Sync Across Machines

OpenSpec files (`openspec/`) travel with git automatically.

If you want to also carry **Engram session memory** (past decisions, session context) to a new machine:

```bash
# On the old machine — copy Engram DB
# (see import_export_guide.md for full details)

# On the new machine — restore it to
# Windows: C:\Users\<YOU>\.engram\
# macOS:   ~/.engram/
```

Without this, you'll still have:
- ✅ All specs, designs, tasks in `openspec/` (git-tracked)
- ✅ Code in the repo
- ❌ Past session context (Engram memory)

The AI will rebuild session context as you work. OpenSpec files are always the source of truth.

---

## 8. Quick Reference

```bash
# First time on a new machine
git clone <repo>
code .
/sdd-init
use recommended

# Each new feature
/sdd-new "feature-name"    # plan
/sdd-apply "feature-name"  # build
/sdd-verify "feature-name" # check
/sdd-archive "feature-name"# close

# Quick changes (known scope)
/sdd-ff "feature-name"     # fast-forward plan
/sdd-apply "feature-name"  # build

# Just looking around
/sdd-explore "something"
```

---

**Need more?** Check `import_export_guide.md` for full machine migration, or `openspec/baseline/spec.md` for the project reference.
