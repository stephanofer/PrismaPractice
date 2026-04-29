# Practice Hera — Application Services

## Application Services — capa de orquestación del sistema

### Decisión conceptual

Se define que los **Application Services** NO deben convertirse en:

- managers gigantes
- contenedores caóticos de lógica mezclada
- wrappers vacíos sin criterio

Se define que deben ser:

- **servicios pequeños, explícitos y orientados a casos de uso**

### Por qué esta decisión es correcta

Porque ya definimos bien el dominio. Ahora necesitamos la capa que conecte:

- entidades del dominio
- persistencia
- hot-state
- runtimes
- flujos reales del jugador

Sin convertir todo en plugins obesos ni en clases “God Object”.

---

## Regla madre de Application Services

### El dominio decide

- reglas
- compatibilidades
- estados válidos
- identidad de las entidades

### Los Application Services orquestan

- lecturas/escrituras
- transiciones
- validaciones de flujo
- coordinación entre módulos
- side effects controlados

### Los runtimes adaptan

- eventos de plataforma
- comandos
- menús
- feedback visual

#### Traducción simple

- `practice-core` piensa
- `practice-data` persiste
- plugin/runtime conecta con Paper/Velocity

---

## Lista recomendada de Application Services

La recomendación final es arrancar con estos servicios y NO inventar más hasta necesitarlos.

### 1. `ProfileService`

#### Responsabilidad

- bootstrap del jugador al entrar por primera vez
- carga del perfil actual
- acceso al perfil público
- actualización de metadata básica

#### Debe orquestar

- profile persistente
- settings persistentes
- season context actual

#### No debe hacer

- matchmaking
- lógica de match
- social completa

---

### 2. `PlayerStateService`

#### Responsabilidad

- leer y actualizar el estado global actual del jugador
- validar transiciones de estado
- resolver acciones permitidas según estado + settings + contexto

#### Debe orquestar

- Redis player state
- policy de transición
- locks si hacen falta

#### Es crítico porque

sin este servicio cada runtime empezaría a inventar su propia lógica de estado.

---

### 3. `QueueService`

#### Responsabilidad

- entrar a cola
- salir de cola
- validar elegibilidad para una cola
- materializar `QueueEntry`

#### Debe orquestar

- `Queue Definition`
- player state
- restricciones sociales/party
- Redis queue state

#### No debe hacer

- elegir rival final por sí solo
- crear el match completo por sí solo

Eso pertenece al matchmaking + match service.

---

### 4. `MatchmakingService`

#### Responsabilidad

- evaluar candidatos dentro de una cola
- aplicar política quality-first / speed-first
- respetar ping range, región y expansión temporal
- decidir pairing válido

#### Debe orquestar

- queue state activo
- reglas de matching
- región óptima
- score de calidad del pairing

#### No debe hacer

- teletransportar jugadores
- administrar runtime Paper directamente

Su trabajo es decidir el emparejamiento correcto.

---

### 5. `ArenaAllocationService`

#### Responsabilidad

- encontrar arena compatible
- reservarla
- marcar estados operativos
- liberarla al terminar o fallar

#### Debe orquestar

- `Arena Definition`
- `Arena Reservation`
- compatibilidad con modo/player type/región

#### No debe hacer

- lógica de score/rank
- lógica social

---

### 6. `MatchService`

#### Responsabilidad

- crear `Match Session`
- congelar configuración efectiva
- coordinar creación, inicio, progreso y cierre del match
- persistir resultado e historial derivado

#### Debe orquestar

- `Mode Definition`
- `Queue Definition`
- `Match Session`
- arena asignada
- runtime target
- SR/result impact

#### Decisión fuerte

Este es uno de los servicios más importantes del sistema. Pero incluso así, NO debe tragarse matchmaking, social ni queueing completo.

---

### 7. `RatingService`

#### Responsabilidad

- calcular cambios de SR
- recalcular rangos por modo
- recalcular Global Rating
- aplicar impacto competitivo post-match

#### Debe orquestar

- reglas de rating
- datos competitivos persistentes
- peaks y season impact

#### No debe hacer

- decidir ganador del match
- manejar runtime

Trabaja sobre resultados ya resueltos.

---

### 8. `HistoryService`

#### Responsabilidad

- construir y persistir el resumen/historial del match
- exponer consultas del historial reciente
- preparar datos consumibles por menús y futura web

#### Debe orquestar

- metadata
- player snapshots
- stats agregadas
- eventos mínimos
- inventory final snapshot

#### Nota

Puede recibir como fuente principal un `Match Session` ya terminado.

---

### 9. `PartyService`

