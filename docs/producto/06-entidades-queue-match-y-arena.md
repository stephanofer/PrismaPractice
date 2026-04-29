# Practice Hera — Entidades Queue, Match y Arena

## Definición central de “Queue” en Practice

### Decisión conceptual

Se define que una **Queue** NO debe modelarse como:

- un modo
- un match
- una party
- un botón del menú

Se define que una **Queue** debe ser un:

- **contexto de búsqueda y emparejamiento**

En otras palabras:

- el `Mode Definition` dice **qué gameplay se juega**
- la `Queue` dice **cómo se busca rival para jugarlo**

### Por qué esta decisión es correcta

Porque permite separar correctamente:

- identidad del gameplay
- política competitiva
- player type
- restricciones de matchmaking
- tipo de acceso del jugador

Y evita errores clásicos como acoplar Sword directamente a una única cola rígida.

---

## Modelo recomendado de `Queue Definition`

La recomendación final es que una cola tenga estos bloques conceptuales.

### 1. Identidad

- `identifier`
- `displayName`
- `description`
- `menuItem`

Esto permite representar la cola en hub, menús y logs de forma limpia.

### 2. Clasificación de la cola

La cola debe declarar qué tipo de experiencia representa.

Campos recomendados:

- `queueType`
  - `RANKED`
  - `UNRANKED`
  - más adelante podrían existir otros tipos si realmente hacen falta

- `competitive`
- `rated`

#### Decisión fuerte

- **Ranked y Unranked deben ser colas distintas**
- no deben ser un simple flag suelto pegado al match

### 3. Relación con el modo

La cola debe referenciar explícitamente:

- `modeId`

Y opcionalmente, cuando haga falta en el futuro:

- `modeVariant` o contexto especializado

Ejemplo conceptual:

- `ranked-sword-1v1`
- `unranked-sword-1v1`

Ambas usan el mismo `Mode Definition` base, pero no son la misma cola.

### 4. Player type / team format

La cola debe fijar con claridad el formato de enfrentamiento.

Campos recomendados:

- `playerType`
  - 1v1
  - 2v2
  - 3v3
  - 4v4

#### Decisión actual

- Ranked inicial será **1v1 solamente**
- Unranked podrá crecer luego con otros formatos

### 5. Política de matchmaking

La cola debe declarar qué política usa el sistema de matchmaking.

Campos recomendados:

- `matchmakingProfile`
  - `QUALITY_FIRST`
  - `SPEED_FIRST`

- `usesSr`
- `usesPingRange`
- `usesRegionSelection`
- `searchExpansionProfile`

#### Decisión fuerte

- Ranked usa perfil **quality-first**
- Unranked usa perfil **speed-first**

### 6. Restricciones de acceso

La cola debe poder declarar quién puede entrar y bajo qué condiciones.

Campos recomendados:

- `requiresSoloState`
- `requiresPartyState`
- `allowedPlayerStates`
- `blockedPlayerStates`
- `permissionRequirements`

Esto es importante porque ya decidimos que estar en party puede bloquear ciertas acciones, y eso debe poder modelarse por cola.

### 7. Restricciones sociales / de grupo

La cola debe declarar cómo interactúa con el sistema social.

Campos recomendados:

- `blockedIfInParty`
- `leavePartyRequired`
- `partyJoinAllowed`
- `spectatorAllowed`

#### Decisión actual

- Ranked 1v1 debe bloquear entrada mientras el jugador mantenga estados grupales incompatibles

### 8. Política de arena / region / runtime

La cola debe poder expresar lo necesario para materializar el match.

Campos recomendados:

- `arenaSelectionPolicy`
- `regionPolicy`
- `targetRuntime`
  - por ejemplo Match runtime

Esto no significa que la cola asigne ella misma la arena, sino que declara qué necesita el flujo para resolverla.

### 9. Política competitiva visible

La cola debe decir qué impacto tiene sobre el perfil competitivo del jugador.

Campos recomendados:

- `affectsSr`
- `affectsVisibleRank`
- `affectsGlobalRating`
- `affectsSeasonStats`
- `affectsLeaderboards`

