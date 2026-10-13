# Instrucciones del proyecto

## Documentos: convertir a Markdown antes de leer
- Cuando el usuario comparta un documento (PDF, Word, PowerPoint, Excel, HTML, EPUB…), guárdalo en
  `documentos/originales/` y conviértelo con la skill **markitdown**:
  `markitdown documentos/originales/<archivo> -o documentos/md/<archivo>.md`
- Lee siempre la versión `.md` de `documentos/md/` en lugar del original: ocupa muchos menos tokens.
- Si el `.md` ya existe y el original no cambió, reutilízalo en vez de convertir otra vez.
- Si el PDF es escaneado (imágenes sin texto) el `.md` saldrá casi vacío: avisa al usuario.

## Proyecto Sabor Limeño (`sabor-limeno/`)
- Dos partes que comparten un mismo proyecto de Firebase (Firestore + Authentication):
  - **Web móvil del cliente** (`sabor-limeno/web-cliente/`): HTML, CSS y JavaScript; se trabaja en VS Code.
  - **App del personal** (mesero, cocina, cajero, administrador): **debe ser compatible con Android Studio**.
    Proyecto *Empty Views Activity*, lenguaje **Java** (no Kotlin), Minimum SDK **API 28** (Android 9),
    vistas XML (no Jetpack Compose). Debe verse bien en tablets y teléfonos.
    Antes de dar dependencias, confirmar si el proyecto usa `build.gradle` (Groovy) o `build.gradle.kts` (Kotlin DSL).
- La app Android debe usar exactamente el modelo de datos y los estados documentados en `sabor-limeno/README.md`.
- Al dar código Android, indicar en qué archivo y carpeta va cada parte y cómo ejecutarlo en Android Studio
  (emulador de teléfono/tablet o dispositivo físico).
