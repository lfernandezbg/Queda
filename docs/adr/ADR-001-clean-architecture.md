# ADR-001 — Arquitectura limpia y módulos

- Estado: ACCEPTED
- Fecha: 2026-07-15

## Contexto

Queda debe evolucionar desde una app local hasta posibles capacidades de sincronización sin acoplar el dominio a Android.

## Decisión

Usar arquitectura limpia y módulos con dependencias dirigidas hacia el dominio.

## Consecuencias

- Dominio testeable en JVM.
- Infraestructura reemplazable.
- Más disciplina en mappers y límites.
- Prohibido acceder desde UI directamente a Room.
