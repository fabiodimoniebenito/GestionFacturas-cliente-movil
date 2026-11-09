# Guía de uso — GestionFacturas

GestionFacturas te ayuda a **organizar y ver** tu actividad económica si eres freelance o autónomo:
anotas tus **gastos**, tus **ingresos** y tus **clientes**, y un panel te muestra cuánto has
facturado, cobrado y gastado, y cuánto ganas con cada cliente.

> **Importante:** esta aplicación sirve para tener visibilidad y orden. **No es asesoría fiscal ni
> facturación oficial.** Consulta con tu gestor para cualquier trámite con Hacienda.

Esta guía describe la versión de **escritorio**, que es la que está completa. La versión de Android
comparte las pantallas, pero su diseño para móvil y el guardado de archivos están pendientes.

---

## 1. Antes de empezar

La aplicación necesita que el **servidor (backend)** esté en marcha. Si ves el mensaje
*"No se pudo conectar con el servidor. Comprueba tu conexión."*, el servidor no está disponible:
comprueba que esté arrancado y vuelve a intentarlo.

### Crear una cuenta
1. En la pantalla de inicio pulsa **¿No tienes cuenta? Regístrate**.
2. Rellena nombre, correo y contraseña y pulsa **Registrarme**. Entrarás directamente.

### Iniciar y cerrar sesión
- En **Iniciar sesión** escribe tu correo y contraseña y pulsa **Entrar**.
- La sesión se **recuerda** al cerrar la aplicación: la próxima vez entrarás sin escribir nada.
- Para salir, pulsa **Cerrar sesión** en el menú lateral.
- Si introduces mal los datos verás "credenciales incorrectas". Tras demasiados intentos seguidos
  el servidor te pedirá esperar unos segundos o minutos.

---

## 2. Moverte por la aplicación

A la izquierda hay un **menú lateral** con: **Dashboard**, **Clientes**, **Gastos** e **Ingresos**.
Debajo aparecen tu correo, el botón de **tema** y **Cerrar sesión**.

- **Tema claro / oscuro:** el icono del sol o de la luna cambia entre ambos. La aplicación recuerda tu elección.
- **Volver:** las pantallas de detalle y los formularios tienen un botón **Volver** o **Cancelar**.
  En escritorio no existe botón de atrás del sistema.
- **Barra de desplazamiento:** si el contenido no cabe, aparece una barra a la derecha. Puedes
  arrastrarla o usar la rueda del ratón.
- **Actualizar:** las listas tienen un botón para recargar los datos.

---

## 3. Dashboard

Es la pantalla de inicio tras entrar. Resume tu actividad en un periodo.

### Elegir el periodo
- **Este mes**, **Trimestre actual** (el que aparece por defecto) o **Este año**: cargan el periodo
  completo de inmediato.
- **Personalizado:** escribe las fechas *Desde* y *Hasta* con el formato **AAAA-MM-DD**
  (por ejemplo `2026-01-31`) y pulsa **Aplicar**. La fecha *Desde* no puede ser posterior a *Hasta*.

Debajo se muestra el rango que está aplicado.

### Con IVA / Sin IVA
Este interruptor solo cambia **qué cifras ves** (con o sin IVA). No recalcula nada ni vuelve a pedir datos.

### Qué muestra
- **Resumen:** facturado, cobrado, gastos, **beneficio de caja** (lo cobrado menos los gastos) y
  **beneficio facturado** (lo facturado menos los gastos). Un beneficio negativo se ve en rojo.
- **Rentabilidad por cliente:** cuánto aporta cada cliente, en el orden que da el servidor.
  *"Gastos generales (sin cliente)"* agrupa los gastos que no asignaste a ningún cliente.
- **Pendientes de cobro:** los ingresos que aún no has cobrado y su total. Pulsa uno para abrir su
  detalle. Esta sección no depende del periodo elegido.

Cada sección carga por separado: si una falla, verás su error con un botón **Reintentar** y las demás
siguen funcionando. El botón **Actualizar** recarga todo.

### Exportar a Excel
1. Elige y aplica el periodo que quieres.
2. Pulsa **Exportar a Excel**. Mientras se prepara verás un indicador de progreso.
3. Se abre el cuadro de **guardado** del sistema, con un nombre propuesto del tipo
   `gestionfacturas_2026-01-01_2026-03-31.xlsx`, normalmente en tu carpeta de Descargas o Documentos.
   Elige dónde guardarlo.
4. Al terminar verás *"Archivo guardado en …"* con la ruta.

Cosas a tener en cuenta:
- Se exporta el **periodo aplicado**, no lo que estés escribiendo en los campos personalizados sin aplicar.
- El botón está desactivado mientras exporta o si el rango personalizado no es válido.
- El archivo **siempre incluye las cifras con IVA (Total) y sin IVA (Base)**, da igual lo que marque
  el interruptor Con IVA / Sin IVA.
