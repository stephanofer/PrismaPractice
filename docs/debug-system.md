# PrismaPractice Debug System

## Objetivo

Tener un debug **centralizado, barato y útil** para encontrar bugs reales sin llenar consola con ruido.

El sistema combina:

- **eventos estructurados**
- **buffer en memoria** de eventos recientes
- **watchers temporales** por jugador/match/trace/categoría
- **snapshots operativos** desde comando
- **niveles por categoría** configurables

---

## Dónde vive

- módulo compartido: `prismapractice-debug`
- config runtime: `debug.yml`
- comandos Paper: `/prismapractice debug ...`

---

## Comandos

- `/prismapractice debug status`
  - estado general del sistema de debug
- `/prismapractice debug runtime`
  - snapshot rápido de runtime + MySQL + Redis
- `/prismapractice debug recent [limit]`
  - últimos eventos del buffer
- `/prismapractice debug player <player>`
  - snapshot de jugador online + eventos recientes vinculados
- `/prismapractice debug watch player <player> [minutes]`
- `/prismapractice debug watch match <matchId> [minutes]`
- `/prismapractice debug watch trace <traceId> [minutes]`
- `/prismapractice debug watch category <category> [minutes]`
- `/prismapractice debug watch clear`
- `/prismapractice debug category <category> <off|basic|verbose|trace>`
  - override runtime en caliente

Permiso: `prismapractice.admin.debug`

---

## Uso recomendado

### Bug de un jugador

1. `/prismapractice debug watch player <player>`
2. reproducí el problema
3. `/prismapractice debug player <player>`
4. `/prismapractice debug recent 20`

### Sospecha de categoría puntual

1. `/prismapractice debug watch category state`
2. reproducí el flujo
3. revisá `recent`

### Necesidad operativa rápida

- `/prismapractice debug runtime`

---

## Convención para futuras features

Usar siempre el `DebugController` del runtime y mantener estos patrones:

- `*.requested`
- `*.completed`
- `*.rejected`
- `*.failed`
- `*.slow`

Categorías ya base:

- `bootstrap`
- `command`
- `profile`
- `player.lifecycle`
- `state`
- `permission`
- `queue`
- `match`
- `storage.mysql`
- `storage.redis`
- `redis.connection`
- `scoreboard`
- `ui`
- `reload`

Regla: si una feature nueva no entra en una categoría existente, agregá una nueva **sólo si realmente hace falta**.

---

## Cómo instrumentar bien

### Sí

- adjuntar contexto: `playerId`, `playerName`, `matchId`, `traceId`, etc.
- usar `warn/error` para fallos reales
- usar `measure(...)` para operaciones lentas
- preferir watchers antes que subir todo el debug global

### No

- no usar `printStackTrace()`
- no loguear secretos/tokens/passwords
- no loguear cada tick o cada query sin necesidad
- no inventar logs aislados fuera del sistema

---

## Edge cases importantes

- si una categoría está en `off`, los `warn/error` igual quedan registrados
- los watchers expiran solos
- el buffer es circular: guarda lo reciente y descarta lo más viejo
- `off` en YAML debe ir entre comillas para no romper el parseo
- los overrides por comando son **runtime-only**; restart los limpia

---

## Criterio general

Primero activá debug **sobre el problema**, no sobre todo el servidor.

Queremos diagnóstico, no spam.
