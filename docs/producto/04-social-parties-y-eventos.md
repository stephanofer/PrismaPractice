# Practice Hera — Social, parties y eventos

## Parties y Party Duels

Sí, se van a poder:

- crear parties
- jugar entre party
- hacer party duels

Esto sugiere más adelante:

- party state
- party invites
- party privacy/rules
- party queueing o challenges

---

## Eventos hosteados por jugadores con rango

Va a existir un apartado de **eventos** creados por jugadores, específicamente jugadores con rango de paga.

### Idea base

Un jugador con rango puede:

- hostear
- crear o activar un evento

Entonces:

- se inicia el evento
- todos los jugadores pueden entrar
- los jugadores pueden unirse y jugar

### Variantes mencionadas

Los eventos pueden tener:

- diferentes mapas
- diferentes modos
- diferentes kits

Muchos kits probablemente van a ser los mismos de ranked y unranked, pero aplicados a modos distintos.

### Ejemplos de eventos mencionados

- modo torneo
  - se enfrentan 2
  - el mejor pasa
  - luego otros 2
  - luego otros 2
  - puede avanzar por eliminación
- modo por grupos
- gana el último usuario
- gana el último equipo
- parkour
- dropped

Esto implica algo MUY importante: “eventos” probablemente sea un subdominio flexible, no una sola feature pequeña.

---

## Sistema de friends

También va a existir un sistema de friends, descrito como:

- ultra intuitivo
- super bonito

### Nota de ownership final

La experiencia de Friends sigue existiendo para el jugador, pero su ownership ya NO pertenece al desarrollo interno de Practice.

Friends queda como sistema social externo de red y su definición final está en:

- `docs/producto/16-friends-como-sistema-externo.md`

### Comandos mencionados

- `/friend add (player)` → send or accept a friend request
- `/friend remove (player)` → remove a friend
- `/friend request` → view pending friends request
- `/friends` → open friends menu

Esto implica:

- solicitudes pendientes
- aceptar/rechazar
- lista de amigos
- integración con menú
- posiblemente online status y acciones rápidas

### Decisión arquitectónica final para Friends

Friends queda externalizado respecto de Practice:

- existe para la UX del jugador
- Practice puede consultarlo e integrarlo
- pero su desarrollo y ownership viven en un sistema social de red aparte

Ver:

- `docs/producto/16-friends-como-sistema-externo.md`

### Recomendación de comportamiento inicial para requests sociales

#### Friend requests

- expiran luego de un tiempo razonable
- si ambos jugadores se envían request mutuamente, se puede autoaceptar
- no deberían crearse duplicados absurdos
- deben funcionar cross-server

#### Duel requests

- deben respetar settings del receptor
- si el jugador está en match, cola incompatible o estado no interactuable, deben bloquearse o rechazarse claramente
- conviene expiración corta

#### Party invites

- deben respetar settings del receptor
- conviene expiración razonable
- deben poder funcionar cross-server si la UX final lo requiere

### Principio general de retención

Toda interacción social debe ser:

- rápida
- visible
- clara
- sin spam
- respetando settings del usuario

Porque un sistema social bueno aumenta retención; uno ruidoso o confuso la destruye.

### Modelo recomendado de requests sociales

La recomendación inicial para Friends, Duels y Party Invites es diseñarlos con la misma filosofía:

- UX rápida
- comportamiento predecible
- respeto total a settings
- protección anti-spam
- funcionamiento cross-server cuando aplique

---

## Friend Requests — comportamiento recomendado

### Comando base ya definido

- `/friend add (player)` → send or accept a friend request
- `/friend remove (player)` → remove a friend
- `/friend request` → view pending friends request
- `/friends` → open friends menu

### Reglas recomendadas

#### 1. Autoaccept por solicitud recíproca

Si A envía request a B, y B luego usa `/friend add A`, la solicitud debe:

- **autoaceptarse**
- crear la relación de amistad directamente
- limpiar requests pendientes relacionadas

Esto reduce fricción y hace que el sistema se sienta inteligente.

#### 2. Expiración

La friend request debe expirar automáticamente luego de un tiempo razonable.

Recomendación inicial:

- **5 minutos**

Por qué:

- suficiente para responder sin apuro
- no deja requests zombies eternas
- evita ruido en menús y estado social

#### 3. No duplicados

No debe existir spam de múltiples solicitudes iguales entre los mismos dos jugadores.

