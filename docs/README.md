# Documentación de Queda

Esta carpeta contiene la documentación viva del producto y de su arquitectura.

## Regla de mantenimiento

La documentación describe únicamente decisiones vigentes, reglas implementadas o decisiones necesarias para la siguiente fase.

Cada cambio relevante debe actualizar en el mismo commit:

1. La RFC o ADR afectada.
2. El código.
3. Los tests.
4. La documentación de validación, cuando corresponda.

## Estructura

- `roadmap.md`: fases del proyecto.
- `architecture.md`: arquitectura y reglas de dependencias.
- `phase-1-plan.md`: alcance y criterios de cierre de la Fase 1.
- `domain/`: glosario, invariantes, decisiones y RFC de dominio.
- `adr/`: decisiones arquitectónicas.
