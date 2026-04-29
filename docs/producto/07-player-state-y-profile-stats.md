# Practice Hera — Player State y Profile / Stats

## Definición central de “Player State” en Practice

### Decisión conceptual

Se define que el **estado del jugador** NO debe quedar implícito en:

- el servidor en el que está
- flags sueltas desperdigadas
- checks repetidos en menús, listeners y comandos

Se define que debe existir un **Player State System** explícito que modele en qué situación global está el jugador dentro de Practice.

### Por qué esta decisión es correcta

Porque el estado del jugador afecta directamente:

- si puede entrar a cola
- si puede recibir duel requests
- si puede usar party actions
- si puede spectear
- si puede entrar a FFA
- si puede participar en eventos
- si puede transferirse
- si puede ser matcheado

Sin este sistema, todo termina en ifs repetidos y contradicciones entre runtimes.

---

## Modelo recomendado de `Player State`

La recomendación final es separar:

- **estado global principal**
- **subestado contextual**
- **restricciones derivadas**

### 1. Estado global principal

Estados recomendados:

- `OFFLINE`
- `HUB`
- `IN_QUEUE`
- `TRANSFERRING`
- `IN_MATCH`
- `SPECTATING`
- `IN_PUBLIC_FFA`
- `IN_PARTY_FFA`
- `IN_EVENT`
- `EDITING_LAYOUT`

#### Decisión fuerte

Estos estados deben ser mutuamente excluyentes a nivel principal.

No quiero un jugador “medio en cola, medio en FFA, medio transfiriéndose”.

### 2. Subestado contextual

Además del estado principal, el jugador puede tener un subestado descriptivo.

Ejemplos:

- en `IN_QUEUE`
  - `WAITING_MATCH`

- en `IN_MATCH`
  - `WAITING_PLAYERS`
  - `PRE_FIGHT`
  - `IN_ROUND`
  - `ROUND_ENDING`

- en `IN_EVENT`
  - `REGISTERED`
  - `ACTIVE`
  - `ELIMINATED`
  - `SPECTATING_EVENT`

Esto mejora muchísimo la coherencia sin inflar el estado principal.

### 3. Restricciones derivadas

El Player State System debe permitir derivar si una acción está permitida o no.

Ejemplos:

- `canReceiveDuels`
- `canSendDuels`
- `canJoinQueue`
- `canJoinFfa`
- `canJoinEvent`
- `canEditLayout`
- `canReceivePartyInvite`
- `canSpectate`
- `canBeTransferred`

#### Decisión fuerte

Estas restricciones no deben vivir hardcodeadas por cada menú. Deben resolverse desde una política central basada en estado + settings + contexto.

---

## Reglas operativas recomendadas por estado

### `HUB`

- interacción total
- puede recibir requests sociales
- puede entrar a cola
- puede abrir menús
- puede editar layouts

### `IN_QUEUE`

- interacción parcial
- puede recibir social actions según configuración
- no debe poder entrar a otra cola incompatible
- no debe poder iniciar acciones que rompan la coherencia del matchmaking

### `TRANSFERRING`

- interacción bloqueada casi por completo
- no recibe acciones nuevas importantes
- no debe aceptar cambios de estado paralelos

### `IN_MATCH`

- interacción social fuertemente restringida
- no recibe duel requests nuevas
- no entra a colas
- no modifica layout
- spectate depende del contexto y settings del match

### `IN_PUBLIC_FFA`

- interacción parcial
- puede mantener algunas acciones sociales
- no debe mezclarse con cola o match competitivo al mismo tiempo

### `IN_PARTY_FFA`

- similar a experiencia grupal activa
- restricciones más parecidas a sesión grupal que a hub libre

### `IN_EVENT`

- depende del template del evento
- pero en general no debe superponerse con cola o match competitivo paralelo

### `EDITING_LAYOUT`

- estado de interacción enfocada
- no debe poder ser arrastrado silenciosamente a otras acciones destructivas sin validación clara

---

## Recomendación estructural fuerte

La mejor forma de pensarlo es así:

- **`Player State`** = fotografía global actual del jugador en Practice
- **`State Policy`** = reglas que dicen qué acciones están permitidas por estado
- **`Transition Rules`** = reglas válidas para pasar de un estado a otro

Y NO así:

- cada runtime inventa sus propios flags
- cada menú decide por su cuenta si algo se puede hacer
- cada comando chequea estados distintos de forma inconsistente

---

