# AGENTS.md — buddy-fantasy

Guía para agentes de código (Claude Code, Codex, Copilot…). La fuente de verdad funcional es `docs/requirements.md`: ante cualquier duda de negocio, léelo antes de decidir.

## Qué es

Fantasy para una liga de peñas de domingo. Un administrador registra **un único equipo real** por liga, sus partidos, resultados y estadísticas. Los managers valoran a los jugadores tras cada partido y compiten con sus alineaciones en una clasificación.

- **v1 NO incluye:** apps nativas, pagos, chat, varias competiciones por liga, compraventa de jugadores entre managers. No lo implementes sin que se pida.
- **Objetivo:** un grupo de amigos en un VPS barato, sin bloquear la migración a cloud.
- **Stack:** Java 21, Spring Boot 3, Spring Modulith, PostgreSQL + Flyway, login solo con Google, `@Scheduled` + ShedLock; Angular web mobile first en `frontend/` (Node 26 LTS).

## Estado

Fase 1 completa; Fase 2 (valoraciones) con el desarrollo del backend completo (plan 09). Fase 0: hechos el esqueleto del backend, el modelo base con el login de Google y el esqueleto de `frontend/` (plan 11, cerrado: Angular 22, contrato tipado con el OpenAPI, sistema de diseño, shell, pantallas de identidad y liga, Caddy, E2E de humo y CI); faltan Dockerfile, Compose de producción y VPS. Siguiente en la Fase 2: las pantallas de manager (plan `12`, en curso: hechos su backend, el boceto, los servicios de API, los componentes de `ui/` y `shared/` el shell de cinco destinos y las pantallas de plantilla, ficha, calendario, partido y reglas, tareas 2 a 8) y las de admin con el E2E de la jornada completa (plan `13`, por redactar); la prueba real necesita además el VPS. No adelantes trabajo de fases posteriores salvo que se pida. Detalle en [docs/agents/status.md](docs/agents/status.md).

## Mapa: lee lo que necesites antes de tocar código

| Archivo | Cuándo |
| --- | --- |
| [docs/agents/status.md](docs/agents/status.md) | Qué hay hecho por módulo y qué falta; roadmap |
| [docs/agents/architecture.md](docs/agents/architecture.md) | Stack, módulos y sus RF, reglas entre módulos, invariantes técnicas |
| [docs/agents/domain-rules.md](docs/agents/domain-rules.md) | Puntos, valoraciones, alineaciones, mercado, roles |
| [docs/agents/conventions.md](docs/agents/conventions.md) | Defaults y límites, estilo Java, casos de uso, OpenAPI, migraciones, commits |
| [docs/agents/frontend.md](docs/agents/frontend.md) | Reglas del frontend (FE-xx), cómo añadir un endpoint o un componente |
| [docs/agents/testing.md](docs/agents/testing.md) | Tests obligatorios, infraestructura de integración y escaneos |
| [docs/agents/commands.md](docs/agents/commands.md) | Build, tests, arranque local y variables de entorno |
| `docs/NN-*-plan.md` | Plan de cada módulo, con su *Orden de ejecución* |

## Reglas core inviolables

1. **Límites y defaults en configuración.** Todo valor por defecto o límite de negocio va en `application.yml` (`<módulo>.defaults` / `<módulo>.limits`) y se lee por su puerto (`*DefaultsRepository` / `*LimitsRepository`). Se comprueba en el dominio en cada escritura, nunca al cargar. Nada de constantes en código, anotaciones `@Size`/`@Max` ni límites en la BD.
2. **Modulith.** Cero accesos a internals de otros módulos: solo la API pública del paquete raíz o eventos de dominio. Cada módulo es dueño de sus tablas; nada de joins entre módulos.
3. **Excepciones.** Usa las de `shared/domain` (`DomainValidationException`, `ForbiddenException`, `NotFoundException`, `ConflictException` siempre con `ErrorCode`, `UnexpectedException`), que `ApiExceptionHandler` traduce a `ProblemDetail`. Nunca las genéricas del JDK ni excepciones comprobadas.
4. **Constructores.** `@RequiredArgsConstructor` sobre campos `private final`; sin `@Autowired` en producción. A mano solo si tienen lógica, en entidades JPA o con `@AllArgsConstructor` en agregados que se reconstruyen.
5. **OpenAPI.** Todo endpoint nuevo o modificado se documenta en el mismo cambio con springdoc (`@Operation`, `@Parameter`, `@Schema`, `@ApiResponse`), con DTO web `*Request`/`*Response` propios.
6. **Plazos en el servidor** con un `Clock` inyectable (nunca `Instant.now()`); fechas en UTC (`Instant`/`timestamptz`).
7. **Idempotencia.** Todo `@Scheduled` lleva ShedLock; jobs, recálculos y listeners toleran ejecutarse dos veces (incluso a la vez) sin duplicar efectos ni eventos.
8. **Autorización en cada endpoint** por rol **y liga**. Los roles son por liga y el `ADMIN` siempre juega como manager.
9. **Valoraciones anónimas.** Ninguna API devuelve votos individuales de otros usuarios, tampoco al admin.
10. **Migraciones.** Nunca edites una migración aplicada: crea `V<n>__*.sql` nueva y añádela a `migration-checksums.txt`.
11. **Tests.** Cada `@Test` con `@DisplayName` en inglés; integración con Testcontainers (nunca H2) y la infraestructura compartida de [testing.md](docs/agents/testing.md).
12. **Frontend.** Sigue las reglas FE-xx de `docs/requirements.md` (resumen en [frontend.md](docs/agents/frontend.md)): tipos solo del OpenAPI, ninguna feature importa otra, ningún límite de negocio en el cliente y estilos solo con tokens.

Código, identificadores y commits en **inglés**; textos de la interfaz y de OpenAPI en **español**.

## Flujo de trabajo

- **No hagas commits ni push salvo que el usuario lo pida.** Deja los cambios en el árbol de trabajo. Commits en Conventional Commits, citando `RF-xx`/`RNF-xx`.
- **Autoría:** los commits y PR van solo con la identidad git del usuario (`git config user.name`/`user.email`). Nada de trailers `Co-Authored-By`, `Generated with…` ni enlaces de sesión de ningún agente, y no cambies autor ni committer.
- **Marca el progreso** en el mismo cambio que completa la tarea: ✅ en la columna `Hecho` del plan, `- [x]` en el roadmap de `docs/requirements.md` cuando el ítem esté completo, y actualiza [status.md](docs/agents/status.md) (y la sección *Estado* de aquí) al cerrar un ítem.
- Si añades comandos o variables de entorno, actualiza [commands.md](docs/agents/commands.md).
- **Decisiones abiertas:** ninguna por ahora. Si una tarea depende de una, pregunta o déjala configurable; no la resuelvas por tu cuenta.

## Comandos rápidos

```bash
docker compose -f deploy/docker-compose.dev.yml up -d   # Postgres y SeaweedFS
cd backend && mvn -B verify                             # build + todos los tests (requiere Docker)
mvn test -Dtest='LineupLockTests' -Djacoco.skip=true    # una clase
```
