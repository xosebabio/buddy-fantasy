# Convenciones de código

## Valores por defecto y límites: en configuración, nunca en código

Todo valor por defecto y todo límite de negocio (longitudes máximas, máximos de goles y estadísticas, rangos de dorsal, tope de horas de bloqueo, tamaño máximo de foto…), los que ya existen y los que lleguen con nuevos módulos o parámetros, se hace así:

- Se declara en `application.yml` bajo `<módulo>.defaults` o `<módulo>.limits` (por ejemplo `league.defaults`, `match.limits`), con un comentario si no es obvio. Así se puede sobrescribir como cualquier propiedad (variable de entorno, perfil…).
- Se enlaza con un record `@ConfigurationProperties("<módulo>.defaults|limits")` en `<módulo>/adapter/out/config/` y se convierte a objetos de dominio (`LeagueSettings`, `ScoringRules`, `<Módulo>Limits`).
- El resto del módulo lo lee solo a través de un puerto `<Módulo>DefaultsRepository` o `<Módulo>LimitsRepository` en `application/port/out/` (por ejemplo `LeagueDefaultsRepository`, `MatchLimitsRepository`), implementado por `Properties<Módulo>DefaultsRepository` o `Properties<Módulo>LimitsRepository`. Otros módulos no lo leen: piden lo que necesiten por la API pública del módulo dueño.
- El adaptador construye los objetos de dominio al arrancar, así que un valor inválido impide arrancar. Los valores por defecto también se comprueban contra los límites.
- Los límites se comprueban en el dominio en **cada escritura** (el handler pasa el `<Módulo>Limits` al aggregate: `Match.schedule(..., limits)`), **nunca al cargar** lo ya guardado: bajar un límite no rompe los datos existentes. No van en anotaciones `@Size`/`@Max` de los DTO web, que exigen constantes.
- La BD **no repite los límites de negocio**: solo guarda reglas estructurales (no negativo, enums, `NOT NULL`, unicidad) y columnas sin longitud fija (`VARCHAR` sin tamaño). Así se puede subir un límite sin migración.
- Siguen siendo constantes solo las de **formato o almacenamiento**, que no tiene sentido configurar: rango del tipo de columna (`Short.MAX_VALUE` en un `SMALLINT`), precisión de `NUMERIC(10, 2)`, longitud de la clave de foto, tamaño del token de invitación, definición de cada formato (picas 0–4, nota 0–10), porcentajes ≤ 100. Los límites configurables se validan al arrancar contra ellas.
- Nada de métodos `defaults()` estáticos ni constantes de valores por defecto o límites de negocio en el dominio o los handlers.
- En tests unitarios usa `TestLeagueDefaults`, `TestLeagueLimits`, `TestSquadLimits`, `TestMatchLimits` (o su equivalente en un módulo nuevo, con `TestApplicationYaml`), que leen el `application.yml` real; en tests de Spring, inyecta el repositorio. No copies los valores en los tests salvo para comprobar que coinciden con los requisitos.
- Las migraciones no leen esta configuración: un `DEFAULT` de columna solo rellena filas ya existentes.
- La configuración técnica de un adaptador (por ejemplo `squad.photos.s3.upload-ttl`) va en su propio record de properties del adaptador, sin repositorio.

## Estilo Java

- **Estilo funcional primero.** Prioriza streams (`stream().map/filter/collect`, `IntStream`, `Optional`) frente a bucles `for` y `while` convencionales. Usa un bucle solo cuando el stream quede claramente menos legible (por ejemplo, salidas anticipadas con estado mutable o efectos que dependen del orden).
- **Excepciones propias primero.** Lanza excepciones del proyecto, siempre no comprobadas (que extiendan de `RuntimeException`), en lugar de las genéricas del JDK (`IllegalArgumentException`, `IllegalStateException`…) o de excepciones comprobadas. Usa las genéricas de `shared/domain` (`DomainValidationException` → 400, también para configuración inválida al arrancar; `ForbiddenException` → 403; `NotFoundException` → 404; `ConflictException` → 409, con un `ErrorCode` si el cliente debe distinguir el motivo; `UnexpectedException` → 500, para invariantes rotas y fallos técnicos, también en lugar de un `orElseThrow()` sin argumentos), que `ApiExceptionHandler` ya traduce a `ProblemDetail`, o crea una propia que extienda de ellas o de `RuntimeException` cuando ninguna encaje. No declares `throws` de excepciones comprobadas en el dominio ni en los handlers: si una librería lanza una, envuélvela en una propia.
- **Constructores con Lombok.** Siempre que se pueda, genera los constructores con `@RequiredArgsConstructor` sobre campos `private final`, tanto para la inyección de dependencias (handlers, adaptadores, controladores, jobs, listeners) como en el resto de clases, en lugar de escribirlos a mano. Prefiérelo a `@AllArgsConstructor`, que solo se usa cuando de verdad hace falta (por ejemplo, un agregado con campos no `final` que se reconstruye desde la persistencia, como `Match` o `Player`). Escribe el constructor a mano solo si tiene lógica (validar, construir objetos de dominio al arrancar…), si hay varios o si es el privado de una clase de utilidades; los de las entidades JPA siguen a mano. Nada de `@Autowired` en código de producción (en las clases de test de Spring, `@Autowired` en campos es lo normal). Los `record` no lo necesitan.
- Los mappers (entidad ↔ dominio, DTO ↔ dominio) son clases con `@lombok.experimental.UtilityClass`, con métodos sin `static` explícito. Una interfaz o clase por archivo, también los repositorios Spring Data.