Esto mantiene claro por qué Ranked y Unranked no pueden confundirse.

### 10. Visibilidad y UX

La cola debe declarar cómo se presenta al usuario.

Campos recomendados:

- `visibleInMenu`
- `menuCategory`
- `sortOrder`
- `statusMessagingProfile`

Porque una cola también es una pieza de UX, no solo backend.

---

## Recomendación estructural fuerte

La mejor forma de pensarlo es así:

- **`Mode Definition`** = experiencia jugable reusable
- **`Queue Definition`** = contexto de acceso y matchmaking para esa experiencia
- **`Queue Entry`** = jugador o grupo actualmente buscando partida en esa cola
- **`Matchmaking Policy`** = lógica que evalúa candidatos dentro de la cola

Y NO así:

- el modo hace de cola
- la cola hace de match
- el menú define la lógica competitiva

---

## Ejemplos conceptuales

### Ejemplo 1 — Ranked Sword 1v1

- usa `modeId = sword`
- `queueType = RANKED`
- `playerType = 1v1`
- `matchmakingProfile = QUALITY_FIRST`
- afecta SR, rank, global rating, season y leaderboard
- bloquea estados incompatibles como party activa

### Ejemplo 2 — Unranked Sword 1v1

- usa `modeId = sword`
- `queueType = UNRANKED`
- `playerType = 1v1`
- `matchmakingProfile = SPEED_FIRST`
- no afecta SR/rank principal
- sigue usando región y ping range, pero con tolerancia más laxa

---

## Decisiones concretas que tomo

### 1. Queue y Mode deben ser entidades separadas

Sí.

Esto es obligatorio si querés escalar bien ranked, unranked y futuras variantes.

### 2. Ranked y Unranked deben ser colas distintas

Sí.

Comparten modo, pero no comparten identidad competitiva.

### 3. Queue debe poder bloquear por estado social

Sí.

Ya definimos parties, requests y estados incompatibles; la cola debe integrarse con eso declarativamente.

### 4. Queue debe declarar su política de matchmaking

Sí.

No quiero esa lógica escondida en condicionales externos. Tiene que estar modelada.

### 5. Queue debe influir explícitamente en perfil competitivo

Sí.

No podemos dejar implícito qué colas afectan SR, ranks, season o leaderboard.

---

## Conclusión

La entidad **Queue** pasa a ser la pieza que conecta:

- el `Mode Definition`
- la política de matchmaking
- las restricciones del jugador
- la materialización del match

`Mode` define qué se juega.
`Queue` define cómo se entra a buscarlo.

---

## Definición central de “Match” en Practice

### Decisión conceptual

Se define que un **Match** NO debe modelarse como:

- solo una teletransportación a una arena
- solo una sesión Paper runtime sin identidad de dominio
- solo un resultado final persistido

Se define que un **Match** debe ser una:

- **sesión competitiva/jugable materializada a partir de una Queue y un Mode**

En otras palabras:

- `Mode` define qué gameplay base se juega
- `Queue` define cómo se encontró ese enfrentamiento
- `Match` representa la ejecución real de esa experiencia

### Por qué esta decisión es correcta

Porque el Match es el punto donde confluyen:

- matchmaking
- arena
- región
- runtime target
- jugadores/equipos
- rules activas
- rounds
- resultado
- historial y telemetría

Y si no se modela como entidad real, todo termina desparramado entre plugins, caches y callbacks.

---

## Modelo recomendado de `Match Session`

La recomendación final es que un Match tenga estos bloques conceptuales.

### 1. Identidad

- `matchId`
- `createdAt`
- `startedAt`
- `endedAt`
- `status`

El Match debe tener identidad propia desde antes de iniciar realmente, no recién al persistir resultado.

### 2. Origen del match

- `queueId`
- `modeId`
- `playerType`
- `queueType`
- `matchmakingProfileUsed`

Esto deja trazabilidad clara de por qué ese match existe.

### 3. Participantes

- `participants`
- `teamsOrSides`
- `partyReferences` si aplica en contextos grupales futuros
- `playerStateSnapshotAtCreation` mínimo si hace falta validar integridad

#### Decisión fuerte

