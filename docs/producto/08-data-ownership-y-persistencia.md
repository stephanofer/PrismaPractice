# Practice Hera — Data Ownership y persistencia

## Data Ownership — qué vive en MySQL y qué vive en Redis

### Decisión conceptual

Se define que:

- **MySQL es la fuente de verdad**
- **Redis es el hot-state y la capa de coordinación rápida**

Y esto NO puede quedar ambiguo.

Porque si mezclamos ownership, después pasa lo peor:

- datos duplicados con versiones distintas
- estados imposibles de reconciliar
- bugs de desincronización
- lógica inventada por cada runtime

### Regla madre

#### MySQL debe guardar

- datos que deben sobrevivir reinicios
- datos históricos
- datos de identidad
- datos competitivos persistentes
- datos auditables

#### Redis debe guardar

- presencia actual
- estado actual del jugador
- colas activas
- matches activos
- locks/cooldowns
- proyecciones rápidas
- coordinación entre runtimes

---

## Qué debe vivir en MySQL

### 1. Player profile persistente

- identidad base del jugador
- first seen / last seen
- settings persistentes
- visibilidad/perfil público si aplica

### 2. Datos competitivos persistentes

- SR por modo
- rango por modo
- Global Rating
- rango global
- peaks importantes
- estado de placements cuando aplique

### 3. Seasons e histórico competitivo

- snapshot de season
- stats por season
- cortes históricos
- datos all-time
- rewards manuales entregados o registrables si luego hace falta

### 4. Match history persistente

- metadata del match
- datos por jugador
- stats agregadas
- eventos mínimos importantes
- snapshot final del inventario

### 5. Relaciones sociales persistentes

- social preferences persistentes
- historial social básico si luego hace falta

### 6. Layouts persistentes

- layout por kit/modo del jugador

### 7. Arenas / definiciones configurables persistentes

- arena definitions
- regiones/spawns/config operativa

### 8. Catálogos/config persistentes del sistema

- modes
- rules
- queues
- event templates
- configuraciones administrativas persistentes

---

## Qué debe vivir en Redis

### 1. Presencia actual del jugador

- online/offline práctico
- runtime actual
- server actual
- player state actual
- subestado contextual si hace falta

### 2. Queue state

- entradas activas de cola
- timestamps de entrada
- snapshots rápidos para matchmaking
- ventanas de expansión por tiempo

### 3. Match state activo

- matches en curso
- estado operativo del match
- participantes activos
- arena reservada
- runtime asociado
- score/round state activo si hace falta coordinación rápida

### 4. Arena reservation / operational state

- arena reservada
- arena en uso
- arena reseteando
- locks temporales

### 5. Requests sociales efímeras

- duel requests pendientes
- party invites pendientes

#### Decisión fuerte

Las requests pendientes son efímeras y temporales. No tiene sentido tratarlas como core persistence pesada en MySQL desde el inicio.

### 6. Cooldowns, locks y anti-spam

- cooldowns sociales
- locks de transición
- locks de matchmaking
- protections de duplicidad temporal

### 7. Proyecciones rápidas / counters vivos

- jugadores en hub
- jugadores en cola
- jugadores en match
- jugadores en FFA
- contadores globales visibles

### 8. Delivery / coordinación cross-runtime

- eventos de transición
- señales de invalidación
- sincronización operativa rápida

---

## Qué NO debe pasar

### Error 1 — Persistir en Redis algo que es fuente de verdad

Ejemplo malo:

- SR oficial solo en Redis
- historial oficial solo en Redis

No.

Si importa a largo plazo o a nivel competitivo, va a MySQL.

### Error 2 — Usar MySQL para hot-paths de runtime

Ejemplo malo:

- consultar MySQL cada vez que cambia estado de cola
- consultar MySQL en pleno combate para decisiones rápidas

No.

Eso rompe performance y escala mal.

### Error 3 — Dual ownership ambiguo

