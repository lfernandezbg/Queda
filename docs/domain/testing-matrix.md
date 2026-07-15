# Matriz inicial de tests de dominio

| Regla | Tipo de test |
|---|---|
| Cantidad exacta no negativa | Unit + property |
| Consumo nunca supera disponible | Unit + property |
| Conversión solo dentro de dimensión | Unit |
| Nivel aproximado válido | Unit |
| Existencia y producto del mismo hogar | Unit |
| Movimiento entre hogares rechazado | Unit |
| Abrir dos veces rechazado o idempotente según API | Unit |
| Inactivo no admite operaciones | Unit |
| Orden FEFO determinista | Unit + property |
| Vencido antes que futuro | Unit |
| Abierto como desempate | Unit |
| Completar compra no crea stock | Unit |
| Sugerencia de compra idempotente | Unit + property |
| Eventos conservan orden y datos | Unit |