- Si cierras el cuadro de guardado sin elegir nada, no pasa nada y no se muestra ningún mensaje.
- En **Android** el guardado todavía no está disponible y verás un aviso.

---

## 4. Clientes

Aquí gestionas a las personas o empresas para las que trabajas.

- **Nuevo cliente:** pulsa el botón, rellena **Nombre** y **NIF** (obligatorios) y, si quieres,
  correo y teléfono. Pulsa **Guardar**.
- **Editar:** cambia nombre, correo o teléfono. El **NIF no se puede modificar** una vez creado.
- **Desactivar:** el cliente deja de estar activo pero sigues viendo sus datos, marcado como
  **Inactivo**. Pide confirmación.

> **Ojo con el NIF:** no puedes crear dos clientes con el mismo NIF, y los clientes **desactivados
> también cuentan**. Hoy **no se pueden reactivar**, así que su NIF no se puede reutilizar. Piénsalo
> antes de desactivar.

---

## 5. Gastos

Los gastos son lo que pagas en tu actividad (facturas de proveedores, herramientas, etc.).

- **Lista:** cada gasto muestra emisor, base imponible e IVA. Los que están en **Pendiente de revisión**
  destacan.
- **Detalle:** pulsa un gasto para ver todos sus datos (estado, fechas, categoría, cliente, si es deducible…).
- **Nuevo gasto:** el único dato obligatorio es el **Total**. Puedes indicar emisor, fecha
  (`AAAA-MM-DD`), base imponible, IVA, moneda y cliente. Los importes llevan hasta 2 decimales y
  aceptan coma o punto.
- **Editar:** desde el detalle. Aquí también puedes asignar la **categoría** y marcar si es **deducible**
  (al crear no se puede). Por una limitación del servidor, al editar tienes que **volver a escribir el Total**.
- **Marcar como revisado:** confirma que has comprobado el gasto. Solo está disponible para gastos en
  borrador o procesados.
- Si el servidor dice que un gasto "ya no se puede editar en su estado actual", es una regla del servidor.

Aún no existe la subida de facturas escaneadas (digitalización) ni el borrado de gastos.

---

## 6. Ingresos

Los ingresos son lo que facturas a tus clientes.

- **Lista y filtros:** **Todos**, **Pendientes** o **Cobrados**.
- **Nuevo ingreso:** necesitas **al menos un cliente activo**. Si no tienes ninguno, la pantalla te
  ofrece **Crear cliente**. Elige el cliente y escribe el **Total** (obligatorio); concepto, fecha,
  base imponible e IVA son opcionales. La moneda es EUR por defecto. La aplicación **no calcula**
  la base ni el IVA por ti: escribe los valores que quieras registrar.
- **Detalle:** muestra el estado de cobro, las fechas y los importes.
  - **Registrar cobro:** para un ingreso pendiente. Te propone la fecha de hoy y puedes cambiarla
    (`AAAA-MM-DD`).
  - **Revertir cobro:** para un ingreso cobrado, vuelve a dejarlo pendiente. Pide confirmación.
- Si el servidor rechaza una operación de cobro (por ejemplo, ya estaba cobrado), verás su mensaje.

Los ingresos **no se pueden editar** después de creados: el servidor no lo permite todavía. Revisa
los datos antes de guardar.

---

## 7. Problemas frecuentes

| Qué ves | Qué hacer |
|---------|-----------|
| "No se pudo conectar con el servidor" | Comprueba que el servidor esté en marcha y tu conexión. |
| "Demasiados intentos…" | Espera el tiempo indicado y vuelve a intentarlo. |
| Te devuelve a la pantalla de inicio de sesión | La sesión caducó o se cerró: inicia sesión otra vez. |
| "Importe no válido" | Usa solo números, con coma o punto y hasta 2 decimales. |
| "Fecha no válida" | Escribe la fecha como `AAAA-MM-DD`. |
| "Ya existe un cliente con ese NIF" | Puede ser un cliente desactivado; no se puede reactivar. |
| Un elemento "ya no existe" | Se borró o cambió en otro sitio; la lista se actualiza sola. |
| La exportación falla | Pulsa de nuevo; si persiste, comprueba el servidor y el periodo elegido. |

## 8. Privacidad y seguridad

- La aplicación guarda tu sesión en tu equipo para que no tengas que entrar cada vez. Por ahora ese
  almacenamiento **no está cifrado**: usa la aplicación solo en un equipo de confianza y cierra sesión
  si compartes el ordenador.
- Tu contraseña se escribe enmascarada y nunca se guarda ni se muestra en registros.
