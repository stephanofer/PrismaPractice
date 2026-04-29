# Practice Hera — Rangos iniciales y rules por defecto

## Propósito

Cerrar dos pendientes importantes antes del desarrollo fuerte:

1. una primera propuesta simple de **rangos e intervalos**
2. un primer bloque coherente de **rules por defecto por modo semilla**

Esto NO pretende ser el balance final eterno.
Pretende dejarnos una base limpia, simple y suficientemente buena para arrancar sin improvisar.

---

## Rangos iniciales recomendados

### Filosofía

Como primera versión conviene mantener el sistema simple, legible y fácil de comunicar.

La propuesta recomendada es esta:

- Iron
- Gold
- Emerald
- Diamond
- Netherite
- Master
- Grandmaster

### Intervalos iniciales recomendados

- **Iron** → 0 - 999
- **Gold** → 1000 - 1299
- **Emerald** → 1300 - 1599
- **Diamond** → 1600 - 1899
- **Netherite** → 1900 - 2199
- **Master** → 2200 - 2399
- **Grandmaster** → 2400+

### Por qué esta propuesta es correcta por ahora

- mantiene nombres claros y familiares
- evita un ladder ridículamente complejo al principio
- deja una progresión visible y aspiracional
- funciona bien tanto para rank por modo como para rank global

### Nota importante

Estos intervalos son una **base inicial operativa**.
Más adelante podremos ajustar:

- nombres
- thresholds
- densidad por tier
- distribución real según datos de jugadores

Pero para empezar, esto es suficientemente simple y coherente.

---

## Dónde conviene definir si un modo regenera vida o no

### Verificación conceptual

Sí conviene tratarlo como **rule**, no como parámetro de arena.

### Decisión recomendada

- la regeneración vanilla de vida debe modelarse como parte del **Rule System**
- NO como parámetro propio de la arena

### Por qué

Porque la regeneración de vida define comportamiento de gameplay, no naturaleza espacial del mapa.

En simple:

- **Arena** define dónde puede jugarse
- **Rule** define cómo se comporta esa experiencia

### Traducción práctica

Si querés que un modo:

- regenere vida naturalmente → no activás la rule que la bloquea
- NO regenere vida naturalmente → activás `Health does not regenerate`

### Qué sí puede aportar el arena

El arena puede declarar capacidades del entorno como:

- si tolera líquidos
- si tolera block place/break
- si tiene void/lava/water relevantes

Pero NO debería ser dueño de reglas como regeneración vanilla, golden apple cooldown o potion effects.

### Conclusión fuerte

Si algo altera el comportamiento general del combate, conviene que viva en **Mode + Rule Set efectivo**.
No en la arena.

---

## Rules por defecto recomendadas por modo semilla

### Principio usado

Estoy usando exclusivamente la base de rules ya definida en el proyecto y el criterio de no sobrecomplicar.

La idea es:

- pocos defaults bien elegidos
- que representen correctamente el modo
- que después se puedan ajustar sin romper el modelo

---

## 1. Sword — rules por defecto recomendadas

### Activas por defecto
- Hunger does not go down
- No fall damage
- Disable crafting slots
- Remove empty bottle

### No activas por defecto
- Health does not regenerate
- Disable break/place blocks
- Ender pearl cooldown
- Golden Apple cooldown
- God apple cooldown

### Lectura del modo

Sword debería sentirse como el melee base más limpio del sistema.
No conviene meterle demasiadas reglas especiales de entrada.

---

## 2. Axe — rules por defecto recomendadas

### Activas por defecto
- Hunger does not go down
- No fall damage
- Disable crafting slots
- Remove empty bottle

### No activas por defecto
- Health does not regenerate
- Disable break/place blocks
- Ender pearl cooldown
- Golden Apple cooldown
- God apple cooldown

### Lectura del modo

Axe comparte bastante base con Sword en su primera aproximación.
La diferencia fuerte la pone el kit/gameplay, no hace falta diferenciarlo artificialmente con 20 rules.

---

## 3. BuildUHC — rules por defecto recomendadas

### Activas por defecto
- Hunger does not go down
- Golden head insta consumable, gives a golden apple effect
- Health does not regenerate
- Dying by touching lava
- Dying by touching water
- Dying by touching void

### Opcionales muy probables según tuning posterior
- Timed block
- Only self-placed block breaking
- Golden Apple cooldown
- Ender pearl cooldown

### No activas por defecto en la primera base
- Disable break/place blocks

### Lectura del modo

BuildUHC necesita ya una identidad más marcada.
Acá sí tiene sentido usar varias rules que lo distingan del melee base.

---

## 4. CrystalPvP — rules por defecto recomendadas

### Activas por defecto
- Hunger does not go down
- Disable Totem
- Disable Elytra
- Disable crafting slots
- Dying by touching void

### Opcionales según tuning posterior
- Health does not regenerate
- Ender pearl cooldown

### Lectura del modo

Crystal tiene un skill ceiling alto y ya bastante identidad por su propio kit/combate.
No conviene sobrerregularlo al principio si todavía no tenemos datos reales del servidor.

---

## 5. Pot — rules por defecto recomendadas

### Activas por defecto
- Hunger does not go down
- Health does not regenerate
- Remove empty bottle
- Disable crafting slots

### Opcionales según tuning posterior
- No fall damage

### Lectura del modo

Pot sí necesita que la curación principal venga de su mecánica propia.
Por eso acá la rule clave es claramente:

- `Health does not regenerate`

---

## 6. SMP — rules por defecto recomendadas

### Activas por defecto
- Hunger does not go down
- Golden Apple cooldown
- Ender pearl cooldown
- Disable crafting slots

### Opcionales según tuning posterior
- Health does not regenerate
- Disable item drop

### Lectura del modo

SMP debería sentirse más táctico/survival que Sword/Axe, pero sin convertirse en un caos de 15 reglas especiales desde el día 1.

---

## Resumen ultra simple por modo

### Sword
- limpio y base

### Axe
- parecido a Sword en su primera base

### BuildUHC
- identidad más fuerte, varias rules activas

### CrystalPvP
- kit/combate pesa más que una montaña de rules

### Pot
- necesita claramente `Health does not regenerate`

### SMP
- más táctico, con cooldowns razonables

---

## Decisiones concretas que tomo

### 1. Los rangos iniciales deben ser simples

Sí.

Iron, Gold, Emerald, Diamond, Netherite, Master y Grandmaster es una base sana y fácil de comunicar.

### 2. La regeneración vanilla debe modelarse como rule

Sí.

La arena no debe ser dueña de esa lógica.

### 3. Los modos base no deben arrancar todos hipercargados de rules

Sí.

La regla general correcta es: identidad clara con pocos defaults buenos.

### 4. Pot y BuildUHC sí justifican más diferencia inicial vía rules

Sí.

Porque ahí las reglas realmente cambian el comportamiento del modo de manera importante.

---

## Conclusión

Con esta definición ya tenemos:

- una primera escala de rangos clara
- una decisión correcta sobre regeneración vanilla
- un primer bloque coherente de rules por defecto para los modos semilla

Eso ya alcanza para bajar estas decisiones a implementación sin inventar comportamiento a mitad del desarrollo.