#### Responsabilidad

- crear/disolver party
- invitar/expulsar miembros
- transferir liderazgo
- resolver restricciones grupales

#### Debe orquestar

- party state activo
- party invites pendientes
- presencia de miembros
- restricciones por estado

---

### 10. `DuelRequestService`

#### Responsabilidad

- enviar/aceptar/cancelar duel requests
- validar settings/estado
- convertir aceptación válida en flujo hacia party duel o match challenge según contexto

#### Nota

Si el sistema necesita verificar amistad para `only friends duels`, eso debe llegar desde un gateway externo al sistema de Friends de red.

---

### 11. `FfaService`

#### Responsabilidad

- entrada/salida de FFA público
- lifecycle operativo del jugador en FFA
- stats activas de FFA
- integración con modos/rules/layouts reutilizados

#### Decisión fuerte

FFA público tiene loop propio. Merece servicio propio.

---

### 12. `EventService`

#### Responsabilidad

- crear evento hosteado desde template
- abrir/cerrar inscripciones
- iniciar/cancelar evento
- coordinar lifecycle del evento activo

#### Debe orquestar

- event templates
- hosted event active state
- presencia de jugadores
- restricciones del template

---

### 13. `LeaderboardProjectionService`

#### Responsabilidad

- construir/actualizar proyecciones de leaderboard
- exponer top global y top por modo/season
- mantener lecturas rápidas

#### Decisión fuerte

Los leaderboards NO deben salir de consultas improvisadas sobre perfiles completos.

---

### 14. `StatsProjectionService`

#### Responsabilidad

- actualizar stats agregadas a partir de resultados/historial
- mantener separación entre all-time, season, per-mode y social/activity

#### Nota

Esto evita mezclar cálculo de stats con el flujo del match en tiempo real.

---

## Servicios que NO recomiendo crear todavía

Para respetar el principio de evitar sobreingeniería, NO recomiendo crear desde ya:

- `CosmeticsService` complejo
- `NotificationService` genérico monstruoso
- `AdminEverythingManager`
- `NetworkService` todoterreno

Si más adelante aparecen casos reales, se modelan. No antes.

---

## Orquestación recomendada del flujo principal

### Ranked / Unranked principal

1. `ProfileService` asegura contexto del jugador
2. `PlayerStateService` valida estado actual
3. `QueueService` intenta entrada a cola
4. `MatchmakingService` encuentra pairing válido
5. `ArenaAllocationService` reserva arena compatible
6. `MatchService` crea sesión efectiva
7. runtime ejecuta la partida
8. `MatchService` cierra resultado
9. `RatingService` aplica impacto competitivo si corresponde
10. `HistoryService` persiste historial
11. `StatsProjectionService` actualiza agregados
12. `LeaderboardProjectionService` refresca proyecciones relevantes
13. `PlayerStateService` devuelve al jugador a estado consistente

### Social / Party

1. `PartyService` resuelve grupo e invitaciones de party
2. `DuelRequestService` resuelve desafíos directos
3. un gateway externo de Friends resuelve chequeos de amistad cuando haga falta
4. `PlayerStateService` gobierna compatibilidad de acciones

### Eventos

1. `EventService` crea desde template
2. `PlayerStateService` valida entradas
3. `ArenaAllocationService` / runtime resuelven espacio según tipo
4. `StatsProjectionService` y `HistoryService` persisten lo que corresponda

---

## Decisiones concretas que tomo

### 1. Sí a servicios pequeños y explícitos

Sí.

Prefiero 12-15 servicios con responsabilidad clara antes que 3 managers gigantes imposibles de mantener.

### 2. `MatchService`, `QueueService`, `MatchmakingService` y `ArenaAllocationService` deben existir separados

Sí.

Separar esos cuatro es CLAVE para que el corazón competitivo no se vuelva una masa acoplada.

### 3. Social interno de Practice debe separarse al menos en Party y Duel Request

Sí.

Parecen cercanos, pero no son lo mismo a nivel de flujo ni de restricciones; Friends queda externalizado.

### 4. Leaderboards y stats deben salir de proyecciones, no de lógica embebida en match runtime

Sí.

Eso protege rendimiento y claridad del sistema.

### 5. FFA y Events merecen servicios propios

Sí.

Porque tienen loop y lifecycle propios.

---

## Conclusión

La capa de **Application Services** es la que va a convertir todo este dominio bien definido en un sistema implementable, sin romper principios ni caer en sobreingeniería.

Dominio sólido arriba.
Persistencia/hot-state abajo.
Servicios explícitos en el medio.
Runtimes finos en la superficie.
