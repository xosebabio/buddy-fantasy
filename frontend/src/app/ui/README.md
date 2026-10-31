# Dirección visual

Identidad de la interfaz (FE-28), cerrada con el usuario en la tarea 8 del plan 11 con la skill `.agents/skills/frontend-design`. Las reglas de estilo están en `docs/requirements.md` (FE-24 a FE-31, RNF-12) y aquí no se repiten: este documento dice cómo se usan. El boceto de referencia es [`docs/design/visual-direction.html`](../../../../docs/design/visual-direction.html) (ábrelo en el navegador): login, mis ligas, la alineación con el shell, el cambio de liga y el escritorio, en los dos temas. Las pantallas de manager tienen el suyo, [`docs/design/manager-screens.html`](../../../../docs/design/manager-screens.html), descrito en _Composición_.

## Idea

Es el fantasy del equipo de los domingos: lo usa un grupo de amigos en el móvil, el día del partido y el día siguiente. Su mundo es el de un partido aficionado: petos, dorsales, el campo y el acta. La interfaz es sobria y deja el color a los jugadores.

1. **El peto es lo memorable.** Cada jugador se dibuja siempre igual: un peto con el color de su posición y su dorsal. Es el único sitio donde la interfaz usa color por gusto.
2. **El resto, callado.** Listas en filas a todo lo ancho con divisores, no en tarjetas. Superficies separadas por fondo y borde, no por sombras.
3. **Las cifras mandan.** Puntos, valores, jornadas y plazos van en Barlow Condensed con números tabulares, más grandes que su etiqueta.
4. **Al alcance del pulgar.** La acción principal, abajo y a todo lo ancho; la navegación, debajo de ella.

## El peto

| Aspecto       | Regla                                                                                                                                                     |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Forma         | Peto sin mangas con cuello en V: `clip-path: polygon(0 10%, 30% 0, 50% 16%, 70% 0, 100% 10%, 100% 100%, 0 100%)`, proporción 1 : 1,12.                    |
| Color         | El de su posición (`--bf-color-position-*`), con el dorsal en `--bf-color-on-color` (casi negro) en los dos temas.                                        |
| Dorsal        | Barlow Condensed 700, tabular, a la mitad del ancho; centrado y algo bajo (relleno superior del 14 % del ancho, calculado sobre su tamaño, nunca en `%`). |
| Tamaños       | 30 px en listas, 44 px en el campo, 72 px en el login.                                                                                                    |
| Selección     | Anillo de 2 px en `--bf-color-accent` (lavanda) alrededor, nunca otro relleno.                                                                            |
| Vacío         | El contorno discontinuo sin dorsal: un hueco por llenar (estado vacío, plaza libre en la alineación).                                                     |
| Accesibilidad | Decorativo: el nombre y la posición van en texto al lado o en la etiqueta accesible; el color nunca es el único dato.                                     |

Tiene dominio (la posición), así que vive en `shared/` (`bf-bib`, con `position`, `number` y `size`: `small`, `medium` o `large`); lo estrena el login. Cuando se añadan las fotos de los jugadores (RF-09 ya las admite, opcionales), la variante con foto se diseñará entonces: el peto sigue siendo lo que se ve por defecto y lo que tiene quien no tiene foto.

## Color

`npm run palette` (`scripts/palette.mjs`) genera `styles/_palette.scss`: los seis colores de FE-25, el rojo y el ámbar, cada uno con su escala 50–900 en OKLCH (la misma luminosidad por paso en todos los tonos, así que un paso da el mismo contraste en cualquiera), y los neutros cálidos (50–950). `styles/_tokens.scss` construye con ellos los tokens semánticos de la tabla, y `styles/_themes.scss` los aplica: claro en `:root`, oscuro con `data-theme="dark"` o con `prefers-color-scheme` si no hay elección guardada. Los componentes solo usan los tokens semánticos, nunca la paleta.

