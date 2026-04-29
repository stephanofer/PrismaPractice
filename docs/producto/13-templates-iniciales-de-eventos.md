# Practice Hera — Templates iniciales de eventos hosteados

## Propósito

Cerrar el pendiente de cuáles son los **templates iniciales exactos** con los que debería arrancar el framework de eventos hosteados.

Esto NO significa cerrar el sistema de eventos a solo estos casos.
Significa definir el primer set serio, útil y coherente para:

- retención
- variedad
- prestigio
- testing real
- implementación sin caos

---

## Principios usados para decidir

Los templates iniciales se eligieron con estos criterios:

1. **mitad competitivos, mitad sociales**
   - como ya definimos antes

2. **plantillas predefinidas, no libertad caótica del host**
   - el host configura lo esencial, no todo lo imaginable

3. **reutilización del ecosistema Practice cuando conviene**
   - modos
   - kits
   - rules
   - arenas

4. **no meter demasiados tipos especiales de golpe**
   - porque eventos ya tienen su propio lifecycle y si encima sumamos demasiada variedad al inicio, ensuciamos el desarrollo

5. **cada template debe tener razón de existir**
   - o aporta prestigio competitivo
   - o aporta actividad/retención social

---

## Decisión final recomendada — templates iniciales exactos

### 1. Bracket Tournament

#### Qué es
Evento competitivo de eliminación por rondas/bracket.

#### Por qué entra
- es el template competitivo más importante
- refuerza prestigio
- encaja perfecto con la identidad de Practice
- reutiliza muy bien modos, kits, arenas y match flow

#### Qué debe permitir configurar
- modo
- kit base si aplica por ese modo
- mapa/arena pool
- tamaño máximo de participantes
- formato del bracket si más adelante se expande

#### Qué NO debería abrir todavía
- variantes demasiado raras de bracket desde el día 1

#### Naturaleza
- **template competitivo principal**

---

### 2. Last Man Standing (LMS)

#### Qué es
Evento de eliminación hasta que queda un único jugador vivo.

#### Por qué entra
- es fácil de entender para los jugadores
- tiene buen valor de participación
- genera tensión, actividad y visibilidad social
- sirve muy bien como puente entre competitivo y entretenimiento

#### Qué debe permitir configurar
- modo
- kit
- mapa/arena
- cantidad máxima de jugadores
- condición de inicio

#### Naturaleza
- **template híbrido competitivo/social**

---

### 3. Hosted FFA Session

#### Qué es
Un FFA hosteado por un jugador con permisos/rango, usando modo/kit/mapa controlados.

#### Por qué entra
- es excelente para retención y actividad rápida
- es fácil de hostear y de entender
- reutiliza perfecto las bases de Practice y FFA
- no requiere la rigidez estructural de un torneo

#### Qué debe permitir configurar
- modo
- kit
- mapa/arena
- límite de jugadores
- tiempo máximo si se desea

#### Naturaleza
- **template social/actividad principal**

---

### 4. Parkour Challenge

#### Qué es
Evento de parkour hosteado bajo una plantilla controlada.

#### Por qué entra
- aporta variedad fuerte sin depender del loop PvP puro
- ayuda a retención de jugadores que no quieren estar peleando todo el tiempo
- genera actividad social y descanso del loop competitivo duro

#### Qué debe permitir configurar
- mapa
- límite de jugadores
- timer si aplica
- checkpoints/rules básicas del template

#### Naturaleza
- **template social/variedad**

---

### 5. Dropped Event

#### Qué es
Evento especial de tipo Dropped bajo template predefinido.

#### Por qué entra
- ya fue mencionado explícitamente desde la visión inicial
- aporta identidad distinta
- suma variedad social/competitiva según se ejecute

#### Qué debe permitir configurar
- mapa
- capacidad
- reglas básicas del template

#### Naturaleza
- **template especial inicial**

---

## Qué NO recomiendo meter todavía como template inicial oficial

### 1. Group Stage Tournament complejo

#### Por qué no todavía
- tiene mucho valor, sí
- pero agrega mucha complejidad estructural demasiado pronto
- bracket simple ya cubre el núcleo competitivo inicial

### 2. Templates totalmente custom del host

#### Por qué no todavía
- rompe control de calidad
- aumenta edge cases
- contradice la filosofía de plantillas predefinidas

### 3. Variantes sociales demasiado exóticas

Si no aportan valor claro desde el día 1, mejor dejarlas para expansión.

---

## Cómo debería pensar el sistema estos templates

La forma correcta es:

- `Event Template = contrato predefinido`
- `Hosted Event = instancia activa creada desde ese template`

### Ejemplo
- `Bracket Tournament` = template
- “Stephanofer Cup #1” = hosted event real creado desde ese template

Esto mantiene el sistema limpio y escalable.

---

## Qué debería poder tocar el host en estos templates

### Configurable en lo esencial
- nombre visible del evento si se quiere permitir
- modo
- kit
- mapa/arena
- capacidad máxima
- iniciar
- cancelar
- cerrar entradas

### No configurable libremente todavía
- estructura interna profunda del template
- reglas locas incompatibles
- combinaciones que rompan coherencia

---

## Decisiones concretas que tomo

### 1. Bracket Tournament sí o sí debe ser template inicial

Sí.

Es el template que más valor competitivo y aspiracional aporta.

### 2. Hosted FFA también debe entrar sí o sí

Sí.

Es el mejor template social/rápido para mantener actividad.

### 3. Parkour y Dropped deben entrar como templates iniciales de variedad

Sí.

Ya estaban en la visión del producto y ayudan a que eventos no se sientan solo como “torneos PvP con otro nombre”.

### 4. LMS debe entrar como template intermedio entre competitivo y social

Sí.

Da tensión, claridad y participación masiva sin sobrediseño.

### 5. Group-stage y variantes más pesadas quedan para expansión

Sí.

No las rechazo; solo no conviene meterlas en el primer bloque.

---

## Lista final de templates iniciales recomendados

- Bracket Tournament
- Last Man Standing (LMS)
- Hosted FFA Session
- Parkour Challenge
- Dropped Event

---

## Conclusión

El primer bloque correcto de templates de eventos debe demostrar dos cosas:

1. que el framework sirve para prestigio competitivo
2. que también sirve para actividad y retención social

Por eso la mezcla inicial correcta queda en:

- Bracket Tournament
- LMS
- Hosted FFA Session
- Parkour Challenge
- Dropped Event
