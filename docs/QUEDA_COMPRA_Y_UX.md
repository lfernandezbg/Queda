# Compra compartida y navegación

La versión 5 de Room añade `shopping_entries` y `pending_shopping_operations`. La migración 4→5 solo crea tablas e índices: conserva intactas las tablas de inventario y los datos existentes. No se aplica una migración de descenso, porque eliminar cualquiera de las nuevas tablas perdería compras o cambios pendientes; para volver a una versión anterior hay que restaurar una copia de la base de datos anterior a la actualización. `MigrationTest` comprueba el inventario conservado, la unicidad de nombres por hogar y las tablas nuevas.

Compra usa Room como fuente visible. Añadir o cambiar el estado crea una operación local en la misma transacción; la cola se sube con recibos idempotentes a `households/{id}/shopping/{entryId}`. El ID procede de un hash del hogar y el nombre normalizado para que dos altas sin conexión del mismo nombre converjan. Se envía el estado deseado, nunca un toggle implícito. Firestore escucha cambios en tiempo real y el botón Actualizar consulta de nuevo el servidor. «Comprado» no modifica inventario: el usuario confirma el stock al escanear o añadir.

La navegación principal ofrece Hoy, Inventario, Escanear, Compra y Más. La cabecera de la sesión se reduce a nombre del hogar, estado y Actualizar; invitaciones y cambio de cuenta pasan a Más. El escaneo continuo conserva la cámara mientras se revisa cada producto, evita nuevas lecturas durante esa revisión y solo guarda tras confirmación. Un producto ya existente suma su cantidad mediante la operación exacta de dominio; un producto de presencia se marca como disponible.

La sesión autenticada puede cargar perfil y hogar desde caché cuando Firestore informa de red no disponible. Un dispositivo que nunca abrió ese hogar conectado necesitará la primera conexión para obtener sus datos.

Las nuevas reglas de Firestore deben validarse en el emulador y publicarse por separado tras revisar el resultado de la suite. El ZIP de código por sí solo no cambia las reglas de producción.