Si ya hay una pendiente:

- no se crea otra
- se informa que ya existe una request activa

#### 4. Restricciones lógicas

No se debe permitir:

- enviarse request a uno mismo
- enviar request a alguien que ya es amigo
- enviar request a quien te tiene bloqueado por settings

#### 5. Cross-server

Las friend requests deben funcionar aunque ambos jugadores estén en distintos servidores de la network.

#### 6. Anti-spam

Recomendación inicial:

- **cooldown de 3 segundos** por target para reenviar acción social del mismo tipo

Esto no debe sentirse pesado, solo evitar abuso y flood.

---

## Duel Requests — comportamiento recomendado

### Filosofía

Las duel requests son interacciones rápidas de gameplay. Deben ser más ágiles que Friends, pero también más estrictas en estados válidos.

### Reglas recomendadas

#### 1. Expiración corta

Recomendación inicial:

- **30 segundos**

Por qué:

- si alguien quiere duelar, la intención es inmediata
- una request vieja pierde sentido muy rápido

#### 2. Respeto total a settings

La request debe bloquearse si el receptor tiene:

- duel requests desactivadas
- only friends duels activado y el emisor no es amigo

#### 3. Estados no interactuables

No debe poder enviarse o aceptarse duel si el emisor o receptor está en estado incompatible, por ejemplo:

- dentro de match
- en countdown/pre-fight irreversible
- en cola incompatible o bloqueada
- en evento que no permita interacción
- en transición entre servidores/estados

#### 4. No duplicados

Si ya existe una duel request activa entre los mismos jugadores para el mismo contexto:

- no crear otra
- informar claramente que ya existe

#### 5. Cooldown anti-spam

Recomendación inicial:

- **3 segundos** por target

#### 6. Contexto claro de la invitación

La duel request debe incluir claramente:

- modo/kit
- tipo de duelo si aplica
- formato relevante si aplica

Así la invitación no es ambigua.

---

## Party Invites — comportamiento recomendado

### Filosofía

Party Invites deben favorecer juego social y retención, así que conviene que sean cómodas, pero sin spam.

### Reglas recomendadas

#### 1. Expiración

Recomendación inicial:

- **60 segundos**

Por qué:

- más generosa que duel request
- suficiente para coordinarse socialmente

#### 2. Respeto total a settings

La invite debe bloquearse si el receptor tiene party requests desactivadas.

#### 3. Restricciones lógicas

No debe poder invitarse si:

- el target ya está en una party incompatible
- el emisor no tiene permisos/estado para invitar
- el jugador está en un estado no interactuable severo

#### 4. No duplicados

Si ya existe una invite activa de la misma party al mismo jugador:

- no duplicarla

#### 5. Cooldown anti-spam

Recomendación inicial:

- **3 segundos** por target

#### 6. Cross-server

La invite debería poder funcionar cross-server siempre que la network pueda resolver correctamente la aceptación y posterior reunión del grupo.

---

## Prioridad y limpieza de requests

### Recomendación general

- una request aceptada limpia su pendiente
- una request expirada desaparece sola
- una request invalidada por cambio de estado debe cancelarse limpiamente

Ejemplos:

- si aceptás friend request, ya no queda pendiente
- si entrás a match, una duel request vieja incompatible debe invalidarse
- si te unís a otra party, invites viejas incompatibles deben cancelarse

---

## Estados y restricción social recomendada

### Estados sociales/interactuables

Conviene pensar al jugador con una capacidad de interacción social según su estado global.

Ejemplo conceptual:

- **Hub / Lobby** → interacción total
- **FFA público** → interacción parcial
- **En cola** → interacción parcial según acción
- **En match** → interacción muy restringida
- **En evento** → depende del tipo de evento
- **Transfiriéndose** → interacción bloqueada

Esto evita lógica caótica repartida por todos lados.

---

## Decisión fuerte de UX

La mejor UX para retención es esta:

- comandos rápidos
- menú bonito
- requests visibles
- expiraciones cortas y coherentes
- autoaccept cuando tiene sentido
- cero spam innecesario
- feedback claro sobre por qué algo fue rechazado

Así Friends, Duels y Parties se sienten premium en vez de molestos.

---

## Sistema de Party — modelo recomendado

### Filosofía

La Party debe ser una herramienta **social-operativa rápida** para jugar juntos, no una mini-guild persistente.

### Estructura

Se definió:

- **un solo líder**
- el liderazgo debe poder **transferirse fácilmente**
- no hace falta sistema de roles adicional en esta primera gran fase

### Tamaño máximo

El tamaño máximo debe ser:

- **configurable**
- y además ampliable por permisos/rangos de donación

Esto permite monetización sin romper la arquitectura base.

### Acciones que la Party debe soportar

La recomendación inicial es que sí o sí pueda:

- invitar jugadores
- aceptar/rechazar invites
- expulsar miembros
- abandonar party
- transferir liderazgo
- abrir menú de party
- chat de party
- ver miembros y su estado/servidor actual
- iniciar party duels
- iniciar party FFA
- entrar juntos a experiencias grupales compatibles

### Qué pasa si el líder se desconecta

La mejor decisión NO es disolver siempre la party ni pasarla aleatoriamente.

La decisión correcta es:

- transferir liderazgo automáticamente al **miembro elegible más antiguo de la party que siga online**
- si no queda nadie online, la party se disuelve

#### Por qué

- disolver siempre genera fricción innecesaria
- asignar aleatoriamente es mala UX y puede sentirse arbitrario
- usar antigüedad online dentro de la party es predecible, simple y justa

### Restricciones por estar en Party

Sí: estar en party debe bloquear o modificar ciertas acciones individuales.

Pero OJO: esto debe ser **configurable por feature** y no una regla rígida hardcodeada para todo.

Ejemplos de restricciones configurables:

- entrar solo a ranked
- entrar solo a ciertas colas
- ir a ciertos modos FFA
- aceptar ciertas invitaciones incompatibles
- iniciar ciertas acciones individuales mientras la party exista

Esto permite adaptar la UX según el tipo de experiencia.

### Principio general

Si una acción rompe la coherencia del grupo, debe bloquearse o pedir salir de la party primero.

### Conclusión arquitectónica

La Party debe ser:

- simple en roles
- flexible en capacidad
- fuerte en UX grupal
- configurable en restricciones
- preparada para coordinar experiencias sociales sin meter complejidad innecesaria

---

## Eventos hosteados por jugadores — modelo recomendado

### Filosofía general

La decisión correcta es tratarlos como un **framework base de eventos** con:

- plantillas predefinidas
- configuración esencial controlada
- posibilidad real de escalar nuevos tipos de eventos sin dolor

No conviene construir un sistema aislado y rígido para cada evento. Conviene una base común y luego casos especiales solo cuando rompan ese modelo.

### Tipos iniciales de eventos

Se quieren contemplar inicialmente, como mínimo:

- torneo bracket
- FFA hosteado
- parkour
- dropped

Y el sistema debe quedar preparado para sumar nuevos tipos después con el menor dolor posible.

### Nivel de libertad del host

El host debe elegir sobre **plantillas predefinidas**.

Eso es lo correcto por producto y por arquitectura, porque:

- evita configuraciones absurdas o rotas
- simplifica UX
- mantiene control de calidad
- reduce bugs y edge cases

### Qué puede configurar el host

El host debe poder tocar lo esencial, siempre que ese tipo de evento lo soporte, por ejemplo:

- iniciar evento
- cancelar evento
- elegir mapa
- elegir modo
- elegir kit
- definir opciones básicas relevantes del evento

La regla correcta es:

- **configurable en lo esencial**
- **cerrado en lo peligroso o innecesario**

### Recomendación estratégica para retención

La mejor decisión es que los eventos sean **mitad competitivos y mitad sociales**, pero con un enfoque operacional claro:

- **el sistema base de eventos debe servir a la retención social**
- **algunos templates deben reforzar el prestigio competitivo**

#### Traducción práctica

- torneos/brackets → refuerzan prestigio, competición y status
- FFA hosteado / dropped / parkour → refuerzan actividad, variedad y tiempo dentro de la network

### Por qué esta mezcla es la correcta

Porque si hacés eventos solo competitivos:

- reducís participación casual
- dependés demasiado de skill gap y presión competitiva

Y si hacés eventos solo sociales:

- perdés prestigio
- bajás el valor aspiracional del ecosistema Practice

La mezcla correcta mantiene:

- actividad
- variedad
- prestigio
- retención

### Conclusión arquitectónica

La base correcta para eventos es:

- **framework de eventos**
- **templates predefinidos**
- **configuración esencial por tipo**
- **crecimiento fácil a nuevos eventos**
- mezcla de valor **competitivo + social**
