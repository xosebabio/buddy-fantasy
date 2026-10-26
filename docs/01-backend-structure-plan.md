# Plan · Estructura del backend

Estado: **borrador, D1–D14 resueltas** (Fase 0 · Fundamentos).
Objetivo: definir el esqueleto de `backend/` como monolito modular (Spring Modulith) con capas y arquitectura hexagonal, antes de escribir código de negocio.

## 1. Principios

1. **Monolito modular**: un paquete de primer nivel por módulo (`identity`, `league`, `squad`, `match`, `rating`, `lineup`, `scoring`, `market`, `notification`), según `AGENTS.md`.
2. **Hexagonal dentro de cada módulo**: el dominio no depende de Spring, JPA ni HTTP. Los puertos los define el dominio/aplicación; los adaptadores los implementan.
3. **Capas** dentro de cada módulo: `domain` ← `application` ← `adapter` (la dependencia siempre apunta hacia dentro).
4. **Entre módulos**: solo mediante la API pública del módulo (paquete raíz) o eventos de dominio. Nunca se importan internals de otro módulo.
5. **Cada módulo es dueño de sus tablas**; sin joins SQL entre módulos.
6. Un `Clock` inyectable en toda lógica con plazos (nunca `Instant.now()`).

## 2. Estructura de repositorio

```
backend/
├── pom.xml                        # Maven, un único módulo de build (D1, D3)
└── src/
    ├── main/
    │   ├── java/com/buddyfantasy/     # paquete base com.buddyfantasy (D2)
    │   │   ├── BuddyFantasyApplication.java
    │   │   ├── shared/                # kernel compartido (mínimo, ver §5)
    │   │   ├── identity/
    │   │   ├── league/
    │   │   ├── squad/
    │   │   ├── match/
    │   │   ├── rating/
    │   │   ├── lineup/
    │   │   ├── scoring/
    │   │   ├── market/
    │   │   └── notification/
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/          # ver duda D5 (Flyway por módulo o global)
    └── test/
        └── java/com/buddyfantasy/
            ├── ModularityTests.java   # ApplicationModules.of(...).verify()
            └── <módulo>/...
```

## 3. Estructura interna de un módulo (plantilla)

Ejemplo con `league`:

```
league/
├── LeagueApi.java                 # API pública (interfaz) que otros módulos pueden usar
├── LeagueCreated.java             # eventos de dominio públicos
├── package-info.java              # @ApplicationModule (dependencias permitidas)
├── domain/                        # núcleo puro, sin Spring/JPA
│   ├── League.java                # agregado
│   ├── LeagueId.java              # value objects
│   ├── Membership.java
│   └── LeagueSettings.java
├── application/                   # casos de uso
│   ├── usecases/                  # un paquete por caso de uso, con su command/query y su handler (D6, D7, D8)
│   │   ├── createleague/
│   │   │   ├── CreateLeague.java          # record: implements Command<LeagueId>
│   │   │   └── CreateLeagueHandler.java   # implements CommandHandler<CreateLeague, LeagueId>
│   │   └── getleaguesummary/              # las queries solo cuando haga falta (D8)
│   │       ├── GetLeagueSummary.java      # record: implements Query<LeagueSummary>
│   │       └── GetLeagueSummaryHandler.java
│   └── port/
│       └── out/                   # puertos de salida
│           ├── LeagueRepository.java
│           └── DomainEventPublisher.java
└── adapter/
    ├── in/
    │   ├── web/                   # controllers REST + DTOs + mappers
    │   └── event/                 # listeners de eventos de otros módulos
    └── out/
        ├── persistence/           # entidades JPA, repos Spring Data, mappers
        └── ...                    # otros adaptadores (email, push…)
```

Reglas de dependencia (verificadas con ArchUnit, ver §6):

| Desde | Puede depender de |
| --- | --- |
| `domain` | solo `domain` (y `shared` kernel puro) |
| `application` | `domain`, `application` |
| `adapter` | `application`, `domain`; frameworks |
| otro módulo | solo paquete raíz del módulo (`LeagueApi`, eventos) |

Los controllers solo conocen el `CommandBus` (y `QueryBus` si aplica) y los records `Command`/`Query`; los handlers solo conocen puertos `out`.

### 3.1 Patrón Command + bus (D6, D7)

Contratos en `shared.application` (Java puro, sin Spring):

```java
public interface Command<R> {}
public interface CommandHandler<C extends Command<R>, R> { R handle(C command); }
public interface CommandBus { <R> R dispatch(Command<R> command); }
```

