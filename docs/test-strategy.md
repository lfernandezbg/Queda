# Estrategia de testing — Queda

Los únicos resultados válidos para cada ejecución son:

- `PASS`
- `FAIL`
- `NO EJECUTADO`

| Gate | Local | CI |
|---|---:|---:|
| Tests unitarios | PASS | NO EJECUTADO |
| Tests de arquitectura | PASS | NO EJECUTADO |
| Android Lint | PASS | NO EJECUTADO |
| Detekt | PASS | NO EJECUTADO |
| Ktlint | PASS | NO EJECUTADO |
| JaCoCo report | PASS | NO EJECUTADO |
| JaCoCo verification | PASS | NO EJECUTADO |
| Assemble Debug | PASS | NO EJECUTADO |
| Assemble Release | PASS | NO EJECUTADO |
| Assemble E2E | PASS | NO EJECUTADO |
| Aislamiento release | PASS | NO EJECUTADO |
| Instrumentados API 33 | PASS | NO EJECUTADO |
| Instrumentados API 28 | NO EJECUTADO | NO EJECUTADO |
| Maestro 2.6.0 individual | PASS | NO EJECUTADO |
| Maestro 2.6.0 suite | PASS | NO EJECUTADO |
| Android CI | NO EJECUTADO | NO EJECUTADO |
| Android Instrumented | NO EJECUTADO | NO EJECUTADO |
| Android Maestro | NO EJECUTADO | NO EJECUTADO |

## Estado de la fase

La Fase 0 tiene validación local completa.

Solo podrá considerarse cerrada globalmente cuando:

1. Android CI termine en `PASS`.
2. Android Instrumented sobre API 28 termine en `PASS`.
3. Android Maestro sobre API 28 termine en `PASS`.
4. No exista ningún resultado declarado sin una ejecución real.