# Practice Hera — Entidades Modo y Rule

## Definición central de “Modo” en Practice

### Decisión conceptual

Se definió que un **Modo** NO debe modelarse como:

- una cola
- un evento
- un servidor
- una experiencia runtime aislada

Se definió que un **Modo** debe ser una:

- **definición reusable de gameplay**

Eso significa que el modo representa la base jugable que luego puede reutilizarse en distintos contextos, por ejemplo:

- ranked
- unranked
- FFA público
- party FFA
- party duel
- eventos hosteados

### Por qué esta decisión es correcta

Porque permite:

- escalar nuevos modos sin dolor
- evitar ladders hardcodeadas
- reutilizar kits, rules y stats
- separar correctamente gameplay de runtime/cola/evento

### Flags de uso por modo

Sí, cada modo debe tener flags/capacidades explícitas que indiquen dónde puede utilizarse.

Ejemplos:

- usable en ranked
- usable en unranked
- usable en FFA público
- usable en party FFA
- usable en party duel
- usable en eventos

Esto evita supuestos implícitos y vuelve el sistema mucho más mantenible.

### Stats relevantes por modo

Sí, las estadísticas relevantes deben depender del modo.

No todos los modos deben medir exactamente lo mismo.

Ejemplos conceptuales:

- Sword → hits, crits, damage, combo
- Pot → pot accuracy, heals used
- Boxing → combo e intercambios relevantes
- Sumo → ringout / win condition específica
- Parkour → tiempo, checkpoints, fallos
- Dropped → supervivencia / eliminación

Esto es importante para:

- match history
- perfiles
- leaderboards
- analítica de combate

### Estructura lógica recomendada de un modo

Un modo debería contemplar al menos estos bloques conceptuales:

1. identidad
2. capacidades/flags de uso
3. player types compatibles
4. kit base / política de layout
5. match defaults
6. rule set default
7. flags competitivas
8. stats relevantes
9. arena requirements
10. compatibilidad contextual

### Versión refinada recomendada de `Mode Definition`

La versión refinada recomendada, a nivel conceptual, es esta:

#### 1. Identidad

- `identifier`
- `displayName`
- `description`
- `icon/menuItem`

#### 2. Capacidades / flags de uso

- `rankedEnabled`
- `unrankedEnabled`
- `publicFfaEnabled`
- `partyFfaEnabled`
- `partyDuelEnabled`
- `hostedEventEnabled`

#### 3. Player types compatibles

- `supportedPlayerTypes`
  - 1v1
  - 2v2
  - 3v3
  - 4v4
  - public FFA
  - party FFA

#### 4. Kit / Layout policy

- `premadeKitId`
- `layoutEditable`
- `layoutRestrictions`

#### 5. Match defaults

- `defaultRoundsToWin`
- `preFightDelay`
- `maxMatchDuration`
- `spectatorPolicy`

#### 6. Rule set default

- `defaultRules`
- `ruleParameters`

#### 7. Flags competitivas

- `rated`
- `visibleRankEnabled`
- `contributesToGlobalRating`
- `leaderboardEnabled`
- `placementsEnabled`

#### 8. Stats relevantes

- `relevantStats`

Esto permite que cada modo declare explícitamente qué stats tienen sentido para:

- historial
- perfil
- leaderboard
- resumen web/in-game

#### 9. Arena requirements

- `requiredArenaType`
- `arenaConstraints`

#### 10. Compatibilidad contextual

- `allowedQueueContexts`
- `allowedEventTemplates`
- `allowedRuntimeContexts`

### Por qué esta versión refinada es la correcta

Porque separa claramente:

- la identidad del modo
- dónde puede usarse
- cómo se juega
- qué reglas lo modifican
- cómo compite
- qué estadísticas importan

Y además deja margen real para:

- agregar campos
- quitar campos
- extender comportamiento

sin romper la idea central del dominio.

### Conclusión

La entidad **Modo** pasa a ser una de las piezas centrales del dominio de Practice.

No representa una cola ni un evento concreto, sino una base jugable reusable sobre la que viven ranked, unranked, FFA, party systems y eventos.

---

## Definición central de “Rule” en Practice

### Decisión conceptual

Se define que una **Rule** NO debe ser:

- un if suelto perdido en listeners
- una excepción hardcodeada dentro de un modo
- una condición enterrada en un runtime específico

Se define que una **Rule** debe ser una:

- **unidad declarativa y reusable de comportamiento de gameplay**

En otras palabras:

- el `Mode Definition` declara qué rules tiene activas
- la `Rule` define qué comportamiento altera
- el runtime aplica esas reglas según el contexto de ejecución

### Por qué esta decisión es correcta

Porque permite:

- agregar variaciones de gameplay sin crear modos nuevos al pedo
- reutilizar comportamiento entre ranked, unranked, FFA, party FFA y eventos
- evitar lógica duplicada y condiciones caóticas por todos lados
- mantener el dominio auditable y extensible

---

## Modelo recomendado de `Rule Definition`

La recomendación final es modelar cada Rule con estos bloques conceptuales.

### 1. Identidad

- `identifier`
- `displayName`
- `description`

Esto permite referenciarla con claridad en configs, menús internos, auditoría y tooling futuro.

### 2. Tipo de regla

Cada rule debe pertenecer a una categoría funcional clara.

Categorías recomendadas:

- `COMBAT`
- `MOVEMENT`
- `WORLD`
- `ITEM`
- `HEALTH`
- `REGEN`
- `PROJECTILE`
- `WIN_CONDITION`
- `INVENTORY`
- `POTION`
- `SPECIAL`