| Token                                       | Uso                                           | Claro                                 | Oscuro                              |
| ------------------------------------------- | --------------------------------------------- | ------------------------------------- | ----------------------------------- |
| `bg`                                        | Fondo de la página                            | neutro cálido muy claro (≈ `#F6F5F2`) | ≈ `#121211`                         |
| `surface`                                   | Barras, hojas, navegación, campos             | blanco                                | un paso sobre `bg` (≈ `#1C1B19`)    |
| `border`, `border-strong`                   | Divisores; contornos de controles             | neutros 200 y 500                     | neutros 800 y 500                   |
| `text`, `text-muted`                        | Texto; metadatos y etiquetas                  | casi negro (neutro 900); neutro 600   | casi blanco (neutro 50); neutro 400 |
| `brand`                                     | Botón principal, pestaña activa               | verde base                            | verde base                          |
| `brand-text`, `brand-subtle`                | Texto verde; fondo del destino activo         | verde 700; verde 50                   | verde 400; verde 900                |
| `on-color`                                  | Texto sobre cualquier tono base (botón, peto) | casi negro                            | casi negro                          |
| `accent`, `accent-text`, `accent-subtle`    | Selección, enlaces, etiqueta _Admin_          | lavanda base, 700, 50                 | lavanda base, 300, 900              |
| `focus`                                     | Anillo de foco de teclado                     | lavanda 600                           | lavanda 300                         |
| `danger`, `danger-text`                     | Errores y puntos negativos                    | rojo base, 700                        | rojo base, 300                      |
| `warning`, `warning-text`, `warning-subtle` | Plazos a punto de vencer                      | ámbar base, 800, 50                   | ámbar base, base, 900               |
| `position-*`                                | Petos y chips de posición                     | tonos base                            | tonos base                          |
| `pitch`, `pitch-stripe`, `pitch-line`       | Césped a franjas y sus líneas                 | `#123D29`, `#164530`, blanco al 55 %  | iguales                             |
| `on-pitch`, `on-pitch-muted`                | Nombre y valor del jugador en el campo        | blanco; blanco al 78 %                | iguales                             |
| `scrim`                                     | Velo bajo hojas y diálogos                    | casi negro al 45 %                    | negro al 60 %                       |

- El botón principal lleva **texto casi negro sobre el verde** (FE-25), también en oscuro.
- El verde de marca es para actuar y para lo positivo; el lavanda, para lo seleccionado y el foco. No se intercambian.
- El campo es igual en los dos temas: un verde oscuro elegido para que los cuatro colores de posición y el anillo lavanda pasen de 3:1 sobre el césped, y el texto blanco de 10:1.
- Sin texto sobre el rojo base (3,6:1 con casi negro): un error se marca con `danger` como borde o icono y su texto va en `danger-text`.
- `border-strong` es el contorno de los inputs, que los identifica: llega a 3:1 en los dos temas (WCAG 1.4.11), y por eso es el neutro 500 en claro y en oscuro.
- Los colores de posición no llegan a 3:1 sobre `bg` en claro: sirven porque el peto es decorativo y un chip de posición lleva el texto en `on-color`.
- `styles/contrast.spec.ts` comprueba los pares de texto (4,5:1) y de gráficos con información (3:1) en los dos temas, leyendo los tokens reales. Un par nuevo en un componente se añade allí.

## Tipografía

Escala de tercera mayor (1,25) sobre 16 px, en `--bf-font-size-1` a `-8`:

| Paso | Tamaño | Fuente y peso                    | Uso                                                          |
| ---- | ------ | -------------------------------- | ------------------------------------------------------------ |
| 8    | 61 px  | Condensed 700, interlineado 0,95 | Título del login                                             |
| 7    | 49 px  | Condensed 700                    | Cifra protagonista (puntos de la jornada)                    |
| 6    | 39 px  | Condensed 700, 1,05              | Título de pantalla                                           |
| 5    | 31 px  | Condensed 700, 1,1               | Título de pantalla con barra de liga (_Jornada 12_)          |
| 4    | 25 px  | Condensed 600                    | Nombre de liga en listas, títulos de hoja, cifras de resumen |
| 3    | 20 px  | Condensed 600                    | Subtítulos de sección, cambio de liga                        |
| 2    | 16 px  | Barlow 400 / 600                 | Texto, botones, inputs (nunca menos)                         |
| 1    | 13 px  | Barlow 400 / 600                 | Metadatos, ayudas, etiquetas                                 |

