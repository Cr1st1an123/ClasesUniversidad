# ClasesUniversidad

## Skills de Claude Code instaladas

| Skill | Tipo | Ubicación / origen |
|---|---|---|
| **find-skills** | Skill del proyecto | `.claude/skills/find-skills` ([vercel-labs/skills](https://github.com/vercel-labs/skills)) |
| **impeccable** (frontend) | Skill del proyecto | `.claude/skills/impeccable` ([pbakaus/impeccable](https://github.com/pbakaus/impeccable)) — usar `/impeccable init`, `/impeccable audit`, `/impeccable polish`, etc. |
| **superpowers** | Plugin | `superpowers@superpowers-marketplace` ([obra/superpowers](https://github.com/obra/superpowers)) |
| **claude-mem** | Plugin | `claude-mem@thedotmack` ([thedotmack/claude-mem](https://github.com/thedotmack/claude-mem)) |

Los plugins están declarados en `.claude/settings.json`; al abrir el proyecto en Claude Code
se te pedirá confiar en la carpeta y se instalarán automáticamente. Si no ocurre, ejecuta:

```
/plugin marketplace add obra/superpowers-marketplace
/plugin marketplace add thedotmack/claude-mem
/plugin install superpowers@superpowers-marketplace
/plugin install claude-mem@thedotmack
```
