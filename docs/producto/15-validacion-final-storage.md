# Practice Hera — Validación final de storage

## Propósito

Validar y cerrar el contrato inicial de storage antes de empezar implementación fuerte.

Este documento NO redefine desde cero `08-data-ownership-y-persistencia.md`.
Lo que hace es:

- confirmar qué decisiones quedan aprobadas
- corregir huecos importantes
- dejar claro qué NO vamos a hacer
- marcar el contrato final recomendado con el que se puede empezar a desarrollar

---

## Criterios de validación usados

La validación final se hizo con estos principios:

1. **MySQL como verdad persistente**
2. **Redis como hot-state y coordinación**
3. **nada de dual ownership ambiguo**
4. **nada de sobreingeniería de tablas si todavía no aporta valor**
5. **estructura fuerte donde importa; flexibilidad razonable donde conviene**
6. **hot paths fuera de MySQL**
7. **catálogos administrables sin romper el dominio**

---

## Validación general

### Lo que queda aprobado

- el corte MySQL/Redis está bien planteado
- el uso de JSON en catálogos/configs iniciales está bien planteado
- ratings, matches, seasons y settings deben seguir estructurados
- queue state, player state, active match state, arena reservation, requests efímeras y counters deben seguir en Redis
- leaderboards deben seguir como proyecciones rápidas, no como consultas ad hoc a perfiles gigantes

### Lo que necesitaba corrección

Hay dos huecos importantes que había que cerrar antes de implementar:

1. faltaba una tabla persistente clara para **kits base**
2. faltaba una tabla/config persistente clara para **definición de rangos**

Esas dos piezas son necesarias porque:

- el cliente puede crear/editar contenido in-game
- los modos referencian kits
- los ratings referencian rangos visibles

---

## Correcciones aprobadas sobre el diseño previo

### 1. Agregar tabla `practice_kits`

#### Por qué
No alcanza con tener layouts personalizados del jugador si no existe una fuente persistente para el kit base administrable.

#### Tabla aprobada
`practice_kits`

#### Campos base recomendados
- `kit_id` (pk)
- `display_name`
- `description`
- `base_contents_json`
- `armor_contents_json`
- `offhand_contents_json`
- `editable_layout`
- `layout_restrictions_json`
- `enabled`
- `created_at`
- `updated_at`

#### Decisión fuerte
Los kits base deben ser contenido persistente administrable. No quiero que queden escondidos solo dentro de un modo o hardcodeados en runtime.

---

### 2. Agregar tabla `practice_rank_tiers`

#### Por qué
Los nombres e intervalos de rango no deberían quedar perdidos como convención implícita si el sistema va a mostrar ranks por modo y rank global.

#### Tabla aprobada
`practice_rank_tiers`

#### Campos base recomendados
- `rank_key` (pk)
- `display_name`
- `min_sr`
- `max_sr` (nullable para tier abierto)
- `sort_order`
- `enabled`
- `created_at`
- `updated_at`

#### Decisión fuerte
Aunque el sistema de rating sea simple al principio, la definición de tiers tiene que vivir como dato administrable y no solo como una idea suelta en código.

---

### 3. Friends queda fuera del ownership de Practice

#### Decisión fuerte
No se aprueba tabla propia de `practice_friendships` ni friend requests dentro de Practice.

#### Por qué
Friends se externaliza como sistema social de red. Practice solo debe integrarlo vía gateway.

---

### 4. No crear tablas persistentes de estado activo que no lo necesitan

#### Se mantiene la decisión
NO recomiendo crear en MySQL tablas como:

- `practice_active_parties`
- `practice_active_queue_entries`
- `practice_active_matches`
- `practice_pending_social_requests`

#### Por qué
Eso sería duplicar hot-state en persistencia y generar ownership ambiguo.

---

### 5. No espejar perfiles completos en Redis por defecto

#### Se mantiene la decisión
Redis puede tener cachés puntuales o proyecciones útiles.
Pero NO vamos a meter una copia completa del profile/settings/ratings de cada jugador “porque sí”.

#### Por qué
- ensucia invalidación
- aumenta complejidad sin necesidad
- contradice el principio de mantener Redis enfocado en hot-state real

---

### 6. Telemetría de combate de altísima frecuencia NO debe vivir en Redis como stream gigante inicial

#### Decisión fuerte
Durante la ejecución del match, el runtime puede acumular datos finos en memoria del servidor responsable.

Luego:
- el resultado agregado
- los eventos mínimos importantes
- el snapshot final

