# RFC-003 — Ubicaciones

- Estado: ACCEPTED
- Fecha: 2026-07-15
- Alcance: V1

## Contexto

El usuario necesita saber dónde está cada existencia, pero no siempre registrará una ubicación.

## Decisión

`Location` es una entidad del hogar.

Atributos mínimos:

- `LocationId`
- `HouseholdId`
- nombre
- orden
- estado activo

La referencia desde `StockItem` es opcional.

## Reglas

- La ausencia de ubicación es válida.
- Los nombres se validan y normalizan para evitar duplicados lógicos.
- Una ubicación inactiva no recibe nuevas existencias.
- Eliminar una ubicación con stock activo no borra stock.
- V1 permite mover existencias a otra ubicación o dejarlas sin ubicación.
- La jerarquía nevera/balda/cajón queda fuera de V1.

## Alternativas descartadas

- Ubicación obligatoria.
- Ubicación como texto libre en cada existencia.
- Jerarquías arbitrarias desde V1.

## Consecuencias

- Baja fricción.
- Posibilidad de filtrar por ubicación.
- Necesidad de tratar `unknown` en UI.

## Tests obligatorios

- No mover entre hogares.
- Mover conserva identidad y cantidad.
- Ubicación inactiva rechaza nuevos movimientos.
- Desactivar ubicación no elimina existencias.
