# RFC-001 — Producto y existencia

- Estado: ACCEPTED
- Fecha: 2026-07-15
- Alcance: V1

## Contexto

Un mismo producto puede existir varias veces con distintas fechas, ubicaciones, cantidades y estados.

## Problema

Modelar únicamente un total por producto impide representar envases abiertos, lotes con caducidades diferentes y ubicaciones distintas.

## Decisión

Se separan dos conceptos:

### Product

Identidad reutilizable y descriptiva.

Atributos mínimos:

- `ProductId`
- `HouseholdId`
- nombre normalizado
- nombre visible
- categoría opcional
- unidad preferida opcional
- estado activo

### StockItem

Existencia física concreta.

Atributos mínimos:

- `StockItemId`
- `HouseholdId`
- `ProductId`
- cantidad
- ubicación opcional
- estado abierto o cerrado
- fecha de compra opcional
- fecha de apertura opcional
- caducidad opcional
- consumo preferente opcional
- fecha de creación
- fecha de actualización
- estado operativo

## Estado operativo

- `ACTIVE`
- `DEPLETED`
- `DISCARDED`
- `ARCHIVED`

`ACTIVE` participa en inventario disponible.

## Operaciones

- Crear.
- Abrir.
- Consumir.
- Corregir.
- Mover.
- Descartar.
- Agotar.
- Archivar.

## Alternativas descartadas

- Un único total por producto.
- Guardar fechas en producto.
- Guardar ubicación en producto.
- Borrar físicamente al llegar a cero.

## Consecuencias

- Mayor fidelidad.
- FEFO natural.
- Más filas de almacenamiento.
- Necesidad de vistas agregadas por producto.

## Tests obligatorios

- Dos existencias del mismo producto conservan identidades independientes.
- Cambiar una existencia no modifica otra.
- Una existencia no puede referenciar otro hogar.
- Estados inactivos no participan en disponibilidad.
