# Practice Hera — Índice de documentación de producto

## Nota

La idea es poder navegar rápido por el producto sin perder información ni trabajar sobre un único archivo gigante.

## Archivos disponibles

- [`fases-de-desarrollo.md`](./fases-de-desarrollo.md) — documento unificado para ordenar el desarrollo, el estado de avance por fases, criterios de salida, riesgos y pendientes antes de implementar.
- [`01-panorama-del-producto.md`](./01-panorama-del-producto.md) — visión general del producto, núcleo competitivo, settings, matchmaking, estadísticas en tiempo real, bootstrap inicial del jugador y flujo principal de uso.
- [`02-rating-e-historial-competitivo.md`](./02-rating-e-historial-competitivo.md) — modelo de SR, Global Rating, rangos, seasons y diseño del historial/resumen de match.
- [`03-gameplay-base-modos-rules.md`](./03-gameplay-base-modos-rules.md) — base de gameplay reusable: modos, kits, rules configurables, player types y layouts.
- [`04-social-parties-y-eventos.md`](./04-social-parties-y-eventos.md) — requests sociales, parties, party duels y framework de eventos hosteados.
- [`05-entidades-modo-y-rule.md`](./05-entidades-modo-y-rule.md) — definición formal de `Mode Definition` y `Rule Definition` como piezas centrales del dominio.
- [`06-entidades-queue-match-y-arena.md`](./06-entidades-queue-match-y-arena.md) — definición formal de `Queue`, `Match Session` y `Arena`.
- [`07-player-state-y-profile-stats.md`](./07-player-state-y-profile-stats.md) — sistema de estado global del jugador, transiciones, restricciones y modelo de perfil/stats.
- [`08-data-ownership-y-persistencia.md`](./08-data-ownership-y-persistencia.md) — ownership entre MySQL y Redis, matriz por entidad y diseño inicial concreto de tablas/claves.
- [`09-application-services.md`](./09-application-services.md) — capa de servicios de aplicación, responsabilidades y flujo recomendado de orquestación.
- [`10-sistema-de-creacion-y-contenido-semilla.md`](./10-sistema-de-creacion-y-contenido-semilla.md) — define que modos, kits, queues y arenas deben poder crearse in-game y diferencia esa capacidad del contenido semilla inicial recomendado.
- [`11-queues-semilla-activas-iniciales.md`](./11-queues-semilla-activas-iniciales.md) — define las primeras queues competitivas activas recomendadas para arrancar el servidor sin mezclar todavía teams, FFA ni eventos en el bloque central.
- [`12-rangos-y-rules-por-defecto.md`](./12-rangos-y-rules-por-defecto.md) — propone una escala inicial simple de rangos y deja definidos los rules defaults recomendados para los modos semilla, incluyendo la decisión sobre regeneración vanilla como rule.
- [`13-templates-iniciales-de-eventos.md`](./13-templates-iniciales-de-eventos.md) — define el primer set exacto de templates de eventos hosteados para arrancar con una mezcla sana de prestigio competitivo y retención social.
- [`14-modos-base-vs-modos-especiales.md`](./14-modos-base-vs-modos-especiales.md) — define la frontera exacta entre modos base configurables y modos especiales, y propone una estrategia simple para extender comportamiento custom sin contaminar el sistema base.
- [`15-validacion-final-storage.md`](./15-validacion-final-storage.md) — valida definitivamente el contrato inicial de MySQL y Redis, corrige huecos críticos y deja el storage aprobado para implementación.

## Observación

El archivo original `docs/fase-0-definicion-producto.md` se conserva como fuente completa original, pero esta carpeta pasa a ser la forma recomendada de navegar el contenido fragmentado del producto.
