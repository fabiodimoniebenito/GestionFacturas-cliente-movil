# Decisiones y pendientes

Recopilado el 2026-10-19 a partir del trabajo en los dos repositorios (`GestionFacturas-api` y
`GestionFacturas-cliente-movil`). Cada punto indica qué está abierto, por qué importa y qué haría
yo. Los puntos están agrupados según quién tiene que actuar y ordenados por cuánto bloquean.

Leyenda: **DECIDIR** = necesita tu elección · **HACER** = necesita una acción tuya (yo no puedo o
no debo hacerla) · **VIGILAR** = sin acción ahora, revisar más adelante.

---

## 1. Decisiones que bloquean el trabajo próximo

### 1.1 Representación del dinero en el cliente — DECIDIR (bloquea las rebanadas de gastos e ingresos)
`BigDecimal` no existe en `commonMain`, y el backend envía los importes como números JSON simples
sin escala declarada.
- **Opciones:** (a) `String` tal como llega, (b) `Long` en céntimos, (c) una librería decimal
  multiplataforma.
- **Restricciones ya acordadas:** nunca `Double`/`Float`; sin aritmética con importes en el cliente
  (el backend calcula totales y beneficio).
- **Mi sugerencia:** decidirlo antes de empezar la rebanada de gastos. Si el cliente solo muestra y
  envía importes, (a) es lo que menos código lleva; si los formularios tienen que validarlos o
  darles formato, (b) o (c).

### 1.2 Cómo llegan al contrato del cliente las funciones nuevas del backend — DECIDIR (bloquea la rebanada de gastos)
La rama del backend `feature/gasto-edicion-revision` añade `PUT /api/gastos/{id}` y
`POST /api/gastos/{id}/revisar`, `categoriaId` al editar, y siembra 11 categorías. El
`docs/api-contract.md` del cliente y su CLAUDE.md siguen diciendo que editar y revisar gastos
FALTAN.
- **Hace falta:** fusionar antes el PR del backend y después actualizar el contrato (volver a
  auditar `/v3/api-docs`).
- **Preguntas del contrato que siguen abiertas hasta entonces:** cómo recibe un gasto su categoría
  (`CrearGastoRequest` sigue sin `categoriaId`), y que `GastoResponse` no tenga `total` (¿es
  intencionado o falta el campo? Calcularlo en el cliente rompe la regla de "sin aritmética").

### 1.3 Huecos restantes de la API (sección 6 del contrato) — DECIDIR
- **Editar ingresos** no existe: añadirlo al backend o descartar esa pantalla.
- **Filtro de ingresos pendientes:** usar `GET /api/dashboard/pendientes-cobro`, filtrar en el
  cliente por `estadoCobro`, o añadir un parámetro de filtro en el backend. (La rama de gastos ya
  declara en el OpenAPI los valores permitidos de `estadoCobro`, lo que hace más seguro filtrar en
  el cliente.)
- **Nulabilidad de las respuestas:** el OpenAPI no declara `required` en las respuestas. O lo
  declara el backend, o los DTO del cliente mantienen valores por defecto/nulables (enfoque actual).
- **Endpoints públicos y respuestas de error sin documentar en el OpenAPI** (`security: []` en
  `/api/auth/**` y `/api/health`; cuerpos de 400/401/404/409/429).
- **Clientes:** si los clientes inactivos aparecen en la lista, y que no hay acción de reactivar.

---

## 2. Cosas que solo puedes hacer tú — HACER

### 2.1 Abrir y fusionar los pull requests
`gh` no está instalado en esta máquina, así que no se abrió ningún PR. Las dos ramas del backend
están subidas.
- **Backend `fix/auth-401-entry-point`** — cuerpo JSON `{"error": ...}` y cabecera
  `WWW-Authenticate: Bearer` en el 401. Comparar:
  https://github.com/fabiodimoniebenito/GestionFacturas-api/pull/new/fix/auth-401-entry-point
