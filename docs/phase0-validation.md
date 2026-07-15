# Validación final de la Fase 0 — Queda

## Estado general

`PASS LOCAL — PENDIENTE DE CI`

## Entorno

- JDK del daemon: 17
- Toolchain de compilación: 17
- Gradle: 8.13
- AGP: 8.13.2
- Kotlin: 2.3.21
- minSdk: 28
- compileSdk: 36
- targetSdk: 36
- Dispositivo: emulator-5554 (Pixel 7 Pro AVD)
- Android: 13
- API: 33
- Maestro: 2.6.0

## Gates locales

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
| Instrumented tests | PASS |
| Maestro 00_launch_app | PASS |
| Maestro 01_reset_and_launch | PASS |
| Maestro 02_seed_empty_and_launch | PASS |
| Maestro suite | PASS |

## APK

| Variante | Ruta | SHA-256 |
|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | `CB804BDEDA79B3237166A36F47A2F2AF89319EDDC946CF1D02FED18345367E7C` |
| Release | `app/build/outputs/apk/release/app-release-unsigned.apk` | `EE1378B38E4988C2D0C6364DD639DB27EEDFC1A4B764494CC5DA00DE58C2163F` |
| E2E | `app/build/outputs/apk/e2e/app-e2e.apk` | `435F6DF21AFB57F4E86A3F714014BD6D0E000649B5DA80B817EA04AD9BFE9EC5` |

## CI

| Workflow | Resultado |
|---|---:|
| Android CI | NO EJECUTADO |
| Android Instrumented | NO EJECUTADO |
| Android Maestro | NO EJECUTADO |

## Limitaciones

- El APK de release generado no está firmado (`app-release-unsigned.apk`), lo cual es el comportamiento esperado en esta fase técnica.
- `verify-release-isolation` en el entorno local utiliza una implementación personalizada en PowerShell/Python que no depende de herramientas externas del SDK Android (como `apkanalyzer`), garantizando portabilidad máxima en la validación de aislamiento de DEX y Manifiesto.
- Los workflows de GitHub Actions están configurados pero pendientes de ejecución en el servidor remoto tras el push.

## Confirmación

No se ha iniciado la Fase 1.