- Pesos que se cargan: Barlow 400, 500 y 600; Barlow Condensed 500, 600 y 700; solo latín (`styles/_fonts.scss`).
- En los estilos de un componente, `@use 'ui/styles/typography' as type;` da `type.display(3)` a `type.display(8)` (fuente, peso, tamaño e interlineado de la tabla) y `type.figures` (condensed con números tabulares). Los puntos de corte, en `ui/styles/breakpoints` (`bp.md`, `bp.lg`).
- Todas las cifras con `font-variant-numeric: tabular-nums`. Los puntos llevan signo (`+14`, `−3` con el signo menos tipográfico), en `brand-text` o `danger-text`.
- Una cifra va con su unidad o contexto más pequeño y apagado al lado (`87,4 / 100 M`, `1 de 2`) y la etiqueta debajo.
- Sin mayúsculas sostenidas en etiquetas, sin sobretítulos y sin resaltar una palabra suelta en un titular. Texto en frases de menos de 80 caracteres.

## Espacio, radios, sombras y movimiento

- **Espaciado** en base 4: `--bf-space-1` a `-8` = 4, 8, 12, 16, 24, 32, 48 y 64 px. Margen lateral de la página: 16 px en móvil.
- **Radios por jerarquía:** `tag` 4 px (etiquetas, petos, campo), `control` 10 px (botones, inputs, anillos de selección), `sheet` 20 px (esquinas superiores de hojas) y `round` (avatares). Las filas de lista no tienen radio.
- **Sombras:** solo en lo que flota sobre la página: `overlay` (hojas, toasts) y `raised` (menús). Nada de sombras en filas ni en tarjetas.
- **Movimiento:** `--bf-motion-fast` 120 ms (cambios de estado), `--bf-motion-base` 220 ms (hojas que entran) con `ease-out`. La única animación que no responde a una acción es la caída de los cuatro petos (1, 4, 8, 9) en el login; lo demás solo contesta a algo que hace la persona. Con `prefers-reduced-motion`, sin desplazamientos.
- **Foco:** contorno de 2 px en `focus` separado 2 px, en todo lo que se pueda enfocar (global, en `styles/_reset.scss`).
- **Objetivos táctiles** de 44 px como mínimo; botones de 48 px de alto.

## Iconos

Lucide con `@lucide/angular`: un componente por icono, importado por separado (`imports: [LucideHouse]` y `<svg lucideHouse></svg>`). Trazo de 2, en `currentColor`; 24 px en la navegación, 20 px en botones y 16 px junto a texto pequeño (`size`). Sin `title`, el icono queda oculto al lector de pantalla: el texto lo lleva el botón o la etiqueta de al lado.

## Componentes

Los de `ui/` (FE-18) se ven en `/_ui` con todos sus estados y en los dos temas. Cómo se usan:

| Componente                                | Uso                                                                                                                                                                                                                                                                                                                      |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `bfButton`                                | `<button bfButton variant="primary" block>`: `primary` (la acción principal, una por pantalla), `secondary`, `quiet` y `danger` (lo que no se deshace, con contorno y texto rojos); `loading` lo deshabilita con un spinner. También en `<a>`.                                                                           |
| `bfIconButton`                            | Solo icono, 44 × 44 px, con su nombre en `label`.                                                                                                                                                                                                                                                                        |
| `bf-field` + `bfInput`                    | Etiqueta, control, `hint` y `error` (el texto del `code` del servidor); el campo da el id y `aria-describedby` al control.                                                                                                                                                                                               |
| `bf-card`                                 | Un bloque aparte (la vista previa de una invitación); las listas no son tarjetas.                                                                                                                                                                                                                                        |
| `bfList` + `bfListItem`                   | Filas a todo lo ancho (`bleed` anula el margen de la página); `size="large"` o `compact`; slots `bfListItemLeading`, `bfListItemMeta` y `bfListItemTrailing`; `current` para la fila en uso. Una fila enlace lleva chevron.                                                                                              |
| `bf-badge`                                | `neutral`, `brand`, `accent` (_Admin_) y `warning` (plazos).                                                                                                                                                                                                                                                             |
| `bf-avatar`                               | Foto (sin `Referer`, para las de Google) o iniciales si no hay o no carga; decorativo.                                                                                                                                                                                                                                   |
| `Sheet`                                   | Hoja inferior modal (CDK Dialog): `bfSheetTitle` la nombra y `bfSheetClose` o `DialogRef.close` la cierran. En móvil sustituye a los diálogos.                                                                                                                                                                           |
| `Confirm`                                 | `confirm.ask({ title, message, confirm, tone })` emite `true` o `false`; empieza con el foco en _Cancelar_.                                                                                                                                                                                                              |
| `Toasts` + `bf-toaster`                   | `toasts.show({ message, tone, action, dismissed })`; se anuncian con `LiveAnnouncer`; sin acción se van a los 5 s, con acción esperan. `bf-toaster` va una vez, en el componente raíz, por encima de la navegación inferior si está en pantalla (`--bf-toaster-bottom`, en `styles/_overlay.scss`).                      |
| `bf-skeleton`                             | `line`, `title`, `avatar` y `block`, quietos; la página marca la región con `aria-busy`.                                                                                                                                                                                                                                 |
| `bf-segmented`                            | Una elección entre pocas opciones a la vista (el tema): radios nativos en un grupo con su `label`, `[(value)]`; la opción elegida en lavanda.                                                                                                                                                                            |
| `bf-tabs` + `bfTab` + `bfTabPanel`        | Pestañas dentro de una página (_Managers_ / _Jugadores_), a la izquierda: `[(selected)]` con el valor de cada `bfTab`, y cada `bfTabPanel` con el mismo valor se ve mientras su pestaña está elegida. Flechas, Inicio y Fin (`FocusKeyManager` del CDK) mueven y eligen; solo la elegida está en el orden de tabulación. |
| `bf-tab-nav` + `bfTabLink`                | Pestañas de ruta (_General_ / _Por jornada_), que reparten el ancho: una navegación con su `label` y un `routerLink` por pestaña, activa con `aria-current="page"`. `bleed` lleva la línea a los bordes, como en `bfList`.                                                                                               |
| `select[bfSelect]`                        | Select nativo dentro de `bf-field`, con el aspecto de `bfInput`; el campo le pinta el chevron.                                                                                                                                                                                                                           |
| `bf-stepper`                              | Número entero con − y + de 44 px y la cifra grande en medio (`role="spinbutton"`: flechas arriba y abajo, Inicio y Fin; los botones quedan fuera del orden de tabulación). `min`, `max` y `step` los da la página; con `empty`, el valor puede ser `null` y el primer paso va a `start`.                                 |
| `bf-countdown`                            | Tiempo hasta un instante del API (_1 d 4 h_, _3 h 20 min_, _12 min_, _menos de 1 min_), con el texto de antes como contenido (_Se bloquea en_). En ámbar en las 3 últimas horas; se actualiza cada minuto, y cada segundo en el último; emite `elapsed` al llegar a cero, no si ya había pasado.                         |
| `bf-notice`                               | Aviso en línea: `info`, `warning` y `danger`, con su icono, o el que se le pase como contenido (`<svg lucideBan>`). No es región viva: si hay que anunciarlo, la página le pone `role`.                                                                                                                                  |
| `bf-figure`                               | Cifra con su unidad (`[bfFigureUnit]`, más pequeña y apagada) y la etiqueta (`label`) debajo: `medium` para un resumen, `large` para una posición; `tone="danger"` si está mal (presupuesto superado).                                                                                                                   |
| `bfDisclosure`                            | `<li bfDisclosure>` en un `bfList`: la fila (los mismos huecos que `bfListItem`) es un botón con `aria-expanded` y chevron que abre su `[bfDisclosurePanel]` debajo. Es un `CdkAccordionItem` (`[(expanded)]`, `disabled`); dentro de un `cdkAccordion`, solo una abierta.                                               |
| `bf-load-error`                           | Lo que se pudo leer no llegó: aviso (`role="alert"`) y _Reintentar_, que emite `retry` (la página llama a `reload()`).                                                                                                                                                                                                   |
| `bf-empty-state`                          | El contorno del peto, el mensaje y su acción (`bfEmptyStateAction`).                                                                                                                                                                                                                                                     |
| `bf-top-bar` + `bf-wordmark`              | Barra superior fija con el inicio y `bfTopBarEnd`.                                                                                                                                                                                                                                                                       |
| `bf-nav-bar`, `bf-side-nav` + `bfNavItem` | Navegación inferior y lateral; cada destino es un `routerLink` con su icono y texto, activo con `aria-current="page"`.                                                                                                                                                                                                   |