Ejemplo malo:

- Redis y MySQL ambos creyendo tener “el estado real” de una cola o de un match activo

No.

Hay que definir claramente qué es persistente y qué es operativo.

---

## Recomendación estructural fuerte

La forma correcta de pensarlo es:

- **MySQL** = verdad persistente
- **Redis** = verdad operativa actual
- **proyecciones** = derivaciones optimizadas según necesidad

### Traducción simple

- si querés saber qué pasó históricamente → MySQL
- si querés saber qué está pasando AHORA → Redis

---

## Decisiones concretas que tomo

### 1. Player state actual vive en Redis

Sí.

Porque cambia rápido, coordina runtimes y no debe depender de lecturas persistentes pesadas.

### 2. SR/ranks/history viven en MySQL

Sí.

Porque son identidad competitiva persistente.

### 3. Queue active state vive en Redis

Sí.

Porque es hot-state puro.

### 4. Match activo vive operativamente en Redis, pero su salida/historial vive en MySQL

Sí.

Ese corte es exactamente el correcto.

### 5. Counters globales y visibilidad en tiempo real deben salir de Redis/proyecciones rápidas

Sí.

No de consultas pesadas cada vez.

---

## Conclusión

La arquitectura correcta de datos para Practice es:

- **MySQL para identidad, competitivo, histórico y configuración persistente**
- **Redis para coordinación, presencia, colas, matches activos, requests efímeras y contadores vivos**

Ese corte mantiene:

- coherencia
- rendimiento
- auditabilidad
- escalabilidad

---

## Matriz operativa de ownership por entidad

Para que el diseño no quede abstracto, esta es la tabla recomendada de ownership real por entidad.

| Entidad | Source of Truth | Redis / Hot State | Notas |
|---|---|---|---|
| Practice Profile | MySQL | Cache opcional si hace falta | Identidad persistente del jugador |
| User Settings | MySQL | Cache opcional | Configuración persistente por usuario |
| Current Player State | Redis | Redis | Estado operativo actual del jugador |
| Player Presence | Redis | Redis | Online/offline, runtime y server actual |
| SR por modo | MySQL | Cache/proyección opcional | Identidad competitiva persistente |
| Rango por modo | MySQL | Cache/proyección opcional | Derivado o persistido según implementación |
| Global Rating | MySQL | Cache/proyección opcional | Valor competitivo persistente |
| Global Rank | MySQL / proyección persistente | Cache opcional | Mejor como proyección optimizada |
| Season snapshots | MySQL | No | Histórico y corte de season |
| All-time stats | MySQL | Cache opcional | Agregados persistentes |
| Season stats | MySQL | Cache opcional | Agregados por season |
| Mode stats | MySQL | Cache opcional | Agregados por modo |
| Social/activity stats | MySQL | Cache opcional | Parties joined, events joined, etc. |
| Duel Request pendiente | Redis | Redis | Efímera, expirable |
| Party Invite pendiente | Redis | Redis | Efímera, expirable |
| Party actual | Redis | Redis | Estado grupal operativo actual |
| Queue Definition | MySQL | Cache opcional | Catálogo/config persistente |
| Queue Entry activa | Redis | Redis | Hot-state puro |
| Matchmaking search window | Redis | Redis | Expansión por tiempo y coordinación |
| Match Session activa | Redis | Redis | Lifecycle operativo actual |
| Match Result / Match History | MySQL | No | Histórico y auditoría |
| Match stats detalladas | MySQL | No | Persistencia del resumen/historial |
| Inventory final snapshot | MySQL | No | Parte del match history |
| Arena Definition | MySQL | Cache opcional | Configuración persistente |
| Arena Reservation / Arena State | Redis | Redis | Reserva, uso, resetting, locks |
| Mode Definition | MySQL | Cache opcional | Catálogo/config persistente |
| Rule Definition | MySQL | Cache opcional | Catálogo/config persistente |
| Rule Set efectivo en match | Redis durante ejecución / MySQL al persistir resultado | Redis | En vivo durante match; congelado en histórico al finalizar |
| Event Template | MySQL | Cache opcional | Configuración persistente |
| Hosted Event activo | Redis | Redis | Lifecycle operativo del evento |
| Hosted Event history | MySQL | No | Si se decide persistir histórico del evento |
| Leaderboard projection | Redis + persistencia/proyección en MySQL si conviene | Redis | Debe ser lectura optimizada, no cálculo ad hoc |
| Global counters (hub, queue, ffa, match) | Redis | Redis | Tiempo real |
| Cooldowns / anti-spam | Redis | Redis | Temporales por naturaleza |
| Transition locks | Redis | Redis | Protegen race conditions y duplicidad |