- Cada caso de uso de escritura = un `record` que implementa `Command<R>` + un handler que implementa `CommandHandler`. La interfaz `CommandHandler` es el puerto de entrada explícito (D7).
- `SpringCommandBus` (adaptador, en `shared.infrastructure`) resuelve el handler por tipo de comando y aplica los aspectos comunes como decoradores/pipeline:
  1. **Transacción** (`TransactionTemplate`; una transacción por comando).
  2. Validación de bean (`jakarta.validation`) del comando.
  3. Logging/métricas.
  4. (Más adelante) autorización por liga, ver §8.
- Los handlers **no** llevan `@Transactional`: lo aporta el bus. Un comando = una transacción = un agregado (los efectos en otros módulos van por eventos).
- Los controllers construyen el comando desde el DTO y llaman a `bus.dispatch(...)`.
- Los comandos y handlers son **internos** del módulo; otros módulos no los despachan (usan `*Api` o eventos).
- Jobs `@Scheduled` y listeners de eventos también entran por el bus, para reutilizar transacción e idempotencia.

### 3.2 Lecturas (D8)

CQRS **solo cuando sea necesario**: por defecto, una lectura simple puede usar el puerto `out` del repositorio. Cuando una pantalla necesite una proyección (clasificación, ficha con estadísticas…), se crea un `Query<R>` + `QueryHandler` que consulta directamente con `JdbcClient`/proyecciones, sin pasar por el dominio, y un `QueryBus` simétrico (sin transacción de escritura, `readOnly`). Las proyecciones solo leen tablas del propio módulo.

## 4. Comunicación entre módulos

- **Consultas síncronas** (p. ej. "¿es X admin de la liga Y?"): interfaz `*Api` en el paquete raíz del módulo propietario; implementada en `application`.
- **Eventos de dominio** (preferidos para flujos): `MatchClosed`, `RatingWindowClosed`, `MatchdayScored`… publicados con `ApplicationEventPublisher` y consumidos con `@ApplicationModuleListener`.
- Spring Modulith **event publication registry** (tabla JDBC) para entrega fiable/at-least-once → los listeners deben ser idempotentes (invariante del proyecto).
- Dependencias previstas (a validar):

```
identity  ← league ← squad ← match ← rating ← scoring ← market
                  ↖ lineup ────────────────↗
notification ← (eventos de todos)
```

## 5. Kernel compartido (`shared`)

Mantenerlo **mínimo** y sin lógica de negocio: `Clock` config, contratos CQRS (`Command`, `CommandHandler`, `CommandBus`, `Query`…) y su implementación Spring, tipos base (`AggregateRoot`, `DomainEvent`), errores comunes, utilidades de seguridad (`CurrentUser`), config de Jackson/ProblemDetails. Se declara como `@ApplicationModule(type = OPEN)`.

## 6. Verificación automática

- `ModularityTests`: `ApplicationModules.of(BuddyFantasyApplication.class).verify()` (obligatorio por AGENTS.md).
- **ArchUnit** para las reglas hexagonales dentro del módulo (domain sin Spring/JPA; adapter no usado desde application; etc.).
- Tests de integración con Testcontainers + PostgreSQL (no H2).
- Generar documentación de módulos con `Documenter` (PlantUML) como parte del build (opcional).

## 7. Persistencia

- Entidades JPA **separadas** del modelo de dominio, con mappers en `adapter/out/persistence` (hexagonal estricto, D4). El dominio no lleva anotaciones JPA.
- Flyway: un único `db/migration`; el módulo va en el nombre del archivo (`V<n>__<modulo>_<descripcion>.sql`, p. ej. `V2__league_create_tables.sql`) y las tablas llevan prefijo de módulo (`league_*`, `match_*`) para dejar clara la propiedad (D5).
- Fechas: `Instant` ↔ `timestamptz`.

## 8. Seguridad transversal

- `identity` expone el principal autenticado; `league` expone `LeagueAccess` (rol por liga). Cada endpoint comprueba rol **en la liga** (los roles son por liga, no globales).
- Spring Security configurado en `identity/adapter/in/web` (o `shared`, ver D6).

## 9. Plan de ejecución (tareas)

