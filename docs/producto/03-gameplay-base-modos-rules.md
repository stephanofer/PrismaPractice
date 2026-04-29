# Practice Hera — Gameplay base, modos y rules

## Arenas

Sí, va a haber:

- múltiples arenas

Esto probablemente implique más adelante:

- pool de arenas por modo/kit
- disponibilidad
- selección automática o ponderada
- administración de arena occupancy

---

## Modos y kits

Sí, va a haber:

- múltiples modos
- diferentes kits

También se mencionó explícitamente que el usuario va a poder editar sus kits/layouts.

### Decisión clave: sistema flexible de modos base

Acá hay una decisión de arquitectura MUY importante.

No queremos diseñar los modos como lógica rígida hardcodeada para cada ladder desde el principio.

La idea es separar:

1. **Modos base configurables**
2. **Modos especiales con comportamiento propio**

### 1. Modos base configurables

Estos son modos que pueden construirse a partir de una estructura común y configuración declarativa.

Al crear un modo base, se quiere poder definir al menos:

- `identifier`
- `display name`
- `player types` habilitados
  - 1v1
  - 2v2
  - 3v3
  - 4v4
  - FFA de party
  - FFA público
- `premade kit`
- `menu item`
- `match settings`
  - default rounds to win
  - pre-fight delay
  - duración máxima del match

Esto significa que muchos modos deberían nacer como una combinación de:

- metadata del modo
- kit base
- settings del match
- conjunto de rules

### 2. Modos especiales con comportamiento propio

Algunos modos sí tienen comportamiento demasiado particular y probablemente necesiten lógica específica.

Ejemplos claros:

- Sumo
- Boxing
- Bridge
- Dropped

La meta es que estos casos especiales existan, pero que el sistema esté diseñado para que la mayoría de modos no requieran una implementación completamente nueva.

---

## Sistema de Rules configurables

Esto se definió como un concepto muy poderoso y central para el diseño de modos.

La idea es manejar un sistema de **rules configurables** que alteren el comportamiento del match/modo sin tener que crear un modo nuevo desde cero cada vez.

### Catálogo inicial oficial de rules

Verificado: las **rules iniciales** serán exactamente esta lista base que definiste. No la tomo como “ejemplos sueltos”, sino como el primer catálogo oficial sobre el que vamos a diseñar el Rule System.

- Hunger does not go down
- Disable break/place blocks
- Dying by touching lava
- Timed block
- Golden Apple cooldown
- Only arrow damage
- No fall damage
- Soup restores health
- God apple cooldown
- Dying by touching water
- Dying by touching void
- Water slowly damage
- Arrows disappear on ground
- Only self-placed block breaking
- Ender pearl cooldown
- Immune to losing health
- Disable Elytra
- Disable Totem
- Golden head insta consumable, gives a golden apple effect
- Disable crafting slots
- Health does not regenerate
- Disable item drop
- Pressure plate instant win
- Extra hearts
- No hit delay
- Potion effects
- Remove empty bottle
- Disable ender pearl

### Nota importante

Este catálogo inicial es considerable y suficiente para diseñar el sistema correctamente desde el principio.

Más adelante podremos:

- agregar nuevas rules
- eliminar alguna si deja de tener sentido
- ajustar parámetros o compatibilidades

Pero la base inicial validada es esta lista exacta.

### Implicación arquitectónica

Esto empuja el diseño hacia algo así:

- un **Mode Definition**
- un **Match Settings Definition**
- un **Rule Set**
- handlers/evaluadores de reglas enchufables

Y NO hacia un sistema donde cada ladder sea una clase gigante con lógica mezclada.

### Beneficio principal

Poder crear muchos modos nuevos simplemente configurando:

- identidad
- kit
- settings
- rules

Y reservar código especial solo para ladders verdaderamente excepcionales.

---

## Tipos de enfrentamiento y relación con los modos

### Player types

Se definió que los **player types son configurables por modo**.

Eso significa que un modo puede declarar cuáles variantes soporta, por ejemplo:

- 1v1
- 2v2
- 3v3
- 4v4
- FFA de party
- FFA público

Importante: el modo no está obligado a soportar todos.
Cada modo habilita solo los tipos que tengan sentido.

### Ranked

Se definió explícitamente que:

- **Ranked será solo 1v1**

Eso simplifica muchísimo el sistema competitivo principal, porque:

- el SR competitivo base no depende de equipos en la primera fase
- el leaderboard ranked core se puede centrar en ladders 1v1
- el matchmaking ranked no necesita lógica inicial de team assembly competitiva

### FFA público

Se definió que:

- **FFA público es totalmente aparte**

Va a tener:

- sus propias estadísticas
- su propio comportamiento
- su propia lógica de actividad

Pero OJO: eso NO significa que tenga que reinventar todo.

También tiene sentido que reutilice piezas del ecosistema Practice, por ejemplo:

- modos/kits del Practice cuando aplique
- kit layouts personalizados del jugador
- rules configurables para el mundo/arena FFA

Por lo tanto, aunque tenga identidad y stats propias, no debe confundirse con el flujo de match competitivo ni duplicar sistemas base innecesariamente.

### Party FFA

La idea planteada es:

- crear una party
- invitar amigos
- jugar en una arena en formato FFA entre los miembros de la party

Y acá la aclaración es importante:

- **Party FFA es parte del Practice**
- por lo tanto debe usar el ecosistema del Practice:
  - modos
  - kits
  - layouts
  - rules
  - y demás piezas compartidas que apliquen

### Conclusión arquitectónica

La separación correcta, por ahora, queda así:

- **Competitive Match System**
  - ranked 1v1
  - unranked con variantes configurables según modo
- **Public FFA System**
  - runtime/loop y estadísticas propias
  - reutiliza kits, layouts, modos y rules del ecosistema Practice cuando convenga
- **Party FFA / custom combat session**
  - experiencia social dentro del Practice
  - usa las piezas del Practice en vez de inventar un sistema paralelo

---

## Kit layouts / personalización del inventario

El usuario va a poder:

- acomodar su hotbar a su estilo
- agregar items
- sacar items
- editar el layout del kit

Esto implica pensar desde el negocio:

- diferencia entre kit base del servidor y layout personalizado del jugador
- validación de qué se puede mover y qué no
- persistencia de layouts por modo/kit
