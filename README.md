# Queda

## Arquitectura
Proyecto Android multi-módulo siguiendo Clean Architecture y principios SOLID.

### Módulos
- `:app`: Punto de entrada, inyección de dependencias y navegación principal.
- `:core:*`: Módulos transversales (data, domain, model, database, designsystem, testing).
- `:feature:*`: Módulos de funcionalidad (inventory, onboarding, settings, shopping, today).
- `:quality:architecture`: Tests de integridad arquitectónica con ArchUnit.
- `:benchmark` & `:baselineprofile`: Herramientas de rendimiento.

## Versiones
- **JDK**: 17
- **Gradle**: 8.13
- **AGP**: 8.13.2
- **Kotlin**: 2.3.21
- **Compose BOM**: 2026.06.01
- **compileSdk / targetSdk**: 36
- **minSdk**: 28

## Cómo compilar
```powershell
.\gradlew.bat assembleDebug
```

## Calidad y Gates
Para ejecutar todos los gates locales:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-all-fast.ps1
```

## Maestro E2E
Requiere Maestro 2.6.0.
Para ejecutar flujos de humo:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-maestro-smoke.ps1 -DeviceSerial <SERIAL>
```

## Alcance Fase 0
- Infraestructura base configurada.
- Sistema de diseño inicial.
- Pipeline de CI/CD.
- Herramientas de calidad (Detekt, Ktlint, Jacoco, ArchUnit).
- **Confirmación**: No existe funcionalidad de negocio implementada en esta fase.
