# Iteración 001 — Inventory Exact Add/List

## Resultado de usuario
Un usuario puede añadir alimentos con nombre y cantidad exacta, visualizarlos en una lista ordenada alfabéticamente y comprobar que los datos persisten tras reiniciar la aplicación.

## Alcance
- Pantalla de inventario con estados (Loading, Empty, Content, Error).
- Formulario de alta para cantidad exacta.
- Validación de nombre (1-80 caracteres, sin caracteres de control).
- Validación de cantidad (positiva, máx 3 decimales).
- Prevención de duplicados por nombre normalizado.
- Persistencia local con Room (transaccional).
- Household estable interno (`local-household-v1`).
- Navegación entre inventario y alta.

## Fuera de alcance
- Cantidades aproximadas.
- Edición o eliminación.
- Ubicaciones.
- Fechas.
- Sincronización.

## Arquitectura
- `core:model`: Definición de `ProductName`, `Product`, `StockItem`, `InventoryItem`.
- `core:domain`: `ExactQuantityInputParser`, `InventoryRepository` (interfaz), Casos de uso.
- `core:data`: `OfflineInventoryRepository`, `LocalCurrentHouseholdIdProvider`.
- `core:database`: `ProductEntity`, `StockItemEntity`, `InventoryDao`, `QuedaDatabase`.
- `feature:inventory`: `InventoryViewModel`, `InventoryScreen`, `AddExactItemViewModel`, `AddExactItemScreen`.

## Contrato de ProductName
- Normalización: `lowercase(Locale.ROOT)` y colapso de espacios.
- Estabilidad: El `normalizedKey` es la base para la comparación y el índice único en BD.

## Contrato de cantidad de entrada
- Formatos aceptados: "1", "1.5", "1,25", "0.001".
- Rechazados: negativos, cero, >3 decimales, notación científica.

## Tablas Room
- `products`: `id`, `householdId`, `displayName`, `normalizedName`. Índice único en `(householdId, normalizedName)`.
- `stock_items`: `id`, `householdId`, `productId`, `quantityAmount`, `quantityUnit`. FK a `products.id` con `CASCADE`.

## Transacción
- El alta de un ítem de inventario inserta un `Product` y un `StockItem` dentro de una transacción `@Transaction` en el DAO, previa comprobación de duplicados.

## Household local
- Se utiliza un `CurrentHouseholdIdProvider` que devuelve siempre `local-household-v1`.

## Estados de UI
- `Loading`: Indicador de progreso.
- `Empty`: Mensaje de bienvenida y botón de añadir.
- `Content`: Lista de alimentos con nombre y cantidad formateada.
- `Error`: Mensaje de error y botón de reintentar.

## Navegación
- `inventory`: Pantalla principal.
- `inventory/add-exact`: Formulario de alta.

## Errores
- Mensajes específicos para validación de nombre, cantidad, duplicados y fallo de almacenamiento.

## Tests
- Unitarios de modelo, parser, casos de uso, repositorio y viewmodel.
- Integración de DAO con Room.
- UI con Compose Testing.
- Propiedades con Kotest (8.000 iteraciones).
- Regresión y Arquitectura.

## Maestro
- 12 flujos de humo que cubren desde el lanzamiento hasta la persistencia y validaciones.

## Limitaciones
- Esta iteración solo soporta un `StockItem` por `Product` en el flujo de alta, aunque el esquema soporta múltiples.
- No se admite el cambio de household en esta fase.