Aunque Ranked inicial sea 1v1, el modelo de Match debe nacer preparado para lados/equipos, porque Unranked y otros contextos van a crecer.

### 4. Ubicación operativa

- `region`
- `arenaId`
- `arenaType`
- `runtimeServerId`
- `runtimeType`

Esto es obligatorio para debug, recovery y auditoría.

### 5. Configuración efectiva del match

El Match NO debe depender solo del `Mode Definition` en vivo, sino de una configuración efectiva resuelta al momento de creación.

Campos recomendados:

- `effectiveRules`
- `effectiveMatchSettings`
- `effectiveKitReference`
- `effectiveRoundsToWin`
- `effectivePreFightDelay`
- `effectiveMaxDuration`

#### Por qué

Porque si mañana cambia un modo o una rule, el Match histórico no puede reinterpretarse con datos distintos a los que realmente usó.

### 6. Ciclo de vida / estados

El Match debe tener estados explícitos.

Estados recomendados:

- `CREATED`
- `ALLOCATING`
- `TRANSFERRING`
- `WAITING_PLAYERS`
- `PRE_FIGHT`
- `IN_PROGRESS`
- `ROUND_ENDING`
- `COMPLETED`
- `CANCELLED`
- `FAILED`

#### Por qué

Sin estados claros, recovery, debugging y UX se vuelven caóticos.

### 7. Modelo de rounds/series

Como ya definimos lógica best-of-X, el Match debe contemplar explícitamente:

- `seriesFormat`
- `roundsToWin`
- `currentRound`
- `roundResults`
- `scoreboardBySide`

#### Decisión fuerte

El modelo de Match debe soportar series desde el principio. No hay que meter bo3 como parche encima de bo1.

### 8. Política de spectators

El Match debe saber:

- si permite spectators
- bajo qué condiciones
- si fue restringido por settings de los jugadores

Esto no tiene que quedar perdido en listeners del runtime.

### 9. Resultado competitivo

El Match debe declarar explícitamente:

- `winner`
- `loser` o `winningSide`
- `resultType`
  - normal win
  - disconnect
  - timeout
  - special win condition
- `srChanges`
- `rankImpact`
- `seasonImpact`

Esto es crítico para trazabilidad competitiva.

### 10. Historial y telemetría

El Match debe ser el dueño lógico de:

- metadata del match
- snapshot final por jugador
- stats agregadas
- eventos mínimos importantes
- snapshot final de inventario por slots

O sea: el historial no es un agregado externo raro; nace de la sesión Match.

### 11. Cancelación / falla / recovery

El Match debe contemplar caminos no felices.

Campos recomendados:

- `cancelReason`
- `failureReason`
- `recoverable`
- `cleanupStatus`

#### Por qué

Porque en una network real va a haber:

- disconnects
- transfer failures
- arena issues
- timeouts
- players que no cargan

Si no modelás eso desde el dominio, terminás parchando errores por todos lados.

---

## Recomendación estructural fuerte

La mejor forma de pensarlo es así:

- **`Mode Definition`** = experiencia base
- **`Queue Definition`** = contexto de acceso y matchmaking
- **`Match Session`** = ejecución real y auditable de esa experiencia
- **`Match History`** = proyección/persistencia de salida derivada del Match

Y NO así:

- el servidor Paper "es" el match
- la arena "es" el match
- el resultado persistido "es" el match

---

## Ejemplos conceptuales

### Ejemplo 1 — Ranked Sword 1v1 bo3

- nace desde `ranked-sword-1v1`
- usa `modeId = sword`
- región elegida por mejor calidad común
- arena asignada
- effective rules + settings congelados
- score por rounds
- resultado final con impacto de SR

### Ejemplo 2 — Unranked Sword 1v1

- misma base jugable
- distinta cola
- distinta política competitiva
- historial igual de consistente
- sin impacto en SR principal

---

## Decisiones concretas que tomo

### 1. Match debe tener configuración efectiva congelada

Sí.

Esto es obligatorio para histórico correcto y trazabilidad.

### 2. Match debe modelar estados explícitos

Sí.

Sin eso, el runtime se vuelve opaco y difícil de recuperar.

