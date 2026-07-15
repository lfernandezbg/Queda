# Maestro E2E en Queda

## Versión
- **Maestro**: 2.6.0 (fijada para Fase 0)

## Preparación
1. Compilar el APK de E2E: `./gradlew :app:assembleE2E`
2. Instalar el APK en el dispositivo: `adb install app/build/outputs/apk/e2e/app-e2e.apk`

## Deep Links y Tags
- El esquema E2E es `queda-e2e`.
- Comandos soportados via URI:
  - `queda-e2e://reset`
  - `queda-e2e://seed/empty`

## Flujos Smoke
- `00_launch_app.yaml`: Verifica el arranque básico.
- `01_reset_and_launch.yaml`: Verifica el comando de reset.
- `02_seed_empty_and_launch.yaml`: Verifica el comando de seed vacío.

## Ejecución
```bash
maestro --device=<SERIAL> test .maestro/flows/smoke
```

## Resultados
Los reportes se generan en `.maestro/results/report.xml` en formato JUnit.