## Transiciones válidas recomendadas

Ejemplos de transiciones sanas:

- `HUB -> IN_QUEUE`
- `IN_QUEUE -> TRANSFERRING`
- `TRANSFERRING -> IN_MATCH`
- `IN_MATCH -> HUB`
- `HUB -> IN_PUBLIC_FFA`
- `IN_PUBLIC_FFA -> HUB`
- `HUB -> EDITING_LAYOUT`
- `EDITING_LAYOUT -> HUB`
- `HUB -> IN_EVENT`
- `IN_EVENT -> HUB`

### Decisión fuerte

Las transiciones inválidas deben rechazarse explícitamente.

Ejemplo:

- `IN_MATCH -> IN_QUEUE` directamente = no
- `TRANSFERRING -> EDITING_LAYOUT` = no
- `IN_PUBLIC_FFA -> IN_MATCH` sin flujo de salida/entrada válido = no

---

## Integración con settings y social

El estado del jugador NO reemplaza settings, los complementa.

Ejemplo:

- si el estado permite recibir duels, todavía hay que chequear si el setting del usuario también lo permite
- si el estado permite spectate, todavía hay que chequear si el jugador objetivo lo tiene habilitado

### Regla simple

- **state dice si es posible estructuralmente**
- **settings dicen si el usuario lo permite**

---

## Decisiones concretas que tomo

### 1. Debe existir un estado global principal único

Sí.

Es la forma más limpia de evitar contradicciones entre sistemas.

### 2. Deben existir subestados contextuales

Sí.

Dan precisión sin romper la claridad del estado principal.

### 3. Las acciones permitidas deben derivarse centralmente desde estado + settings + contexto

Sí.

Eso evita lógica duplicada y errores de UX.

### 4. Las transiciones deben ser explícitas

Sí.

No quiero cambios implícitos mágicos entre runtimes.

### 5. `TRANSFERRING` debe ser estado real

Sí.

En una network, transferencia no es un detalle: es una etapa crítica donde pasan muchos edge cases.

---

## Conclusión

El **Player State System** pasa a ser la columna vertebral de coherencia operativa del jugador dentro de Practice.

Sin él, social, colas, matches, FFA y eventos se pisan.
Con él, el sistema se vuelve predecible, seguro y mantenible.

---

## Definición central de “Profile / Stats System” en Practice

### Decisión conceptual

Se define que el **perfil del jugador** NO debe ser:

- solo un contenedor de stats sueltas
- solo una pantalla linda del menú
- solo una fila grande en base de datos sin límites claros

Se define que el perfil debe ser una:

- **identidad consultable del jugador dentro del ecosistema Practice**

Y el sistema de stats debe ser una:

- **proyección estructurada del rendimiento, actividad e identidad competitiva/social del jugador**

### Por qué esta decisión es correcta

Porque el perfil va a ser usado por:

- menú de estadísticas
- perfiles públicos
- leaderboards
- seasons
- historial
- social contextual de Practice
- futura capa web

Si no se separa bien qué pertenece al perfil y qué pertenece a stats/proyecciones, el modelo termina siendo un monstruo difícil de escalar.

---

## Modelo recomendado del `Practice Profile`

La recomendación final es separar el sistema en 5 bloques grandes.

### 1. Identidad base del perfil

Esto representa quién es el jugador dentro de Practice.

Campos recomendados:

- `playerId / uuid`
- `currentName`
- `normalizedName`
- `firstSeenAt`
- `lastSeenAt`
- `profileVisibility` si más adelante hace falta
- `currentGlobalRating`
- `currentGlobalRank`

#### Decisión fuerte

El perfil debe tener identidad pública clara aunque parte de sus stats especializadas nazcan lazy.

### 2. Datos competitivos actuales

Esto representa el estado competitivo vigente del jugador.

Debe contemplar:

- SR por modo
- rango por modo
- placements state por modo si aplica
- Global Rating actual
- rango global actual
- resumen competitivo actual de la season vigente

#### Importante

Esto es “estado actual”, no histórico completo.

### 3. Estadísticas agregadas por dimensión

Acá está una decisión MUY importante.

No conviene meter todas las stats en un solo bloque.

La separación correcta es:

#### a. All-time stats

Ejemplos:

- wins/losses totales
- matches played totales
- kills/deaths globales si aplica
- activity totals
- eventos ganados totales

#### b. Season stats

Ejemplos:

- ranked wins de la season
- ranked losses de la season
- top placement de la season
- global rating peak de la season
- stats FFA/evento de la season si se deciden mostrar

#### c. Mode stats

Ejemplos:

- Sword ranked wins/losses
- Sword current SR
- Sword peak SR
- Pot-specific performance stats
- stats relevantes según el modo

#### d. Social/activity stats

Ejemplos:

- parties joined
- events hosted
- events joined
- social participation signals

#### Decisión fuerte

Las stats deben dividirse por dimensión, no guardarse como una bolsa gigante indiferenciada.

### 4. Proyecciones de leaderboard

El perfil no debe ser el leaderboard mismo, pero sí debe exponer la información necesaria para alimentar leaderboards.

Ejemplos:

- current leaderboard-eligible values
- seasonal leaderboard values
- per-mode leaderboard values

#### Decisión fuerte

Los leaderboards deben leerse desde proyecciones optimizadas, no calcularse ad hoc leyendo perfiles gigantes cada vez.

### 5. Perfil público presentable

El sistema debe poder proyectar un perfil público bonito y útil, con:

- identidad del jugador
- rango global
- global rating
- rangos/SR por modo
- season highlights
- historial reciente
- stats importantes
- señales sociales relevantes

#### Decisión fuerte

El perfil público no debe exponer todo. Debe exponer lo valioso para prestigio, comparación y retención.

---

## Qué stats recomiendo considerar base desde ahora

### Competitivas base

- ranked matches played
- ranked wins
- ranked losses
- win rate
- current SR per mode
- peak SR per mode
- current global rating
- peak global rating

### Historial / performance

- recent matches count
- recent win/loss trend
- average match duration si resulta útil

### FFA base

- FFA kills
- FFA deaths
- streak peak
- time played

### Eventos base

- events hosted
- events joined
- events won

### Sociales base

- friends count
- party sessions joined

### Decisión fuerte

No recomiendo inflar el perfil inicial con 200 métricas exóticas. Mejor empezar con stats que:

- den valor al jugador
- alimenten leaderboards
- sirvan para comparación real
- tengan buena relación señal/ruido

---

## Relación entre Match History y Profile Stats

Esto es MUY importante.

La recomendación correcta es:

- `Match History` guarda el detalle de cada sesión
- `Profile Stats` guardan agregados/proyecciones derivadas

#### Traducción simple

- el historial responde: “qué pasó en este match”
- el perfil responde: “qué representa ese jugador a lo largo del tiempo”

No conviene mezclar esas dos responsabilidades.

---

## Relación entre Season y All-Time

La separación correcta es:

- **current state**
- **current season projections**
- **all-time projections**

#### Decisión fuerte

No quiero que una season nueva borre la identidad histórica del jugador. Quiero que resetee la capa competitiva vigente, pero conservando histórico y prestigio acumulado.

---

## Qué recomiendo mostrar en el perfil público principal

La mejor primera versión del perfil público debería priorizar:

- nombre
- rango global actual
- Global Rating actual
- top 3 o top N modos más fuertes
- season actual resumida
- historial reciente
- algunas stats clave
- badges/reconocimientos futuros si se agregan

### Por qué

Porque el perfil público debe ser:

- aspiracional
- legible
- comparable
- útil para retención

No un dashboard técnico lleno de ruido.

---

## Decisiones concretas que tomo

### 1. Perfil y stats deben separarse conceptualmente

Sí.

Perfil = identidad pública y estado actual.
Stats = proyecciones agregadas por dimensión.

### 2. Debe haber separación explícita entre all-time, season y per-mode

Sí.

Eso es obligatorio si querés seasons sanas y perfiles útiles.

### 3. El perfil público debe priorizar prestigio y comparación

Sí.

No hace falta mostrar absolutamente todo para que sea valioso.

### 4. Los leaderboards deben vivir sobre proyecciones, no sobre consultas improvisadas a perfiles completos

Sí.

Esto es una decisión de performance y diseño desde ahora.

### 5. FFA, eventos y social deben tener stats propias, pero sin contaminar la identidad competitiva core

Sí.

Eso mantiene limpio el corazón del Practice competitivo sin perder riqueza social.

---

## Conclusión

El **Profile / Stats System** pasa a ser la capa de identidad, prestigio y proyección del jugador dentro de Practice.

`Match History` cuenta lo que pasó.
`Profile` muestra quién sos.
`Stats` resumen lo que lograste.
`Leaderboards` comparan ese valor dentro del ecosistema.
