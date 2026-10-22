# Cambios pendientes tras la rebanada de clientes

Escrito el 2026-10-22, al cerrar la rebanada de clientes (rama `feature/clientes`). Recoge lo que
hay que cambiar, ya sea en el **backend** (`GestionFacturas-api`) o en el **cliente Kotlin** (este
repositorio). Complementa `docs/decisiones-pendientes.md` (que recoge decisiones generales) y no
lo sustituye.

Importante: la rebanada de clientes **no se ha probado todavía contra el backend real**. Todo lo
que dice que el backend "hace" algo se basa en lo que indicó el propietario o en el contrato
(`docs/api-contract.md`), no en una respuesta real observada. Los puntos marcados con
*(sin verificar)* hay que confirmarlos primero.

Leyenda de prioridad: **Alta** = hay un caso de uso que hoy no se puede completar · **Media** =
funciona pero la experiencia es mala o frágil · **Baja** = mejora.

---

## 1. Backend

### 1.1 NIF de clientes desactivados: no se puede reutilizar ni recuperar — Prioridad Alta
- **Qué pasa:** crear un cliente con un NIF que ya existe devuelve 409, y el backend cuenta también
  los clientes desactivados. No existe un endpoint para reactivar. Resultado: si se desactiva un
  cliente por error (o se quiere volver a trabajar con él), el usuario no puede volver a darlo de
  alta ni recuperarlo desde ninguna aplicación.
- **Qué cambiar (elegir una):**
  1. Añadir un endpoint de reactivación, por ejemplo `POST /api/clientes/{id}/reactivar`
     (o `PUT` con un campo `activo`), con transición válida solo desde inactivo.
  2. Que la comprobación de NIF duplicado ignore los clientes desactivados del mismo usuario.
  3. Como mínimo, que el 409 diga si el cliente que ya existe está desactivado y cuál es su `id`,
     para que el cliente pueda ofrecer reactivarlo.
- **Mi recomendación:** la opción 1, y añadir 3 (el conflicto con detalle) para poder enlazar desde
  el aviso del formulario.
- **Estado en el cliente:** el formulario ya avisa en el 409 de que puede tratarse de un cliente
  desactivado (`cliente_form_error_nif_duplicado`). Ver 2.1 para la parte del cliente.

### 1.2 Declarar en el OpenAPI qué devuelve `GET /api/clientes` respecto a los inactivos — Prioridad Media
- **Qué pasa:** el contrato dice que no está claro si la lista incluye clientes inactivos (no hay
  parámetro de filtro, aunque la respuesta trae `activo`). El cliente asume que **sí** los incluye
  y los muestra con la etiqueta "Inactivo" *(sin verificar)*.
- **Qué cambiar:** documentarlo, y valorar un parámetro opcional (`?activo=true|false`) para que el
  cliente pueda ofrecer "ocultar inactivos" sin traer datos de más.

### 1.3 Cuerpos de error documentados y con un código estable — Prioridad Media
- **Qué pasa:** los cuerpos de 400/404/409/429 no están en el OpenAPI. Un 409 sirve igual para
  "NIF duplicado" (clientes), "email ya registrado" (registro) y "transición de cobro inválida"
  (ingresos). El cliente hoy deduce el motivo por el recurso, no por el contenido.
- **Qué cambiar:** declarar los esquemas de error y añadir un código estable (por ejemplo
  `{"codigo": "NIF_DUPLICADO", "error": "..."}`), para que el cliente no dependa del texto.
- **Relación:** punto 8 de `docs/api-contract.md` (sección 6) y 1.3 de `decisiones-pendientes.md`.

### 1.4 Idioma de los mensajes de validación (400) — Prioridad Media
- **Qué pasa:** el formulario de cliente muestra bajo cada campo el texto que envía el backend
  (`campos[nombre|nif|email|telefono]`). Si esos mensajes salen en inglés (idioma por defecto de
  Bean Validation) el usuario verá idiomas mezclados *(sin verificar)*.
- **Qué cambiar:** archivo de mensajes en español en el backend, o ver 2.4.
- **Relación:** 3.3 de `decisiones-pendientes.md`.

### 1.5 `CobroInvalidoException` sin manejador (409) — Prioridad Media, afecta a ingresos
- Ya recogido en 4.1 de `decisiones-pendientes.md`; no es de clientes, pero el cliente ya depende
  de que el 409 llegue como 409 (`ErrorAuth.Conflicto`).

---

## 2. Cliente Kotlin

### 2.1 Reactivar un cliente desactivado — Prioridad Alta (depende de 1.1)
- Cuando exista el endpoint: método `reactivar(id)` en `ClienteRepository` (dominio), `ClienteApi`
  y `ClienteRepositoryImpl`; botón "Reactivar" en las filas con la etiqueta "Inactivo" de
  `ClientesScreen`; acción y tests en `ClientesViewModel`.
- Si el 409 pasa a traer el `id` del cliente existente (1.1, opción 3): enlazar desde el aviso del
  formulario a "Reactivar ese cliente".
- Hasta entonces la limitación queda documentada en `CLAUDE.md` y `README.md`.

