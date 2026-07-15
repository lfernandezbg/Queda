# RFC-002 — Modelo de cantidad

- Estado: ACCEPTED
- Fecha: 2026-07-15
- Alcance: V1

## Contexto

El usuario puede conocer una cantidad exacta o solo una estimación visual.

## Decisión

`StockQuantity` es un tipo sellado con dos modos:

### Exact

- valor decimal no negativo;
- unidad compatible;
- precisión explícita.

Unidades V1:

- `UNIT`
- `GRAM`
- `KILOGRAM`
- `MILLILITER`
- `LITER`

Las conversiones solo ocurren dentro de la misma dimensión.

### Approximate

Niveles ordenados:

- `EMPTY`
- `ALMOST_EMPTY`
- `LOW`
- `MEDIUM`
- `HIGH`
- `FULL`

`EMPTY` finaliza la disponibilidad activa.

## Reglas

- No se mezcla exacto y aproximado en una misma operación.
- Cambiar de modo requiere una corrección explícita.
- Consumir exacto exige una cantidad exacta compatible.
- Consumir aproximado usa una transición o nivel objetivo explícito.
- No se infiere porcentaje numérico universal desde un nivel.
- La UI puede mostrar iconos o descripciones, pero el dominio conserva el enum.

## Alternativas descartadas

- Representar todo como porcentaje.
- Usar `Double`.
- Convertir automáticamente niveles a cantidades.
- Permitir cadenas libres.

## Consecuencias

- Tipado seguro.
- Menos precisión falsa.
- Operaciones diferentes según modo.

## Tests obligatorios

- Nunca se obtiene cantidad negativa.
- Conversiones válidas entre g/kg y ml/l.
- Conversión incompatible falla.
- Transiciones aproximadas válidas.
- `EMPTY` produce agotamiento operativo.
