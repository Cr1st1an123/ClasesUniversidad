# Instrucciones del proyecto

## Documentos: convertir a Markdown antes de leer
- Cuando el usuario comparta un documento (PDF, Word, PowerPoint, Excel, HTML, EPUB…), guárdalo en
  `documentos/originales/` y conviértelo con la skill **markitdown**:
  `markitdown documentos/originales/<archivo> -o documentos/md/<archivo>.md`
- Lee siempre la versión `.md` de `documentos/md/` en lugar del original: ocupa muchos menos tokens.
- Si el `.md` ya existe y el original no cambió, reutilízalo en vez de convertir otra vez.
- Si el PDF es escaneado (imágenes sin texto) el `.md` saldrá casi vacío: avisa al usuario.
