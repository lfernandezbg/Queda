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
9. A presence quantity indicates only availability (Hay / No hay).
10. A stock item uses a single tracking mode at any time: exact, presence, or approximate.
11. "No hay" (Absent) state preserves the product and stock identity.
12. There is no implicit conversion between tracking modes.
13. Consuming can never exceed the exact quantity available.
14. A correction can increase or decrease, but never produce an invalid quantity.
15. Incompatible dimensions (e.g., MASS vs VOLUME) cannot be combined.
16. Exact calculations use safe decimal representation (BigDecimal); Double or Float are forbidden.
17. No compatible operation loses precision; if the output unit cannot represent it (max 3 decimals), the base unit is used.
18. Domain operations do not modify the original objects (immutability).
19. Approximate consumption requires the target level to be strictly lower than the current one.
20. Exact quantities are limited to 3 decimal places.

## State
21. A discarded or depleted stock item is not active.
22. An inactive stock item cannot be consumed or opened.
23. An open stock item cannot be closed via normal operations.
24. Opening date cannot exist if the state is closed.
25. An operation cannot have a date earlier than the stock item creation.
26. Discarding is distinct from consumption.
27. Correction is distinct from consumption or discarding.

## Dates
28. Expiration date and best-before date are distinct concepts.
29. If both exist, strict expiration has priority for safety.
30. Opening date cannot be later than the operation moment.
31. Unknown dates are represented as absence, not default values.

## Locations
32. Location is optional.
33. Deleting a location with active stock items requires an explicit policy (transfer or leave as unknown).
34. Moving a stock item preserves its identity and quantity.

## History
35. Relevant domain actions generate immutable events.
36. History is not modified to hide corrections.
37. Undoing an action creates a compensatory action; it does not erase the original event.

## Priority
38. Consumption priority is deterministic for the same inputs.
39. An expired stock item never has lower urgency than a future one.
40. When dates are equal, an open stock item is prioritized over a closed one.
41. When all factors are equal, a stable criterion is used to avoid random ordering.

## Shopping List
42. A shopping entry never alters the inventory directly without explicit confirmation.
