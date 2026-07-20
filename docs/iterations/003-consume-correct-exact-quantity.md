# Validación — Consumir y Corregir Cantidad Exacta

## Estado

PASS

## Resultado de usuario

Se ha implementado y validado la funcionalidad para consumir y corregir cantidades exactas en el inventario, asegurando la integridad de los datos mediante operaciones atómicas y validaciones estrictas de dominio.

El usuario puede:

1. Pulsar sobre un alimento del inventario para abrir el menú de gestión de cantidad.
2. Seleccionar "Consumir" para restar una cantidad del stock actual.
3. Ver una previsualización en tiempo real de la cantidad resultante (incluyendo unidad) tras el consumo.
4. Seleccionar "Corregir cantidad" para establecer un nuevo valor absoluto de stock.
5. Cambiar la unidad de medida durante la edición, siempre que sea compatible con la dimensión original (ej. g <-> kg).
6. Recibir errores de validación claros si intenta consumir más de lo disponible, usar cantidades no positivas, o si no hay cambios en una corrección.
7. Confirmar la operación y ver el inventario actualizado instantáneamente.
8. Cerrar y relanzar la aplicación verificando que los cambios persisten.

## Alcance validado

- [x] Operaciones atómicas en repositorio (Read-Modify-Write con transacciones Room)
- [x] Lógica de dominio pura para consumo y corrección con validación estricta
- [x] Previsualización dinámica basada en resultados reales de dominio (sin duplicar aritmética en ViewModel)
- [x] Hoja modal (Bottom Sheet) para selección de acción y edición
- [x] Validación de unidades compatibles (por dimensión)
- [x] Gestión de errores específicos (insuficiente, no positivo, sin cambios, no encontrado)
- [x] Tests unitarios y de integración de dominio (189 tests)
- [x] Tests de ViewModel cubriendo todos los estados del flujo (44 tests)
- [x] Tests instrumentados de repositorio y base de datos (23 tests)
- [x] Tests instrumentados de UI verificando interacciones y accesibilidad (28 tests)
- [x] Test de integración de aplicación 100% determinista (2 tests, 3 ejecuciones consecutivas exitosas)
- [x] Flujos Maestro verificando persistencia tras relanzar la app

## Casos funcionales

| Caso | Resultado |
|---|---|
| Abrir menú de gestión al pulsar fila | PASS |
| Consumir cantidad válida (misma unidad) | PASS |
| Consumir cantidad válida (unidad compatible, ej. 500g de 1kg) | PASS |
| Previsualización de consumo en tiempo real con unidad | PASS |
| Corregir cantidad a nuevo valor positivo | PASS |
| Rechazar consumo superior al disponible | PASS |
| Rechazar consumo igual al disponible | PASS |
| Rechazar corrección sin cambios (canónicos) | PASS |
| Mantener unidad actual al abrir formularios | PASS |
| Cerrar hoja modal tras éxito | PASS |
| Deshabilitar acciones durante el envío | PASS |

## Tests JVM

| Suite | Pasados |
|---|---:|
| core:domain | 189 |
| core:data | 0 |
| feature:inventory | 44 |
| **Total** | **233** |

## Tests instrumentados

| Módulo | Pasados |
|---|---:|
| core:data — Repository | 9 |
| core:database — Room | 14 |
| feature:inventory — Compose | 28 |
| app — Integration | 2 |
| **Total** | **53** |

Comandos ejecutados:

```powershell
.\gradlew.bat :core:domain:test :core:data:test :feature:inventory:testDebugUnitTest
.\gradlew.bat :core:data:connectedDebugAndroidTest :core:database:connectedDebugAndroidTest :feature:inventory:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

## Flujos Maestro (Smoke)

| Flujo | Resultado |
|---|---|
| 12_consume_quantity.yaml | PASS |
| 13_correct_quantity.yaml | PASS |
| 14_reject_invalid_mutation.yaml | PASS |
