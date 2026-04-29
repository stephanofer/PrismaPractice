# Practice Hera — Fases de Desarrollo

## Propósito

Su objetivo es darnos un solo lugar para:

1. entender el **orden lógico de construcción** del producto
2. recordar los **principios de desarrollo**
3. ver el **estado real** de cada fase
4. saber **qué debe salir** de cada fase
5. detectar **pendientes y riesgos** antes de implementar

No reemplaza los documentos temáticos de `docs/producto/`.

La relación correcta queda así:

- `docs/producto/*.md` temáticos → definen el producto y la arquitectura en detalle
- `docs/producto/fases-de-desarrollo.md` → ordena el desarrollo, el estado de avance y los criterios para pasar de fase

---

## Principios rectores

### 1. Base antes que features
- No arrancamos por lo visible.
- Primero definimos contratos, modelo de estado, ownership de datos y servicios.
- Si la base está floja, todo lo demás queda acoplado y frágil.

### 2. Las reglas de negocio viven en módulos compartidos
- `practice-api` = contratos y tipos compartidos.
- `practice-core` = reglas de negocio y casos de uso.
- `practice-data` = MySQL/Redis e implementación de acceso a datos.
- Los plugins runtime adaptan Paper/Velocity, UX, comandos, eventos e integración.

### 3. Cada fase debe destrabar la siguiente
- Una fase no existe para “avanzar visualmente”, existe para reducir incertidumbre real.
- Si algo va a ser reutilizado por varios runtimes, se resuelve antes que una feature localizada.

### 4. No se empieza una feature sin prerequisitos
Antes de implementar algo hay que tener claro:
- prerequisitos de dominio
- prerequisitos de persistencia
- prerequisitos de hot-state
- prerequisitos de UX/runtime

Si falta una base, se pausa la feature y se construye primero esa pieza.

### 5. Negocio primero, UX después
Si un menú, botón o comando necesita lógica que todavía no existe en core, se frena y se define primero en core.

### 6. La dependencia compartida gana prioridad
Si una feature depende de otra y esa otra además la usan varios módulos, esa dependencia va primero.

### 7. Vertical slices sí, pero sobre base real
Podemos construir slices visibles, pero solo sobre dominio, storage y servicios ya resueltos.

### 8. Si aparece una dependencia escondida, se pausa
No se parchea por apuro. Se crea la pieza base faltante y después se retoma el flujo.

### 9. Menos sobreingeniería, más claridad
- servicios pequeños
- ownership claro
- runtime fino
- nada de managers gigantes
- nada de configs mágicas que escondan el dominio

---

## Estado actual global

### Fase actual
- **Fase 0 — Lenguaje del negocio y definición del producto**

### Estado actual
- **Muy avanzada**

### Ya está fuertemente definido
- visión del producto y flujo principal del jugador
- núcleo competitivo ranked/unranked
- SR por modo, Global Rating y rangos
- seasons simples con corte manual e histórico
- historial de match y snapshot final de inventario
- settings por usuario y defaults iniciales
- bootstrap del jugador en primer login
- política de matchmaking
- friends, requests sociales, parties y eventos hosteados
- `Mode Definition`
- `Rule Definition`
- `Queue Definition`
- `Match Session`
- `Arena Definition`
- `Player State System`
- `Profile / Stats System`
- ownership MySQL vs Redis
- matriz de ownership por entidad
- `Application Services`
- persistencia inicial concreta con tablas MySQL y claves Redis recomendadas
- Friends externalizado como sistema social de red fuera del ownership de Practice

### Pendientes para cerrar la fase con prolijidad

### Ya cerrado dentro de esta fase
- corrección conceptual: el sistema debe soportar creación/expansión in-game de modos, kits, queues y arenas; el documento correcto pasa a ser `docs/producto/10-sistema-de-creacion-y-contenido-semilla.md`
- contenido semilla inicial recomendado documentado sin tratarlo como límite del sistema
- queues semilla activas iniciales documentadas en `docs/producto/11-queues-semilla-activas-iniciales.md`
- escala inicial simple de rangos documentada en `docs/producto/12-rangos-y-rules-por-defecto.md`
- rules por defecto recomendadas por modo semilla documentadas en `docs/producto/12-rangos-y-rules-por-defecto.md`
- templates iniciales exactos de eventos documentados en `docs/producto/13-templates-iniciales-de-eventos.md`
- frontera entre modos base y modos especiales documentada en `docs/producto/14-modos-base-vs-modos-especiales.md`
- validación final de storage documentada en `docs/producto/15-validacion-final-storage.md`

### Riesgos actuales
- querer pasar a código sin cerrar el recorte inicial de modos/queues reales
- empezar runtimes antes de cerrar contratos compartidos
- duplicar lógica entre hub, match y FFA

