# AgroCampo — Android · Caso AgroSierra

Aplicación Android para técnicos de campo de AgroSierra, desarrollada en **Kotlin Multiplatform + Compose**, con **Ktor Client, SQLDelight, Koin y arquitectura por capas**. El repositorio se llama `Proyect-PharmaMobil` porque conserva prácticas anteriores; **AgroCampo** es la funcionalidad evaluada para la Unidad 2.

- Backend: [AgroAPI-Contreras](https://github.com/Lucky-pixel-web/AgroAPI-Contreras)
- Rama de cierre: `fix/cierre-unidad2-contreras`
- Documentación anterior del proyecto: [docs/README_PHARMAMOBIL_ANTERIOR.md](docs/README_PHARMAMOBIL_ANTERIOR.md)

## 1. Requisitos y ejecución

1. Iniciar Oracle, preparar el usuario `AGRODB` y levantar el backend según su README. La API debe atender en **http://localhost:8080**.
2. Abrir este repositorio con Android Studio, sincronizar Gradle y elegir la configuración `androidApp`.
3. Encender el emulador Android (o adaptar la URL para un dispositivo físico) y ejecutar la aplicación.
4. Iniciar sesión con un usuario de prueba. Las contraseñas se configuran en el backend; no deben incluirse tokens reales en este repositorio.

En Windows, desde la raíz del repositorio:
```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :shared:testAndroidHostTest
```
La segunda tarea ejecuta pruebas host de Android; las pruebas comunes también están en `shared/src/commonTest` y se ejecutan con las tareas compatibles de Gradle. Para listar las disponibles: `.\gradlew.bat :shared:tasks --all`.

**URL base en emulador:** `http://10.0.2.2:8080/api/v1/`, configurada en `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.android.kt`. `localhost` desde el emulador apuntaría al propio Android y no al backend del PC. Esta URL HTTP es para desarrollo local, no para producción.

## 2. Contrato de AgroAPI

Todas las rutas salvo `POST /api/v1/auth/login` requieren token JWT (`Authorization: Bearer <token>`).

| Método | Ruta | Función |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | Login: `usuario`, `contrasena`; devuelve token y permisos |
| GET | `/api/v1/usuarios/me` | Perfil y privilegios |
| GET | `/api/v1/lotes?pagina=0&tamanio=20&valle=...&campaniaCerrada=false` | Lista paginada y filtros |
| GET | `/api/v1/lotes/{id}` | Detalle del lote |
| GET | `/api/v1/lotes/{id}/labores?pagina=0&tamanio=20` | Labores paginadas |
| POST | `/api/v1/labores` | Registrar labor |
| PUT | `/api/v1/labores/{id}` | Corregir labor |
| DELETE | `/api/v1/labores/{id}` | Eliminar labor |
| POST | `/api/v1/lotes/{id}/cerrar-campania` | Cerrar campaña (permiso específico) |

Ejemplo de solicitud de labor (requiere `LABOR_CREAR`):
```json
{
  "loteId": 1,
  "tipo": "RIEGO",
  "fecha": "2026-10-28",
  "cantidad": 2.0,
  "observacion": "Riego programado",
  "cantidadJornales": 3
}
```
La paginación devuelve `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas` y `ultima`. Los errores del servidor usan códigos HTTP diferenciados y, para validaciones, `validationErrors` por campo.

## 3. Roles y permisos

| Rol | Permisos |
| --- | --- |
| TECNICO | `LOTE_LEER`, `LABOR_LEER`, `LABOR_CREAR`, `LABOR_EDITAR` |
| JEFE_CAMPO | Los anteriores, además `LABOR_ELIMINAR` y `CAMPANIA_CERRAR` |
| SUPERVISOR_CALIDAD (SC-B) | `LOTE_LEER`, `LABOR_LEER`, `LABOR_EDITAR` |
| ADMINISTRADOR | `LOTE_LEER`, `LABOR_LEER`, `USUARIO_GESTIONAR` |

La interfaz oculta acciones sin permiso, pero **el backend vuelve a comprobar los privilegios**. El token no se debe guardar en texto plano. Respuestas típicas: **400** formulario inválido, **401** token ausente/vencido, **403** privilegio insuficiente, **404** entidad inexistente, **409** campaña cerrada.

## 4. Arquitectura y flujo offline-first

```text
Compose (AgroCampoScreen)
  -> AgroViewModel
  -> casos de uso / contrato AgroRepositorio
  -> AgroRepositorioOffline
     |-> AgroLocal -> SQLDelight (AgroDb, SQLite)
     +-> AgroApi -> Ktor HttpClient -> AgroAPI
```

- **Fuente para pintar el padrón:** SQLDelight mediante `Flow` observable; no espera una respuesta HTTP para mostrar los lotes.
- **Registro y corrección offline:** se guardan localmente con `pendiente = true`; las labores nuevas reciben identificador provisional negativo.
- **Sincronización:** primero se envían los pendientes, después se descargan lotes y labores y se actualiza la caché. Se preservan labores pendientes en la base local.
- **RF-A02:** sesión conservada con almacenamiento seguro Android y recuperación posterior.
- **RF-A03:** última sincronización exitosa persistida en SQLite y visible sin red.
- **RF-A05:** errores de API tipados y mensajes de validación del formulario.
- **SC-A:** combinación del filtro local de valle y campañas abiertas, incluso sin red.
- **SC-C:** campo `cantidadJornales` desde DTO hasta SQLDelight y formulario.
- **SC-D:** contador observable de labores pendientes por lote.

**Alcance conocido:** `AgroRepositorioOffline.eliminar` envía directamente al servidor cuando el ID es positivo; eliminar una labor ya sincronizada sin red no utiliza una cola local de eliminación. No se presenta aquí como una operación offline implementada.

## 5. Ubicación de los archivos

- `shared/src/commonMain/kotlin/pe/upeu/agrosierra/domain/`: modelos, repositorio y casos de uso.
- `shared/src/commonMain/kotlin/pe/upeu/agrosierra/data/remote/`: DTO, HTTP y traducción de errores.
- `shared/src/commonMain/kotlin/pe/upeu/agrosierra/data/local/`: acceso observable a SQLite.
- `shared/src/commonMain/kotlin/pe/upeu/agrosierra/data/repository/`: coordinación local/remota.
- `shared/src/commonMain/kotlin/pe/upeu/agrosierra/presentation/`: ViewModel, pantalla y validaciones.
- `shared/src/commonMain/sqldelightAgro/`: consultas y migraciones de AgroDb.
- `shared/src/androidMain/`: implementación Android (driver SQLite, monitor de conectividad, URL base, almacenamiento seguro).
- `shared/src/commonTest/kotlin/pe/upeu/agrosierra/`: pruebas comunes de DTO, errores y sincronización.
- `shared/src/androidHostTest/kotlin/pe/upeu/agrosierra/`: filtros, contador y fecha en SQLite.

## Firma de entrega (release)

La versión de entrega se firma con un almacén propio (`contreras.jks`) que **no está en el repositorio**. Las credenciales se leen desde `keystore.properties` en la raíz del proyecto, ignorado por Git:

    storeFile=C:/ruta/a/contreras.jks
    storePassword=********
    keyAlias=contreras
    keyPassword=********

- `.gitignore` excluye `keystore.properties`, `*.jks` y `*.keystore`.
- Sin ese archivo, el proyecto abre y compila en debug; solo el release exige las credenciales.
- Huella SHA-256 del certificado: `46:B1:55:5A:1F:42:DD:C5:95:AC:6A:57:4D:69:56:FC:23:97:CC:A6:2C:8C:2A:25:BC:7C:26:16:27:0D:CF:47`
- Versión: `versionCode 1`, `versionName 1.0.0`, `applicationId pe.edu.upeu.pharmamobil`.
- Minificación (R8) activada: la app release se probó en un dispositivo real (TECNO KI5k, Android 12) y la serialización y Koin funcionan sin reglas adicionales.

Generar los artefactos:

    .\gradlew.bat :androidApp:bundleRelease     # .aab (entregable)
    .\gradlew.bat :androidApp:assembleRelease   # .apk (pruebas)

## 6. Pruebas y evidencias a entregar

Comprobar login, roles, paginación, crear/corregir/eliminar con permisos, filtro por valle y campañas abiertas sin internet, registro pendiente, reconexión sin duplicados, contador observable, fecha de última sincronización y validaciones. Guardar los resultados de pruebas y capturas en el **PDF de evidencias**, incluyendo el modo avión, login y rechazo por privilegios. Exportar por separado la colección Postman del backend.

## 7. Git y entrega

El examen requiere desarrollo en ramas, revisión mediante Pull Requests, incorporación a `develop` y posteriormente a `main`, y etiqueta **`v1.0-unidad2`** sobre el commit evaluado. No trabajar directamente en `main`. No crear etiquetas finales ni fusionar sin comprobar la ejecución y los archivos entregables.

> Este README describe el código de la rama de cierre; no certifica por sí solo que los tests pasen en todas las máquinas. En el proyecto solo se evalúa Android.
