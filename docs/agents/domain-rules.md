# Reglas de dominio (no romper)

Resumen operativo de `docs/requirements.md`, que manda ante cualquier duda.

Todos los parámetros configurables (ventana, formato, bloqueo, cambios, tamaño, k, tope…) se guardan **por liga**, y una liga nueva nace con los de `league.defaults` (ver *Valores por defecto y límites* en [conventions.md](conventions.md)). Ver la tabla completa en `docs/requirements.md`.

## Roles

Los roles son **por liga**, no globales. Un usuario puede ser administrador de una liga y manager en otra. Toda comprobación de permisos recibe la liga como contexto. Una membership tiene un único rol (`ADMIN` o `MANAGER`) y **el administrador siempre juega como manager** en su liga: trata `ADMIN` como manager al preguntar si alguien puede alinear, valorar o puntuar.

## Puntuación por acción (precargada, editable por liga)

| Acción | Portero | Defensa | Medio | Delantero |
| --- | --- | --- | --- | --- |
| Gol | +6 | +5 | +4 | +3 |
| Gol de penalti | +3 | +3 | +3 | +3 |
| Penalti parado | +3 | +3 | +3 | +3 |
| Penalti fallado | −2 | −2 | −2 | −2 |
| Asistencia | +1 | +1 | +1 | +1 |
| Gol en propia puerta | −3 | −3 | −3 | −3 |
| Tarjeta amarilla | −1 | −1 | −1 | −1 |
| Doble amarilla | −3 | −3 | −3 | −3 |
| Roja directa | −5 | −5 | −5 | −5 |

- El gol de penalti vale +3 **en cualquier posición** (no se suma además el gol normal).
- La doble amarilla vale −3 **en total**; no se suman las dos amarillas.
- La tabla es un dato por liga, sembrado desde `league.defaults.scoring-rules` de `application.yml` con estos valores; nunca constantes en código.

## Puntos

- Puntos del jugador en la jornada = puntos por acciones + puntos por valoración. **Sin porcentajes ni pesos** entre ambas partes.
- Puntos del manager = suma de los puntos de sus jugadores alineados en la alineación **bloqueada**.
- Si un jugador no recibe valoraciones, o menos que el mínimo de votos de la liga (por defecto 1): solo puntos por acciones (o la nota por defecto de la liga, si está configurada). Sin valoraciones son 0 puntos, no los −2 de 0 picas.

## Valoraciones

- Se abren al cerrar el partido; ventana por defecto de 24 h (configurable).
- Solo se valora a jugadores que jugaron. Valorar es opcional.
- Formato por liga: picas 0–4 (por defecto) o nota decimal 0–10.
- Conversión de la media a puntos, por defecto: picas 0 = −2, 1 = 2, 2 = 6, 3 = 10, 4 = 14 (`4 × media − 2`); nota decimal, 1 punto por punto de nota. Se convierte la media (no cada voto) y el resultado se redondea al entero, mitad hacia arriba. La conversión es un parámetro por liga sembrado desde `league.defaults`; nunca constantes en código.
- Editables mientras la ventana siga abierta.
- **Anónimas**: ninguna API devuelve valoraciones individuales de otros usuarios, tampoco al admin; solo la media.
- **Cancelación (RF-41)**: con la ventana abierta, el admin puede cancelar las valoraciones de un manager en un partido: se borran y queda excluido de valorar ese partido (también si aún no había votado). Es definitivo; con la ventana cerrada ya no se puede.

## Alineaciones

- N jugadores (por defecto 7), formación por posiciones opcional: mínimo de jugadores por posición configurable por liga (por defecto 1 portero y 1 por línea).
- Varios managers pueden alinear al mismo jugador (sin exclusividad). En el futuro será un parámetro de la liga.
- Editable hasta X horas antes del partido (por defecto 2 h); máximo Y cambios por jornada (por defecto 2).
- Si el manager no la toca, se arrastra la de la jornada anterior.
- Presupuesto máximo de la alineación según el valor vigente de los jugadores (activado por defecto, 100 M; el admin puede desactivarlo). Se comprueba al guardar y al bloquear; si al bloquear lo supera, esa jornada no puntúa.
- Los cambios se cuentan por diferencia final contra la alineación bloqueada del partido anterior; sustituir a un jugador dado de baja es gratis.

## Valor de mercado

- Tras cerrar cada jornada: `Δvalor = k × (puntos − media de la jornada) × valor`, con `k = 0,05` por defecto, tope de ±10 % por jornada y valor mínimo.
- Guarda el histórico de valores (se pinta en una gráfica).