| # | Tarea | Resultado |
| --- | --- | --- |
| 1 | Generar proyecto Spring Boot 3 (Java 21) en `backend/` con dependencias base | Arranca vacío |
| 2 | Añadir Lombok (D12), springdoc-openapi (D13), Modulith, Flyway, Postgres, Security, Validation, Actuator, ShedLock, Testcontainers, ArchUnit | Build verde |
| 3 | Crear paquetes de los 9 módulos con `package-info.java` y plantilla hexagonal (carpetas + `.gitkeep`/clase mínima) | Estructura visible |
| 4 | `shared`: Clock, contratos Command/Query + `SpringCommandBus` (transacción, validación, logging), base types, errores (ProblemDetail) | Compila |
| 4b | Test del bus: despacha al handler correcto, abre transacción, hace rollback ante excepción | Test verde |
| 5 | `ModularityTests` + tests ArchUnit de reglas hexagonales | Fallan si se rompe una regla |
| 6 | `docker-compose` de desarrollo (Postgres) en `deploy/` + `application.yml` por variables de entorno | `docker compose up` + arranque |
| 7 | Migración Flyway inicial (`V1__baseline.sql`) | Flyway aplica en Testcontainers |
| 8 | CI mínimo en GitHub Actions (build + tests) | Pipeline verde |
| 9 | Actualizar la sección *Comandos* de `AGENTS.md` | Documentado |

Fuera de alcance de este plan: lógica de negocio de cualquier módulo (Fase 1+), frontend.

## 10. Decisiones y dudas

Marcadas con una recomendación (⭐). Las iremos resolviendo una a una; la respuesta se anotará aquí.

| ID | Duda | Opciones | Estado |
| --- | --- | --- | --- |
| D1 | **Build tool** | Maven ⭐ · Gradle Kotlin DSL | **Resuelta: Maven** |
| D2 | **Paquete base / groupId** | `com.buddyfantasy` ⭐ · `es.<tu-dominio>` · otro | **Resuelta: `com.buddyfantasy`** |
| D3 | **Multi-módulo de build** | Un único módulo Maven con paquetes ⭐ · Un submódulo de build por módulo | **Resuelta: un único módulo Maven; los límites los verifican Modulith y ArchUnit** |
| D4 | **Modelo de dominio vs. entidades JPA** | Separados con mappers ⭐ · Entidades JPA como dominio | **Resuelta: separados, hexagonal estricto** |
| D5 | **Flyway: ubicación de migraciones** | Un único `db/migration` con prefijo de módulo en el nombre ⭐ · Carpeta por módulo (`db/migration/league/…`) con locations múltiples · Un esquema PostgreSQL por módulo | **Resuelta: un único `db/migration`, módulo en el nombre del archivo y prefijo de tabla** |
| D6 | **Casos de uso: estilo** | Un servicio por caso de uso (`CreateLeagueService`) ⭐ · Un servicio de aplicación por agregado con varios métodos | **Resuelta: patrón Command con `Command`/`CommandHandler` y `CommandBus` que aporta transacción y aspectos comunes (§3.1)** |
| D7 | **Puertos `in`** | Interfaces explícitas por caso de uso ⭐ · Controllers llaman directamente al servicio de aplicación (sin interfaz `in`) | **Resuelta: sí, interfaces explícitas (`CommandHandler` es el puerto de entrada)** |
| D8 | **Comandos/queries** | CQS ligero: lecturas de pantalla con proyecciones directas (JdbcClient/JPA proyecciones) saltando dominio ⭐ · Todo pasa por el dominio | **Resuelta: CQRS solo cuando sea necesario (§3.2)** |
| D9 | **Dónde vive la configuración de Spring Security** | En `identity` ⭐ · En `shared` | **Resuelta: en `identity`** |
| D10 | **API pública entre módulos** | Interfaces `*Api` + DTOs/records propios ⭐ · Solo eventos (sin consultas síncronas) | **Resuelta: interfaces `*Api` + records propios** |
| D11 | **Mapeo DTO↔dominio** | A mano ⭐ (explícito, sin magia) · MapStruct | **Resuelta: a mano** |
| D12 | **Librerías de apoyo** | Lombok: no ⭐ (usar `record`) · sí | **Resuelta: Lombok permitido, pero `record` siempre que sea posible (Lombok solo donde un record no encaje, p. ej. agregados mutables o entidades JPA)** |
| D13 | **Documentación de API** | springdoc-openapi (generar cliente Angular) ⭐ · sin OpenAPI por ahora | **Resuelta: springdoc-openapi** |
| D14 | **Eventos** | Modulith event registry en JDBC ⭐ · solo eventos en memoria (sin persistencia) | **Resuelta: registro de eventos de Modulith en JDBC** |

Ejemplo de cómo se resolverá cada una: tras tu respuesta, cambio el estado a **Resuelta: <decisión>** y ajusto las secciones afectadas.