### Reglas prácticas de lectura

#### Leer de MySQL cuando

- importa el histórico
- importa la persistencia real
- importa auditoría
- importa identidad duradera

#### Leer de Redis cuando

- importa el ahora
- importa coordinación entre runtimes
- importa performance de hot path
- importa evitar carreras y duplicados temporales

### Decisión fuerte

Si una entidad tiene versión en MySQL y Redis, NO significa doble ownership.

Significa:

- MySQL conserva la verdad persistente
- Redis mantiene una proyección o estado operativo temporal

Ese matiz es CLAVE para no destruir la arquitectura.

---

## Persistencia concreta — diseño inicial MySQL + Redis

### Objetivo

Bajar el ownership ya definido a contratos concretos de persistencia para poder empezar a desarrollar sin ambigüedad.

### Principio rector

- **MySQL** guarda identidad, histórico, competitivo y configuración persistente.
- **Redis** guarda hot-state, coordinación y proyecciones rápidas.

No se busca un modelo perfecto y eterno. Se busca un modelo inicial **limpio, auditable, escalable y fácil de evolucionar**.

---

## MySQL — tablas iniciales recomendadas

### 1. `practice_players`

#### Propósito
Identidad base persistente del jugador dentro de Practice.

#### Campos base recomendados
- `player_id` (uuid, pk)
- `current_name`
- `normalized_name`
- `first_seen_at`
- `last_seen_at`
- `created_at`
- `updated_at`

---

### 2. `practice_player_settings`

#### Propósito
Settings persistentes por usuario.

#### Campos base recomendados
- `player_id` (pk/fk)
- `chat_enabled`
- `duel_requests_enabled`
- `friends_only_duels_enabled`
- `friend_requests_enabled`
- `spectate_enabled`
- `lobby_players_visible`
- `party_requests_enabled`
- `event_alerts_enabled`
- `scoreboard_enabled`
- `ping_range_setting`
- `updated_at`

---

### 3. `practice_modes`

#### Propósito
Catálogo persistente de `Mode Definition`.

#### Campos base recomendados
- `mode_id` (pk)
- `display_name`
- `description`
- `icon_material`
- `icon_data_json`
- `ranked_enabled`
- `unranked_enabled`
- `public_ffa_enabled`
- `party_ffa_enabled`
- `party_duel_enabled`
- `hosted_event_enabled`
- `layout_editable`
- `premade_kit_id`
- `default_match_settings_json`
- `competitive_flags_json`
- `relevant_stats_json`
- `arena_requirements_json`
- `context_compatibility_json`
- `enabled`
- `created_at`
- `updated_at`

---

### 4. `practice_rules`

#### Propósito
Catálogo persistente de `Rule Definition`.

#### Campos base recomendados
- `rule_id` (pk)
- `display_name`
- `description`
- `category`
- `scope`
- `parameter_schema_json`
- `compatible_contexts_json`
- `execution_phases_json`
- `conflicts_with_json`
- `telemetry_hints_json`
- `visibility_hints_json`
- `enabled`
- `created_at`
- `updated_at`

---

### 5. `practice_mode_rules`

