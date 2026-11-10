# Conceptos de diseño

Maquetas HTML independientes (una carpeta por concepto, abre su `index.html`; el `index.html` de esta
carpeta enlaza a todas). Son **exploraciones visuales**: no forman parte de la compilación y no se han
llevado a código Compose. Todas usan los mismos datos de ejemplo del dashboard, la lista de gastos y,
en el 05, la pantalla de digitalizar.

| Concepto | Dirección | Rasgo característico |
|---|---|---|
| `01-talonario` | Cercano y humano | Sellos de goma para los estados; resumen como hoja de cuentas con borde perforado |
| `02-libro-mayor` | Sobrio, institucional | Estado de resultados con raya simple y doble raya contables; cifras en serif |
| `03-ticket` | Ticket de caja | Monocromo; vídeo inverso para negativos y pendientes; código de barras |
| `04-archivador` | Organización por color | Separadores de archivador; cada área tiene un color con significado fijo |
| `05-movil-suizo` | Móvil, suizo | Barra inferior con cámara central; bloque de color plano y cifra condensada grande |
| `06-noche-instrumentos` | Oscuro de origen | Un color por magnitud (facturado, cobrado, gastos); cifras grandes en peso ligero |
| `07-libreta` | Libreta de ahorros | Cifras en tinta violeta de matriz de puntos; sellos de ventanilla; agujeros de encuadernación |
| `08-hoja-de-calculo` | Hoja de cálculo | Rejilla con cabeceras A/B/C y filas; barra de fórmulas que describe la celda elegida; pestañas de hoja |
| `09-tablero-estados` | Tablero por estados | Columnas por estado de ingresos y gastos; cada tarjeta ofrece su acción (solo lectura) |
| `10-narrativo` | Dashboard en frases | El resumen se lee como una frase con las cifras subrayadas; el detalle queda debajo |

## Reglas que respetan todas
- Los importes se muestran tal cual los da el backend (sin cálculos en el cliente); el interruptor
  «Con IVA / Sin IVA» solo elige qué cifras se enseñan.
- La rentabilidad por cliente va en el orden del backend, sin ordenar ni comparar (por eso no hay barras).
- Los importes negativos tienen un tratamiento propio, no solo un signo.
- Los borradores (pendientes de revisión) se distinguen visualmente, y tras digitalizar se avisa:
  «Revisa los datos extraídos antes de marcarlo como revisado».
- Modo claro y oscuro (01 y 02 con botón; 04 a 10 siguen el del sistema; 03 es siempre papel).
- En el 09, los ejemplos de ingresos cobrados son datos inventados para la maqueta. Es solo lectura: las
  transiciones de estado las decide el backend.

## Notas
- Las fuentes se cargan desde Google Fonts, así que hace falta conexión para verlas como están
  pensadas. Llevar un concepto a la app implica empaquetar las fuentes como recursos de Compose
  (sin dependencias nuevas) y definir el esquema de colores y la tipografía en `App`.
- Cada maqueta incluye sus colores con el rol de Material 3 al que corresponderían (01 y 02 en tabla).
