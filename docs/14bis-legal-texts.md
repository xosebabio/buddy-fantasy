# Textos legales · borrador (14 bis, tarea 3)

Borrador para revisar antes de maquetar (tarea 4). No está publicado. Las notas entre corchetes son decisiones abiertas o comprobaciones pendientes: se quitan al maquetar. Cada `###` será un `h2` en la página. El texto sigue el [plan](14bis-legal-pages-plan.md) (§3). Donde añado algo que el plan no cubría, lo digo en la lista de abajo.

## Decisiones y comprobaciones

1. **Decidido: administrador (plan §12.5.1).** Si el administrador borra su cuenta, se cierra su liga. Lo leo como borrado completo: la liga desaparece para todos sus miembros, con sus partidos, alineaciones, valoraciones y puntos. Si querías otra cosa (por ejemplo, dejarla solo de lectura), dímelo y cambio el texto. El procedimiento SQL para cerrar una liga no está escrito todavía: `deploy/README.md` §10 lo deja pendiente.
2. **Decidido: base para conservar datos de juego (plan §12.4.1).** Se acepta el interés legítimo (art. 6.1.f) en que la clasificación sea estable.
3. **Registro de eventos (plan §12.5.2).** El borrador asume la opción (a): se queda sin plazo y lo dice. Si se elige (b), `completion-mode: delete`, se quita esa frase.
4. **IP en los logs de Caddy (plan §12.5.3).** Solo se comprueba en el VPS tras el primer despliegue. Hasta entonces, la frase sobre la IP tiene dos versiones.
5. **Decidido: jugadores sin cuenta (art. 14 RGPD).** Publicar la política y dar el contacto basta; no se avisa a cada jugador.
6. **Duración de la cookie `SESSION`.** El código no fija `Max-Age`, así que debería ser de sesión. No lo he visto en un navegador real: comprobarlo con las herramientas de desarrollo antes de publicar.
7. **Transferencias (DPF).** Confirmar en la lista oficial del Marco de Privacidad de Datos UE-EE. UU. que Google y AWS siguen adheridas.
8. **Corrección: plazo de las copias.** El plan decía «hasta 30 días» (decisión 8). En el bucket son 30 días para `daily/` y 14 para `deploy/` (plan 14, decisión 13). El texto dice las dos cifras.

---

## Política de privacidad (`/privacy`)

Última actualización: 2 de noviembre de 2026

### Quién es el responsable

Buddy Fantasy es un proyecto personal, gratuito y sin ánimo de lucro. El responsable del tratamiento de tus datos es Xosé Babío Sabín. Para cualquier duda o petición sobre tus datos, escribe a docksidegarage@gmail.com.

### Qué datos tratamos

- **De tu cuenta de Google:** tu identificador de Google, tu correo, tu nombre, la dirección de tu foto de perfil, y las fechas de alta y de tu último acceso.
- **De tu juego:** las ligas en las que estás y tu rol en cada una, tus alineaciones, tus valoraciones y tus puntos.
- **De los jugadores del equipo:** nombre, dorsal, posición, estadísticas de cada partido, valoraciones y valor. Cuando se active, también su foto. Ver «Jugadores sin cuenta».
- **Técnicos:** la sesión que te mantiene dentro y los registros de errores del servidor. Los registros no incluyen tu correo ni tu nombre. En un caso raro, un error inesperado puede dejar tu identificador de Google. [Pendiente 4: si el VPS confirma la IP, se añade «y tu dirección IP»; si no, se escribe «ni tu dirección IP».]

No usamos analítica, publicidad ni rastreadores. Tampoco cargamos scripts ni fuentes de terceros. La única excepción es la foto de perfil, que se carga desde Google (ver «Encargados y terceros»).

### Para qué los usamos y con qué base

- **Prestarte el servicio** con los datos de tu cuenta y de tu juego. Es lo que pides al entrar (base: art. 6.1.b del RGPD).
- **Seguridad, registros de errores y copias de seguridad:** interés legítimo (art. 6.1.f), para proteger el servicio y poder recuperarlo si falla.
- **Datos de los jugadores:** interés legítimo de la liga en llevar su juego (art. 6.1.f). Un jugador puede oponerse o pedir que se borren sus datos o se cambie su nombre (ver «Tus derechos»).
- **Conservar alineaciones, puntos y valoraciones ya contados** aunque se borre una cuenta: interés legítimo en que la clasificación de la liga sea estable (art. 6.1.f).

Los puntos y el valor de los jugadores se calculan con reglas fijas y automáticas. No hay decisiones que produzcan efectos jurídicos sobre ti. Para entrar necesitas tu cuenta de Google; no te pedimos ningún otro dato personal.

