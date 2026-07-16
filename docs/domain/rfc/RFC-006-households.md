# RFC-006 — Hogar, IDs y evolución multiusuario

- Estado: ACCEPTED
- Fecha: 2026-07-15
- Alcance: V1

## Contexto

V1 es local y de un único hogar, pero el modelo no debe impedir una futura sincronización familiar.

## Decisión

Todas las entidades relevantes incluyen `HouseholdId`.

Los identificadores:

- se generan en cliente;
- son globalmente únicos;
- son inmutables;
- no dependen de IDs autoincrementales de base de datos.

## V1

- Un hogar local.
- Un dispositivo puede trabajar sin cuenta.
- No hay backend.
- No hay conflictos remotos.

## Preparación futura permitida

- IDs estables.
- timestamps.
- eventos de dominio.
- pertenencia explícita a hogar.

## Preparación futura prohibida

- Interfaces de sincronización vacías.
- campos de servidor no utilizados.
- versiones vectoriales especulativas.
- lógica de conflictos sin requisito real.

## Alternativas descartadas

- IDs autoincrementales como identidad de dominio.
- Omisión total de hogar.
- Implementar sincronización anticipada.

## Tests obligatorios

- IDs no vacíos e inmutables.
- Entidades de hogares distintos no se mezclan.
- Operaciones cruzadas fallan.
