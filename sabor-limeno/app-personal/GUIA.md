# App del personal — Guía para Android Studio

App Android (Java, API 28, Groovy) para el personal de Sabor Limeño. Usa la **misma base de datos de Firebase** que la web del cliente.

| Fase | Contenido | Estado |
|---|---|---|
| **1** | Ingreso del personal + módulo **Mesero** (mesas, abrir atención, confirmar, rechazar, cancelar y entregar pedidos) | ✅ esta guía |
| 2 | Módulos **Cocina** y **Cajero** | pendiente |
| 3 | Módulo **Administrador** (productos, mesas, usuarios, historial) y pedido asistido | pendiente |

Mientras no estén las fases 2 y 3, el **panel de pruebas** (`https://TU-PROYECTO.web.app/panel/`) sigue sirviendo para los pasos de cocina y caja.

---

## Paso 1 · Publicar las reglas y el índice nuevos (VS Code)

La app consulta los pedidos de todas las mesas a la vez; eso necesita una regla y un índice nuevos.

1. Copia a tu carpeta `sabor-limeno` los archivos actualizados **`firestore.rules`**, **`firestore.indexes.json`** (nuevo) y **`firebase.json`**.
2. En VS Code, con la carpeta `sabor-limeno` abierta: **Terminal → Nuevo terminal** (Command Prompt) y ejecuta:
   ```
   firebase deploy --only firestore
   ```
3. El índice tarda **unos minutos** en construirse. Puedes verlo en la consola de Firebase → **Firestore Database → Índices → Exenciones de campo individuales**.

---

## Paso 2 · Crear el proyecto en Android Studio

1. **File → New → New Project… → Empty Views Activity → Next**.
2. Completa así (**respeta los nombres**, el código depende de ellos):

   | Campo | Valor |
   |---|---|
   | Name | `SaborLimeno` |
   | Package name | `com.example.saborlimeno` |
   | Language | `Java` |
   | Minimum SDK | `API 28 ("Pie"; Android 9.0)` |
   | Build configuration language | `Groovy DSL (build.gradle)` |

3. **Finish** y espera a que termine de sincronizar (barra inferior).

---

## Paso 3 · Conectar la app con Firebase

1. Consola de Firebase → **⚙ Configuración del proyecto → Tus apps → Agregar app → icono de Android**.
2. **Nombre del paquete:** `com.example.saborlimeno` → **Registrar app**.
3. **Descarga `google-services.json`**. Puedes saltar los demás pasos del asistente (se hacen abajo).
4. En Android Studio, arriba a la izquierda cambia la vista de **Android** a **Project**.
5. Arrastra `google-services.json` dentro de la carpeta **`SaborLimeno/app/`** (al mismo nivel que `build.gradle` del módulo `app`, **no** dentro de `src`).

---

## Paso 4 · Agregar Firebase a Gradle

### 4.1 `build.gradle (Project: SaborLimeno)` — el de la carpeta raíz
Agrega la segunda línea dentro de `plugins { }`:
```groovy
plugins {
    alias(libs.plugins.android.application) apply false
    id 'com.google.gms.google-services' version '4.4.2' apply false
}
```

### 4.2 `build.gradle (Module :app)` — el de la carpeta `app`
Agrega el plugin dentro de `plugins { }`:
```groovy
plugins {
    alias(libs.plugins.android.application)
    id 'com.google.gms.google-services'
}
```
Y al **final** del bloque `dependencies { }`, sin borrar lo que ya hay:
```groovy
    // Firebase (la versión de cada librería la define el BoM)
    implementation platform('com.google.firebase:firebase-bom:33.7.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

### 4.3 Sincronizar
Pulsa **Sync Now** (barra amarilla arriba). Debe terminar sin errores rojos.

---

## Paso 5 · Copiar el código

Copia los archivos de esta carpeta (`app-personal/app/src/main/`) a la **misma ruta** de tu proyecto (`SaborLimeno/app/src/main/`) con el explorador de archivos de Windows. Cuando pregunte, elige **Reemplazar**.

| Copiar desde `app-personal/app/src/main/` | A tu proyecto `SaborLimeno/app/src/main/` |
|---|---|
| `java/com/example/saborlimeno/` (carpetas `modelos`, `datos`, `ui` y `MainActivity.java`) | `java/com/example/saborlimeno/` — reemplaza `MainActivity.java` |
| `res/layout/` (5 archivos) | `res/layout/` — reemplaza `activity_main.xml` |
| `res/values/` (`colors.xml`, `themes.xml`, `integers.xml`, `dimens.xml`) | `res/values/` — reemplaza `colors.xml` y `themes.xml`; **no toques** `strings.xml` |
| `res/values-night/themes.xml` | `res/values-night/` — reemplaza |
| `res/values-sw600dp/` (carpeta completa) | dentro de `res/`, junto a `values/` (queda `res/values-sw600dp/`): valores para tablet |
| `res/drawable/fondo_etiqueta.xml` | `res/drawable/` |
| `AndroidManifest.xml` | reemplaza el tuyo |

Vuelve a Android Studio: los archivos aparecen solos. Luego **Build → Clean Project** y **Build → Rebuild Project**.

### Cómo está organizado el código (RNF10)
```
com.example.saborlimeno
├── MainActivity.java        ← P01 · ingreso del personal
├── modelos/                 ← clases de datos: Usuario, Mesa, Atencion, Pedido, ItemPedido, EstadoPedido
├── datos/Repositorio.java   ← todo el acceso a Firebase (las pantallas no usan Firestore directamente)
└── ui/                      ← pantallas y adaptadores
    ├── MeseroActivity.java      ← P05 · mesas y avisos
    ├── MesaDetalleActivity.java ← P06 · pedidos de una mesa
    ├── MesaAdapter.java, PedidoAdapter.java
    └── Navegacion.java, Dialogos.java, Formato.java
