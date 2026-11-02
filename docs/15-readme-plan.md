# 15 · README del proyecto — plan

Plan del `README.md` de la raíz, que hoy solo tiene el título. El repo es público (plan 14, decisión 6), así que el README es lo primero que ve cualquiera que llegue a GitHub: tiene que explicar qué es buddy-fantasy, que es un **proyecto de aprendizaje puesto en producción** y qué significa eso, y llevar a la documentación que ya existe. No es un requisito del roadmap ni adelanta trabajo de la Fase 3: es documentación.

> El README **enlaza, no copia.** Las reglas, el estado y los comandos ya viven en `docs/requirements.md`, `docs/agents/*` y `deploy/README.md`. Si el README los repite, se desfasan. Solo resume lo que no cambia con cada tarea y apunta a la fuente para el detalle.

## 1. Alcance

| Tema             | Decisión                                                                                                                                                        |
| ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Fichero          | `README.md` de la raíz. `frontend/README.md` y `deploy/README.md` se quedan como están; el general los enlaza.                                                  |
| Contenido        | Qué es, el carácter de aprendizaje en producción, estado, arquitectura, decisiones técnicas, cómo se trabaja, arranque en local, tests y despliegue (§3).       |
| Recursos         | Capturas de la app con datos ficticios y un diagrama de arquitectura en Mermaid (§4).                                                                           |
| Fuera de alcance | Reescribir las guías de `docs/agents/`, traducir la documentación existente, una web del proyecto, `CONTRIBUTING.md` o plantillas de issues (salvo decisión 7). |

## 2. Decisiones

Las cerró el usuario al revisar el plan (tarea 1).

| #   | Tema                        | Decisión                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Origen             |
| --- | --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ |
| 1   | Para quién                  | Para desarrolladores y reclutadores: un escaparate técnico de qué se ha construido, cómo y por qué, con peso en las decisiones y lo aprendido. Los amigos de la liga no leen el README: para ellos están la app y `/privacy`.                                                                                                                                                                                                                                                                                                                                                                                           | Usuario            |
| 2   | Idioma                      | Español, como el resto de la documentación y la interfaz.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Usuario            |
| 3   | «Aprendizaje en producción» | Una sección propia que diga las dos cosas: el objetivo es aprender (por eso hay piezas más serias de lo que pide una liga de amigos), y hay usuarios reales (por eso hay backups cifrados, RGPD, CSP, E2E y despliegue con vuelta atrás). Y qué no es: ni producto comercial, ni registro abierto, ni soporte. Los cuatro objetivos: arquitectura backend (monolito modular, hexagonal, eventos de dominio con Spring Modulith), frontend moderno (Angular zoneless con signals, librería de componentes propia, accesibilidad), operar en producción (Docker, CI/CD, VPS, backups, RGPD) y trabajar con agentes de IA. | Usuario            |
| 4   | Desarrollo con agentes      | Decirlo: el repo se trabaja con agentes de código y `AGENTS.md`, los planes numerados y `docs/agents/` son la forma de hacerlo. Es parte de lo que se aprende y explica por qué la documentación es tan detallada. Se cuenta como parte del aprendizaje: el flujo requisitos → plan → agente → revisión.                                                                                                                                                                                                                                                                                                                | Usuario            |
| 5   | Licencia                    | MIT, con `LICENSE` en la raíz a nombre de Xosé Babío Sabín.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | Usuario            |
| 6   | Enlace a la app             | Enlazar `https://buddyfantasy.com` aclarando que es una liga privada por invitación, y enlazar `/privacy` y `/terms`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Usuario            |
| 7   | Contribuciones              | Una línea: es un proyecto personal; los issues son bienvenidos y los PR, mejor tras hablarlo en un issue. Sin `CONTRIBUTING.md`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Usuario            |
| 8   | Capturas                    | Tres o cuatro en móvil (inicio de la jornada, alineación en el campo, valorar, clasificación), en tema claro, generadas con Playwright sobre los datos de los E2E. **Nunca con datos reales** de la liga (RGPD). En `docs/readme/`.                                                                                                                                                                                                                                                                                                                                                                                     | Usuario            |
| 9   | Diagrama                    | Mermaid dentro del README (GitHub lo pinta, se versiona como texto): navegador → Caddy → Spring Boot (módulos) → PostgreSQL, con Google OAuth y los backups a S3.                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Propuesta aceptada |
| 10  | Badges                      | Solo el de CI de `develop` y el de la licencia MIT. Nada de cobertura ni contadores.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Propuesta aceptada |

## 3. Contenido

En este orden. Entre paréntesis, de dónde sale cada sección; el texto la resume y enlaza.