### Quién ve tus datos

- Los **miembros de tu liga** ven tu nombre y tu foto, tus alineaciones una vez bloqueadas, y tus puntos y tu posición en la clasificación.
- Tus **valoraciones son anónimas**. Nadie, tampoco el administrador, ve el voto individual de otra persona: solo se publica la media y el número de votos.
- Si el administrador te excluye de valorar un partido, lo ves tú, sin motivo.
- **Nadie fuera de tu liga** ve nada de tu juego.

### Encargados y terceros

- **Hetzner Online GmbH** (Alemania): aloja el servidor donde funcionan la aplicación y su base de datos.
- **Amazon Web Services EMEA SARL**: guarda las copias de seguridad en un bucket de la UE. Las copias van cifradas antes de salir del servidor, y la clave para abrirlas no está ni en el servidor ni en AWS.
- **Google**, como responsable independiente: cuando entras con Google, trata tus datos de acceso según su propia política de privacidad. La foto de tu perfil se carga desde sus servidores (`googleusercontent.com`), así que Google ve la dirección IP desde la que se muestra.

### Transferencias fuera de la UE

Los datos del servicio están en la UE: el servidor, en Alemania, y las copias, en un bucket de la UE. Google y Amazon Web Services son empresas de Estados Unidos adheridas al Marco de Privacidad de Datos UE-EE. UU. [Pendiente 7.]

### Cuánto tiempo los guardamos

- **Tu cuenta y tu juego:** mientras exista tu cuenta.
- **Alineaciones, puntos y valoraciones ya contados:** se conservan aunque borres tu cuenta. En la liga aparecen como «Antiguo miembro». Son datos seudonimizados: sin tu cuenta no se puede saber quién eres a partir de ellos, pero siguen siendo datos personales.
- **Liga cerrada por el borrado de su administrador:** sus datos de juego se borran con ella. Las copias de seguridad los guardan hasta que caducan.
- **Registro interno de eventos:** guarda identificadores de lo que pasa en la liga (un partido cerrado, una alineación bloqueada), sin nombres ni correos. No tiene un plazo de borrado fijo. [Pendiente 3: si se elige (b), se quita esta frase.]
- **Sesión:** hasta que cierras sesión o caduca por inactividad (30 minutos sin actividad).
- **Copias de seguridad:** 30 días las diarias y 14 días las de antes de cada despliegue. Tus datos pueden seguir en ellas hasta que caduquen, también después de borrar tu cuenta. [Pendiente 8.]
- **Registros de errores del servidor:** Docker guarda hasta tres ficheros de 10 MB por servicio, y los más antiguos se borran solos.

### Tus derechos

Puedes pedir:

- acceder a tus datos;
- corregirlos;
- borrarlos;
- oponerte a su tratamiento o pedir que se limite;
- recibirlos en un formato común para llevártelos (portabilidad).

Para hacerlo, escribe a docksidegarage@gmail.com. Te respondemos en un mes como máximo. Hasta que la app permita borrar la cuenta desde dentro, el borrado se pide por correo y se hace a mano.

Darte de baja de una liga no borra tu cuenta.

Si eres administrador de una liga, borrar tu cuenta cierra la liga: desaparece para todos sus miembros, con sus partidos, alineaciones, valoraciones y puntos. Escríbenos antes para organizarlo. Para ti no hay plazo fijo, porque el cierre se hace a mano.

Si no estás conforme con cómo tratamos tus datos, puedes reclamar ante la Agencia Española de Protección de Datos (aepd.es).

### Jugadores sin cuenta

El administrador de la liga registra a los jugadores del equipo. Si estás en la plantilla y no tienes cuenta, esta política es la información sobre cómo tratamos tus datos. Puedes pedir que se borren o que se cambie tu nombre escribiendo a docksidegarage@gmail.com.

### Cookies y almacenamiento local

No usamos cookies de publicidad ni de analítica, y por eso no mostramos un aviso de cookies. Las que usamos son técnicas y hacen falta para que la app funcione, así que no necesitan tu consentimiento. Si esto cambia, actualizaremos esta página.

