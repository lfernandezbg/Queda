# RFC-002: Quantity Model

## Context
Project Queda requires a robust way to handle product quantities, supporting both exact measurements (e.g., 500g, 1.5L) and approximate levels (e.g., "Almost Empty", "Full").

## Decisions

### 1. Data Representation
- **BigDecimal Mandatory**: All exact quantities must use `java.math.BigDecimal` to avoid floating-point precision issues.
- **Pure Kotlin**: The quantity model resides in `core:model` and operations in `core:domain`, with zero dependencies on Android, Room, or any third-party library except the Kotlin standard library and `BigDecimal`.

### 2. Exact Quantities (`ExactQuantity`)
- **Normalized Values**: `ExactQuantity` stores amounts in a normalized form (using `stripTrailingZeros()`).
- **Zero Normalization**: `0.000` is normalized to `BigDecimal.ZERO`.
- **Precision Limit**: Maximum of three (3) decimal places are allowed. Any amount exceeding this limit will be rejected during construction.
- **Mixed Dimensions**: Operations between different dimensions (e.g., MASS + VOLUME) are strictly prohibited.
- **Fractional Units**: The `UNIT` (COUNT) dimension supports fractions (e.g., 0.5 units).

### 3. Approximate Quantities (`ApproximateQuantity`)
- **Explicit Levels**: `EMPTY`, `ALMOST_EMPTY`, `LOW`, `MEDIUM`, `HIGH`, `FULL`.
- **Explicit Order**: Levels have an explicit numeric order (0 to 5) used for validation during consumption.

### 4. Operations (`QuantityOperations`)
- **No Rounding or Truncation**: The system never invents precision. If an operation result cannot be represented with 3 decimal places in the target unit, it must either fallback to a base unit or fail if still not representable.
- **Base Unit Fallback**: Operations (`add`, `subtract`, `consume`) are performed in base units (GRAM, MILLILITER, UNIT). 
    - The result preserves the unit of the first operand if representable with 3 decimals.
    - If not representable in the preferred unit, the system falls back to the base unit.
- **Consumption Validation**:
    - `ExactQuantity`: Cannot consume more than available (no negative stock).
    - `ApproximateQuantity`: `consumeApproximate` requires the target level to be strictly lower than the current level.
- **Explicit Conversion**: `convert()` can fail if the conversion results in more than 3 decimal places (e.g., 0.001g to kg).

### 5. Implementation Constraints
- No UI components or persistence (Room/DAOs) are implemented for quantities in Phase 1.1.
- No `Double` or `Float` allowed in any part of the quantity domain.
- No use of Enum `.ordinal` for business logic.
