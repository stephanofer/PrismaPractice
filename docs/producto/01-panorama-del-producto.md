# Practice Hera — Panorama del producto

## Nota

Este documento captura, sin omitir ideas funcionales importantes, la visión inicial expresada para el producto Practice de Hera. Esto es una base viva: se va a seguir refinando fase por fase.

---

## Identidad inicial del producto

Practice NO va a ser un simple practice mínimo.

Va a ser un sistema competitivo PvP muy fuerte, con corazón en:

- **Ranked**
- **Unranked**
- **matchmaking impecable**
- **SR (Skill Rating) + rangos**
- **leaderboards y clasificaciones**
- **múltiples kits y modos**
- **múltiples arenas**
- **warmup sólido**

Pero además va a expandirse a un ecosistema PvP/social más grande con:

- **FFA**
- **parties**
- **party duels**
- **historial de partidas**
- **perfiles públicos**
- **progresión social**
- **eventos hosteados por jugadores con rango**
- **friends**
- **settings avanzados por usuario**

---

## Núcleo competitivo

### Ranked y Unranked

El producto va a tener:

- colas **Ranked**
- colas **Unranked**

### Lógica de rondas

Los matches pueden tener lógica de rondas.

Ejemplo:

- mejor de 3
- mejor de X rondas según modo o kit

Importante:

- no todas las partidas necesariamente tienen que funcionar igual
- según el modo o el kit, el formato puede cambiar

Esto implica que el dominio de match debe contemplar desde el inicio:

- formatos de serie
- conteo de rondas ganadas
- condición de victoria por serie
- variantes por modo o kit

---

## FFA

FFA existe como espacio para que los usuarios:

- entren
- maten el tiempo
- jueguen grupalmente

Además va a haber:

- varios modos de FFA
- varios maps
- selector de modos

Ejemplos dados:

- BuildUHC FFA
- CrystalPvP FFA

Esto implica que FFA no es una sola sala genérica, sino un conjunto de experiencias PvP por modo.

---

## Leaderboards y clasificaciones

Esto fue marcado como **core del Practice**.

Por lo tanto el proyecto debe contemplar desde temprano:

- leaderboards
- clasificaciones
- rank displays
- quizás top global y top por modo/kit
- actualización frecuente de estadísticas competitivas

Esto NO puede ser un agregado tardío. Tiene que influir en la arquitectura desde temprano.

---

## Perfiles públicos y progresión social

También va a haber:

- perfiles públicos
- progresión social

Esto sugiere al menos:

- perfil consultable
- visualización de stats/rank
- identidad competitiva del jugador
- capa social más allá de solo entrar a pelear

---

## Settings por usuario

Va a haber una sección de settings por usuario.

Ejemplos mencionados:

- activar o desactivar el chat
- activar o desactivar que te puedan enviar duels
- activar o desactivar recibir solo duel requests de amigos
- activar o desactivar el envío de solicitudes de friends
- activar o desactivar si los jugadores pueden spectear tus partidas
- activar o desactivar ver jugadores en el lobby
- activar o desactivar que te puedan enviar solicitud de party
- activar o desactivar alertas de torneos o “hosteo de eventos”
- activar o desactivar el scoreboard

### Ping Range para matchmaking

También se quiere un setting de **Ping Range** para limitar el matchmaking a oponentes con ping dentro del rango seleccionado.

Opciones mencionadas:

- Within 25ms of you
- Within 50ms of you
- Within 75ms of you
- Within 100ms of you
- Within 125ms of you
- Within 150ms of you
- Within 200ms of you
- Within 250ms of you
- No Limit

Esto es relevante porque afecta directamente al diseño del matchmaking.

---

## Matchmaking

Se dejó explícitamente como requisito crítico:

> Debemos tener el mejor, moderno, optimizado, impecable, eficiente sistema de matchmaking.

Esto significa que el matchmaking no puede verse solo como “sacar dos jugadores de una cola”.

Va a tener que considerar, probablemente:

- SR/rating
- kit o modo
- región
- ping range
- tiempos de cola
- calidad de match
- fairness vs rapidez

Este punto es de arquitectura core.

### Decisión inicial de diseño

La recomendación base para Practice competitivo es esta:

- **Ranked = quality first, speed second**
- **Unranked = speed first, quality second**
- el sistema debe usar **matching por score de calidad**, no un simple “primer rival disponible”
- las restricciones deben **relajarse progresivamente por tiempo en cola**, pero sin romper reglas duras importantes

### Reglas duras iniciales

Para considerar una pareja válida, ambos jugadores deben cumplir al menos:

- misma cola / mismo modo
- mismo tipo de enfrentamiento compatible
- no estar bloqueados por otro estado del sistema
- existir al menos una **región común jugable** para ambos
- respetar el **Ping Range** configurado por ambos jugadores

### Ping Range como preferencia real del usuario

El setting de Ping Range debe tratarse como una preferencia fuerte del usuario.

Eso significa que:

