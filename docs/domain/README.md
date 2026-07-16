# Dominio de Queda

Queda modela alimentos y productos domésticos que existen físicamente en un hogar.

El centro del dominio no es el catálogo de productos, sino la **existencia concreta**.

Ejemplo:

- Producto: leche semidesnatada.
- Existencia: un brik concreto, comprado el 12 de julio, abierto, situado en la balda superior y con cantidad aproximada media.

Un producto puede tener varias existencias simultáneas, con distintas fechas, ubicaciones, cantidades y estados.

## Agregados principales

### Household

Límite lógico de los datos del hogar.

### Product

Define la identidad reutilizable del producto.

### StockItem

Representa una existencia física concreta.

### Location

Representa una ubicación válida dentro del hogar.

### ShoppingEntry

Representa una intención de compra.

## Principios

- La ubicación puede ser desconocida.
- La cantidad puede ser exacta o aproximada.
- Una existencia nunca tiene cantidad negativa.
- El historial conserva acciones relevantes.
- La eliminación física no forma parte de operaciones normales.
- La prioridad de consumo es una política pura y determinista.
- Los datos inciertos se representan explícitamente; no se inventan.