```

---

## Paso 6 · Ejecutar

### En el emulador
1. **Device Manager** (icono de celular a la derecha) → **+ → Create Virtual Device**.
2. Crea una **tablet**: categoría *Tablet* → **Pixel Tablet** → imagen del sistema **API 34 o superior** (descárgala si hace falta) → Finish.
3. (Opcional) Crea también un **teléfono**, por ejemplo *Pixel 8*.
4. Elige el dispositivo en la barra superior y pulsa **▶ Run**.

### En una tablet o celular real
1. En el dispositivo: **Ajustes → Acerca del teléfono** → toca 7 veces **Número de compilación** (activa Opciones de desarrollador).
2. **Ajustes → Sistema → Opciones de desarrollador → Depuración por USB**: activar.
3. Conéctalo por USB, acepta el aviso en el dispositivo, elígelo en Android Studio y pulsa **▶ Run**.

---

## Paso 7 · Probar con la web

1. Ingresa con tu usuario **administrador** o crea en Firebase un usuario con rol `mesero` (Authentication + documento en `usuarios`).
2. Verás las mesas. Toca una **Libre** → **Abrir atención**.
3. Desde un celular, escanea el QR de esa mesa (o abre su enlace) y envía un pedido.
4. En la tablet, la mesa muestra **“1 por confirmar”** al instante. Tócala → **Confirmar** (o **Rechazar** / **Cancelar** con motivo).
5. En el **panel de pruebas**, pulsa **Iniciar preparación** y **Marcar listo** (cocina, hasta la fase 2).
6. En la tablet aparece **“1 listo(s) para entregar”** → entra a la mesa → **Confirmar entrega**.
7. Cobra y cierra la mesa desde el panel de pruebas (cajero, hasta la fase 2). La tablet la muestra **Libre** otra vez.

> Los usuarios con rol `cocina` o `cajero` todavía ven el mensaje “estará disponible en la siguiente versión”.

---

## Errores comunes

| Mensaje | Causa y solución |
|---|---|
| `File google-services.json is missing` | El archivo no está en `SaborLimeno/app/`. Revisa el Paso 3. |
| `No matching client found for package name` | El paquete registrado en Firebase no es `com.example.saborlimeno`. Regístralo igual que el proyecto. |
| `cannot find symbol R` / `package com.example.saborlimeno does not exist` | Tu proyecto tiene otro *Package name*. Créalo como indica el Paso 2. |
| `resource style/Theme.SaborLimeno not found` | El proyecto no se llama `SaborLimeno`. Créalo con ese nombre o cambia el nombre del tema en `themes.xml` y `AndroidManifest.xml`. |
| `FAILED_PRECONDITION: The query requires an index` | El índice del Paso 1 no se publicó o todavía se está construyendo: espera unos minutos. |
| `PERMISSION_DENIED` | Faltan las reglas del Paso 1, o tu usuario no tiene documento en `usuarios` con `activo = true`. |
| “Este usuario no está registrado en el sistema” | Crea su documento `usuarios/{UID}` con `nombre`, `rol` y `activo`. |

Si aparece otro error, copia el mensaje completo de la pestaña **Build** o **Logcat** y compártelo.