#### Propósito
Asignación de rules por modo con parámetros efectivos por defecto.

#### Campos base recomendados
- `mode_id` (pk/fk parcial)
- `rule_id` (pk/fk parcial)
- `enabled`
- `parameters_json`
- `created_at`
- `updated_at`

---

### 6. `practice_queues`

#### Propósito
Catálogo persistente de `Queue Definition`.

#### Campos base recomendados
- `queue_id` (pk)
- `display_name`
- `description`
- `queue_type`
- `mode_id`
- `player_type`
- `matchmaking_profile`
- `uses_sr`
- `uses_ping_range`
- `uses_region_selection`
- `search_expansion_profile_json`
- `access_rules_json`
- `social_rules_json`
- `arena_policy_json`
- `competitive_impact_json`
- `visibility_json`
- `enabled`
- `created_at`
- `updated_at`

---

### 7. `practice_arenas`

#### Propósito
Catálogo persistente de `Arena Definition`.

#### Campos base recomendados
- `arena_id` (pk)
- `display_name`
- `description`
- `arena_type`
- `region`
- `runtime_type`
- `server_pool`
- `icon_material`
- `allowed_modes_json`
- `blocked_modes_json`
- `allowed_player_types_json`
- `supported_event_templates_json`
- `universal_region_json`
- `battle_region_json`
- `positions_json`
- `environment_capabilities_json`
- `reset_strategy_json`
- `selection_weight`
- `featured`
- `enabled`
- `created_at`
- `updated_at`

---

### 8. `practice_player_mode_ratings`

#### Propósito
Estado competitivo actual por modo.

#### Campos base recomendados
- `player_id` (pk/fk parcial)
- `mode_id` (pk/fk parcial)
- `current_sr`
- `current_rank_key`
- `peak_sr`
- `peak_rank_key`
- `placements_completed`
- `placements_progress`
- `season_id`
- `updated_at`

#### Nota
Si el rating vigente es por season, esta tabla representa el estado actual de la season activa.

---

### 9. `practice_player_global_rating`

#### Propósito
Estado global competitivo actual del jugador.

#### Campos base recomendados
- `player_id` (pk/fk)
- `current_global_rating`
- `current_global_rank_key`
- `peak_global_rating`
- `peak_global_rank_key`
- `season_id`
- `updated_at`

---

### 10. `practice_player_mode_stats`

#### Propósito
Stats agregadas por jugador y por modo.

#### Campos base recomendados
- `player_id` (pk/fk parcial)
- `mode_id` (pk/fk parcial)
- `scope_type` (`ALL_TIME` / `SEASON`)
- `season_id` (nullable según scope)
- `stats_json`
- `updated_at`

#### Decisión fuerte
Para stats agregadas por modo, arrancar con `stats_json` es más flexible que abrir 80 columnas demasiado pronto.

---

### 11. `practice_player_core_stats`

#### Propósito
Stats agregadas globales no atadas a un modo específico.

#### Campos base recomendados
- `player_id` (pk/fk parcial)
- `scope_type` (`ALL_TIME` / `SEASON`)
- `season_id` (nullable)
- `stats_json`
- `updated_at`

Ejemplos:
- total ranked wins/losses
- total events hosted/joined/won
- total FFA kills/deaths si decidimos consolidarlas acá o en tabla separada

---

### 12. `practice_player_kit_layouts`

#### Propósito
Layouts personalizados por jugador y modo/kit.

#### Campos base recomendados
- `player_id` (pk/fk parcial)
- `mode_id` (pk/fk parcial)
- `layout_key` (pk parcial si hace falta múltiple variante)
- `layout_json`
- `updated_at`

---

### 13. `practice_friendships`

#### Propósito
Relación social persistente.

#### Campos base recomendados
- `friendship_id` (pk)
- `player_a_id`
- `player_b_id`
- `created_at`

#### Decisión fuerte
Normalizar internamente el par para evitar duplicados invertidos.