---

## Orden global de construcción

Vamos a desarrollar en este orden:

1. **Lenguaje del negocio y definición del producto**
2. **Contratos compartidos y modelo de dominio**
3. **Persistencia y hot-state**
4. **Servicios de aplicación compartidos**
5. **Hub como puerta principal del sistema**
6. **Match como corazón competitivo**
7. **FFA como loop complementario y reutilizable**
8. **Proxy como capa fina de routing y coordinación**
9. **Hardening, observabilidad y estabilización**

Esto es INTENCIONAL.
El proyecto no son “4 plugins”. Es un dominio compartido con varios runtimes.

---

## Fase 0 — Lenguaje del negocio y definición del producto

### Objetivo
Definir con precisión qué producto estamos construyendo, cómo se usa, qué conceptos centrales existen y qué límites de negocio gobernarán la arquitectura.

### Por qué esta fase existe
Porque arrancar escribiendo clases antes de entender negocio es EXACTAMENTE como se arruina una arquitectura de Practice seria.

### Qué debe salir de esta fase
- glosario del negocio
- flujo principal del jugador
- entidades centrales del dominio
- relaciones entre competitivo, social, FFA y eventos
- invariantes base del sistema
- ownership de datos a nivel conceptual
- primera capa concreta de persistencia y services

### Ya definido en esta fase
- producto competitivo-social con ranked, unranked, FFA, social, parties, events y leaderboards
- Hub como puerta central
- flujo principal: Hub → Queue → Match → resumen → Hub
- ratings, ranks, seasons, historial, settings, social y data ownership
- entidades centrales y application services

### Pendientes de cierre de Fase 0
- catálogo inicial exacto de modos de salida
- catálogo inicial exacto de queues de salida
- set inicial exacto de templates/eventos realmente activos
- nombres finales e intervalos de rangos
- validación final del contrato de storage

### Entregables de cierre de Fase 0
- documento de producto fragmentado y navegable
- entidades principales del dominio claras
- ownership de datos definido
- lista de pendientes antes de modelado técnico final

### Criterio de salida
La fase cierra cuando ya no haya ambigüedad grave sobre:
- qué producto estamos construyendo
- qué entidades existen
- qué ownership tiene cada dato
- qué piezas deben implementarse primero

---

## Fase 1 — Contratos compartidos y modelo de dominio

### Objetivo
Traducir la definición del producto a contratos y tipos compartidos sólidos en `practice-api` y `practice-core`.

### Se construye acá
- identifiers y value objects
- enums de estados, tipos y categorías
- contratos de repositorios/gateways
- contratos de servicios de dominio/aplicación
- reglas de validación y elegibilidad
- eventos de dominio útiles

### Entidades que deberían empezar a modelarse acá
- Mode
- Rule
- Queue
- Match Session
- Arena
- Player State
- Profile/Stats aggregates
- relaciones sociales base

### Lo que NO debe pasar acá
- nada de lógica Paper/Velocity
- nada de listeners como dueños del dominio
- nada de acceso real a MySQL/Redis todavía si el contrato no está cerrado

### Criterio de salida
- los conceptos importantes ya tienen contrato/tipo estable
- las reglas principales se pueden testear sin runtime
- hub/match/ffa/proxy dependen de contratos, no de supuestos

### Dependencias previas
- Fase 0 suficientemente cerrada

---

## Fase 2 — Persistencia y hot-state

### Objetivo
Implementar el ownership real entre MySQL y Redis sobre los contratos ya definidos.

### Se construye acá
- repositorios MySQL
- stores/projections Redis
- mappings de tablas y claves
- estrategia de locks/cooldowns
- serialización de state y snapshots
- bootstrap/config de infraestructura

### Se baja a código acá
- tablas MySQL definitivas o primera migración seria
- claves Redis y estructuras reales
- lectura/escritura de profile, settings, ratings, friendships, layouts
- queue state, match active state, arena reservations, requests y counters

### Criterio de salida
- cada entidad importante tiene storage real implementado
- no hace falta “inventar storage” dentro del runtime
- MySQL y Redis ya tienen ownership claro y consistente

### Dependencias previas
- contratos/gateways definidos en Fase 1

---

## Fase 3 — Servicios de aplicación compartidos

### Objetivo
Construir la capa de orquestación que conecta dominio + storage + hot-state.

### Se construye acá
- `ProfileService`
- `PlayerStateService`
- `QueueService`
- `MatchmakingService`
- `ArenaAllocationService`
- `MatchService`
- `RatingService`
- `HistoryService`
- `FriendsService`
- `PartyService`
- `DuelRequestService`
- `FfaService`
- `EventService`
- `LeaderboardProjectionService`
- `StatsProjectionService`

