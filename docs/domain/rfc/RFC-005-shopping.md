# RFC-005 — Lista de compra

- Estado: ACCEPTED
- Fecha: 2026-07-15
- Alcance: V1

## Contexto

La lista de compra debe permitir registrar intención sin alterar la realidad del inventario.

## Decisión

`ShoppingEntry` es una entidad independiente.

Atributos mínimos:

- `ShoppingEntryId`
- `HouseholdId`
- `ProductId` opcional
- texto visible
- cantidad deseada opcional
- estado
- origen
- fecha de creación
- fecha de finalización opcional

Estados:

- `PENDING`
- `COMPLETED`
- `DISMISSED`

Orígenes:

- `MANUAL`
- `LOW_STOCK_SUGGESTION`
- `DEPLETED_SUGGESTION`

## Reglas

- Completar no crea stock.
- Registrar stock desde una compra es una acción explícita posterior.
- Una sugerencia no se añade automáticamente sin política autorizada.
- Las sugerencias deben ser idempotentes para evitar duplicados.
- Una entrada puede existir sin producto catalogado.

## Alternativas descartadas

- Convertir automáticamente compra en existencia.
- Mezclar entradas con existencias futuras.
- Eliminar entradas completadas sin historial.

## Tests obligatorios

- Completar no altera inventario.
- No duplicar sugerencia lógica activa.
- Entrada libre sin `ProductId`.
- Estados y transiciones válidas.