---

### 14. `practice_seasons`

#### Propósito
Registro de seasons y cortes históricos.

#### Campos base recomendados
- `season_id` (pk)
- `display_name`
- `status` (`ACTIVE`, `CLOSED`, etc.)
- `started_at`
- `ended_at`
- `closed_manually_by`
- `notes`
- `created_at`

---

### 15. `practice_matches`

#### Propósito
Metadata principal persistente del `Match Session` terminado.

#### Campos base recomendados
- `match_id` (pk)
- `queue_id`
- `mode_id`
- `queue_type`
- `player_type`
- `region`
- `arena_id`
- `runtime_server_id`
- `status_final`
- `result_type`
- `series_format`
- `rounds_to_win`
- `started_at`
- `ended_at`
- `duration_ms`
- `effective_settings_json`
- `effective_rules_json`
- `winner_side`
- `created_at`

---

### 16. `practice_match_players`

#### Propósito
Snapshot por jugador dentro de un match persistido.

#### Campos base recomendados
- `match_id` (pk/fk parcial)
- `player_id` (pk/fk parcial)
- `player_name`
- `side_key`
- `result`
- `sr_before`
- `sr_after`
- `sr_delta`
- `health_final`
- `ping_final`
- `armor_snapshot_json`
- `inventory_snapshot_json`
- `consumables_snapshot_json`
- `effects_snapshot_json`
- `stats_json`

---

### 17. `practice_match_events`

#### Propósito
Eventos mínimos importantes del historial de match.

#### Campos base recomendados
- `match_event_id` (pk)
- `match_id` (fk)
- `event_type`
- `round_number`
- `actor_player_id` (nullable)
- `target_player_id` (nullable)
- `payload_json`
- `occurred_at`

---

### 18. `practice_event_templates`

#### Propósito
Plantillas persistentes del framework de eventos hosteados.

#### Campos base recomendados
- `event_template_id` (pk)
- `display_name`
- `event_type`
- `base_mode_id` (nullable)
- `configuration_json`
- `enabled`
- `created_at`
- `updated_at`

---

### 19. `practice_hosted_event_history`

#### Propósito
Histórico persistente de eventos hosteados si decidimos guardarlo desde primera fase.

#### Campos base recomendados
- `hosted_event_id` (pk)
- `event_template_id`
- `host_player_id`
- `status_final`
- `region`
- `arena_id`
- `started_at`
- `ended_at`
- `summary_json`

---

## Redis — claves y estructuras iniciales recomendadas

### Convención general

Usar prefijo claro:

- `practice:`

Y namespaces por agregado:

- `practice:player:*`
- `practice:queue:*`
- `practice:match:*`
- `practice:arena:*`
- `practice:party:*`
- `practice:social:*`
- `practice:event:*`
- `practice:counter:*`

---

### 1. Player presence y state

#### Claves recomendadas
- `practice:player:{playerId}:presence`
- `practice:player:{playerId}:state`

#### Estructura sugerida
Redis Hash

Campos ejemplo:
- `online`
- `runtime`
- `server`
- `state`
- `substate`
- `updatedAt`

---

### 2. Queue entries activas

#### Claves recomendadas
- `practice:queue:{queueId}:entries`
- `practice:player:{playerId}:queue`

#### Estructura sugerida
- Sorted Set para entries por timestamp
- Hash/String auxiliar por jugador para acceso directo

#### Mi recomendación
- `Sorted Set` por cola usando `joinedAt` como score
- `Hash` o `String` por jugador con snapshot breve de queue entry

---

### 3. Matchmaking windows / search expansion

#### Claves recomendadas
- `practice:queue:{queueId}:search:{playerId}`

#### Estructura sugerida
Redis Hash

Campos ejemplo:
- `joinedAt`
- `currentExpansionStage`
- `lastEvaluatedAt`
- `pingRangeSetting`
- `regionHints`

---