- si un jugador elige un rango de ping estricto, el sistema no debería ignorarlo silenciosamente
- si la cola no encuentra rival por esa razón, es mejor informar que sugeriría ampliar el rango antes que hacer un match injusto sin avisar

### Selección de región

La región del match no debería ser simplemente fija por defecto si el sistema puede elegir mejor.

La lógica recomendada es:

- evaluar las regiones posibles para ambos jugadores
- elegir la **mejor región común**
- priorizar la menor combinación de:
  - ping máximo entre ambos
  - diferencia de ping entre ambos
  - estabilidad/calidad esperada del match

### Ranked matchmaking — política recomendada

En Ranked 1v1, la prioridad debe ser:

1. calidad competitiva del rival
2. calidad de conexión
3. tiempo de espera razonable

#### Expansión progresiva recomendada para Ranked

Los valores exactos se pueden ajustar luego, pero la lógica inicial recomendada es:

- **0-15s**
  - rival muy cercano en SR
  - mejor región común disponible
  - match de máxima calidad posible

- **15-30s**
  - ampliar un poco el rango de SR
  - permitir más regiones comunes viables

- **30-45s**
  - ampliar más el rango de SR
  - seguir priorizando fairness por ping

- **45-60s**
  - permitir una búsqueda claramente más amplia
  - mantener todavía límites sanos de calidad

- **60s+**
  - búsqueda amplia, pero sin generar matches absurdos
  - no romper preferencias fuertes ni crear partidas evidentemente malas

### Unranked matchmaking — política recomendada

En Unranked, la prioridad debe ser:

1. rapidez saludable
2. match suficientemente justo
3. conexión aceptable

La recomendación es que Unranked use el SR del modo como referencia de habilidad cuando exista, pero con tolerancia más amplia que Ranked.

Eso permite:

- evitar emparejamientos totalmente rotos
- no convertir Unranked en una cola lenta e hiperestricta

### Por qué esta decisión es la mejor base

Porque un Practice competitivo se arruina por dos extremos:

- **si priorizás solo velocidad**, el ranked se siente injusto y amateur
- **si priorizás solo perfección**, las colas se vuelven lentas y frustrantes

La solución correcta es:

- empezar estricto
- relajar inteligentemente
- no traicionar settings importantes del jugador
- separar claramente el comportamiento de Ranked y Unranked

---

## Estadísticas globales en tiempo real

También se quieren estadísticas operativas y visibles que se actualicen constantemente.

Ejemplos mencionados:

- cuántas personas están en el hub de practice
- cuántas están en partida
- cuántas están en FFA
- cuántas están en cola
- y muchas estadísticas más que se van actualizando a cada momento

Esto implica desde temprano:

- presencia global
- proyecciones o contadores hot-state
- actualización en tiempo real o casi real

---

## Conclusión actual de producto

La visión inicial combina tres capas:

1. **Competitive Practice core**
   - ranked
   - unranked
   - rounds / series
   - SR + rangos
   - matchmaking premium
   - leaderboards

2. **PvP activity layer**
   - FFA
   - múltiples modos
   - múltiples kits
   - múltiples arenas
   - historial de peleas
   - layouts de kits

3. **Social + retention layer**
   - parties
   - party duels
   - friends
   - perfiles públicos
   - settings por usuario
   - eventos hosteados por jugadores con rango
   - progresión social

En resumen: el producto no es un plugin chico. Es una **plataforma Practice PvP competitiva y social**, con foco fuerte en calidad de matchmaking, ranking y experiencia del jugador.

---

## Modelo recomendado de primer login del jugador

### Decisión general

La mejor decisión para este proyecto es un modelo **híbrido**:

- **perfil base + settings + contexto mínimo** creados en el primer login
- **datos específicos por modo/sistema** creados de forma **lazy** cuando realmente se usan

### Por qué esta es la mejor decisión

Porque el proyecto quiere:

- mucha flexibilidad en modos
- posibilidad de agregar ladders nuevas sin dolor
- buena arquitectura
- evitar estructuras vacías innecesarias

Si creáramos SR, stats y layouts para todos los modos desde el primer login, acoplaríamos el bootstrap del jugador al catálogo completo de modos. Eso es mala base para un sistema que justamente quiere poder crecer y cambiar modos con facilidad.

### Qué debe existir sí o sí desde el primer login

#### 1. Practice Player Profile base

Debe persistirse un perfil base del jugador con al menos:

- uuid
- nombre actual
- nombre normalizado si hace falta para búsqueda
- first seen
- last seen
- metadata básica del perfil

#### 2. Settings por defecto

El jugador debe nacer con settings completas por defecto, al menos para:

- chat enabled = **ON**
- duel requests enabled = **ON**
- friends-only duels enabled = **OFF**
- friend requests enabled = **ON**
- spectate enabled = **ON**
- lobby players visible = **ON**
- party requests enabled = **ON**
- event alerts enabled = **ON**
- scoreboard enabled = **ON**
- ping range = **Within 100ms of you**

Esto evita ambigüedad y hace que el jugador ya tenga una configuración consistente desde su primer ingreso.