Los estilos de lo que el CDK pinta fuera de los componentes (velo, hoja y su título) y los de `bf-field` (el control es contenido proyectado) son globales: `styles/_overlay.scss` y `field/field.scss` sin encapsulación.

## Composición

### Shell en móvil

```
┌──────────────────────────────┐
│ Os Domingueiros ▾        (XB)│  barra superior: liga actual (abre la hoja de cambio) y perfil
├──────────────────────────────┤
│ Jornada 12   [◷ Se bloquea…] │  título condensed y, si lo hay, el plazo en ámbar
│ Contra CD Arrabal, dom 2 nov │
│                              │
│   contenido                  │
│                              │
│ [     Acción principal     ] │  botón a todo lo ancho, sobre la navegación
├──────────────────────────────┤
│ Jornada Alin. Valorar Clas. Liga│  navegación inferior: icono Lucide y etiqueta;
└──────────────────────────────┘    el activo, en verde con una barra arriba
```

- Fuera de una liga (`/leagues`, `/profile`), la barra lleva el logotipo de texto «BuddyFantasy», con _Fantasy_ en `brand-text`, y no hay navegación inferior.
- La navegación solo muestra los destinos que existen (FE-30).
- Con cinco destinos, cada uno crece desde el ancho de su texto (`flex: 1 1 auto`), no a partes iguales: a partes iguales, _Clasificación_ no cabe en 360 px a 13 px.
- **`lg` (≥ 1024):** navegación lateral de 248 px en `surface` con el logotipo, el cambio de liga, los destinos (el activo con fondo `brand-subtle`) y el perfil abajo. El contenido va en dos columnas: la principal (el campo) y la de apoyo (cifras, banquillo y la acción).

### Mis ligas

```
Tus ligas                      título de pantalla (paso 6)
──────────────────────────────
Os Domingueiros            ›   nombre en condensed (paso 4)
[Admin]                        etiqueta solo si administras
──────────────────────────────
Veteranos do Sar           ›
Manager
──────────────────────────────

[        + Crear liga        ]
¿Te han invitado? Abre el enlace que te pasaron.
```

Vacío: el contorno de un peto y _Aún no juegas en ninguna liga. Crea la de tu equipo o pide el enlace de invitación a quien la lleve._

### Pantallas de manager

Cerradas en la tarea 3 del plan 12; el boceto es [`docs/design/manager-screens.html`](../../../../docs/design/manager-screens.html). Reglas comunes:

- **Cabecera de jornada:** título (paso 5) con el plazo (`bf-countdown`) a su derecha y, debajo, el partido en texto apagado (_Contra CD Arrabal, domingo 2 de noviembre, 11:00_; _Ganamos 3–1 a Racing Vilariño_). El plazo va con borde neutro y pasa a ámbar en las 3 últimas horas.
- **Un protagonista por pantalla:** el campo, la cifra de la última jornada, la posición en la clasificación, el peto grande de la ficha. Lo demás, en filas.
- **Puntos** con `bf-points`: signo y menos tipográfico, `brand-text` si suman, `danger-text` si restan y neutro en 0. Los totales de la clasificación general no llevan signo.
- **Desgloses bajo petición:** una fila con puntos que se pueden explicar es un `bfDisclosure` con chevron hacia abajo; al abrirse enseña el detalle debajo, con el mismo sangrado que la fila.
- **Sin acción principal** en las pantallas que solo se miran (clasificación, ficha): nada fijo sobre la navegación.

