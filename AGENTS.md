# Queda — Mandatory Agent Instructions

## Authority

The current task prompt defines the functional scope.

Anything not required by the task is out of scope.

Do not add future functionality, speculative abstractions, placeholder
code, unused APIs, temporary production implementations, or duplicate
infrastructure.

## Operating procedure

Before editing:

1. Confirm the current Git branch.
2. Inspect the current implementation and existing tests.
3. Inspect `git diff` and `git status`.
4. Identify the root cause of every requested defect.
5. Reuse existing architecture and conventions.

Then implement, test, inspect the resulting diff, and fix all failures
before reporting completion.

Do not stop after producing a plan.

## Git

Never execute:

- git add
- git commit
- git push
- git merge
- git rebase
- git tag
- git reset
- git restore
- git checkout
- git clean

Never change branches or open a pull request.

Leave all changes unstaged.

## Architecture

Preserve this dependency direction:

core:model
→ core:domain
→ core:data
→ feature
→ app

`core:database` contains Room infrastructure and is accessed by
`core:data`, never by features or ViewModels.

Rules:

- `core:model` and `core:domain` are pure Kotlin.
- UI never accesses DAO or Room entities.
- ViewModels depend on use cases, not DAOs.
- Composables contain no business or persistence logic.
- Domain contracts expose no Android, Room, Compose, Hilt, SQLite,
  entities, cursors, or technical exceptions.
- Room entities and DAOs remain inside `core:database`.
- Repository implementations remain inside `core:data`.
- App is responsible only for composition and navigation.
- Preserve previously approved architecture tests.

## UX and design system

Queda must use one coherent visual system across all features.

Generic visual primitives, tokens, theme, reusable states, and generic
accessibility behavior belong in `core:designsystem`.

Feature-specific UI models and components remain inside their feature
module.

`core:designsystem` must never depend on:

- domain models;
- repositories;
- use cases;
- feature modules;
- navigation;
- ViewModels;
- Room;
- database entities.

Do not create parallel themes, duplicated token sets, or feature-local
copies of generic buttons, fields, chips, scaffolds, dialogs, sheets,
loading states, empty states, or error states.

Before creating a visual component:

1. Search the existing design system.
2. Reuse an existing component when it satisfies the contract.
3. Extend it only when the new behavior is generic.
4. Keep domain-specific presentation inside the owning feature.

Reusable Composables should:

- be stateless where practical;
- receive state and callbacks explicitly;
- avoid `NavController`;
- contain no business or persistence logic;
- use string resources for user-visible text;
- expose stable accessibility semantics;
- support large font sizes and small screens;
- use touch targets of at least 48 dp;
- avoid communicating information only through color.

Use the shared spacing, color, typography, shape, elevation, and motion
tokens. Do not introduce arbitrary visual values when an existing token
fits.

Visual direction:

- minimal and premium;
- strong hierarchy;
- generous but efficient spacing;
- restrained surfaces and elevation;
- one primary accent color;
- red only for errors and destructive actions;
- functional motion rather than decoration;
- no fake controls or metadata for functionality that does not exist.

Do not expose navigation, actions, states, filters, metadata, or
destinations before their roadmap functionality exists.

Compose previews may be used as a component catalog. Do not add a
production catalog screen or route.

## Quality

Fix root causes. Do not hide failures.

Never add:

- empty tests;
- tests containing only comments;
- assertions unrelated to the test name;
- branches that ignore unexpected failures;
- nested `runTest`;
- arbitrary delays or sleeps;
- mocks when deterministic fakes are sufficient;
- broad exception catches;
- `allowMainThreadQueries` in production;
- destructive Room migrations;
- `GlobalScope`;
- `Double` or `Float` for quantities;
- TODO, FIXME, placeholders, or `NotImplementedError`.

Every new or modified regression test must fail against the defective
behavior and pass after the correction.

Test counts are evidence, not a target. Do not create tests merely to
reach a number.

## Scope control

Modify only files directly required by the current task.

Before finishing, compare `git diff --name-only` with the authorized
scope.

If solving the task truly requires a file outside the authorized
scope, do not modify it. Report the exact file and reason as FAIL.

## Validation

Run the exact commands required by the task.

A gate is PASS only when the command was actually executed and exited
successfully.

Do not infer that tests passed.

Do not report instrumented tests without their real result.

Do not report CI as executed before push and pull request.

If an environment problem prevents validation, report FAIL and the
exact failing command.

## CI and end-to-end test determinism

CI failures must be investigated from evidence, not assumptions.

When a test passes locally but fails in CI:

1. Compare the exact failing step with the previous successful run.
2. Inspect screenshots, hierarchy dumps, logs, and uploaded artifacts.
3. Confirm the actual UI state and actual entered values.
4. Modify production code only when the artifact proves a production defect.
5. Modify the test only when the artifact proves test instability or an
   incorrect test assumption.

Do not make repeated speculative changes such as:

- arbitrary waits;
- fixed sleeps;
- unrelated retries;
- changing screen size only to hide a layout problem;
- weakening assertions;
- changing production behavior without evidence;
- repeatedly rerunning a deterministic failure without investigation.

A single contradictory CI run may be retried once.

When the same failure repeats, inspect its artifacts before modifying code.

### Compose instrumented tests

A root screen being visible does not prove that asynchronous content is
ready for interaction.

Wait for the exact semantics condition required by the action.

Use bounded condition-based waits such as `waitUntil`.

Never use arbitrary sleeps.

The test must still fail when the expected state never appears.

Run KtLint formatting and checking for every modified test source set
before push.

### Maestro

Use stable, deterministic input values.

Do not use leading or trailing spaces in `inputText` to test normalization.
Virtual keyboards and IMEs may transform whitespace into punctuation or
other characters.

