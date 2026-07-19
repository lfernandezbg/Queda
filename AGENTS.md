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