Esto no es solo prolijidad: ayuda a validación, compatibilidad y debugging.

### 3. Scope de aplicación

La rule debe declarar en qué scope impacta.

Ejemplos:

- `PLAYER`
- `TEAM`
- `MATCH`
- `WORLD`

Ejemplo práctico:

- `Golden Apple cooldown` impacta jugador
- `Disable break/place blocks` impacta mundo/match
- `Pressure plate instant win` impacta match/win condition

### 4. Activación

Una rule debe poder existir en tres estados conceptuales:

- activada
- desactivada
- activada con parámetros

Porque muchas rules no son solo booleanas.

Ejemplos:

- `Ender pearl cooldown = 16s`
- `Golden Apple cooldown = 20s`
- `Extra hearts = 4`
- `Timed block = 10s`

### 5. Parámetros

Las rules deben aceptar parámetros tipados cuando haga falta.

Tipos recomendados:

- boolean
- int
- double
- duration
- enum/lista cerrada
- potion effect set

Esto evita reglas hardcodeadas por variante y hace que el sistema sea realmente poderoso.

### 6. Contextos compatibles

Cada rule debe declarar dónde puede usarse.

Ejemplos:

- compatible con ranked
- compatible con unranked
- compatible con public FFA
- compatible con party FFA
- compatible con hosted events

Y también, cuando haga falta:

- compatible con ciertos tipos de modo
- incompatible con ciertos event templates

Esto es importante porque no toda rule tiene sentido en todo contexto.

### 7. Fase de ejecución

Cada rule debe declarar cuándo entra en juego.

Fases recomendadas:

- `PRE_MATCH`
- `MATCH_START`
- `IN_MATCH`
- `ON_DAMAGE`
- `ON_MOVE`
- `ON_ITEM_USE`
- `ON_BLOCK_PLACE`
- `ON_BLOCK_BREAK`
- `ON_DEATH`
- `ON_WIN_CHECK`
- `POST_MATCH`

Esto baja muchísimo el caos al aplicar reglas.

### 8. Conflictos / incompatibilidades

Algunas rules pueden ser incompatibles entre sí.

Ejemplos conceptuales:

- `Health does not regenerate` vs una rule que fuerce regeneración especial
- `Disable ender pearl` vs `Ender pearl cooldown`
- `Disable item drop` vs mecánicas que dependan de dropeo

Por lo tanto, una rule debe poder declarar:

- `conflictsWith`
- o una política de precedencia

### 9. Stats/telemetría relacionadas

Algunas rules impactan qué stats tiene sentido capturar o mostrar.

Ejemplos:

- `Potion effects` puede habilitar telemetría asociada
- `Only arrow damage` hace más importantes stats de projectile
- `Timed block` puede volver relevante una stat de block lifecycle si alguna vez se desea

No hace falta sobrecomplicarlo ahora, pero el modelo debe admitirlo.

### 10. Mensajería / visibilidad

Una rule debería poder indicar si merece feedback visible para jugador/admin.

Ejemplos:

- mostrar cooldown activo
- mostrar mensaje cuando una acción está bloqueada por rule
- mostrar condición especial de victoria

Esto no significa acoplarla a UI, solo permitir que la UX la represente bien.

---

## Recomendación estructural fuerte

La mejor forma de pensarlo es esta:

- **`Rule Definition`** = contrato declarativo
- **`Rule Set`** = colección de rules activas + parámetros
- **`Rule Engine / Rule Handlers`** = aplicación runtime de esas rules

Y NO esto:

- modo gigante con 200 ifs
- listeners llenos de flags sueltas
- lógica repetida por cada ladder

### Traducción simple

- el modo dice **qué reglas usa**
- el rule set dice **con qué configuración exacta**
- el runtime dice **cómo se aplican en ejecución**

---

## Ejemplos de cómo encaja esto

### Ejemplo 1 — Sword base

- no hunger loss
- no fall damage
- disable crafting slots

### Ejemplo 2 — BuildUHC-like

- block place/break permitido
- golden head insta consumable
- water/lava behavior especial

### Ejemplo 3 — Event variant

- base de Sword
- extra hearts
- pressure plate instant win
- max duration ajustada

O sea: no creás un modo totalmente nuevo si solo cambian algunas rules.

---

## Decisiones concretas que tomo

### 1. Las rules deben ser reutilizables entre contextos

Sí.

Una buena rule no debería pertenecer solo a ranked o solo a eventos si conceptualmente puede aplicarse en más lugares.

### 2. Las rules deben poder tener parámetros

Sí.

Sin parámetros, el sistema se queda chico demasiado rápido.

### 3. Debe existir validación de compatibilidades

Sí.

No podemos permitir combinaciones absurdas o conflictivas sin validación.

### 4. Las rules deben ser declarativas antes que hardcodeadas

Sí.

La implementación runtime existirá igual, pero el dominio debe modelarlas declarativamente.

### 5. Los modos especiales seguirán existiendo

Sí.

Este sistema de rules NO elimina la necesidad de modos especiales como Sumo, Boxing, Bridge o Dropped.
Lo que hace es evitar convertir cualquier pequeña variación en un modo especial innecesario.

---

## Conclusión

La entidad **Rule** pasa a ser la segunda pieza más importante del dominio, junto con `Mode Definition`.

`Mode Definition` define la experiencia base.
`Rule Definition` define cómo esa experiencia puede alterarse, especializarse y escalar sin dolor.
