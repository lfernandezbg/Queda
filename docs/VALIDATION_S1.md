# Informe de Validación Completa y Auditoría Final — S1: Hogar Compartido, Sincronización y Open Food Facts

## Estado general

PASS

---

## 1. AVD y Dispositivo de Pruebas Utilizado Exclusivamente
- **Dispositivo AVD de Queda:** `Pixel_7_Pro_API_33_Clean_QUEDA`
- **Serial ADB asignado:** `emulator-5556` (verificado con `adb devices` y `adb -s emulator-5556 emu avd name`).
- **Estado de arranque:** `device` / `sys.boot_completed = 1` tras Cold Boot (`-no-snapshot-load`).
- **AVD de GymProgress (`Pixel_7_Pro_API_33_Clean`, `emulator-5554`):** Permanece encendida, intacta y sin modificaciones ni ejecuciones dirigidas a ella.

---

## 2. Reglas de Seguridad de Cloud Firestore (`firestore.rules`)

### Funciones de validación implementadas:
- `validAmount(amt)`: Exige que la cantidad en modo `EXACT` sea un decimal positivo mayor que cero (`^[0-9]+(\.[0-9]{1,3})?$`), rechazando cantidades negativas (ej. `"-5"`), cero (`"0"`) o formatos con más de 3 decimales (`"1.2345"`).
- `validUnit(u)`: Restringe la unidad al enumerado explícito (`UNIT`, `GRAM`, `KILOGRAM`, `MILLILITER`, `LITER`).
- Coherencia entre modos `EXACT` y `PRESENCE`:
  - `EXACT`: requiere `quantityAmount` y `quantityUnit` válidos, y `isPresent == null`.
  - `PRESENCE`: requiere `isPresent` booleano, y `quantityAmount` y `quantityUnit` nulos.
- Campos `displayName`, `normalizedName` y `barcode` validados en formato y longitud.

### Pruebas ejecutadas en Firebase Emulator Suite (`firebase-tests/firestore-rules.test.mjs`):
- `quantityAmount = "-5"` en creación/actualización -> **RECHAZADA (PERMISSION_DENIED)**.
- `quantityAmount = "0"` en creación/actualización -> **RECHAZADA (PERMISSION_DENIED)**.
- Formatos decimales e incoherencias entre `EXACT` y `PRESENCE` -> **RECHAZADOS**.
- Operaciones legítimas de alta, consumo, corrección y cambio de presencia -> **PERMITIDAS**.

---

## 3. Pruebas de Sincronización Multiusuario en Motor Kotlin (`HouseholdSyncIntegrationTest.kt`)

Prueba de integración end-to-end pura en Kotlin dentro de `:core:data:connectedDebugAndroidTest` ejecutada exclusivamente en `emulator-5556` con dos usuarios autenticados independientes (`userA`, `userB`), dos instancias de Room independientes (`dbA`, `dbB`) y los emuladores locales de Firebase Auth (9099) y Firestore (8080) mediante `adb -s emulator-5556 reverse`.

### Flujos verificados:
1. **Crear y Unirse a Hogar:** Usuario A crea el hogar ("Casa Sync"), genera código de invitación (32 caracteres hex); Usuario B se une.
2. **Alta Visible para Ambos:** Usuario A añade "Arroz" (2 kg); Usuario B lo recibe automáticamente en tiempo real en `dbB`.
3. **Consumo Concurrente:** Usuario A consume 1 kg; Usuario B observa la actualización a 1 kg en `dbB`.
4. **Corrección:** Usuario B corrige la cantidad a 5 kg; Usuario A observa la actualización a 5 kg en `dbA`.
5. **Presencia:** Usuario A añade "Sal" (Hay); Usuario B cambia presencia a "No hay"; Usuario A observa "No hay" en `dbA`.
6. **Cola Sin Conexión y Reconexión:** Usuario B se desconecta (`syncB.stop()`), encola alta local de "Lentejas" en `syncDaoB`. Usuario B reconecta (`syncB.start(...)`), procesa la cola y Usuario A recibe "Lentejas" en `dbA`.
7. **Actualización Manual:** `managerA.refresh()` completa confirmando `UPDATED`.
8. **Arranque Local sin Red:** Lectura de inventario local en Room sin sesión activa de red.