#### Inicio de la jornada

```
Jornada 12            [◷ Se bloquea en 5 h]
Contra CD Arrabal, domingo 2 de noviembre, 11:00
──────────────────────────────
Tu alineación          96,2 / 100 M    ›   fila: cambios en la meta, presupuesto como cifra
1 de 2 cambios
──────────────────────────────
Valora la jornada 11         3 de 9    ›   una fila por ventana abierta, con su cierre
[◷ Cierra en 14 h]
──────────────────────────────
+38          2.º de 6               ›      cifra protagonista (paso 7) y posición (paso 5);
En la jornada 11   En la general           el bloque enlaza a la clasificación

[     Valorar la jornada 11      ]         acción contextual (decisión 14)
```

- Sin alineación guardada: _Aún no tienes alineación_ y la acción _Hacer alineación_. Sin jornadas puntuadas, no hay cifra.
- Entre `lockAt` y el job de bloqueo, una fila _Alineaciones de la jornada 12_ con _Bloqueándose…_; después enlaza a `lineups/:matchId`.
- Sin partidos: el vacío del peto con _Aún no hay partidos. Cuando se programe el siguiente, lo verás aquí._ y sin acción.

#### Alineación

```
Jornada 12          [◷ Se bloquea en 2 h 40 min]   en ámbar: menos de 3 h
Contra CD Arrabal, domingo 2 de noviembre, 11:00
96,2 / 100 M   1 de 2   [Campo|Lista]   cifras y, a su derecha, el segmentado
Presupuesto    Cambios
┌────────────────────────────┐
│            (9)             │          campo: una fila por posición, portería propia abajo;
│      (10)  (8)  (6)        │          peto de 44 px, nombre y valor
│        (4)     (5)         │
│            (1)             │
├────────────────────────────┤
│        ( ) Plaza libre     │          plazas libres al pie, fuera de las líneas
└────────────────────────────┘
[       Descartar cambios      ]         quiet, solo con cambios sin guardar
[      Guardar alineación      ]
```

- El segmentado _Campo_ / _Lista_ va en la fila de las cifras, no debajo: así el campo de 7 cabe entero en 360 px sobre la acción y la navegación.
- Como mucho cinco petos por fila (más se parten en dos), el nombre se recorta con su etiqueta completa en el botón. Las plazas libres van al pie del campo; si faltan mínimos de formación, en la fila de su posición.
- Lo que la comprobación local encuentra se pinta en la cifra (presupuesto en `danger-text`) y en un `bf-notice` de aviso: _Te pasas 1,5 M del presupuesto. Puedes guardarla, pero si se bloquea así no puntúa esta jornada._ Los `warnings` del servidor, igual. Nunca impide guardar.
- **Hoja de candidatos** al tocar un peto o una plaza: título _Cambiar a Pablo_ y _Sin él te quedan 21,0 M_; grupos por posición (la suya primero) con subtítulo apagado, sin los ya alineados; el actual con ✓ _En la alineación_; solo se marca lo que no cabe (_Supera el presupuesto_, en `warning-text`), y se puede elegir igual. Al pie, fijo, _Quitar de la alineación_ (`secondary`).
- **Lista:** una fila compacta por jugador con peto, nombre, posición en texto y valor; tocarla abre la misma hoja.
- **`lg`:** el campo a la izquierda; a la derecha, cifras, segmentado, avisos y la acción. Los candidatos no van en hoja: se abren en esa columna, sin velo, con _Quitar de la alineación_ y _Cerrar_.

#### Valorar un partido

```
Jornada 11                 [◷ Cierra en 14 h]
Ganamos 3–1 a Racing Vilariño
De 0 a 4 picas. Valorar es opcional: deja sin voto a quien no viste.
──────────────────────────────
(1) Diego                                   peto, nombre; posición y acta apagadas
    Portero  1 penalti parado
[0][1][2][3][4]                  Quitar     picas: radios de 44 px, el elegido en lavanda
──────────────────────────────
(5) Xurxo
    Defensa  Tarjeta amarilla
[0][1][2][3][4]                Sin voto
──────────────────────────────
[     Guardar valoraciones     ]
     Has valorado a 3 de 9                  cuenta el borrador
```