- **Backend `feature/gasto-edicion-revision`** — endpoints de edición y revisión, siembra de
  categorías (V8) y valores de enums en el OpenAPI. Comparar:
  https://github.com/fabiodimoniebenito/GestionFacturas-api/pull/new/feature/gasto-edicion-revision
  (el código no se solapa con la rama del 401, pero AMBAS ramas editan `CLAUDE.md` y `README.md`,
  en secciones distintas; el segundo merge puede necesitar un pequeño arreglo manual de conflicto
  ahí).
- **Cliente `feature/login`** — **todavía no está subida.** 9 commits listos para revisar.
- **Orden que usaría:** rama de gastos del backend -> rama del 401 del backend -> `feature/login`
  del cliente, y después actualizar el contrato.
- Opcional: instalar y autenticar `gh` para poder abrirlos desde la terminal.

### 2.2 Probar la rebanada de login contra el backend real — HACER
La rebanada está cubierta por 65 tests unitarios y las compilaciones pasan, pero **nunca se ha
ejecutado contra el backend real ni con `Settings` reales**.
- Escritorio: `./gradlew :desktopApp:run`. El login está limitado a 10/min por IP, así que evita
  bucles de reintento.
- Qué comprobar: registrar -> llega a Home; contraseña incorrecta -> mensaje; 11 logins seguidos ->
  "espera N segundos"; parar el backend -> "sin conexión"; reiniciar la app -> sigue con la sesión
  (Settings persistió); Cerrar sesión -> vuelve a Login.
- Android: usa un emulador (`10.0.2.2` ya está configurado). Un móvil físico necesita la IP del PC
  en la red local en `UrlBase.android.kt` **y** en
  `androidApp/src/debug/res/xml/network_security_config.xml`.
- Sin verificar en ejecución: Multiplatform Settings en Android (su auto-inicialización "no-arg") y
  las propias pantallas de Compose (Enter envía, ancho del formulario, contraseña enmascarada).

### 2.3 Ejecutar las comprobaciones con sesión en la rama de gastos del backend — HACER
No pude llamar a los endpoints nuevos con un token (no conozco ninguna cuenta de prueba y no creé
una). Los comandos están en el resumen anterior: iniciar sesión y luego `GET /api/categorias`
(deben salir las 11 filas sembradas), `PUT /api/gastos/{id}` y `POST /api/gastos/{id}/revisar`.

---

## 3. Decisiones de comportamiento que tomé por ti — DECIDIR (confirmar o cambiar)

### 3.1 Un fallo al renovar el token SIEMPRE cierra la sesión
Tal como se especificó, cualquier fallo al renovar borra la sesión y vuelve a Login, **incluido un
error de red**. Consecuencia: un usuario sin conexión cuando caduca el token de acceso (15 minutos)
queda desconectado.
- **Alternativa:** cerrar la sesión solo ante un 401/403 de la llamada de renovación, y mantenerla
  ante errores de red (la petición simplemente falla y se puede reintentar).
- **Mi sugerencia:** pasar a la alternativa cuando el uso real demuestre que importa; es un cambio
  pequeño en `ApiClient.refrescarAccessToken` más un test.

### 3.2 `registrar` devuelve la sesión, no `Unit`
Al registrarse se inicia sesión justo después, así que `AuthRepository.registrar` devuelve
`Result<Sesion>`. Inofensivo; lo menciono porque difiere del plan original.

### 3.3 Los mensajes de validación del backend se muestran tal como llegan
Los errores de campo (400) enseñan el texto del propio backend bajo el campo. Si esos mensajes no
están en español (los mensajes por defecto de Bean Validation siguen el idioma de la JVM), el
usuario verá idiomas mezclados. Se arregla en el backend (archivo de mensajes) o mapeando los
mensajes conocidos en el cliente.