### 3. Match debe soportar series/rounds nativamente

Sí.

Ya definimos bo3 y variantes por modo; no se parchea después.

### 4. Match debe ser dueño lógico del historial

Sí.

El historial sale del Match, no de listeners desordenados sueltos.

### 5. Match debe modelar failure/cancelation paths

Sí.

Esto es arquitectura seria de network, no plugin local improvisado.

---

## Conclusión

La entidad **Match Session** pasa a ser la ejecución real del corazón competitivo de Practice.

`Mode` define la experiencia.
`Queue` define cómo se accede.
`Match` define cómo esa experiencia ocurrió realmente.

---

## Definición central de “Arena” en Practice

### Decisión conceptual

Se define que un **Arena** NO debe modelarse como:

- solo un nombre bonito en config
- solo una world location suelta
- solo un mapa visual

Se define que un **Arena** debe ser una:

- **unidad reusable, compatible y reservable de ejecución de match o actividad**

En otras palabras:

- `Mode` define qué se juega
- `Queue` define cómo se busca
- `Match` define la sesión real
- `Arena` define **dónde puede ejecutarse correctamente esa sesión**

### Por qué esta decisión es correcta

Porque el arena debe resolver cosas reales de dominio y operación:

- compatibilidad con modos
- compatibilidad con player types
- región
- lifecycle de reserva/uso/liberación
- reutilización segura
- observabilidad/debug

Si el arena no se modela bien, después tenés arenas “ocupadas” fantasma, incompatibilidades raras y lógica hardcodeada por plugin.

---

## Modelo recomendado de `Arena Definition`

La recomendación final es que un Arena tenga estos bloques conceptuales.

### 1. Identidad

- `arenaId`
- `displayName`
- `description`

Debe poder identificarse con claridad para logs, administración y selección.

### 2. Clasificación

El arena debe declarar qué tipo de experiencia soporta.

Campos recomendados:

- `arenaType`
  - `DUEL`
  - `TEAM`
  - `PUBLIC_FFA`
  - `PARTY_FFA`
  - `EVENT`

#### Decisión fuerte

No conviene usar una sola noción genérica de arena para todo sin clasificación. FFA público, duel y evento no tienen exactamente las mismas necesidades operativas.

### 3. Compatibilidad con modos

El arena debe declarar explícitamente qué puede soportar.

Campos recomendados:

- `allowedModes`
- `blockedModes`
- `allowedPlayerTypes`
- `supportedEventTemplates`

#### Por qué

Porque no todas las arenas sirven para todos los modos aunque técnicamente “entren” jugadores adentro.

### 4. Región y runtime

El arena debe vivir asociado a una región y a un contexto operativo.

Campos recomendados:

- `region`
- `runtimeType`
- `serverPool` o referencia equivalente

Esto es importante para matchmaking, asignación y debugging.

### 5. Layout espacial / puntos de uso

El arena debe declarar los puntos necesarios según el tipo.

Ejemplos:

- `spawnA`
- `spawnB`
- `teamSpawns`
- `spectatorSpawn`
- `lobbySpawn` interno si aplica
- `bounds`
- `specialZones`

#### Importante

No hace falta meter lógica visual rara acá; hace falta modelar los puntos funcionales que el runtime necesita.

### Definición operativa al crear arenas

Al momento de crear/configurar una arena, debe poder definirse explícitamente:

- `identifier`
- `displayName`
- `icon`
- posiciones/spawns relevantes según el tipo de arena

Y además dos regiones funcionales muy importantes:

#### 1. Universal region

La **universal region** define el área general del mapa que se usa para:

- protección global
- clonación
- manejo general del espacio de la arena

#### 2. Battle region

La **battle region** define el área real de combate, usada para:

- proteger correctamente el mapa durante la partida
- asegurar que los jugadores no salgan del área válida
- validar límites de combate/movimiento

### Posiciones configurables por arena

Según el tipo de arena, debe poder marcarse por GUI/herramienta de setup al menos:

- `position1`
- `position2`
- `spectatorPosition`
- spawns de duelo (A/B)
- spawns de team si aplica
- posiciones FFA / Party FFA si aplica

