# Domain Invariants

## Identity and Ownership
1. Every aggregate has a non-blank identifier.
2. A stock item belongs to a single household.
3. A product belongs to a single household.
4. A location belongs to a single household.
5. A stock item references a product from the same household.
6. A stock item can only be in one location of the same household.

## Quantity
7. An exact quantity can never be negative.
8. A zero exact quantity represents operational depletion. Zero is internally normalized.
9. An approximate quantity always uses a known level.
10. A stock item uses a single quantity mode at any time: exact or approximate.
11. Consuming can never exceed the exact quantity available.
12. A correction can increase or decrease, but never produce an invalid quantity.
13. Incompatible dimensions (e.g., MASS vs VOLUME) cannot be combined.
14. Exact calculations use safe decimal representation (BigDecimal); Double or Float are forbidden.
15. No compatible operation loses precision; if the output unit cannot represent it (max 3 decimals), the base unit is used.
16. Domain operations do not modify the original objects (immutability).
17. Approximate consumption requires the target level to be strictly lower than the current one.
18. Exact quantities are limited to 3 decimal places.

## State
19. A discarded or depleted stock item is not active.
20. An inactive stock item cannot be consumed or opened.
21. An open stock item cannot be closed via normal operations.
22. Opening date cannot exist if the state is closed.
23. An operation cannot have a date earlier than the stock item creation.
24. Discarding is distinct from consumption.
25. Correction is distinct from consumption or discarding.

## Dates
26. Expiration date and best-before date are distinct concepts.
27. If both exist, strict expiration has priority for safety.
28. Opening date cannot be later than the operation moment.
29. Unknown dates are represented as absence, not default values.

## Locations
30. Location is optional.
31. Deleting a location with active stock items requires an explicit policy (transfer or leave as unknown).
32. Moving a stock item preserves its identity and quantity.

## History
33. Relevant domain actions generate immutable events.
34. History is not modified to hide corrections.
35. Undoing an action creates a compensatory action; it does not erase the original event.

## Priority
36. Consumption priority is deterministic for the same inputs.
37. An expired stock item never has lower urgency than a future one.
38. When dates are equal, an open stock item is prioritized over a closed one.
39. When all factors are equal, a stable criterion is used to avoid random ordering.

## Shopping List
40. A shopping entry never alters the inventory directly without explicit confirmation.
