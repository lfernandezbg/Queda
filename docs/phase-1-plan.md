# Plan de Fase 1 — Dominio puro

## Objetivo

Construir el dominio V1 de Queda como Kotlin puro, con reglas explícitas, tipos seguros y tests exhaustivos.

## Alcance

### Incluido

- Identificadores.
- Producto.
- Existencia.
- Lote.
- Ubicación.
- Cantidad exacta.
- Cantidad aproximada.
- Fechas relevantes.
- Estado abierto o cerrado.
- Consumo.
- Corrección.
- Movimiento.
- Descarte.
- Prioridad de consumo.
- Sugerencia de compra básica.
- Eventos de dominio.
- Errores y resultados.
- Tests unitarios.
- Tests property-based.

### Excluido

- Room.
- DAOs.
- Repositorios reales.
- Hilt.
- Compose.
- ViewModels.
- Navegación.
- Backend.
- Sincronización.
- OCR.
- Código de barras.
- IA.

## Orden de implementación

1. Tipos base e identificadores.
2. Producto y existencia.
3. Cantidades.
4. Ubicaciones.
5. Fechas, apertura y caducidad.
6. Operaciones de inventario.
7. Prioridad de consumo.
8. Lista de compra básica.
9. Eventos y resultados.
10. Tests de propiedades e invariantes.

## Criterios de aceptación

- `core:model` y `core:domain` compilan sin Android.
- Ningún import `android.*`, `androidx.*` o Room.
- Todas las invariantes documentadas tienen tests.
- No hay `TODO`, `FIXME` ni placeholders.
- No hay lógica duplicada.
- No se usan `Double` o `Float` para cantidades monetarias o exactas.
- No se usan cadenas libres para estados.
- No se introducen interfaces especulativas.
- Los tests cubren límites, errores y transiciones.
- CI completa en verde.

## Criterio de cierre

La Fase 1 termina cuando el dominio puede ejecutar en memoria todas las operaciones V1 sin Android ni persistencia.
