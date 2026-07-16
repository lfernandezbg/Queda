# Testing Matrix - Phase 1.1

## JVM Unit Tests (core:model & core:domain)

| Suite | Goal | Tests | Status |
| :--- | :--- | :---: | :---: |
| **Identifiers** | Non-blank, UUID-backed, encapsulated value classes. | 36 | |
| **Quantity Model** | Normalization, 3-decimal limit, BigDecimal usage. | 29 | |
| **Operations** | Arithmetic, conversions, fallback logic, approximate rules. | 71 | |
| **Property-Based** | 16 Invariants verified with 1,000 random iterations each. | 16 | |
| **Integration** | Multi-step domain sequences, state consistency. | 12 | |
| **Regression** | Verification of fixes for edge cases (fallback precision). | 12 | |
| **Architecture** | Protection of Kotlin pure domain, isolation rules. | 12 | |

**Total JVM Methods**: 188  
**Property Iterations**: 16,000  
**Seed**: 20260715L

## Instrumented Tests (app)

| Suite | Goal | Tests | Status |
| :--- | :--- | :---: | :---: |
| **App Shell** | Basic activity launch and navigation shell. | 1 | |

## Maestro Smoke Suite (.maestro)

| Flow | Goal | Status |
| :--- | :--- | :---: |
| **00_launch_app** | App starts and displays main screen. | |
| **01_reset_and_launch** | App handles data reset and restart. | |
| **02_seed_empty_and_launch** | App handles empty state. | |

## Quality Gates

- **Lint**: No errors in app or core modules.
- **Detekt**: No violations. Suppressions only in ArchitectureTest (Phase 0).
- **Ktlint**: Strict formatting enforced.
- **JaCoCo**: Coverage reports generated for new domain logic.
- **Release Isolation**: No E2E or test-only tokens in release builds.