Test whitespace normalization with unit or instrumented tests where the
exact input string is controlled.

Maestro should validate stable end-to-end behavior using values that are
not modified by the keyboard.

Do not use:

- coordinates;
- fixed sleeps;
- keyboard-dependent whitespace;
- autocorrect-dependent input;
- assertions against text that has not been verified in the debug
  hierarchy.

Use `hideKeyboard` only when the keyboard genuinely blocks an intended
interaction or assertion.

Use scrolling only when artifacts prove that the expected element exists
outside the visible viewport.

Every CI Maestro workflow must upload screenshots, logs, and hierarchy
artifacts on failure.

### Emulator coverage

The normal pull-request emulator profile should represent the main target
device while preserving the minimum supported API level.

Changing the emulator profile is not a substitute for responsive UI.

Small-screen and large-font regression coverage should remain in Compose
tests or scheduled CI.

### Before push

For every modified module or source set, run its applicable formatting,
static-analysis, unit, instrumented, build, and end-to-end gates.

Inspect:

- `git diff --check`;
- `git diff --name-only`;
- `git diff --cached --name-only`;
- `git status --short`.

Staging must remain empty until the user explicitly authorizes commit.

Before pushing a manual Kotlin change, run the corresponding KtLint
format and check tasks.

Do not wait for CI to discover basic formatting failures.

Before pushing a Maestro change:

1. Run the individual modified flow.
2. Run the complete Maestro suite.
3. Confirm that only the intended files changed.
4. Confirm that no debug artifact was added to Git.

## CI workflow requirements

CI workflows must:

- run on the expected branch and pull-request events;
- use a supported JDK;
- use a deterministic Android emulator configuration;
- upload test reports when useful;
- upload Maestro debug artifacts on failure;
- preserve release isolation;
- fail clearly when a real gate fails.

Do not hide failures using:

- `continue-on-error`;
- unconditional success exits;
- ignored test results;
- deleted assertions;
- disabled tests;
- broad retries.

Retries are allowed only for infrastructure startup when bounded and
explicitly justified.

## Quantity safety

Quantities must never use binary floating-point arithmetic.

Do not use:

- `Double`;
- `Float`;
- implicit floating-point conversions;
- rounding based on binary floating-point values.

Use the approved exact quantity representation and existing domain value
objects.

All quantity changes must:

- validate input;
- preserve unit dimension compatibility;
- use exact canonical conversion;
- prevent negative results;
- avoid silent clamping;
- avoid stale read-modify-write logic in the UI;
- persist atomically;
- return typed domain outcomes.

UI and ViewModels must never duplicate quantity arithmetic.

## Technical debt and lessons learned

Technical debt and lessons learned are persistent project knowledge.

Before starting any implementation, read:

- `docs/TECHNICAL_DEBT.md`
- `docs/LESSONS_LEARNED.md`

When a review identifies an issue that is intentionally not fixed because
it is P2 or P3, record it in `docs/TECHNICAL_DEBT.md`.

Each entry must include:

- identifier;
- date detected;
- severity: P2 or P3;
- affected module and files;
- concrete problem;
- current impact;
- reason for deferral;
- recommended correction;
- dependencies or preferred iteration;
- status: OPEN, PLANNED, RESOLVED or DISCARDED;
- resolution evidence when closed.

Do not silently ignore deferred findings.

Do not reopen P2 or P3 debt as a blocker for the current iteration unless:

- its severity has increased;
- it causes a reproducible regression;
- it blocks the current requirement;
- it creates data-loss, security, compatibility or architectural risk.

When an investigation produces a reusable lesson, add it to
`docs/LESSONS_LEARNED.md`.

Each lesson must include:

- context;
- incorrect assumption or failed approach;
- evidence that revealed the real cause;
- final solution;
- rule to apply in future iterations.

Before proposing a correction, check whether the same failure or lesson
already exists in these documents.

Do not repeat speculative troubleshooting already disproved by project
evidence.

## Database safety

Never use destructive migrations.

Never change the database schema unless the current task genuinely
requires it.

Before introducing a migration:

1. Confirm the existing schema cannot support the requirement.
2. Confirm compatibility with all previously stored data.
3. Add migration tests.
4. Add downgrade or rollback reasoning where applicable.
5. Document the schema change.

Repository mutations that depend on the latest stored value must execute
inside an atomic database boundary.

Features and ViewModels must never access DAO or Room entities directly.

## Test integrity

A test must prove the behavior described by its name.

Never create false-positive tests.

Do not:

- catch the assertion thrown by the test itself;
- assert only that code executed;
- assert an unrelated node;
- remove a failing assertion without replacing its contract;
- replace a behavioral test with a screenshot-only test;
- use comments as test bodies;
- make tests pass by swallowing unexpected failures.

Negative semantic assertions must directly prove the absence of the
undesired action or property.

A test intended to verify non-clickability must fail when click semantics
are introduced.

A test intended to verify accessibility must inspect real semantics, not
only visible text.

## Documentation accuracy

Documentation must describe only the final implemented behavior.

Do not claim:

- tests were executed when they were not;
- CI passed before a push and completed pull-request run;
- accessibility contracts that are not tested;
- responsive behavior that is not implemented;
- future roadmap behavior as currently available.

Validation documents must contain actual commands and real results.

Do not copy outdated test counts or previous results without re-running
the commands.

## Completion report

Report only:

- PASS or FAIL;
- root causes fixed;
- files changed;
- commands actually executed;
- test and build results;
- remaining limitations;
- `git status --short`.

Never use phrases such as:

- ALL SYSTEMS GO
- 100% COMPLIANT
- functionally PASS
- partial PASS