---

## 4. Consulta y Revisión de Open Food Facts

### Pruebas deterministas con HTTP simulado
- `OpenFoodFactsProductLookupTest.kt`: Pruebas unitarias deterministas de parsing y fallos de red con respuestas HTTP simuladas (`FakeConnection`).
- Flujo Maestro `21_scan_online_suggestion_review.yaml`: Utiliza el catálogo E2E simulado (`queda-e2e://scan?barcode=3017620422003`) para garantizar determinismo sin depender de la API pública en CI.

### Prueba supervisada contra la API real de producción (`OpenFoodFactsLiveApiSupervisedTest.kt`)
- `testLiveApiSupervised()`: Exige estrictamente `ExternalProductResult.Found` con nombre no vacío (`displayValue.isNotBlank()`) para un código existente (`3017620422003`).
- `testLiveApiNonExistentBarcode()`: Exige estrictamente `ExternalProductResult.NotFound` para un código inexistente (`0000000000000`).
- **Aserción corregida:** No acepta `Unavailable` como PASS en la ejecución supervisada real.

---

## 5. Reporte Único y Cuantificación de Flujos Maestro (22 Flujos)

Ejecutados exclusivamente en `emulator-5556` usando `scripts/test-maestro-smoke.ps1 -DeviceSerial emulator-5556`.

### Reporte JUnit XML consolidado (`.maestro/results/report.xml`):
- Total de testcases únicos: **22**
- Fallos: **0**
- Errores: **0**
- Estado de todos los flujos: **SUCCESS**

| ID de Flujo | Archivo de Flujo | Estado |
|---|---|---:|
| `00_launch_app` | `00_launch_app.yaml` | SUCCESS |
| `01_reset_and_launch` | `01_reset_and_launch.yaml` | SUCCESS |
| `02_seed_empty_and_launch` | `02_seed_empty_and_launch.yaml` | SUCCESS |
| `03_inventory_empty_state` | `03_inventory_empty_state.yaml` | SUCCESS |
| `04_add_exact_unit_item` | `04_add_exact_unit_item.yaml` | SUCCESS |
| `05_add_exact_mass_item` | `05_add_exact_mass_item.yaml` | SUCCESS |
| `06_add_exact_volume_comma_item` | `06_add_exact_volume_comma_item.yaml` | SUCCESS |
| `07_add_exact_item_validation` | `07_add_exact_item_validation.yaml` | SUCCESS |
| `08_duplicate_normalized_name` | `08_duplicate_normalized_name.yaml` | SUCCESS |
| `09_cancel_add_item` | `09_cancel_add_item.yaml` | SUCCESS |
| `10_multiple_items_visible` | `10_multiple_items_visible.yaml` | SUCCESS |
| `11_item_persists_after_relaunch` | `11_item_persists_after_relaunch.yaml` | SUCCESS |
| `12_consume_quantity` | `12_consume_quantity.yaml` | SUCCESS |
| `13_correct_quantity` | `13_correct_quantity.yaml` | SUCCESS |
| `14_reject_invalid_mutation` | `14_reject_invalid_mutation.yaml` | SUCCESS |
| `15_scan_new_barcode_item` | `15_scan_new_barcode_item.yaml` | SUCCESS |
| `16_scan_existing_barcode_item` | `16_scan_existing_barcode_item.yaml` | SUCCESS |
| `17_invalid_barcode_rejected` | `17_invalid_barcode_rejected.yaml` | SUCCESS |
| `18_add_presence_item` | `18_add_presence_item.yaml` | SUCCESS |
| `19_toggle_presence` | `19_toggle_presence.yaml` | SUCCESS |
| `20_scan_existing_presence` | `20_scan_existing_presence.yaml` | SUCCESS |
| `21_scan_online_suggestion_review` | `21_scan_online_suggestion_review.yaml` (catálogo E2E simulado) | SUCCESS |

