# Roadmap de Queda

## Fase 0 — Fundación

Estado: completada y validada.

Incluye arquitectura multimódulo, build types, CI, instrumentados, Maestro, análisis estático y aislamiento E2E.

## Fase 1 — Dominio puro

Definir e implementar el modelo de dominio V1 sin Android, Room, Compose ni infraestructura.

Incluye:

- Identidades.
- Producto y existencia.
- Cantidad exacta y aproximada.
- Ubicaciones.
- Lotes y fechas.
- Apertura, consumo, corrección y descarte.
- Prioridad de consumo.
- Lista de compra como concepto de dominio.
- Eventos y resultados de dominio.
- Tests unitarios y property-based.

## Fase 2 — Persistencia

Room, entidades de almacenamiento, DAOs, migraciones, mappers y repositorios locales.

## Fase 3 — Capa de datos y casos de uso

Implementaciones de repositorios, transacciones, flujos, ordenación y consultas.

## Fase 4 — Onboarding

Creación del hogar, ubicaciones iniciales y carga inicial.

## Fase 5 — Inventario exacto

Alta, consulta, edición, consumo, movimiento, apertura y descarte.

## Fase 6 — Inventario aproximado

Niveles aproximados, acciones rápidas y revisión de datos inciertos.

## Fase 7 — Lista de compra

Lista manual, sugerencias y relación con el inventario.

## Fase 8 — Hoy y revisión

Consumir primero, poca existencia, abiertos, caducidades y datos obsoletos.

## Fase 9 — Historial, ubicaciones y ajustes

Historial de movimientos, gestión de ubicaciones, exportación e importación.

## Fase 10 — Endurecimiento

Accesibilidad, rendimiento, perfiles baseline, regresiones y publicación interna.

## Fuera de V1

- Sincronización familiar.
- Backend.
- OCR.
- Escaneo de códigos de barras.
- Inteligencia artificial.
- Recetas.
- Wear OS.
- Widgets.
