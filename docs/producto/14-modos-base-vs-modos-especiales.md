# Practice Hera — Modos base vs modos especiales

## Propósito

Cerrar el límite exacto entre:

- **modos base configurables**
- **modos especiales con comportamiento custom**

Y hacerlo respetando un principio clave del proyecto:

> que crear un modo especial NO sea doloroso, ni ensucie el dominio, ni mezcle lógica por todos lados.

---

## Decisión conceptual

La arquitectura correcta NO es:

- o todo se resuelve con rules
- o todo modo raro se convierte en una implementación totalmente separada y caótica

La arquitectura correcta es esta:

1. **Modo base configurable** cuando el comportamiento puede expresarse con:
   - metadata
   - kit
   - match settings
   - player types
   - rule set
   - stats relevantes

2. **Modo especial** cuando la experiencia necesita:
   - lógica de victoria distinta
   - loop propio
   - interacción muy diferente al combate base
   - stats que ya no encajan bien en el modelo base
   - runtime behavior demasiado particular para resolverlo solo con rules

---

## Regla madre para decidir si algo es modo base o especial

### Es modo base si

puede construirse limpiamente sobre:

- `Mode Definition`
- `Rule Set`
- `Match Session`
- `Arena Definition`
- stats relevantes por modo

sin introducir excepciones raras en:

- win condition
- round model
- loop del jugador
- flujo de runtime

### Es modo especial si

obliga a meter comportamiento custom en el corazón del flujo base.

En otras palabras:

si para soportarlo bien empezás a decir cosas como:

- “excepto en este modo…”
- “si es este caso raro…”
- “en este modo no aplica casi nada del lifecycle normal…”

entonces probablemente ya no es un modo base. Es un modo especial.

---

## Qué debe poder resolver un modo base configurable

Un modo base debe poder cubrir sin dolor:

- identidad del modo
- flags de uso (ranked, unranked, FFA, events, etc.)
- player types compatibles
- kit base
- layout policy
- rounds to win
- pre-fight delay
- duración máxima
- rules activas
- stats relevantes
- requerimientos de arena

### Ejemplos claros de modo base
- Sword
- Axe
- BuildUHC
- CrystalPvP
- Pot
- SMP
- NetheriteOP
- DiamondSMP
- Mace

#### Por qué
Porque aunque cambien bastante entre sí, siguen viviendo cómodamente dentro del modelo:

- combate base
- kit/rules/settings
- queue → match → result

---

## Qué convierte a un modo en especial

Un modo pasa a ser especial cuando necesita una o más de estas cosas:

### 1. Win condition especial dominante
Ejemplos:
- ringout
- pressure plate instant win como centro del modo
- checkpoint/finish line
- survival placement

### 2. Stats centrales totalmente distintas
Ejemplos:
- Parkour: checkpoints, fails, time
- Dropped: placement, survival, eliminations
- Sumo: ringout wins

### 3. Loop de interacción diferente
Ejemplos:
- Parkour no es combate estándar
- Dropped no es un duel clásico
- Bridge tiene objetivos espaciales y scoring distinto

### 4. Lifecycle diferente del match base
Ejemplos:
- fases que no encajan con duel/series
- scoring no basado en rounds/winner estándar
- reinicios o iteraciones propias del modo

### 5. Necesidad de handlers custom reales
Si el modo requiere comportamiento runtime tan propio que un conjunto de rules no alcanza de forma limpia, entonces debe vivir como especial.

---

## Clasificación recomendada de modos ya mencionados

### Modos base configurables
- Sword
- Axe
- BuildUHC
- CrystalPvP
- Pot
- SMP
- NetheriteOP
- DiamondSMP
- Mace

### Modos especiales
- Sumo
- Boxing
- Bridge
- Dropped
- Parkour

### Por qué esta frontera es sana

Porque evita dos errores clásicos:

#### Error 1
Forzar modos especiales dentro del sistema base hasta volverlo sucio.

#### Error 2
Tratar como especiales modos que en realidad podían resolverse con config/rules y terminás creando código innecesario.

---

## Cómo deben vivir los modos especiales en la arquitectura

La clave NO es hacerlos dolorosos.

La clave es darles un lugar claro.

### La estrategia correcta es esta

- compartir la base común del dominio donde tenga sentido
- permitir un **comportamiento custom enchufable** donde haga falta
- no romper el modelo base para acomodar uno especial

