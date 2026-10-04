# ClasesUniversidad

## Skills de Claude Code instaladas

| Skill | Tipo | Ubicación / origen |
|---|---|---|
| **find-skills** | Skill del proyecto | `.claude/skills/find-skills` ([vercel-labs/skills](https://github.com/vercel-labs/skills)) |
| **impeccable** (frontend) | Skill del proyecto | `.claude/skills/impeccable` ([pbakaus/impeccable](https://github.com/pbakaus/impeccable)) — usar `/impeccable init`, `/impeccable audit`, `/impeccable polish`, etc. |
| **markitdown** | Skill del proyecto | `.claude/skills/markitdown` ([K-Dense-AI/claude-scientific-skills](https://github.com/K-Dense-AI/claude-scientific-skills), usa [microsoft/markitdown](https://github.com/microsoft/markitdown)) — convierte documentos a `.md` |
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

## Convertir documentos a Markdown (markitdown)

1. Pon tus archivos en `documentos/originales/` (o simplemente envíaselos a Claude).
2. Claude los convierte a `documentos/md/<archivo>.md` y lee esa versión, que gasta muchos menos tokens.

Uso manual en tu computadora (requiere Python 3.10+):

```
pip install "markitdown[pdf,docx,pptx,xlsx,xls]"
markitdown documentos/originales/clase1.pdf -o documentos/md/clase1.md
```

En las sesiones en la nube, el hook `.claude/hooks/install-markitdown.sh` lo instala automáticamente.