---

## 6. Resumen de Ejecución de Calidad

| Gate / Comprobación | Comando | Código de Salida | Estado |
|---|---|---:|---:|
| **Tests JVM limpios** | `gradlew.bat clean test --rerun-tasks` | `0` | **PASS** (519/519 pasados) |
| **Prueba de Arquitectura** | `gradlew.bat :quality:architecture:testDebugUnitTest` | `0` | **PASS** (1/1 pasado) |
| **Reglas de Firestore** | `npm.cmd --prefix firebase-tests test` | `0` | **PASS** |
| **Instrumentados (`core:designsystem`)** | `gradlew.bat :core:designsystem:connectedDebugAndroidTest` | `0` | **PASS** (8/8 pasados) |
| **Instrumentados (`core:database`)** | `gradlew.bat :core:database:connectedDebugAndroidTest` | `0` | **PASS** (22/22 pasados) |
| **Instrumentados (`core:data`)** | `gradlew.bat :core:data:connectedDebugAndroidTest` | `0` | **PASS** (21/21 pasados) |
| **Instrumentados (`feature:inventory`)** | `gradlew.bat :feature:inventory:connectedDebugAndroidTest` | `0` | **PASS** (47/47 pasados) |
| **Instrumentados (`app`)** | `gradlew.bat :app:connectedDebugAndroidTest` | `0` | **PASS** (2/2 pasados) |
| **Suite Maestro E2E** | `test-maestro-smoke.ps1 -DeviceSerial emulator-5556` (4 shards) | `0` | **PASS** (22/22 pasados) |
| **Android Lint** | `gradlew.bat lint` | `0` | **PASS** |
| **Detekt** | `gradlew.bat detekt` | `0` | **PASS** |
| **KtLint** | `gradlew.bat ktlintFormat ktlintCheck` | `0` | **PASS** |
| **JaCoCo Cobertura** | `gradlew.bat jacocoTestReport jacocoTestCoverageVerification` | `0` | **PASS** |
| **Ensamblado Debug, Release, E2E** | `gradlew.bat :app:assembleDebug :app:assembleRelease :app:assembleE2E` | `0` | **PASS** |
| **Aislamiento de Release** | `verify-release-isolation.ps1` | `0` | **PASS** |
| **Formato Git Diff** | `git diff --check` | `0` | **PASS** (Aviso LF/CRLF informativo, exit 0) |
| **Configuración local** | `gradle.properties` | - | REVISADO LOCALMENTE |

---

## 7. Análisis de Git Diff Check e Índice Git

- **Análisis de `git diff --check`:** El comando devuelve código de salida `0` (ÉXITO). El aviso `warning: in the working copy of '.gitignore', LF will be replaced by CRLF the next time Git touches it` es una advertencia de conversión de finales de línea de Git, no un error de sintaxis ni de espacios en blanco. No existen espacios en blanco al final de línea en ninguna línea modificada.
- **Estado de Staging en Git:**
  - Archivos con entrada preparada previa en el índice (`A` / `AM` en `git status`):
    - `core/data/src/androidTest/AndroidManifest.xml`
    - `core/data/src/androidTest/kotlin/com/luisete/queda/core/data/household/HouseholdSyncIntegrationTest.kt`
    - `core/data/src/androidTest/kotlin/com/luisete/queda/core/data/inventory/OpenFoodFactsLiveApiSupervisedTest.kt`
    - `docs/VALIDATION_S1.md`
  - Resto de archivos modificados en el árbol de trabajo (`M`) o no seguidos (`??`).
  - No se ha realizado `git add`, `commit` ni `push` adicional.

---

## 8. Pendiente Expresamente Declarado

- **NO EJECUTADO:** Verificación interactiva manual del proyecto de producción de Firebase con dos cuentas de correo reales en dos teléfonos Android físicos distintos.
