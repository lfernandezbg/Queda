# Iteration 005: Simple Presence (Hay / No hay)

## Objective
Implement vertical tracking for "Simple Presence": items that don't need exact numerical quantities but just an indication of whether they are available or not.

## Semantics
- Absence does NOT mean deletion.
- A "Presence" item keeps its identity (name, barcode, household) but alternates between "Hay" (Present) and "No hay" (Absent).
- "No hay" items remain visible in the inventory.

## Accepted Cases
- User selects "Solo presencia" when adding an item.
- Presence items show "Hay" or "No hay" in the list.
- Tapping a presence item opens a management sheet with a toggle.
- Barcode scanning works for presence items.

## Migration 2 -> 3
- Added `trackingMode` (EXACT, PRESENCE).
- Added `isPresent` (Boolean).
- Made `quantityAmount` and `quantityUnit` nullable.
- Existing items from version 2 are migrated to `EXACT` mode.

## Affected Modules
- `core:model`: New `PresenceQuantity` and `StockTrackingMode`.
- `core:domain`: New `SetPresenceUseCase` and updated `AddExactInventoryItemUseCase`.
- `core:database`: Schema version 3, migration 2->3, and updated DAO/Entities.
- `core:data`: Updated repository and mappers.
- `feature:inventory`: Updated screens, viewmodels and UI models.

## Coverage and Validation
- **JVM Tests (PASS)**:
    - `core:model`: `StockQuantity` logic.
    - `core:domain`: `AddExactInventoryItemUseCase` (Exact and Presence paths), `SetPresenceUseCase` (Idempotency and states).
    - `feature:inventory`: `InventoryViewModel` (Selection logic for both modes), `AddExactItemViewModel`.
- **Instrumented Tests (LOCAL PASS — 88/88)**:
- **Maestro E2E (LOCAL PASS — 21/21 flows)**:

## Risks or Technical Debt
- Conversion between Exact and Presence mode is not implemented yet.