- El control ocupa su propia línea a todo lo ancho, con _Quitar_ (nombre accesible _Quitar voto a Xurxo_) o _Sin voto_ al final. En nota, la misma línea con `bf-stepper`: `[−] 6 [+]`, con _Sin voto_ dentro mientras está vacío.
- **Cerrada:** cada fila con _Media 3,00 con 6 votos_ y _Tu voto: 3_ (o _Sin tu voto_, _Sin votos_) en la meta y los puntos por valoración a la derecha. La acción pasa a ser _Ver los puntos de la jornada_ (`secondary`). Sin resultados aún, un `bf-notice` informativo (_Calculando los resultados. Esta página se actualiza sola._) y los puntos en _skeleton_.
- **Excluido:** un `bf-notice` informativo con _El administrador ha cancelado tus valoraciones de este partido._ en lugar de los controles, sin acción.

#### Clasificación y puntos de una jornada

```
Clasificación
Tras la jornada 11
[   General   |  Por jornada  ]             pestañas de ruta, a todo lo ancho
──────────────────────────────
1  (LR) Lucía Romero          342   ›       posición en paso 5; un empate la repite, apagada
        11 jornadas, +41 en la última
2  (XB) Xosé Babío            318   ›       tu fila con current
```

```
[   General   |  Por jornada  ]
Jornada
[ Jornada 11, contra Racing Vilariño  ▾ ]  bfSelect, solo jornadas cerradas
Managers  Jugadores                         pestañas de la página, alineadas a la izquierda
──────────────────────────────
2  (XB) Xosé Babío            +38   ˄       bf-disclosure abierta
        (9) Pablo                 +14       su alineación bloqueada, sangrada
            Acciones +4, valoración +10
6  (MV) Manu Vidal              0   ˅
        [Fuera de presupuesto]
```

- Las dos filas de pestañas se distinguen por su ancho: las de ruta reparten el ancho; las de la página van a la izquierda.
- _Jugadores_: los del acta por puntos, con _Acciones +4, valoración +10_ apagado y el total; cada fila abre la ficha.
- Sin datos (`MATCH_NOT_CLOSED`, `MATCH_NOT_SCORED`): el vacío del peto con _Los puntos se publican al cerrar las valoraciones._ y _Valorar la jornada 11_ (`secondary`) si la ventana sigue abierta.
- **`lg`:** _Managers_ y _Jugadores_ dejan de ser pestañas y van en dos columnas con su subtítulo.

#### Ficha del jugador

```
[ 9 ]  Pablo                                peto grande (72 px) y nombre (paso 6)
       [Delantero]                          chip de posición; [Dado de baja] si lo está
17,2 M        15,0 M
Valor actual  Valor inicial
Jornadas
──────────────────────────────
Jornada 11                   +14   ˄
Domingo 26 de octubre
  Gol                         +3            bf-points-breakdown, con las cifras
  Asistencia                  +1            alineadas con las de la fila
  Valoración                 +10
  Total                      +14
```

## Textos

- Se habla de **equipo**, nunca de _peña_.
- Frases normales, verbos claros y sin relleno. Cada botón dice lo que hace (_Crear liga_, _Guardar alineación_) y el aviso de después usa el mismo verbo (_Liga creada_).
- Los errores dicen qué ha pasado y qué hacer, sin disculpas (_No se pudo entrar con Google. Vuelve a intentarlo._). Un vacío invita a la acción que lo llena.
- Sin flechas añadidas al texto de botones o enlaces, ni metadatos unidos con puntos medios.

## Fuera de esta dirección

Texturas o fotos de césped fuera del campo, degradados decorativos, tarjetas idénticas con sombra para cualquier contenido, numeración de cosas que no son una secuencia y animaciones de entrada por secciones.
