# Arquitectura

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21+, Spring Boot 3, Spring Modulith (monolito modular) |
| Base de datos | PostgreSQL + Flyway |
| Autenticación | Spring Security + OAuth2 login (solo Google), sesión en Spring Session JDBC |
| Jobs | `@Scheduled` + ShedLock |
| Frontend | Angular + `@angular/pwa`, mobile first (Angular Material o PrimeNG, por decidir) |
| Notificaciones | webpush-java + SMTP (Brevo, Resend…) |
| Imágenes | S3 + CloudFront (AWS SDK v2, subida por URL prefirmada); SeaweedFS en local y en tests |
| Infra | Docker Compose (app + Postgres + Caddy), GitHub Actions |

Estructura de repositorio prevista (ajústala aquí si cambia):

```
backend/    # Spring Boot (Maven)
frontend/   # Angular PWA
deploy/     # docker-compose, Caddyfile, scripts de backup
docs/       # requisitos, planes y guías para agentes (docs/agents/)
```

## Módulos del backend

Monolito modular con Spring Modulith. Un paquete de primer nivel por módulo:

| Módulo | Responsabilidad | RF |
| --- | --- | --- |
| `identity` | Usuarios, login, perfil, sesiones | RF-01–04 |
| `league` | Ligas, membresías, roles por liga, invitaciones, parámetros | RF-05–08 |
| `squad` | Jugadores del equipo real y su ficha | RF-09–11 |
| `match` | Partidos, convocatoria, resultado, estadísticas | RF-12–16 |
| `rating` | Ventana y valoraciones de los managers | RF-17–23, RF-41 |
| `lineup` | Alineaciones, bloqueo y límite de cambios | RF-24–29 |
| `scoring` | Puntos por jugador/manager y clasificaciones | RF-30–33 |
| `market` | Valor de mercado de los jugadores | RF-34–36 |
| `notification` | Web Push, email, preferencias y centro de avisos | RF-37–40 |
| `datadefinition` | Definiciones de datos compartidas sin lógica (`Position`, `ScoringAction`) | — |

Reglas:

- Los módulos **no acceden a los internals de otros módulos**. Solo usan su API pública (paquete raíz del módulo) o **eventos de dominio** (`MatchClosed`, `RatingWindowClosed`, `MatchdayScored`…).
- `datadefinition` es un módulo hoja y su paquete raíz es API pública: solo enums y records sin lógica, usados por dos o más módulos. No depende de ningún otro módulo; todos pueden depender de él. Si un tipo necesita comportamiento, va a `shared` o a su módulo dueño.
- Mantén un test de verificación de Modulith (`ApplicationModules.of(...).verify()`) que falle si se rompen las dependencias.
- Cada módulo es dueño de sus tablas. Nada de joins SQL entre tablas de módulos distintos.
- Diseña pensando en que un módulo pueda extraerse a servicio algún día: eventos en lugar de llamadas síncronas cuando el flujo lo permita.
- Dentro de cada módulo, arquitectura hexagonal (`domain`, `application`, `adapter/in`, `adapter/out`), verificada por `HexagonalArchitectureTests`. Paquetes y nombres en [conventions.md](conventions.md).

## Invariantes técnicas

- **Plazos en el servidor.** El bloqueo de alineaciones y el cierre de valoraciones se validan siempre en backend. El cliente solo refleja el estado.
- **Jobs idempotentes.** Todo `@Scheduled` lleva ShedLock y puede ejecutarse dos veces sin duplicar efectos. Los recálculos de jornada también son idempotentes (recalcular = mismo resultado).
- **Listeners idempotentes.** Un evento de módulo puede llegar más de una vez: si su listener falla, `ResubmitIncompleteEventsJob` lo reenvía hasta que funcione, y también al arrancar. Todo listener tolera recibir dos veces el mismo evento (incluso a la vez) sin duplicar efectos ni eventos.
- **Autorización en cada endpoint** por rol y liga. Nunca confíes en que el frontend oculte botones.
- **Fechas:** `Instant`/`timestamptz` en UTC en backend y base de datos; conversión a `Europe/Madrid` solo en la interfaz.
- **Auditoría:** los cambios del administrador en partidos y estadísticas quedan registrados para poder recalcular (RNF-04).
- **Sin estado en disco.** Configuración por variables de entorno; nada de secretos en el repo.
- **Seguridad:** HTTPS, CSRF activo, contraseñas con bcrypt/argon2 si hay login con email.
- **RGPD:** mínimos datos personales; el borrado de cuenta debe ser posible.
- **Rendimiento:** < 300 ms en pantallas principales con ~50 usuarios concurrentes. Evita N+1.
