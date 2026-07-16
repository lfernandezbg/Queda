# ADR-003 — Offline first

- Estado: ACCEPTED
- Fecha: 2026-07-15

## Contexto

Registrar o consultar alimentos no debe depender de conexión.

## Decisión

La fuente operativa V1 es local.

## Consecuencias

- Todas las funciones V1 operan sin red.
- Una futura sincronización será una capa adicional.
- La UI no espera a un servidor para confirmar acciones locales.
- Los IDs se generan localmente.
