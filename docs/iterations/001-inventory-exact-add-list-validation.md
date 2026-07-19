# Validación — Inventory Exact Add/List

## Estado

PASS

## Resultado de usuario

Se ha implementado y validado la funcionalidad completa para añadir y listar alimentos con cantidad exacta.

El usuario puede:

1. Abrir la aplicación y acceder al inventario.
2. Ver el estado vacío.
3. Añadir un alimento indicando nombre, cantidad y unidad.
4. Recibir errores de validación ante datos incorrectos.
5. Impedir nombres duplicados mediante normalización.
6. Volver al inventario y ver el alimento guardado.
7. Añadir varios alimentos.
8. Cerrar y relanzar la aplicación conservando los datos.

## Alcance validado

- [x] UI real con Jetpack Compose
- [x] ViewModels
- [x] Casos de uso
- [x] Modelos de dominio
- [x] Repositorio
- [x] Persistencia Room
- [x] Flows reactivos
- [x] Navegación
- [x] Inyección de dependencias
- [x] Validación de entradas
- [x] Accesibilidad básica
- [x] Tests JVM
- [x] Tests instrumentados Room
- [x] Tests instrumentados Compose
- [x] Tests instrumentados de aplicación
- [x] Maestro sobre emulador real

## Casos funcionales

| Caso | Resultado |
|---|---|
| Mostrar inventario vacío | PASS |
| Añadir 6 unidades | PASS |
| Añadir 1,5 kg | PASS |
| Añadir 1,25 l usando coma decimal | PASS |
| Rechazar formulario vacío | PASS |
| Rechazar cantidad cero | PASS |
| Rechazar más de 3 decimales | PASS |
| Rechazar nombre duplicado normalizado | PASS |
| Cancelar sin guardar | PASS |
| Mostrar varios elementos | PASS |
| Mantener los datos tras relanzar la aplicación | PASS |

## Tests JVM

| Suite | Ejecutados | Pasados | Fallidos |
|---|---:|---:|---:|
| ProductName | 18 | 18 | 0 |
| Inventory model | 6 | 6 | 0 |
| Quantity parser | 23 | 23 | 0 |
| Add result | 4 | 4 | 0 |
| Add use case | 24 | 24 | 0 |
| Observe use case | 4 | 4 | 0 |
| Property | 8 | 8 | 0 |
| Repository | 14 | 14 | 0 |
| Inventory ViewModel | 6 | 6 | 0 |
| Add ViewModel | 14 | 14 | 0 |
| Quantity formatter | 6 | 6 | 0 |
| UI model mapping | 1 | 1 | 0 |
| Regression | 10 | 10 | 0 |
| Architecture | 20 | 20 | 0 |

## Tests instrumentados

| Módulo | Ejecutados | Pasados | Fallidos |
|---|---:|---:|---:|
| core:database — Room | 12 | 12 | 0 |
| feature:inventory — Compose | 16 | 16 | 0 |
| app | 1 | 1 | 0 |
| **Total** | **29** | **29** | **0** |

Comando ejecutado:

```powershell
.\gradlew.bat :core:database:connectedDebugAndroidTest :feature:inventory:connectedDebugAndroidTest :app:connectedDebugAndroidTest