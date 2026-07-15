# Estrategia de testing — Queda

Los únicos estados válidos son:

- `PASS`
- `FAIL`
- `NO EJECUTADO`

No se utiliza REA&#8205;DY.

| Gate | Local | CI | Estado actual |
|---|---:|---:|---:|
| Tests unitarios | PASS | NO EJECUTADO | PASS LOCAL |
| Tests de arquitectura | PASS | NO EJECUTADO | PASS LOCAL |
| Android Lint | PASS | NO EJECUTADO | PASS LOCAL |
| Detekt | PASS | NO EJECUTADO | PASS LOCAL |
| Ktlint | PASS | NO EJECUTADO | PASS LOCAL |
| JaCoCo report | PASS | NO EJECUTADO | PASS LOCAL |
| JaCoCo verification | PASS | NO EJECUTADO | PASS LOCAL |
| Assemble Debug | PASS | NO EJECUTADO | PASS LOCAL |
| Assemble Release | PASS | NO EJECUTADO | PASS LOCAL |
| Assemble E2E | PASS | NO EJECUTADO | PASS LOCAL |
| Aislamiento release | PASS | NO EJECUTADO | PASS |
| Instrumentados API 33 | PASS | — | PASS |
| Instrumentados API 28 | NO EJECUTADO | NO EJECUTADO | NO EJECUTADO |
| Maestro 2.6.0 individual | PASS | NO EJECUTADO | PASS |
| Maestro 2.6.0 suite | PASS | NO EJECUTADO | PASS |
| Android CI | — | NO EJECUTADO | NO EJECUTADO |
| Android Instrumented | — | NO EJECUTADO | NO EJECUTADO |
| Android Maestro | — | NO EJECUTADO | NO EJECUTADO |

La Fase 0 solo será `PASS` cuando:

1. Todos los gates locales terminen correctamente.
2. El aislamiento release termine en `PASS`.
3. API 28 termine en `PASS`.
4. Los tres workflows de GitHub Actions terminen en verde.
5. No exista ningún resultado declarado sin evidencia.

Después de ejecutar los gates, actualiza esta tabla con los resultados reales.

No cambies un estado a `PASS` si el comando correspondiente no se ejecutó.
