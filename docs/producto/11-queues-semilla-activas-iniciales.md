# Practice Hera — Queues semilla activas iniciales

## Propósito

Definir con qué **queues activas semilla** conviene arrancar el servidor.

Esto NO define las únicas queues que el sistema soporta.
Define las primeras queues que conviene dejar:

- visibles
- activas
- testeables
- conectadas al flujo principal del Hub

para validar el sistema sin meter complejidad innecesaria.

---

## Aclaración conceptual

Ya quedó definido que:

- `Mode` = qué se juega
- `Arena` = dónde puede jugarse
- `Queue` = cómo entrás a buscar esa experiencia

Entonces, aunque el cliente pueda crear in-game nuevas queues cuando quiera, sigue teniendo sentido definir un set inicial de queues activas para:

- bootstrap
- testing
- primer bloque jugable real
- orden de implementación

---

## Criterios usados para decidir

Las queues semilla activas iniciales se eligieron con estos criterios:

1. **priorizar el flujo principal del producto**
   - Hub → Ranked/Unranked → Match → Hub

2. **no inflar la complejidad inicial**
   - evitar demasiadas colas activas desde el día 1

3. **cubrir variedad competitiva suficiente**
   - no solo un modo, pero tampoco demasiados a la vez

4. **respetar la decisión ya tomada de ranked 1v1 únicamente**

5. **dejar FFA, party duels y eventos fuera de esta primera lista de queues competitivas**
   - porque tienen flujos propios y no deben mezclarse con el primer bloque de queueing competitivo

---

## Decisión final recomendada — queues semilla activas iniciales

La recomendación correcta es arrancar con estas **12 queues semilla activas**:

### Ranked
- Ranked Sword 1v1
- Ranked Axe 1v1
- Ranked BuildUHC 1v1
- Ranked CrystalPvP 1v1
- Ranked Pot 1v1
- Ranked SMP 1v1

### Unranked
- Unranked Sword 1v1
- Unranked Axe 1v1
- Unranked BuildUHC 1v1
- Unranked CrystalPvP 1v1
- Unranked Pot 1v1
- Unranked SMP 1v1

---

## Por qué esta lista es la correcta

### 1. Mantiene una simetría limpia

Cada modo semilla base tiene:

- una versión Ranked
- una versión Unranked

Eso simplifica muchísimo:

- UX del hub
- testing
- comprensión del sistema
- validación del modelo `Mode -> Queue -> Match`

### 2. No mezcla todavía formatos que aumentan mucho la complejidad

No recomiendo que el primer bloque activo de queues incluya de entrada:

- 2v2
- 3v3
- 4v4
- Party queues competitivas

No porque el sistema no pueda soportarlas, sino porque no conviene meter esa complejidad antes de validar limpio el 1v1 base.

### 3. Respeta el principio de no sobrecomplicar

Ya tenemos suficiente complejidad con:

- rating
- matchmaking
- rules
- arenas
- historia
- transferencias

Meter teams en el mismo bloque inicial haría más ruido del necesario.

### 4. Deja espacio para escalar sin dolor

Una vez validado este primer bloque, después es natural sumar:

- queues team-based unranked
- queues especiales
- party challenges más complejos

sin reescribir el corazón del sistema.

---

## Qué NO recomiendo activar en el bloque inicial de queues

### 1. Queues team-based
- 2v2
- 3v3
- 4v4

### 2. Queues experimentales o de modos todavía no priorizados
- NetheriteOP
- DiamondSMP
- Mace

### 3. Lo que no debe modelarse como queue competitiva base
- Public FFA
- Party FFA
- Hosted events
- Parkour
- Dropped

#### Por qué

Porque esas experiencias existen, sí, pero no deben ensuciar el primer bloque central de queueing ranked/unranked.

---

## Cómo debería verse esto en el Hub

La UX correcta inicial sería:

- el jugador toca Ranked o Unranked
- se abre el menú de modos
- ahí aparecen los 6 modos semilla activos
- al elegir uno, entra a la queue específica correspondiente

Ejemplo:

- botón Ranked → menú → Sword → entra a `Ranked Sword 1v1`
- botón Unranked → menú → Pot → entra a `Unranked Pot 1v1`

Esto mantiene la experiencia:

- clara
- rápida
- simétrica
- fácil de entender para el jugador

---

## Decisiones concretas que tomo

### 1. Las queues semilla activas iniciales deben ser 1v1 solamente

Sí.

Es lo más coherente con todo lo ya definido.

### 2. Cada modo semilla activo debe tener Ranked y Unranked

Sí.

Eso simplifica bootstrap, testing y comprensión del producto.

### 3. FFA y eventos no deben entrar a esta lista de queues semilla competitivas

Sí.

Porque tienen flujos operativos distintos.

### 4. Teams quedan como expansión posterior del sistema de queues

Sí.

No se descartan; simplemente no se activan en el primer bloque.

---

## Conclusión

La primera lista correcta de queues semilla activas no debe intentar demostrar todo el poder del sistema.

Debe demostrar estas tres cosas:

1. que el flujo principal competitivo funciona
2. que el modelo de queueing/matchmaking/match está sano
3. que el producto ya se siente serio desde el día 1

Por eso, la decisión inicial recomendada queda en 12 queues activas:

- 6 Ranked 1v1
- 6 Unranked 1v1

sobre los modos:

- Sword
- Axe
- BuildUHC
- CrystalPvP
- Pot
- SMP
