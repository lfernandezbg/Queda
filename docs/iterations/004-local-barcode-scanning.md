# Validación — Escaneo local de código de barras

## Estado

PASS

## Resultado de usuario

Se ha implementado la capacidad de escanear códigos de barras (EAN-8, EAN-13, UPC-A e ITF-14) para agilizar la gestión del inventario. El sistema reconoce automáticamente si un código ya está asociado a un alimento existente o si se trata de un nuevo producto.

El usuario puede:

1. Abrir el escáner desde la pantalla principal de inventario.
2. Escanear un producto nuevo: el sistema le redirigirá al formulario de alta con el código ya asociado.
3. Escanear un producto existente: el sistema le redirigirá al inventario y abrirá automáticamente el menú de gestión de cantidad (Consumir/Corregir) para ese producto.
4. Recibir feedback inmediato si un código es inválido (formato incorrecto o dígito de control fallido).
5. Gestionar los permisos de cámara de forma segura, con explicaciones claras y enlaces a los ajustes si es necesario.
6. Ver un error específico si intenta añadir un producto con un código de barras que ya está en uso.

## Alcance validado

- [x] Detección local mediante ML Kit (sin llamadas a red).
- [x] Validación estricta de formatos retail y dígitos de control.
- [x] Migración de base de datos aditiva y segura (Versión 2) con tests de integridad.
- [x] Flujo de alta con código de barras asociado.
- [x] Manejo explícito de duplicidad de código de barras.
- [x] Flujo de búsqueda y edición rápida para códigos existentes.
- [x] Gestión de permisos de Android (Denied/Permanently Denied) con máquina de estados real.
- [x] Aislamiento completo de E2E (código de test ausente en release DEX/Manifest).
- [x] Cobertura completa de tests unitarios, propiedad, integración y arquitectura.

## Casos funcionales

| Caso | Resultado |
|---|---|
| Botón de escaneo visible en Inventario | PASS |
| Solicitar permiso de cámara al abrir | PASS |
| Rechazar EAN-13 con dígito de control inválido | PASS |
| Escanear nuevo -> Abrir alta con indicador | PASS |
| Escanear existente -> Abrir menú de cantidad | PASS |
| Detectar código duplicado al guardar | PASS |
| Persistencia del código tras reinicio | PASS |
| Reintentar permiso tras denegación simple | PASS |
| Abrir ajustes tras denegación permanente | PASS |
| Limpieza de recursos (CameraX/ML Kit) al salir | PASS |

## Tests JVM

| Suite | Pasados |
|---|---:|
| core:model (Barcode / Property) | 4/4 (Property) |
| core:domain (ResolveScannedBarcodeUseCase) | 8/8 |
| feature:inventory (ViewModel/Unit) | 58/58 |
| quality:architecture | 23/23 |
| **Total** | **93/93** |

## Tests instrumentados

| Módulo | Pasados |
|---|---:|
| core:database (Migration 1->2) | 15/15 |
| core:data (Repository Integration) | 12/12 |
| feature:inventory (UI / Fake Camera) | 40/40 |
| app (E2E Host Integration) | 2/2 |
| **Total** | **69/69** |

## Flujos Maestro (Smoke)

| Flujo | Resultado |
|---|---|
| 15_scan_new_barcode_item.yaml | PASS |
| 16_scan_existing_barcode_item.yaml | PASS |
| 17_invalid_barcode_rejected.yaml | PASS |
| Suite Completa (00-17) | 18/18 PASS |
