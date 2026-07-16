# ADR-002 — Room como persistencia local

- Estado: ACCEPTED
- Fecha: 2026-07-15
- Implementación: Fase 2

## Contexto

V1 necesita persistencia local robusta, consultas reactivas y migraciones controladas.

## Decisión

Usar Room en infraestructura.

## Reglas

- Las entidades Room no salen de `core:database`.
- Los DAOs no llegan a presentación.
- Toda migración tiene test.
- No se usa destructive migration.
- El esquema se exporta y versiona.