1. **Cabecera.** Nombre, una frase («Fantasy para una liga de peñas de domingo»), badges (decisión 10) y una captura.
2. **Qué es.** El juego en cinco o seis líneas: un admin registra un único equipo real, sus partidos y el acta; los managers hacen su alineación con presupuesto y límite de cambios, valoran a los jugadores tras cada partido y compiten en una clasificación. Roles por liga. Enlace a la app (decisión 6). (`requirements.md`, _Visión y alcance_ y _Roles_.)
3. **Un proyecto de aprendizaje en producción.** Decisión 3. Qué se quería aprender y qué obliga a tomarse en serio tener usuarios reales. Puede ser una tabla corta «Lo que pide una liga de amigos / Lo que hay y por qué».
4. **Estado.** Las seis fases con su marca (hecha, en curso, pendiente) y una frase cada una; enlace a `docs/agents/status.md` y al roadmap de `requirements.md` para el detalle. Sin listar tareas: eso se desfasa.
5. **Arquitectura.** El diagrama (decisión 9), la tabla de módulos con su responsabilidad (sin RF) y las dos ideas que la sostienen: monolito modular con Spring Modulith que se comunica por eventos de dominio, y el contrato OpenAPI del que salen los tipos del frontend. (`architecture.md`.)
6. **Stack.** Tabla breve por capa. (`architecture.md`, _Stack_.)
7. **Decisiones técnicas destacadas.** Seis u ocho, una línea cada una con su enlace al plan: límites y defaults en configuración, eventos idempotentes y jobs con ShedLock, reloj inyectable y plazos en el servidor, valoraciones anónimas, Testcontainers y tests de arquitectura, E2E móvil con axe y CSP, backups cifrados con `age` y vuelta atrás. Es la sección que enseña lo aprendido.
8. **Cómo se trabaja.** `requirements.md` como fuente de verdad, un plan por módulo con decisiones y _Orden de ejecución_, `AGENTS.md` y `docs/agents/` para los agentes (decisión 4), ramas `develop`/`main` y Conventional Commits con `RF-xx`.
9. **Arrancar en local.** Requisitos (Java 21, Maven, Node 26 LTS, Docker) y los cinco o seis comandos mínimos: servicios de desarrollo, backend y frontend. Cómo entrar: con un cliente OAuth propio de Google o con el perfil `e2e` (solo local). Enlace a `commands.md` para el resto. (`commands.md`.)
10. **Tests.** Qué hay (unitarios, integración con Testcontainers, arquitectura, Vitest, Playwright) y los comandos; enlace a `testing.md`.
11. **Despliegue.** Un párrafo: imágenes en GHCR, Compose con Caddy en un VPS, despliegue automático desde `main` con comprobación de humo, backups diarios cifrados en S3. Enlace a `deploy/README.md`.
12. **Estructura del repo.** El árbol de `architecture.md` con una línea por carpeta.
13. **Licencia, contacto y privacidad.** Decisiones 5 y 7; el correo del proyecto (el mismo de `/privacy`, no el personal).

## 4. Recursos

- **Capturas.** Un script de Playwright aparte (`frontend/e2e/readme-screenshots.ts` o un proyecto de Playwright propio que no corre en CI) que reutiliza la preparación por API de `e2e/api.ts` y el perfil `e2e`, con viewport móvil y tema claro, y escribe PNG optimizados en `docs/readme/`. Se regenera a mano cuando cambie mucho la interfaz; no se comprueba en CI.
- **Diagrama.** Mermaid en el propio `README.md`.
- Nada de GIF: pesan y se desfasan antes.

## 5. Reglas del texto

- Sin secretos, IP del servidor ni correo personal (plan 14, §8).
- Nada que dependa de la tarea en curso: el estado va por fases y el detalle, en `status.md`.
- Enlaces relativos dentro del repo, para que funcionen en cualquier rama.
- Breve: que se lea en cinco minutos. Lo que necesite más de un párrafo va en su guía y se enlaza.
- Prettier sobre el Markdown, como el resto de `docs/`.

## 6. Comprobaciones

- Todos los enlaces relativos existen (un `grep` de `](` contra el árbol, o `lychee --offline` si se instala).
- Se ve bien en GitHub: Mermaid, tablas, imágenes y badges (revisarlo en la rama antes de fusionar).
- Los comandos de §3.9 funcionan desde un clon limpio siguiendo solo el README hasta ver la pantalla de login.
- Las capturas no tienen ningún nombre real.

## 7. Cambios en la documentación

- `AGENTS.md`: una fila del README en el mapa no hace falta (es la entrada, no una guía); sí una línea en _Flujo de trabajo_: si cambia el stack, una fase o la forma de arrancar, se actualiza el README en el mismo cambio.
- `docs/agents/status.md`: una línea en la Fase 0.
- `LICENSE` (MIT) en la raíz.

## 8. Orden de ejecución

| #   | Tarea                                                         | Resultado                    | Hecho |
| --- | ------------------------------------------------------------- | ---------------------------- | ----- |
| 1   | Revisar este plan con el usuario y cerrar las decisiones (§2) | Decisiones cerradas          | ✅    |
| 2   | `LICENSE` MIT (decisión 5)                                    | Licencia                     | ✅    |
| 3   | Texto del README sin capturas (§3, §5), con el diagrama       | README revisable             | ✅    |
| 4   | Script de capturas y capturas en `docs/readme/` (§4)          | Capturas con datos ficticios |       |
| 5   | Comprobaciones de §6, desde un clon limpio                    | README probado               |       |
| 6   | Documentación de §7 y cerrar el plan                          | Plan cerrado                 |       |

Cada tarea es un commit en Conventional Commits: `docs(readme): …`, `chore: add license` y `test(e2e): …` para el script de capturas.

## 9. Riesgos y notas

- **Desfase.** El mayor riesgo de un README es mentir con el tiempo. Por eso enlaza en vez de copiar y el estado va por fases. La regla de §7 en `AGENTS.md` lo mantiene.
- **Datos personales.** Las capturas y los ejemplos son el sitio fácil para colar un nombre o un correo real. Solo datos de los E2E.
- **MIT y datos.** La licencia cubre el código, no los datos de la liga ni los textos legales con nombre propio: quien lo reutilice debe escribir los suyos. El README lo dice en una línea.
