# Validación Fase 1.1 — Identificadores y cantidades

## Estado

PASS

## Alcance

Dominio Kotlin puro.

Cero Activities nuevas.
Cero Composables nuevos.
Cero YAML Maestro nuevos.
Cero tests Android de cantidades.
Cero dependencias nuevas desde app al dominio.

## Casos funcionales

| Caso | Resultado |
|---|---|
| 1 kg + 0.5 g | SUCCESS (1000.5 g) |
| 1 kg - 0.5 g | SUCCESS (999.5 g) |
| Consumir 0.5 g desde 1 kg | SUCCESS (999.5 g) |
| 1 l + 0.5 ml | SUCCESS (1000.5 ml) |
| 1 l - 0.5 ml | SUCCESS (999.5 ml) |
| 0.001 g → kg | FAILURE (TooManyDecimalPlaces) |

## Tests JVM

| Suite | Ejecutados | Pasados | Fallidos |
|---|---:|---:|---:|
| IDs | 36 | 36 | 0 |
| Modelo | 29 | 29 | 0 |
| Operaciones | 71 | 71 | 0 |
| Property | 16 | 16 | 0 |
| Integración | 12 | 12 | 0 |
| Regresión | 12 | 12 | 0 |
| Arquitectura | 12 | 12 | 0 |

## Property tests

- Propiedades: 16
- Iteraciones por propiedad: 1,000
- Iteraciones totales: 16,000
- Semilla: 20260715L

## Gates

| Gate | Resultado |
|---|---:|
| Unit | PASS |
| Architecture | PASS |
| Lint | PASS |
| Detekt | PASS |
| Ktlint | PASS |
| JaCoCo report | PASS |
| JaCoCo verification | PASS |
| Debug build | PASS |
| Release build | PASS |
| E2E build | PASS |
| Release isolation | PASS |
| Instrumentado base | PASS |
| Maestro 00 | PASS |
| Maestro 01 | PASS |
| Maestro 02 | PASS |
| Maestro suite | PASS |
| Regresión sin clean | PASS |
| git diff --check | PASS |

## CI

NO EJECUTADO

## Limitaciones

- La conversión explícita `convert()` rechaza resultados con más de 3 decimales para evitar redondeos no autorizados.
- Las operaciones mixtas (`add`, `subtract`, `consume`) priorizan la unidad del operando izquierdo siempre que el resultado sea representable, de lo contrario utilizan la unidad base.

## Confirmación

No se ha iniciado ningún bloque posterior de la Fase 1.