### Por qué el Ping Range por defecto debe ser Within 100ms of you

La recomendación inicial es **Within 100ms of you** porque es el mejor equilibrio entre:

- calidad de match
- tiempos de cola razonables
- competitividad sana

#### Por qué no `No Limit`

Porque como default degrada demasiado la calidad competitiva y puede generar primeras experiencias injustas o muy inconsistentes.

#### Por qué no 25ms o 50ms

Porque como default puede ser demasiado estricto para mucha gente y romper innecesariamente la velocidad de cola.

#### Por qué 100ms sí

Porque como punto de partida:

- protege mejor la experiencia competitiva
- sigue siendo razonable para matchmaking inicial
- permite que el jugador estricto baje el rango manualmente
- permite que el jugador más flexible lo amplíe si quiere

#### 3. Contexto de season base

Como habrá seasons desde la primera gran fase, el perfil del jugador debe poder quedar asociado a la season actual, aunque sus datos competitivos específicos nazcan más tarde.

#### 4. Presencia / sesión actual

En hot-state (Redis o equivalente) debe existir el estado operativo actual del jugador, por ejemplo:

- online/offline
- runtime actual
- server actual
- estado general (hub, queue, match, ffa, etc.)

### Qué debe crearse lazy cuando se use

#### 1. SR por modo

El SR por modo debe crearse cuando el jugador use competitivamente ese modo por primera vez.

No conviene crear SR para todos los modos apenas entra, porque:

- hay muchos modos
- pueden aparecer modos nuevos
- ranked es por modo
- no todos los jugadores tocarán todos los ladders

#### 2. Rango por modo

El rango por modo nace naturalmente junto al SR de ese modo.

#### 3. Stats por modo/sistema

Las estadísticas específicas deben nacer cuando el jugador participa en ese sistema:

- ranked stats del modo X
- unranked stats del modo X
- FFA stats
- event stats
- social stats específicas si aplican

#### 4. Layouts de kits

Los layouts deben crearse cuando el jugador use o edite ese kit por primera vez.

Esto tiene mucho más sentido que crear layouts vacíos para todos los modos desde el primer ingreso.

#### 5. Datos específicos de eventos / FFA / sistemas secundarios

Todo registro ultra específico de eventos, FFA público o mecánicas secundarias debe aparecer cuando el jugador realmente interactúe con esas features.

### Qué NO debería persistirse como perfil pesado inicial

No conviene crear desde el primer login:

- SR de todos los modos
- stats de todos los modos
- layouts de todos los kits
- registros vacíos de cada sistema posible
- party state persistente sin uso real

### Conclusión arquitectónica

La decisión correcta para Practice es:

- **perfil base temprano**
- **defaults claros desde el primer ingreso**
- **datos competitivos y específicos creados bajo demanda**

Eso mantiene la arquitectura limpia, flexible y preparada para crecer sin inflar la base de datos con ruido.

---

## Flujo principal ideal del jugador

Se definió un flujo principal muy claro para el uso más frecuente del producto.

### Entrada al Hub

Cuando el jugador entra a Practice, tendrá aproximadamente **7 items principales** en el inventario/hotbar del lobby.

Ejemplo de distribución mencionado:

- **Slot 1** → item de **Queue Unranked**
- **Slot 2** → item de **Queue Ranked**
- **Slot 3** → item para abrir menú de **FFA** y seleccionar modo / ir al servidor FFA
- **Slot 5** → item para **crear una party**
- **Slot 7** → item de **Leaderboards**
- **Slot 8** → item de **Cosméticos**
- **Slot 9** → menú general con acciones como:
  - estadísticas
  - settings
  - match history
  - kit editor

### Flujo principal competitivo

El flujo principal esperado es:

1. el jugador entra a Practice
2. llega al hub con sus items principales
3. elige **Ranked** o **Unranked**
4. se abre un menú con los diferentes modos/kits
5. selecciona, por ejemplo, **Sword**
6. entra a la cola
7. se ejecuta la lógica de matchmaking
8. el matchmaking considera factores que después se definirán mejor, como:
   - ping configurado por el usuario
   - rangos/SR
   - otros criterios del sistema
9. cuando encuentra match, el jugador es enviado al servidor de **Match** con todos los datos necesarios
10. se juega la partida, por ejemplo un **mejor de 3**
11. se determina el ganador
12. se entrega el link/resumen del match
13. se guardan los datos del combate
14. el jugador vuelve al hub
15. puede volver a jugar inmediatamente

### Otros flujos secundarios importantes

Además del flujo principal competitivo, el jugador también puede:

- ir al **FFA**
- hacer **party** con amigos u otros jugadores
- jugar **party duels**
- participar en **torneos/eventos hosteados por usuarios**
- revisar estadísticas, historial y settings

### Conclusión del flujo

Esto confirma algo MUY importante para la arquitectura:

- el **Hub** es la puerta central del sistema
- **Ranked/Unranked + matchmaking + match transfer** son el flujo principal más crítico
- FFA, social y eventos son importantes, pero orbitan alrededor de esa entrada principal
