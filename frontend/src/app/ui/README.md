# Dirección visual

Identidad de la interfaz (FE-28), cerrada con el usuario en la tarea 8 del plan 11 con la skill `.agents/skills/frontend-design`. Las reglas de estilo están en `docs/requirements.md` (FE-24 a FE-31, RNF-12) y aquí no se repiten: este documento dice cómo se usan. El boceto de referencia es [`docs/design/visual-direction.html`](../../../../docs/design/visual-direction.html) (ábrelo en el navegador): login, mis ligas, la alineación con el shell, el cambio de liga y el escritorio, en los dos temas.

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

Tiene dominio (la posición), así que vive en `shared/` (`bf-bib`) y llega con la primera pantalla que lo use. Cuando se añadan las fotos de los jugadores (RF-09 ya las admite, opcionales), la variante con foto se diseñará entonces: el peto sigue siendo lo que se ve por defecto y lo que tiene quien no tiene foto.

## Color

Las escalas 50–900 las genera `scripts/palette.mjs` (tarea 9) a partir de los seis colores de FE-25. Los componentes solo usan los tokens semánticos; los valores aproximados son los del boceto.

| Token                                       | Uso                                           | Claro                                 | Oscuro                           |
| ------------------------------------------- | --------------------------------------------- | ------------------------------------- | -------------------------------- |
| `bg`                                        | Fondo de la página                            | neutro cálido muy claro (≈ `#F6F5F2`) | ≈ `#121211`                      |
| `surface`                                   | Barras, hojas, navegación, campos             | blanco                                | un paso sobre `bg` (≈ `#1C1B19`) |
| `border`, `border-strong`                   | Divisores; contornos de controles             | neutros 200 y 300                     | neutros 800 y 700                |
| `text`, `text-muted`                        | Texto; metadatos y etiquetas                  | casi negro; neutro 600                | casi blanco; neutro 300          |
| `brand`                                     | Botón principal, pestaña activa               | verde base                            | verde base                       |
| `brand-text`, `brand-subtle`                | Texto verde; fondo del destino activo         | verde 700; verde 50                   | verde 400; verde 900             |
| `on-color`                                  | Texto sobre cualquier tono base (botón, peto) | casi negro                            | casi negro                       |
| `accent`, `accent-text`, `accent-subtle`    | Selección, enlaces, etiqueta _Admin_          | lavanda base, 700, 50                 | lavanda base, 300, 900           |
| `focus`                                     | Anillo de foco de teclado                     | lavanda 600                           | lavanda 300                      |
| `danger`, `danger-text`                     | Errores y puntos negativos                    | rojo base, 700                        | rojo base, 300                   |
| `warning`, `warning-text`, `warning-subtle` | Plazos a punto de vencer                      | ámbar base, 800, 50                   | ámbar base, base, 900            |
| `position-*`                                | Petos y chips de posición                     | tonos base                            | tonos base                       |
| `pitch`, `pitch-stripe`, `pitch-line`       | Césped a franjas y sus líneas                 | `#123D29`, `#164530`, blanco al 55 %  | iguales                          |
| `on-pitch`, `on-pitch-muted`                | Nombre y valor del jugador en el campo        | blanco; blanco al 78 %                | iguales                          |
| `scrim`                                     | Velo bajo hojas y diálogos                    | casi negro al 45 %                    | negro al 60 %                    |

- El botón principal lleva **texto casi negro sobre el verde** (FE-25), también en oscuro.
- El verde de marca es para actuar y para lo positivo; el lavanda, para lo seleccionado y el foco. No se intercambian.
- El campo es igual en los dos temas: un verde oscuro elegido para que los cuatro colores de posición y el anillo lavanda pasen de 3:1 sobre el césped, y el texto blanco de 10:1. Si cambia la paleta, el test de contraste (tarea 9) lo vuelve a comprobar.

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

- Pesos que se cargan: Barlow 400, 500 y 600; Barlow Condensed 500, 600 y 700; solo latín.
- Todas las cifras con `font-variant-numeric: tabular-nums`. Los puntos llevan signo (`+14`, `−3` con el signo menos tipográfico), en `brand-text` o `danger-text`.
- Una cifra va con su unidad o contexto más pequeño y apagado al lado (`87,4 / 100 M`, `1 de 2`) y la etiqueta debajo.
- Sin mayúsculas sostenidas en etiquetas, sin sobretítulos y sin resaltar una palabra suelta en un titular. Texto en frases de menos de 80 caracteres.

## Espacio, radios, sombras y movimiento

- **Espaciado** en base 4: `--bf-space-1` a `-8` = 4, 8, 12, 16, 24, 32, 48 y 64 px. Margen lateral de la página: 16 px en móvil.
- **Radios por jerarquía:** `tag` 4 px (etiquetas, petos, campo), `control` 10 px (botones, inputs, anillos de selección), `sheet` 20 px (esquinas superiores de hojas) y `round` (avatares). Las filas de lista no tienen radio.
- **Sombras:** solo en lo que flota sobre la página: `overlay` (hojas, toasts) y `raised` (menús). Nada de sombras en filas ni en tarjetas.
- **Movimiento:** `--bf-motion-fast` 120 ms (cambios de estado), `--bf-motion-base` 220 ms (hojas que entran) con `ease-out`. La única animación que no responde a una acción es la caída de los cuatro petos (1, 4, 8, 9) en el login; lo demás solo contesta a algo que hace la persona. Con `prefers-reduced-motion`, sin desplazamientos.
- **Foco:** contorno de 2 px en `focus` separado 2 px, en todo lo que se pueda enfocar.
- **Objetivos táctiles** de 44 px como mínimo; botones de 48 px de alto.

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

### Alineación (boceto para los planes siguientes)

Cabecera con la jornada, el rival y la hora; el plazo de bloqueo a la derecha del título; presupuesto y cambios como cifras. Debajo, el campo de césped a franjas en vertical (portería propia abajo), con cada jugador como peto, nombre y valor. Tocar un peto lo selecciona (anillo lavanda) para cambiarlo; la vista en lista es la alternativa (FE-19). Acción principal: _Guardar alineación_.

## Textos

- Se habla de **equipo**, nunca de _peña_.
- Frases normales, verbos claros y sin relleno. Cada botón dice lo que hace (_Crear liga_, _Guardar alineación_) y el aviso de después usa el mismo verbo (_Liga creada_).
- Los errores dicen qué ha pasado y qué hacer, sin disculpas (_No se pudo entrar con Google. Vuelve a intentarlo._). Un vacío invita a la acción que lo llena.
- Sin flechas añadidas al texto de botones o enlaces, ni metadatos unidos con puntos medios.

## Fuera de esta dirección

Texturas o fotos de césped fuera del campo, degradados decorativos, tarjetas idénticas con sombra para cualquier contenido, numeración de cosas que no son una secuencia y animaciones de entrada por secciones.
