# Estrategia de Testing - Queda

| Requisito | Unit | Architecture | Instrumented | Maestro | CI | Estado |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| Infraestructura Base | PASS | PASS | PASS | PASS | NO EJECUTADO | READY |
| Aislamiento de Módulos | - | PASS | - | - | NO EJECUTADO | READY |
| Limpieza de Release | - | PASS | - | - | NO EJECUTADO | READY |
| Cobertura (JaCoCo) | PASS | - | - | - | NO EJECUTADO | READY |

## Matriz Inicial
- **Unit**: JUnit 4, enfocado en utilidades y parsers (`core:testing`).
- **Architecture**: ArchUnit 1.4.2, validando dependencias prohibidas y estructura de paquetes.
- **Instrumented**: Compose UI Test Rule, validando el App Root Shell.
- **Maestro**: Smoke tests sobre APK con hooks de control E2E.
- **CI**: GitHub Actions ejecutando gates de compilación, calidad y aislamiento.
