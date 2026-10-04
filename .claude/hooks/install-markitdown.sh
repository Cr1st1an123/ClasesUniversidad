#!/bin/bash
# Instala MarkItDown en las sesiones de Claude Code en la nube (el contenedor se borra en cada sesión).
if [ "$CLAUDE_CODE_REMOTE" != "true" ]; then
  exit 0
fi
command -v markitdown >/dev/null 2>&1 || pip install -q "markitdown[pdf,docx,pptx,xlsx,xls]" >/dev/null 2>&1
exit 0