### Traducción estructural recomendada

- `Mode Definition` sigue existiendo
- el modo especial puede marcarse como `specialMode = true` o equivalente conceptual
- además puede tener un `specialBehaviorKey` o un `specialHandlerType`

#### Qué significa eso

El modo sigue teniendo:
- identidad
- flags de uso
- kit base si aplica
- stats relevantes
- compatibilidades

Pero además declara:

> “para ejecutar mi comportamiento completo, necesito este handler especial”

Eso es MUCHO más limpio que mezclar `if (sumo)` por todo el código.

---

## Modelo recomendado para extensión de modos especiales

La mejor solución simple y escalable es:

### 1. Base compartida obligatoria
Todo modo, incluso uno especial, debe seguir entrando al sistema con:

- `Mode Definition`
- flags/contextos
- stats relevantes
- arena requirements
- visibility/menu data

### 2. Punto de extensión específico
Los modos especiales deben poder enchufar:

- `SpecialModeHandler`

o una idea equivalente, responsable de:

- win condition custom
- loop custom
- validaciones especiales
- runtime behavior especial
- stats específicas del modo

### 3. Reglas claras de ownership

#### El modo base compartido sigue siendo dueño de:
- identidad
- compatibilidades
- configuración declarativa

#### El handler especial es dueño de:
- comportamiento excepcional
- reglas del loop que no encajan en el baseline

---

## Qué NO recomiendo hacer

### 1. No meter toda la lógica especial en `Rule System`

Las rules son poderosas, sí.
Pero no deben convertirse en una excusa para simular cualquier cosa rara.

Si una experiencia necesita loop propio, win condition propia y lifecycle distinto, eso ya no es solo rule set.

### 2. No hardcodear todo en listeners/runtime

Porque ahí matás:
- auditabilidad
- extensibilidad
- testabilidad

### 3. No crear una arquitectura de plugins de plugins gigantesca ahora

No hace falta sobreingeniería absurda.

Con una base compartida + un punto claro de extensión por modo especial alcanza PERFECTAMENTE para el estado actual del proyecto.

---

## Ejemplos prácticos

### Sumo

#### Qué reutiliza
- identity
- queue binding
- arena binding
- player state
- profile/history integration

#### Qué pide especial
- win condition por ringout
- stats distintas
- comportamiento de daño/combate diferente al melee base

### Boxing

#### Qué reutiliza
- base de match/queue/arena

#### Qué pide especial
- scoring/combo logic particular
- tratamiento propio del daño/loop de combate

### Bridge

#### Qué reutiliza
- players/teams/arena/runtime

#### Qué pide especial
- objetivo espacial
- scoring distinto
- progression loop diferente

### Parkour

#### Qué reutiliza
- profile/state/event integration
- arena definition

#### Qué pide especial
- nada que ver con duel estándar como loop principal
- checkpoints, tiempos, fallos

### Dropped

#### Qué reutiliza
- event framework
- arena/runtime/profile/history integration

#### Qué pide especial
- eliminación/supervivencia/placement
- reglas de loop propias

---

## Decisiones concretas que tomo

### 1. Los modos base y especiales deben coexistir sobre una arquitectura compartida

Sí.

No quiero dos sistemas paralelos completamente desconectados.

### 2. Los modos especiales deben tener un punto de extensión claro

Sí.

Algo tipo `SpecialModeHandler` es la dirección correcta.

### 3. Las rules NO deben absorber toda la lógica especial

Sí.

Rules sirven para alterar comportamiento. No para fingir cualquier loop raro.

### 4. Crear un modo especial debe ser fácil, no doloroso

Sí.

La meta es:
- declarar el modo
- marcarlo como especial
- asignar handler
- configurar stats/compatibilidades
- listo

### 5. No hay que contaminar el corazón competitivo base con condicionales por modo especial

Sí.

Ese es exactamente el tipo de deuda que queremos evitar.

---

## Conclusión

La frontera correcta es esta:

- **modo base** = todo lo que entra limpio en config + rules + match base
- **modo especial** = lo que requiere comportamiento custom real

Y la solución correcta es:

- base compartida fuerte
- extensión clara y chica
- cero mezcla caótica

Eso cumple con los principios del proyecto:

- simple
- escalable
- extensible
- sin dolor
- sin sobreingeniería innecesaria
