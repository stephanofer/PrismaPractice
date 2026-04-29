---

## Voice Chat Strategy

Runtime requirements:
- voice chat plugin/mod on proxy
- voice chat plugin/mod on each backend
- if multiple backends share one machine, each backend needs its own UDP port

Current policy:
- Hub -> proximity voice
- FFA -> proximity voice
- Match -> backend + arena isolation
- automatic groups -> **not** phase 1

Meaning of "context":
- our own domain language (`HUB`, `MATCH:<id>`, `FFA:<server>`)
- not a native voice chat feature

Native voice chat features we may use later:
- groups
- static/locational/entity audio channels

Do not implement automatic group lifecycle yet unless a real need appears.