### 3.4 Orden de las comprobaciones al editar un gasto (backend)
`EditarGastoService` valida la categoría y el cliente **antes** que el bloqueo EXPORTADA del
dominio. Un gasto EXPORTADA editado con un cliente inválido devuelve 404, no 409. En ambos casos no
se guarda nada. Cambia el orden solo si te importa el error más específico.

---

## 4. Problemas conocidos que no he tocado — DECIDIR cuándo arreglarlos

### 4.1 `CobroInvalidoException` no tiene manejador (backend)
El CLAUDE.md dice que se mapea a 409, pero `GlobalExceptionHandler` no tiene manejador para ella,
así que una transición de cobro inválida saldría como 500. Esto también afectará a la futura
rebanada de ingresos del cliente. Quedó fuera del alcance (área de ingresos).

### 4.2 El mensaje de error del login permite enumerar cuentas (backend)
Los fallos de login dicen `"Email incorrecto"`, así que un atacante puede saber si un email está
registrado. Arreglo habitual: un único mensaje genérico para email desconocido y contraseña
incorrecta. El cliente muestra su propio texto (no el del servidor) para el 401, así que solo hay
que cambiar el backend.

### 4.3 El KNOWN ISSUE de rentabilidad-por-cliente sigue abierto (backend)
Con dos clientes que tienen ingresos, solo aparecía uno en
`GET /api/dashboard/rentabilidad-clientes`. Se sospecha de `DashboardPersistenceAdapter`; nunca se
diagnosticó. Hace falta un conjunto de datos controlado con varios clientes y una consulta SQL
contra el rango de fechas. La rebanada de dashboard del cliente no debería depender de esa vista
hasta entonces.

### 4.4 Pendientes del backend aplazados para el despliegue
Rotación de refresh tokens, CORS (necesario para el cliente web en Angular, no para
Android/Desktop), cabeceras de seguridad HTTP, rate limiting con Redis, `X-Forwarded-For` tras un
proxy de confianza, límites de tamaño de subida, expiración de buckets del rate limiting, gestor de
secretos para producción, OCR de ingresos, soporte de PDF en el OCR, OCR asíncrono y MinIO.

---

## 5. Pendientes del cliente a revisar más adelante — VIGILAR

| Punto | Cuándo |
|---|---|
| Navigation Compose está en una **versión candidata** (`2.10.0-rc01`) | Pasar a la 2.10.x estable cuando salga |
| Multiplatform Settings 1.3.0 (última versión: nov 2024) compila con Kotlin 2.4.20 pero no se ha ejecutado | Si aparecen problemas en ejecución, cambiar a AndroidX DataStore (preguntar antes) |
| Material 3 está en alfa (`1.12.0-alpha03`, ya venía en la plantilla) | Fijar una versión estable cuando encaje con CMP 1.12.x |
| El almacenamiento de tokens **no está cifrado** | Antes de cualquier versión real (Android Keystore / almacenamiento seguro de cada plataforma) |
| Adaptación del diseño a móvil (navegación inferior) | Después de que funcione la versión de escritorio |
| Subida de facturas (selector de archivos en escritorio, cámara/galería en Android) | Con la rebanada de gastos |
| Caché offline opcional (Room KMP) | Solo si aparece una necesidad real |
| Navigation 3 | Volver a comprobar si la documentación de Compose Multiplatform empieza a recomendarla para proyectos nuevos; la navegación está aislada en `ui/navigation/` |

---

## 6. Mantenimiento

- **Finales de línea:** Git avisa "LF will be replaced by CRLF" en cada archivo nuevo (`core.autocrlf`
  en Windows). Un `.gitattributes` con `* text=auto eol=lf` en cada repositorio lo silenciaría y
  mantendría los finales de línea coherentes entre máquinas.
- **CLAUDE.md del backend:** ya describe los endpoints nuevos de gastos dentro de la rama; no hace
  falta más edición cuando se fusione.
- **CLAUDE.md del cliente:** sigue listando "NOT in the API: edit/delete/review of gastos";
  actualizarlo junto con el contrato cuando se fusione la rama de gastos del backend (ver 1.2).