### 2.2 Un guardado puede dejar la lista desactualizada si la lista aún está cargando — Prioridad Media
- **Dónde:** `ClientesViewModel.onVolverDelFormulario` llama a `onRefrescar()`, que se ignora si
  hay una carga en curso (`estaCargando()`).
- **Fallo posible:** se abre el formulario y se guarda mientras la lista inicial o un refresco
  siguen en vuelo; al volver, la recarga se descarta y la lista puede no incluir el cambio hasta
  pulsar "Actualizar". Poco probable (la lista suele haber terminado), pero real.
- **Qué hacer:** recordar un "refresco pendiente" y ejecutarlo al terminar la carga actual; añadir
  un test (hoy hay uno que fija el comportamiento actual: `alVolverDelFormularioMientrasCargaMantieneElMensajeSinSegundaLlamada`,
  habrá que ajustarlo conscientemente).

### 2.3 Aviso "El NIF no se puede modificar" se pinta como error — Prioridad Baja
- **Dónde:** `ClienteFormScreen`, campo NIF en edición. `CampoTexto` solo admite `error`, así que
  el texto de ayuda sale en rojo y con el campo marcado como error.
- **Qué hacer:** añadir a `CampoTexto` un parámetro de texto de ayuda neutro (y quizá `soloLectura`)
  en `ui/common/Formulario.kt`, sin cambiar el comportamiento de los formularios de login.

### 2.4 Mensajes de validación del backend mostrados tal cual — Prioridad Media (depende de 1.4/1.3)
- Si el backend no se arregla, mapear en el cliente los mensajes conocidos a textos en español
  (`strings.xml`), igual que se hace con `ErrorAuth`.
- Si el backend añade códigos estables (1.3), usarlos para elegir el texto en vez del mensaje.

### 2.5 El aviso de 409 asume que siempre es "NIF duplicado" — Prioridad Baja
- **Dónde:** `ClienteFormScreen.ErrorGeneral`: cualquier `ErrorAuth.Conflicto` al **crear** muestra
  el texto del NIF duplicado. Hoy es el único 409 posible al crear un cliente, pero si el backend
  añade otro conflicto el texto sería engañoso.
- **Qué hacer:** distinguirlo con el código de error cuando exista (1.3).

### 2.6 Las pantallas no tienen tests de interfaz — Prioridad Media
- Los ViewModels tienen tests con repositorio falso (129 tests en total), pero **nada comprueba la
  parte de Compose**: el paso de resultado formulario -> lista (`savedStateHandle` en
  `ShellScreen`), el *snackbar*, el diálogo de confirmación, Enter para enviar, o que los botones
  se deshabiliten al guardar.
- **Qué hacer:** prueba manual con `./gradlew :desktopApp:run` (ver 3) y valorar tests de Compose
  Multiplatform para el flujo crear/editar/desactivar. Sería una dependencia nueva de tests:
  preguntar antes de añadirla.

### 2.7 Menú lateral y diseño — Prioridad Baja
- La entrada "Clientes" está fija como seleccionada (es la única área hecha); cuando haya más áreas
  hay que derivar la selección de la ruta actual y activar las entradas de Gastos, Ingresos y
  Dashboard.
- Las entradas pendientes se atenúan con `alpha` y semántica `disabled()` porque
  `NavigationDrawerItem` no tiene `enabled`; revisar al activarlas.
- El menú no usa iconos (no hay dependencia de iconos). La adaptación a móvil (navegación inferior)
  sigue pendiente, como ya recoge `CLAUDE.md`.

### 2.8 Documentación del contrato desactualizada — Prioridad Baja
- `docs/api-contract.md` (sección 5, fila "Clientes list", y punto 10 de la sección 6) sigue
  diciendo que los inactivos y la reactivación están sin documentar. Actualizarlo cuando el backend
  responda a 1.1 y 1.2. No se ha tocado en esta rebanada a propósito.

---

## 3. Verificación pendiente (la tienes que hacer tú)

Con el backend en marcha y `./gradlew :desktopApp:run` (límite: 50 peticiones/min por usuario):

1. Lista: vacía, con varios clientes, y con el backend parado (mensaje de "sin conexión" + Reintentar).
2. Crear: solo con nombre y NIF; con email inválido (ver el mensaje por campo y su idioma, 1.4);
   con un NIF repetido, activo y desactivado (ver el aviso, 1.1).
3. Editar: que el NIF aparece bloqueado y no se envía; guardar y ver la lista actualizada al volver.
4. Desactivar: confirmación, éxito, y el caso 404 (desactivar un cliente desde otra sesión y luego
   intentar desactivarlo o editarlo aquí: mensaje "ya no existe" + lista recargada).
5. Comprobar que la lista **incluye** los inactivos (1.2) y que se ven con la etiqueta.
6. Pulsar Enter en el formulario, el botón Volver y Cancelar, y que nada se pueda pulsar dos veces
   mientras se guarda.
7. Cerrar sesión desde dentro de un formulario: debe volver a Login sin dejar pantallas atrás.

Lo que salga de esta prueba puede añadir o descartar puntos de este archivo.
