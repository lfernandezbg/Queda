# S1: hogar compartido y sincronización

## Configuración necesaria

1. Crear un proyecto Firebase y registrar la aplicación Android `com.luisete.queda`.
2. Activar **Authentication > Correo electrónico/Contraseña** y crear la base predeterminada de Cloud Firestore.
3. Copiar `google-services.json` a `app/google-services.json`. Está ignorado por Git. El build lee el ID de aplicación, el ID del proyecto y la clave API para inicializar Firebase. Las variantes de pruebas usan el mismo proyecto y los flujos E2E no hacen login.
4. Ejecutar las pruebas de `firebase-tests/` con Firebase Emulator Suite y desplegar `firestore.rules` en ese mismo proyecto antes de usar cuentas reales.

## Comportamiento

- Una persona registra su correo, crea un hogar y comparte un código aleatorio de 128 bits que caduca a los siete días. Otra persona registra su propia cuenta y usa ese código para entrar.
- El inventario sigue en Room. Al crear o unirse al hogar, los alimentos del hogar local anterior se trasladan en una transacción y se encolan como altas para su sincronización.
- Las altas, consumos, correcciones y cambios de presencia se registran en Room junto con el cambio local. La escucha incremental de Firestore recibe cambios del resto de personas; «Actualizar» fuerza una lectura del servidor.
- Cada operación enviada se confirma mediante una transacción de Firestore y un recibo inmutable. Los consumos se recalculan sobre la última cantidad del servidor, en decimal exacto. Una corrección establece una cantidad absoluta y la última transacción confirmada determina el resultado. El cambio de presencia sigue el mismo orden de confirmación.
- Si un consumo ya no cabe o se duplica un nombre o código, la cola conserva la operación y muestra error. «Resolver conflicto» pide confirmación expresa antes de descartar los cambios pendientes de ese alimento y conservar la versión del hogar.
- Firestore limita las lecturas y escrituras al hogar del miembro autenticado. Los códigos de invitación solo se pueden leer conociendo el código exacto y no se pueden listar.

## Validación pendiente en este entorno

No hay `google-services.json`, Android SDK ni acceso para descargar Gradle 8.13. La compilación, las pruebas JVM, instrumentales, Maestro y el emulador Firebase se deben ejecutar con el script local. No se declara PASS hasta contar con los resultados reales.
