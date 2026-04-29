# Practice Hera — Sistema de creación in-game y contenido semilla

## Corrección conceptual importante

Antes habíamos hablado de “catálogo inicial de modos de salida”.

Con la nueva aclaración del proyecto, la forma correcta de pensarlo cambia:

- **modos, kits, queues y arenas deben poder crearse y expandirse in-game**
- por lo tanto, NO conviene pensar el sistema como si dependiera de un catálogo cerrado hardcodeado

La separación correcta pasa a ser esta:

1. **capacidad del sistema para crear y administrar contenido in-game**
2. **contenido semilla inicial** para arrancar el servidor con algo funcional y testeable

---

## Decisión fuerte

El sistema debe estar diseñado para que el cliente pueda crear, editar, activar, desactivar y expandir sin tocar código:

- modos
- kits
- queues
- arenas

Y cuando haga sentido más adelante:

- event templates
- variantes especiales

### Por qué esta decisión es correcta

Porque el objetivo del proyecto no es solo entregar un conjunto fijo de ladders.

El objetivo es entregar una **base operable y administrable** que permita:

- iterar gameplay
- probar combinaciones nuevas
- activar/desactivar contenido
- crecer sin pedir refactors de código por cada cambio del cliente

---

## Qué significa “contenido semilla”

Contenido semilla NO significa:

- catálogo fijo del sistema
- límite arquitectónico
- única lista válida de modos

Contenido semilla sí significa:

- primeros datos precargados para que el servidor arranque funcional
- set base para testing, demo y bootstrap
- punto de partida administrable

### Traducción simple

El sistema debe soportar mucho más.
Pero conviene arrancar con un primer set cargado para no empezar desde vacío absoluto.

---

## Sistema de creación in-game — alcance recomendado

La recomendación correcta es que exista una capacidad administrativa in-game para:

### 1. Modos
- crear modo
- editar modo
- activar/desactivar modo
- definir flags de uso
- definir player types compatibles
- definir match defaults
- definir rules por defecto
- definir stats relevantes
- vincular kit base
- vincular requerimientos de arena

### 2. Kits
- crear kit base
- editar contenido del kit
- definir si el layout es editable
- definir restricciones de layout
- activar/desactivar kit

### 3. Queues
- crear queue
- elegir modo asociado
- elegir tipo (`RANKED` / `UNRANKED`)
- elegir player type
- definir política de matchmaking
- definir impacto competitivo
- activar/desactivar queue
- controlar visibilidad en menú

### 4. Arenas
- crear arena
- editar arena
- definir `identifier`, `displayName`, `icon`
- definir `universal region`
- definir `battle region`
- definir posiciones/spawns
- definir compatibilidades
- activar/desactivar arena

---

## Regla de diseño importante

Crear contenido in-game NO significa libertad caótica.

La arquitectura correcta sigue siendo:

- estructura fuerte del dominio
- validaciones claras
- compatibilidades declarativas
- protección contra configuraciones inválidas

### Ejemplos de validaciones que el sistema debe hacer

- no permitir queue ranked sobre modo no compatible con ranked
- no permitir arena incompatible con el player type del modo/queue
- no permitir rules conflictivas sin resolución
- no permitir activar queue sin modo válido
- no permitir modo sin kit base cuando el contexto lo requiera

---

## Qué NO recomiendo

### 1. No recomiendo hardcodear el catálogo de contenido

Eso iría contra el objetivo del proyecto.

### 2. No recomiendo dejar la creación sin restricciones

Porque el cliente necesita poder crear contenido, pero el sistema tiene que proteger coherencia y calidad.

### 3. No recomiendo que el runtime Paper/Velocity sea el dueño de las definiciones

Las definiciones deben seguir viviendo en:

- dominio compartido
- persistencia/config persistente

Los runtimes solo consumen ese contenido.

---

## Contenido semilla inicial recomendado

Aunque el sistema sea expansible in-game, conviene arrancar con contenido semilla inicial.

### Seed recomendado mínimo

#### Modos base iniciales recomendados
- Sword
- Axe
- BuildUHC
- CrystalPvP
- Pot
- SMP

#### Qué representa este seed
- núcleo competitivo suficiente
- variedad real
- base buena para testing
- set demostrable para el cliente

#### Importante
Este seed NO limita lo que el sistema puede soportar después.

---

## Decisiones concretas que tomo

### 1. El sistema debe soportar creación y expansión in-game de contenido

Sí.

### 2. “Catálogo inicial” no debe tratarse como límite del sistema

Sí.

La forma correcta es hablar de contenido semilla inicial.

### 3. El contenido semilla inicial sigue siendo útil

Sí.

Porque bootstrapear desde cero absoluto también mete fricción innecesaria.

### 4. Toda creación in-game debe pasar por validaciones fuertes

Sí.

Flexibilidad no significa caos.

---

## Conclusión

La decisión correcta para Practice es:

- sistema expansible y administrable in-game
- contenido semilla inicial para arrancar bien
- validación fuerte para proteger coherencia

Eso respeta perfectamente los principios del proyecto:

- flexibilidad real
- arquitectura limpia
- cero hardcodeo innecesario
- escalabilidad sin dolor
