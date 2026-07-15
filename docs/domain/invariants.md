# Invariantes del dominio

## Identidad y pertenencia

1. Todo agregado tiene un identificador no vacío.
2. Una existencia pertenece a un único hogar.
3. Un producto pertenece a un único hogar.
4. Una ubicación pertenece a un único hogar.
5. Una existencia referencia un producto del mismo hogar.
6. Una existencia solo puede estar en una ubicación del mismo hogar.

## Cantidad

7. Una cantidad exacta nunca puede ser negativa.
8. Una cantidad exacta cero representa agotamiento operativo.
9. Una cantidad aproximada siempre utiliza un nivel conocido.
10. Una existencia usa un único modo de cantidad en cada instante: exacto o aproximado.
11. Consumir nunca puede superar la cantidad exacta disponible.
12. Una corrección puede aumentar o reducir, pero nunca producir una cantidad inválida.
13. Las unidades incompatibles no pueden sumarse.
14. Los cálculos exactos usan representación decimal segura, no `Float` ni `Double`.

## Estado

15. Una existencia descartada o agotada no está activa.
16. Una existencia inactiva no puede consumirse ni abrirse.
17. Una existencia abierta no puede volver a cerrarse mediante una operación normal.
18. La fecha de apertura no puede existir si el estado es cerrado.
19. Una operación no puede tener una fecha anterior a la creación de la existencia.
20. El descarte no se confunde con consumo.
21. La corrección no se confunde con consumo ni descarte.

## Fechas

22. La fecha de caducidad y la fecha de consumo preferente son conceptos distintos.
23. Si ambas existen, la caducidad estricta tiene prioridad de seguridad.
24. La fecha de apertura no puede ser posterior al momento de la operación.
25. Las fechas desconocidas se representan como ausencia, no como valores inventados.

## Ubicaciones

26. La ubicación es opcional.
27. Eliminar una ubicación con existencias activas requiere una política explícita de traslado o dejar ubicación desconocida.
28. Mover una existencia conserva su identidad y cantidad.

## Historial

29. Las acciones de dominio relevantes generan eventos inmutables.
30. El historial no se modifica para ocultar correcciones.
31. Deshacer una acción crea una acción compensatoria; no borra el evento original.

## Prioridad

32. La prioridad de consumo es determinista para las mismas entradas.
33. Una caducidad vencida nunca recibe menor urgencia que una futura.
34. A igualdad de fecha, una existencia abierta se prioriza antes que una cerrada.
35. A igualdad completa, se usa un criterio estable para evitar orden aleatorio.

## Lista de compra

36. Una entrada de compra nunca altera el inventario.
37. Marcar una entrada como comprada no crea automáticamente una existencia sin confirmación explícita.
38. No se generan cantidades negativas ni sugerencias duplicadas para el mismo objetivo lógico.
