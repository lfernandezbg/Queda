# Arquitectura de Queda

## Objetivo

Mantener el dominio independiente de Android y permitir que persistencia, UI y sincronización evolucionen sin reescribir las reglas de negocio.

## Capas conceptuales

```text
Presentation
    ↓
Application
    ↓
Domain
    ↑
Infrastructure
```

La dependencia de código siempre apunta hacia el dominio.

## Reglas obligatorias

### Domain

- Kotlin puro.
- No depende de Android.
- No depende de Compose.
- No depende de Room.
- No depende de Hilt.
- No depende de librerías de serialización.
- No contiene detalles de base de datos.
- Expresa invariantes, políticas y resultados.

### Application

- Coordina casos de uso.
- Define puertos de repositorio.
- No conoce Room ni Compose.
- No duplica reglas del dominio.

### Infrastructure

- Implementa repositorios.
- Contiene Room, mappers, transacciones y adaptadores.
- Traduce errores técnicos a errores de aplicación.
- No decide reglas de negocio.

### Presentation

- Compose, ViewModels y navegación.
- Convierte estado de aplicación en UI.
- No calcula FEFO.
- No corrige cantidades.
- No decide transiciones de estado.

## Regla antiacoplamiento

Ninguna clase de UI puede acceder directamente a DAO o base de datos.

Ningún repositorio puede devolver entidades Room fuera de infraestructura.

Ningún caso de uso puede depender de clases Android.

## Estrategia de evolución

Las decisiones se documentan en RFC de dominio o ADR de arquitectura.

Cuando una decisión cambie:

1. Se crea o actualiza una RFC.
2. Se marcan las decisiones sustituidas.
3. Se actualizan código y tests en el mismo commit.