| Nombre | Dónde | Para qué | Cuánto dura |
| --- | --- | --- | --- |
| `SESSION` | Cookie | Mantiene tu sesión iniciada. El código de la página no puede leerla. | Hasta que cierras el navegador o cierras sesión. En el servidor, la sesión caduca tras 30 minutos sin actividad. [Pendiente 6.] |
| `XSRF-TOKEN` | Cookie | Protege las acciones frente a peticiones falsas enviadas desde otras webs. | Hasta que cierras el navegador. |
| `bf.theme` | Almacenamiento local | Guarda si usas el tema claro o el oscuro. Si eliges «sistema», se borra. | Hasta que cambias el tema. Cerrar sesión no la borra. |
| `bf.lastLeague` | Almacenamiento local | Guarda la última liga que abriste, para llevarte a ella desde la pantalla de inicio. | Hasta que abres otra liga. Cerrar sesión no la borra; puedes borrarla desde los datos del sitio en tu navegador. |
| `bf.returnUrl` | Almacenamiento de la pestaña | Guarda a qué página volver después de entrar con Google. | Se borra al usarla o al cerrar la pestaña. |

Google puede poner sus propias cookies cuando entras con Google. Son de Google y se rigen por su política.

### Menores

Buddy Fantasy es para personas de 14 años o más, la edad a la que en España se puede dar el consentimiento para tratar datos en internet. No comprobamos la edad, porque Google no nos la comunica. Si eres menor de 14 años y tus datos están aquí, escribe a docksidegarage@gmail.com y se borran.

### Cambios

Cada versión de esta página lleva su fecha arriba. Si el cambio es importante, te lo avisaremos dentro de la app cuando exista esa función.

---

## Condiciones de servicio (`/terms`)

Última actualización: 2 de noviembre de 2026

### Qué es y quién lo ofrece

Buddy Fantasy es un juego de fantasy para una liga de peñas de domingo. Es gratuito: no hay pagos ni premios. Lo ofrece su responsable, Xosé Babío Sabín, como proyecto personal y sin ánimo de lucro. Contacto: docksidegarage@gmail.com.

### Tu cuenta

- Entras con tu cuenta de Google. Cada persona tiene una sola cuenta.
- Tienes que tener al menos 14 años (ver la política de privacidad). No comprobamos la edad.
- Eres responsable de lo que se hace desde tu cuenta. Si crees que alguien la usa sin permiso, avisa al contacto.

### Ligas y roles

- Cada liga la crea su administrador. El administrador registra el equipo, los jugadores, los partidos y las actas; invita y expulsa managers; y puede cancelar las valoraciones de un manager en un partido.
- Los managers forman su alineación antes de cada partido y valoran a los jugadores después.
- Si eres administrador, solo registras a jugadores que estén de acuerdo en ser registrados, y los quitas o cambias si te lo piden.
- Mientras seas administrador, no puedes salir de tu liga. Si borras tu cuenta como administrador, la liga se cierra para todos sus miembros, con sus partidos y puntos.

### Uso aceptable

No puedes:

- hacerte pasar por otra persona;
- poner nombres de liga o de jugador ofensivos;
- automatizar el uso del servicio, ni sobrecargarlo o atacarlo.

### Valoraciones

Las valoraciones son opiniones sobre el rendimiento de los jugadores. Son anónimas: nadie ve tu voto, solo la media. Valora con respeto y según lo que has visto del partido. Si el administrador te excluye de valorar un partido, lo verás, sin motivo.

### Tu contenido

Lo que escribes, como nombres de liga o de jugador, sigue siendo tuyo. Nos das permiso para guardarlo y mostrarlo a los miembros de tu liga, solo en lo que el servicio necesita.

### Disponibilidad

El servicio se ofrece tal cual. No garantizamos que esté siempre disponible ni que no se pierdan datos, aunque hay copias de seguridad cada día. Podemos cambiarlo o cerrarlo; si lo cerramos, avisaremos antes cuando sea posible.

### Responsabilidad

En lo que permita la ley, el responsable no responde de los daños que se deriven del uso del servicio o de no poder usarlo. Esto no quita ninguno de los derechos que te reconoce la normativa de consumidores.

### Baja y suspensión

- Puedes dejar tu liga cuando quieras, salvo que seas su administrador.
- Puedes pedir el borrado de tu cuenta. Cómo hacerlo está en la [política de privacidad](/privacy).
- El responsable puede suspender una cuenta que incumpla estas condiciones.

### Cambios

Cada versión de estas condiciones lleva su fecha arriba. Si el cambio es importante, te lo avisaremos dentro de la app cuando exista esa función.

### Ley aplicable

Estas condiciones se rigen por la ley española. Si eres consumidor, puedes acudir a los juzgados de tu domicilio.

---

## Textos de enlace

- **Login**, bajo «Entrar con Google»: «Al entrar aceptas las condiciones de servicio y la política de privacidad», con los dos enlaces.
- **Perfil**, sección final, con título «Legal»: «Política de privacidad» y «Condiciones de servicio».
- **Títulos de página** (`<title>`): «Política de privacidad» y «Condiciones de servicio».
