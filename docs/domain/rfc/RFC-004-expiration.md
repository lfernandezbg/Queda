# RFC-004 — Fechas y prioridad de consumo

- Estado: ACCEPTED
- Fecha: 2026-07-15
- Alcance: V1

## Contexto

La aplicación debe ayudar a consumir primero lo más urgente sin inventar fechas.

## Fechas soportadas

- compra;
- apertura;
- caducidad;
- consumo preferente;
- creación;
- última actualización.

## Decisión

La prioridad se calcula mediante una política pura.

Orden principal:

1. Caducidad estricta vencida.
2. Caducidad estricta más cercana.
3. Consumo preferente vencido.
4. Consumo preferente más cercano.
5. Existencia abierta.
6. Mayor antigüedad conocida.
7. Identificador estable como desempate.

## Reglas

- Una fecha desconocida permanece ausente.
- No se estima una caducidad sin marcarla como estimada.
- La apertura aumenta urgencia, pero no sustituye una fecha estricta.
- La política devuelve motivos explicables, no solo una puntuación opaca.
- El reloj se inyecta; no se usa directamente la hora del sistema en tests.

## Resultado sugerido

`ConsumptionPriority`:

- nivel de urgencia;
- posición estable;
- lista de razones;
- fecha relevante opcional.

## Alternativas descartadas

- Una única puntuación sin explicación.
- FIFO puro.
- Priorizar siempre abiertos ignorando caducidad.
- Inventar fechas medias.

## Tests obligatorios

- Vencido antes que futuro.
- Caducidad antes que consumo preferente.
- Abierto como desempate.
- Orden determinista.
- Sin fechas no produce urgencia falsa.