#### Decisión fuerte

Estas posiciones y regiones no deben quedar como datos secundarios improvisados. Deben formar parte explícita del contrato operativo de la arena.

### 6. Requerimientos ambientales

El arena debe declarar restricciones o capacidades del mundo.

Ejemplos:

- soporta block place/break
- soporta liquid interactions
- soporta void/lava/water kill conditions
- soporta pressure-plate win condition
- soporta timed blocks

### Decisión fuerte

No toda compatibilidad debe venir solo del `Mode`. Hay arenas que también imponen o limitan comportamiento por su propia naturaleza.

### 7. Política de reserva

Esto es CRÍTICO.

El arena debe poder pasar por estados operativos claros.

Estados recomendados:

- `AVAILABLE`
- `RESERVED`
- `IN_USE`
- `RESETTING`
- `DISABLED`
- `BROKEN`

#### Por qué

Porque una arena en una network seria no puede ser “true/false ocupada”. Necesitás trazabilidad real.

### 8. Política de reset / cleanup

El arena debe declarar cómo vuelve a estado limpio.

Campos recomendados:

- `resetStrategy`
- `requiresWorldRollback`
- `requiresBlockCleanup`
- `requiresEntityCleanup`
- `cleanupTimeout`

Esto importa muchísimo en modos con bloques, líquidos o efectos temporales.

### 9. Visibilidad / rotación / peso

El arena debe poder participar de selección inteligente.

Campos recomendados:

- `enabled`
- `selectable`
- `selectionWeight`
- `featured`

Esto permite:

- sacar arenas sin borrarlas
- controlar rotación
- priorizar algunas en cierto contexto

### 10. Telemetría y auditoría

El arena debe ser auditable.

Campos recomendados:

- `lastUsedAt`
- `timesUsed`
- `lastFailureReason`
- `healthStatus`

#### Por qué

Porque si una arena empieza a fallar o a dar problemas, tenés que poder detectarlo sin adivinar.

---

## Recomendación estructural fuerte

La mejor forma de pensarlo es así:

- **`Arena Definition`** = contrato estático/casi estático del espacio jugable
- **`Arena Reservation`** = estado operativo temporal de asignación
- **`Arena Allocation Policy`** = lógica que decide cuál arena compatible usar

Y NO así:

- un yaml gigante sin modelo
- una arena “ocupada o no” sin contexto
- el match decidiendo todo ad hoc

---

## Ejemplos conceptuales

### Ejemplo 1 — Arena de duel Sword

- `arenaType = DUEL`
- región `NA`
- soporta `1v1`
- spawns A/B
- spectator spawn
- weight normal
- reset simple

### Ejemplo 2 — Arena BuildUHC-like

- `arenaType = DUEL`
- soporta block placement/break
- requiere cleanup de bloques
- puede necesitar rollback o reset más fuerte

### Ejemplo 3 — Public FFA map

- `arenaType = PUBLIC_FFA`
- múltiples puntos de spawn
- runtime distinto
- stats y lifecycle propios
- no debe confundirse con una arena de duelo

---

## Decisiones concretas que tomo

### 1. Arena debe ser entidad separada de Match

Sí.

El Match la usa; no la define.

### 2. Arena debe tener compatibilidad declarativa

Sí.

No quiero inferencias mágicas ni hardcodeo escondido.

### 3. Arena debe tener estados operativos explícitos

Sí.

Necesitamos reservar, usar, resetear y deshabilitar con trazabilidad.

### 4. Arena debe contemplar cleanup/reset como parte del diseño

Sí.

Especialmente porque ya sabemos que habrá modos con reglas de bloques y comportamiento ambiental.

### 5. FFA público y eventos pueden reutilizar concepto de arena, pero no deben forzarse al mismo flujo operativo que un duel simple

Sí.

Comparten concepto base, pero no tienen exactamente el mismo lifecycle.

---

## Conclusión

La entidad **Arena** pasa a ser el contrato de compatibilidad y ejecución espacial del sistema.

`Mode` dice qué se juega.
`Queue` dice cómo se busca.
`Match` dice qué pasó.
`Arena` dice dónde puede ocurrir correctamente.
