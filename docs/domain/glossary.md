# Glosario del dominio

## Hogar

Límite lógico que agrupa inventario, ubicaciones y lista de compra.

En V1 existe un único hogar local, pero todos los IDs deben permitir una futura sincronización multiusuario.

## Producto

Concepto reutilizable que describe qué es un artículo.

No representa una unidad física ni contiene cantidad disponible.

## Existencia

Unidad física o agrupación concreta disponible en el hogar.

Tiene producto, cantidad, estado, fechas y ubicación opcional.

## Lote

Conjunto de unidades equivalentes adquiridas o registradas juntas.

En V1 una existencia puede actuar como lote operativo. El modelo no debe impedir separar o combinar existencias en el futuro.

## Cantidad exacta

Cantidad expresada mediante valor y unidad verificables.

Ejemplos: 3 unidades, 750 gramos, 1.5 litros.

## Cantidad aproximada

Nivel cualitativo cuando el usuario no conoce una medida exacta.

Ejemplos: lleno, alto, medio, bajo, casi vacío.

## Ubicación

Lugar físico dentro del hogar donde puede encontrarse una existencia.

Puede tener jerarquía visual futura, pero en V1 se modela como entidad plana y ordenable.

## Apertura

Transición de una existencia cerrada a abierta.

Puede establecer una fecha de apertura y alterar su prioridad de consumo.

## Consumo

Reducción intencionada de la cantidad disponible.

Nunca puede producir una cantidad negativa.

## Corrección

Ajuste explícito para reflejar que el inventario registrado era incorrecto.

No se representa como consumo.

## Descarte

Retirada de una cantidad por desperdicio, deterioro o decisión del usuario.

Debe conservar su motivo cuando se proporcione.

## Caducidad

Fecha tras la que el producto puede no ser seguro o válido.

## Consumo preferente

Fecha orientativa de calidad, distinta de la caducidad estricta.

## FEFO

First Expired, First Out.

Prioriza consumir antes las existencias con fecha relevante más cercana.

## Lista de compra

Conjunto de intenciones de compra.

No es inventario futuro ni reserva cantidades.
