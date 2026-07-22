# Queda — Technical Debt

| ID | Date | Severity | Module | Issue | Impact | Reason for Deferral | Recommended Correction | Status |
|---|---|---|---|---|---|---|---|---|
| TD-001 | 2026-07-20 | P3 | feature:inventory | Barcode editing not supported after item creation. | User cannot fix or add a barcode to an existing item without delete/re-add. | Out of scope for Iteration 4. | Implement edit flow for barcode in Product details. | OPEN |
| TD-002 | 2026-07-20 | P3 | feature:inventory | Only one barcode per product supported. | Products with multiple valid barcodes (e.g., regional variations) require multiple inventory entries. | Scope simplification for MVP. | Support collection of barcodes in Product model and entity. | OPEN |
| TD-003 | 2026-07-20 | P2 | feature:inventory | CameraResourceCoordinator lifecycle test coverage is limited. | Production cleanup is implemented but tests only verify executor and idempotent close, not every collaborator invocation. | production functionality verified; improvement deferred to future testing iteration. | Improve dependency-injected cleanup verification. | OPEN |
