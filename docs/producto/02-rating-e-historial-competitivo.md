# Practice Hera — Rating e historial competitivo

## Rating competitivo: SR + rangos

El sistema de rating es **ULTRA importante**.

La definición actual es usar:

- **SR (Skill Rating)**
- **rangos visibles según tu SR**
- **Global Rating**

### SR por modo

Se definió que:

- cada modo tiene su propio **SR independiente**
- el SR sube al ganar
- el SR baja al perder

Esto significa que el skill competitivo se mide por ladder/modo, no con un único valor mezclado para todo.

### Global Rating

Además va a existir un **Global Rating**.

La definición actual es:

- el **promedio simple** de todos los SR por modo del jugador

Ejemplo dado:

- Sword: 1,845
- Axe: 1,916
- UHC: 2,195
- Vanilla: 1,306
- Mace: 2,059
- Pot: 1,888
- NetheriteOP: 1,729
- SMP: 1,689
- DiamondSMP: 1,871

Total:

- 16,498 / 9 modos = **1,833 Global Rating**

Ejemplos mencionados:

- **Unranked / recién empieza** → ejemplo como Vendimia con **798**
- **Diamond III** → rango medio-alto, aprox. **2046–2166 SR**
- **Netherite** → casi top, aprox. **2264 SR**
- **Grandmaster** → rango más alto visible, ejemplo **#1 Verniq con 2300 SR**

Esto sugiere desde el negocio:

- rank tiers visibles
- thresholds por rango
- leaderboard global y por modo
- ranking competitivo como parte central de la retención

### Rangos por modo y rango global

También se definió que existirán:

- **SR por modo**
- **rango por modo**
- **Global Rating**
- **rango global**

Ejemplo preliminar de mapeo por SR:

- Emerald → ~1000 - 1499
- Diamond → ~1500 - 1999
- Netherite → 2000+

OJO: estos nombres e intervalos son ejemplos iniciales. Después se definirán exactamente.

### Seasons

Se definió que:

- **sí habrá seasons desde esta primera gran fase**

Pero con una filosofía SIMPLE.

### Enfoque de seasons

La intención NO es construir un sistema ultra automatizado y complejo desde el principio.

Lo importante es:

- que exista el concepto de corte de season
- que se conserven los datos históricos
- que el sistema quede preparado para manejar seasons

### Definiciones actuales

- la duración puede variar
- el cierre de season puede ser **manual**
- no se quiere depender de que automáticamente, al llegar una fecha, “se corte todo”
- lo importante es poder ejecutar el corte de forma controlada y mantener histórico

### Reset de season

La opción más simple y aceptada por ahora es:

- **reset total**

### Histórico

Se quiere conservar:

- datos por season
- datos **all-time**

### Rewards

Los rewards se darán:

- **manualmente**

### Global Rating en contexto de seasons

Se quiere contemplar ambos:

- **seasonal**
- **histórico / all-time**

### Recomendación inicial sobre placements, decay y streaks

Como el objetivo es un Practice competitivo, la recomendación inicial es:

- **placements iniciales: sí**
- **decay: no al inicio**
- **streak bonus para SR: no al inicio**

#### Por qué

- **placements sí**: ayudan a ubicar más rápido a jugadores nuevos y reducen basura competitiva en los primeros matches
- **decay no al inicio**: agrega complejidad operativa y presión artificial antes de validar bien el sistema principal
- **streak bonus no al inicio**: puede distorsionar demasiado la calidad competitiva; es mejor que el SR refleje dificultad del rival y resultado real antes que rachas infladas

Más adelante esto se puede ajustar sin romper el modelo base.

---

## Historial de partidas y detalle post-match

Sí va a existir historial de partidas.

### Idea principal

Cuando comienza un match, durante el combate se recopila mucha información. Al terminar, se mostrará un enlace en el chat con la información del combate.

Ese enlace puede no estar disponible instantáneamente. Se puede generar por detrás y aparecer unos segundos después, cuando ya esté listo.

### Información ejemplo a mostrar por jugador

Ejemplo de estadísticas como las mencionadas:

- Health
- Hits
- Longest Combo
- Heals Used
- Pot Accuracy
- Crits
- Dmg Dealt
- Dmg Taken
- Ping

Ejemplo textual dado:

#### parrottitan
- Health: 3.0 ❤
- Hits: 20
- Longest Combo: 2
- Heals Used: 0
- Pot Accuracy: 0%
- Crits: 6
- Dmg Dealt: 20.5
- Dmg Taken: 13.6
- Ping: 18ms

### También debe mostrar

- espada restante
- armadura restante
- totems restantes
- items restantes
- inventario/equipamiento relevante de ambos jugadores

### Representación exacta del inventario

También se quiere contemplar que el inventario del jugador pueda guardarse o representarse con suficiente fidelidad como para mostrarlo en web **exactamente en los slots correspondientes**.

Esto es importante para:

- mostrar el estado final real del jugador
- visualizar hotbar/layout como terminó la pelea
- representar armadura, offhand, consumibles y items restantes con precisión

La intención actual es guardar lo realmente importante, sin volver esto excesivo, pero sí dejando el modelo listo para reflejar el inventario con exactitud visual.

### Decisión actual sobre snapshot de inventario

Se definió que:

- se guardará **solo el snapshot final del inventario**
- no hace falta guardar un snapshot inicial separado del loadout para esta primera gran fase

---

## Estructura objetivo del historial de match

Se definió que el historial/resumen del match debe contemplar estos 4 grupos de datos:

### 1. Metadata del match

Debe contemplar, al menos:

- match id
- tipo de match
- modo/ladder
- ranked o unranked
- formato (bo1, bo3, etc.)
- mapa/arena
- región
- timestamps relevantes
- duración
- ganador
- server id o referencia operativa similar

### 2. Datos por jugador

Debe contemplar, al menos:

- uuid
- nombre
- team/side si aplica
- SR antes y después
- resultado
- ping promedio o final
- health/hearts finales
- inventario final
- armor final
- efectos activos si aplica
- consumibles restantes

### 3. Stats agregadas

Debe contemplar, al menos:

- hits landed
- hits received
- combo máximo
- crits
- damage dealt
- damage taken
- pots lanzadas
- pots acertadas
- heals used
- pearls used
- gaps usadas
- totems popped
- blocks placed
- blocks broken

### 4. Eventos mínimos importantes

No se busca una replay completa, pero sí eventos mínimos con valor real.

Debe contemplar eventos como:

- round start/end
- death
- kill o determinación de ganador si aplica
- void/lava/water death
- potion use relevante si aplica
- pearl throw relevante si aplica
- totem pop si aplica
- pressure plate instant win u otra condición especial equivalente

### Criterio general

- sí guardar lo importante
- no guardar una timeline absurda o excesiva en esta primera gran fase
- priorizar resumen potente, consistente y representable en juego/web

### Información global del match

- mapa
- duración
- ladder / kit
- región

Ejemplo mencionado:

- Map: Sea
- Duration: 00:31
- Ladder: Sword
- Region: North America

### Adicionalmente

Dentro del juego también va a haber menús con historial de peleas del jugador.