se persisten a MySQL.

#### Por qué
No conviene llenar Redis con cada hit, cada tick o cada cambio menor si el producto actual no necesita replay engine completa.

---

## Contrato final aprobado — MySQL

### Identidad y settings
- `practice_players`
- `practice_player_settings`

### Configuración de contenido/base del sistema
- `practice_rank_tiers`
- `practice_kits`
- `practice_modes`
- `practice_rules`
- `practice_mode_rules`
- `practice_queues`
- `practice_arenas`
- `practice_event_templates`

### Competitivo y stats
- `practice_player_mode_ratings`
- `practice_player_global_rating`
- `practice_player_mode_stats`
- `practice_player_core_stats`
- `practice_player_kit_layouts`
- `practice_seasons`

### Historial y auditoría
- `practice_matches`
- `practice_match_players`
- `practice_match_events`
- `practice_hosted_event_history` (si se persiste desde esta fase)

---

## Contrato final aprobado — Redis

### Presence / Player State
- `practice:player:{playerId}:presence`
- `practice:player:{playerId}:state`

### Queueing / Matchmaking
- `practice:queue:{queueId}:entries`
- `practice:player:{playerId}:queue`
- `practice:queue:{queueId}:search:{playerId}`

### Active Match
- `practice:match:{matchId}`
- `practice:player:{playerId}:active-match`

### Arena State
- `practice:arena:{arenaId}:state`
- `practice:arena:{arenaId}:lock`

### Party State
- `practice:party:{partyId}`
- `practice:player:{playerId}:party`

### Social efímero
- `practice:social:duel-request:{senderId}:{targetId}`
- `practice:social:party-invite:{partyId}:{targetId}`

### Locks / cooldowns
- `practice:cooldown:{type}:{actorId}:{targetId}`
- `practice:lock:transition:{playerId}`
- `practice:lock:matchmaking:{queueId}:{playerId}`

### Hosted events activos
- `practice:event:{eventId}`
- `practice:event:{eventId}:participants`

### Counters / projections
- `practice:counter:hub`
- `practice:counter:queue`
- `practice:counter:match`
- `practice:counter:ffa`
- `practice:counter:event`
- `practice:leaderboard:global`
- `practice:leaderboard:season:{seasonId}:global`
- `practice:leaderboard:mode:{modeId}`
- `practice:leaderboard:season:{seasonId}:mode:{modeId}`

---

## Índices y restricciones mínimas obligatorias

### MySQL

#### `practice_players`
- índice/unique útil sobre `normalized_name`

#### `practice_mode_rules`
- unicidad por (`mode_id`, `rule_id`)

#### `practice_queues`
- unicidad lógica por (`queue_type`, `mode_id`, `player_type`)

#### `practice_player_mode_ratings`
- unicidad por (`player_id`, `mode_id`, `season_id`)

#### `practice_player_global_rating`
- unicidad por (`player_id`, `season_id`)

#### `practice_player_kit_layouts`
- unicidad por (`player_id`, `mode_id`, `layout_key`)

#### `practice_matches`
- índices útiles por `mode_id`, `queue_type`, `started_at`, `arena_id`

#### `practice_match_players`
- índice por `player_id` para historial rápido del jugador

### Redis

#### Queues
- `Sorted Set` por cola usando `joinedAt` como score

#### Leaderboards
- `Sorted Set` usando SR o Global Rating como score

#### Social requests
- TTL obligatorio según tipo

#### Locks
- TTL obligatorio, siempre

---

## Qué queda explícitamente rechazado

### Rechazado 1 — Mirroring completo MySQL -> Redis
No.

### Rechazado 2 — MySQL en hot path de cola o combate
No.

### Rechazado 3 — Requests sociales persistidas como entidad pesada en MySQL
No.

### Rechazado 4 — Telemetría hipergranular de combate metida de entrada en Redis
No.

### Rechazado 5 — Hiper-normalizar desde el día 1 todos los catálogos configurables
No.

---

## Veredicto final

### El storage queda aprobado con estas condiciones

1. agregar `practice_kits`
2. agregar `practice_rank_tiers`
3. mantener Redis solo para hot-state/proyección/locks
4. no duplicar ownership
5. mantener tipado fuerte en competitivo e histórico
6. mantener JSON solo donde aporta flexibilidad real

Con esas correcciones, el contrato de storage queda lo suficientemente sólido para pasar a implementación sin romper los principios del proyecto.