### Regla
Si varios runtimes necesitan la misma decisión, esa lógica vive acá y NO dentro de cada plugin.

### Criterio de salida
- los workflows principales ya pueden describirse como llamadas a services
- los runtimes empiezan a quedar finos
- ya no hay necesidad de managers monstruosos en plugins

### Dependencias previas
- storage y ownership implementados

---

## Fase 4 — Hub como puerta principal del sistema

### Objetivo
Implementar el primer vertical slice visible del producto usando la base compartida.

### Se construye acá
- bootstrap al entrar
- carga de perfil / settings / presencia
- hotbar inicial y menús principales
- entrada/salida de ranked/unranked
- acceso a FFA
- acceso a party/social
- feedback claro al jugador
- scoreboards/tab/chat básicos si corresponde

### Lo que NO se debe hacer todavía si no está listo
- cosméticos complejos
- UX secundaria no crítica
- polish visual que tape falta de base

### Criterio de salida
- el jugador entra, existe en Practice y puede navegar las experiencias base sin caos

### Dependencias previas
- services compartidos operativos

---

## Fase 5 — Match como corazón competitivo

### Objetivo
Construir el lifecycle real de match competitivo sobre queue, matchmaking y arena allocation ya estables.

### Se construye acá
- creación de Match Session
- transferencia al runtime de match
- espera de jugadores y pre-fight
- rounds/series
- aplicación de kits y rules efectivas
- cierre del match
- persistencia de resultado
- actualización de rating, stats e historial
- retorno consistente al hub

### Criterio de salida
- ranked/unranked pueden jugarse de punta a punta
- bo3/series funciona sin parches
- historial y rating quedan consistentes

### Dependencias previas
- hub funcional
- queue/matchmaking/arena allocation reales

---

## Fase 6 — FFA como loop complementario reutilizable

### Objetivo
Implementar FFA público reutilizando modos, kits, layouts y rules, pero con loop propio.

### Se construye acá
- entrada/salida de FFA
- respawn/spawns
- kills/deaths/streaks
- state del jugador en FFA
- scoreboard/contadores de FFA
- stats de FFA

### Criterio de salida
- FFA ya no es una idea lateral, sino una experiencia funcional paralela al loop competitivo principal

### Dependencias previas
- player state fuerte
- modes/rules reutilizables
- profiles/stats funcionando

---

## Fase 7 — Proxy como capa fina de routing y coordinación

### Objetivo
Mantener el proxy fino, reusable y enfocado en lo que sí le corresponde.

### Se construye acá
- routing/fallback
- coordinación de transferencias
- integración de presencia cross-server
- delivery social cross-server donde aplique
- recovery de fallos de transferencia

### No debe tener
- elo
- reglas de match
- persistencia de negocio como owner

### Criterio de salida
- la red se mueve bien sin meter negocio competitivo dentro del proxy

### Dependencias previas
- matches, hub, FFA y social ya definidos operativamente

---

## Fase 8 — Hardening, observabilidad y estabilización

### Objetivo
Volver el sistema operable, auditable y robusto antes de crecerle más features.

### Se construye acá
- logs claros
- admin/debug commands
- recovery flows
- timeouts y paths de falla
- validación de config
- limpieza de edge cases
- expansión de tests
- revisión de performance real

### Criterio de salida
- el sistema ya no solo funciona: también se puede mantener, diagnosticar y escalar sin dolor

---

## Reglas operativas para pasar de fase

Cada fase debe cerrar dejando explícito:

1. **Objetivo claro**
2. **Alcance exacto**
3. **Qué entra / qué no entra**
4. **Dependencias**
5. **Entregables**
6. **Riesgos**
7. **Decisiones tomadas**
8. **Preguntas abiertas**

Si una fase no puede dejar eso claro, todavía no está bien cerrada.

---

## Riesgos que vamos a vigilar siempre

- empezar por UX antes de base compartida
- duplicar lógica entre runtimes
- mezclar social con competitivo sin restricciones claras
- usar MySQL en hot path donde debería ir Redis
- dejar ownership ambiguo
- meter sobreingeniería prematura
- creer que “ya pensamos suficiente” cuando todavía faltan catálogos concretos de salida

---

## Pendientes globales antes de empezar el desarrollo fuerte

Estos pendientes NO invalidan todo lo definido, pero conviene cerrarlos antes de arrancar a meter código serio en cascada:

- primer bloque real de implementación por módulo

---

## Siguiente paso recomendado después de este documento

Con este archivo ya deberíamos poder pasar a:

1. cerrar los **pendientes concretos de salida**
2. convertir esto en **bloques de implementación reales**
3. arrancar la primera tanda de desarrollo en `practice-api`, `practice-core` y `practice-data`

La prioridad correcta sigue siendo:

- primero contratos/modelo
- después storage
- después services
- después runtimes