### 4. Match sessions activas

#### Claves recomendadas
- `practice:match:{matchId}`
- `practice:player:{playerId}:active-match`

#### Estructura sugerida
Redis Hash

Campos ejemplo:
- `status`
- `queueId`
- `modeId`
- `arenaId`
- `region`
- `runtimeServerId`
- `round`
- `score`
- `createdAt`

---

### 5. Arena operational state

#### Claves recomendadas
- `practice:arena:{arenaId}:state`
- `practice:arena:{arenaId}:lock`

#### Estructura sugerida
- Hash para estado
- Lock key con TTL para reservas/transiciones críticas

Campos ejemplo:
- `status`
- `reservedBy`
- `matchId`
- `updatedAt`

---

### 6. Party active state

#### Claves recomendadas
- `practice:party:{partyId}`
- `practice:player:{playerId}:party`

#### Estructura sugerida
- Hash para metadata de la party
- Set/List para miembros

Campos ejemplo:
- `leaderId`
- `status`
- `createdAt`
- `memberLimit`

---

### 7. Social requests pendientes

#### Claves recomendadas
- `practice:social:friend-request:{senderId}:{targetId}`
- `practice:social:duel-request:{senderId}:{targetId}`
- `practice:social:party-invite:{partyId}:{targetId}`

#### Estructura sugerida
String o Hash con TTL

#### TTL recomendados
- friend request → 5 min
- duel request → 30 s
- party invite → 60 s

---

### 8. Cooldowns / anti-spam / locks

#### Claves recomendadas
- `practice:cooldown:{type}:{actorId}:{targetId}`
- `practice:lock:transition:{playerId}`
- `practice:lock:matchmaking:{queueId}:{playerId}`

#### Estructura sugerida
String con TTL

---

### 9. Hosted events activos

#### Claves recomendadas
- `practice:event:{eventId}`
- `practice:event:{eventId}:participants`

#### Estructura sugerida
- Hash para metadata del evento activo
- Set/Sorted Set para participantes

---

### 10. Counters globales

#### Claves recomendadas
- `practice:counter:hub`
- `practice:counter:queue`
- `practice:counter:match`
- `practice:counter:ffa`
- `practice:counter:event`

#### Estructura sugerida
String/Counter atómico simple

---

### 11. Leaderboard projections rápidas

#### Claves recomendadas
- `practice:leaderboard:global`
- `practice:leaderboard:season:{seasonId}:global`
- `practice:leaderboard:mode:{modeId}`
- `practice:leaderboard:season:{seasonId}:mode:{modeId}`

#### Estructura sugerida
Sorted Set

#### Score recomendado
- SR o Global Rating según leaderboard

---

## Decisiones fuertes de modelado

### 1. Configuración compleja inicial en JSON donde conviene

Sí.

Para `Mode`, `Rule`, `Queue`, `Arena` y `Event Template`, conviene arrancar con varios campos `*_json` en lugar de hiper-normalizar demasiado pronto.

#### Por qué
- reduce fricción inicial
- mantiene flexibilidad
- evita 40 tablas de config prematuras

### 2. Datos core competitivos deben estar más estructurados

Sí.

Profile, ratings, seasons, match metadata y friendships deben tener estructura clara, no todo en blobs.

### 3. Redis debe modelar agregados operativos, no espejos completos de MySQL

Sí.

Solo lo necesario para hot path y coordinación.

### 4. Leaderboards deben usar Sorted Sets en Redis

Sí.

Es la opción más natural para lecturas rápidas y top-N.

### 5. Queue entries deben usar Sorted Set por tiempo

Sí.

Eso simplifica expansión progresiva y matching por antigüedad.

---

## Conclusión

Con este diseño inicial ya tenemos una base concreta para pasar a:

- contratos de repositorio/gateway
- implementación de `practice-data`
- claves Redis reales
- primera capa de servicios

Sin improvisar storage en mitad del desarrollo.