## Casos de uso

- Casos de uso (commands y queries) en `application/usecases/<caso>/`, un paquete por caso de uso con su `Command`/`Query` y su `Handler`.
- Los commands terminan en `Command` y las queries en `Query` (`RegisterOrLoginUserCommand`, `ListMyLeaguesQuery`); el bus único `CommandBus` expone `dispatch` (commands) y `ask` (queries).
- Los handlers de comandos deben ser clases, no lambdas (el bus resuelve el tipo de comando por genéricos).

## Endpoints documentados con OpenAPI

Todo endpoint nuevo (o que cambie) se documenta en el mismo cambio con las anotaciones de springdoc (`io.swagger.v3.oas.annotations`), en español como el resto de textos para el usuario. El documento se sirve en `/v3/api-docs` (OpenAPI 3.0) y `OpenApiDocumentationTests` falla si falta algo:

- **DTO web propios:** los controladores reciben `*Request` y devuelven `*Response` (o `*Body` si es el mismo para leer y escribir), records de `adapter/in/web` que copian la view de `application` con un `static from(View)` (los anidados, como records internos con nombre único). Nunca devuelven views ni tipos de dominio, que no llevan anotaciones de OpenAPI (`HexagonalArchitectureTests` lo impide); los enums de dominio sí pueden ir en un DTO.
- **Descripción:** `@Tag` en el controlador y `@Operation(summary, description)` en cada método: qué hace, rol mínimo en la liga («Rol mínimo: miembro/administrador»), reglas de negocio relevantes (plazos, idempotencia, `RF-xx`).
- **Parámetros:** `@Parameter(description, example)` en cada variable de ruta y de consulta.
- **Campos y validaciones:** `@Schema(description, example)` en el record y en cada uno de sus campos, de entrada y de salida; `nullable = true` (y cuándo es null en la descripción) en los que pueden serlo; `@ArraySchema(arraySchema = @Schema(...))` en las listas. Los rangos fijos van en `minimum`/`maximum`; los límites configurables (`<módulo>.limits`) no caben en anotaciones, que exigen constantes: nómbralos en la descripción con su propiedad (por ejemplo «como mucho `match.limits.sheet-players-max`») en lugar de copiar el valor.
- **Respuestas:** `@ApiResponse` con descripción por cada código posible: el de éxito (`201` con su cabecera `Location`, `204`…), `400`, `403`, `404` y `409`. `OpenApiConfig` añade lo común: el `401`, la sesión y el CSRF, y en cada error el esquema `Problem` y un ejemplo genérico. Cuando el error lleva un `code` (`ErrorCode`) o un motivo propio, enumera los códigos en la descripción y da tu `@ExampleObject` con el `ProblemDetail` real.
- **Ejemplos:** realistas, de una liga de peñas, nunca `string`; los ids y fechas compartidos salen de `ApiExamples`.

## Idioma, migraciones y commits

- Código, identificadores, nombres de tablas y commits en **inglés**; textos de la interfaz en **español**.
- Migraciones Flyway versionadas (`V<n>__descripcion.sql`). **Nunca edites una migración ya aplicada**: crea una nueva (y añade su línea a `migration-checksums.txt`, ver [testing.md](testing.md)).
- Referencia los requisitos por su código (`RF-xx`, `RNF-xx`) en commits, PRs y comentarios cuando aplique.
- Commits en formato Conventional Commits (`feat:`, `fix:`, `chore:`…).

## Frontend

- Mobile first: diseña primero para pantalla de móvil.
