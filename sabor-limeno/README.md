# Sabor Limeño — Web móvil del cliente

Parte web del proyecto *Aplicación móvil para la gestión de pedidos y cuentas en el restaurante Sabor Limeño*.
El cliente escanea el QR de su mesa y, sin instalar nada, puede ver la carta, enviar pedidos, seguir su estado y solicitar la cuenta.
El personal usará la aplicación Android (siguiente etapa), que comparte la misma base de datos de Firebase.

## Estructura

```
sabor-limeno/
├── web-cliente/              ← lo que se publica (Firebase Hosting)
│   ├── index.html            ← las 3 pantallas: Carta, Mi pedido, Mis pedidos
│   ├── css/estilos.css
│   └── js/
│       ├── firebase-config.js  ← AQUÍ van los datos de tu proyecto de Firebase
│       ├── firebase.js         ← inicializa Firebase
│       ├── datos.js            ← acceso a Firestore (lecturas y escrituras)
│       └── app.js              ← lógica de las pantallas
├── herramientas/
│   └── panel-pruebas.html    ← panel TEMPORAL que simula al personal (no se publica)
├── firestore.rules           ← reglas de seguridad de la base de datos
└── firebase.json             ← configuración de Firebase CLI
```

## Modelo de datos (Cloud Firestore)

La aplicación Android deberá usar exactamente estos nombres.

| Colección | Campos |
|---|---|
| `usuarios/{uid}` | `nombre`, `rol` (`mesero`, `cocina`, `cajero`, `administrador`), `activo` |
| `productos/{id}` | `nombre`, `descripcion`, `categoria` (`Entradas`, `Platos`, `Bebidas`, `Postres`), `precio`, `disponible`, `activo`, `imagenUrl` (opcional) |
| `mesas/{idAleatorio}` | `numero`, `activa`, `atencionId` (`null` si está libre). **El id del documento es el que va en el QR.** |
| `atenciones/{id}` | `mesaId`, `mesaNumero`, `abierta`, `cuentaSolicitada`, `fechaApertura`, `abiertaPor`, `fechaCierre`, `pago { medio, importe, fecha, cajero }` |
| `atenciones/{id}/pedidos/{id}` | `items [{ productoId, nombre, precio, cantidad }]`, `observaciones`, `total`, `estado`, `origen` (`CLIENTE`/`MESERO`), `motivo`, `mesaId`, `mesaNumero`, `fechaCreacion`, … |

Estados del pedido: `POR_CONFIRMAR` → `PENDIENTE` → `EN_PREPARACION` → `LISTO` → `ENTREGADO`, o bien `RECHAZADO` / `CANCELADO`.

## Paso 1 · Crear el proyecto de Firebase (una sola vez)

1. Entra a <https://console.firebase.google.com> y crea un proyecto (por ejemplo `sabor-limeno`). Google Analytics no es necesario.
2. **Firestore Database** → *Crear base de datos* → ubicación `southamerica-east1` (o la que prefieras) → modo **producción**.
3. **Authentication** → *Comenzar* → activa **Correo electrónico/contraseña**.
4. **Authentication → Users → Agregar usuario**: crea tu usuario administrador (correo y contraseña). Copia su **UID**.
5. **Firestore → Iniciar colección** `usuarios` → ID del documento = el **UID** copiado → campos:
   - `nombre` (string): tu nombre
   - `rol` (string): `administrador`
   - `activo` (boolean): `true`
6. **Firestore → Reglas**: borra lo que haya, pega el contenido de `firestore.rules` y pulsa **Publicar**.
7. **Configuración del proyecto (⚙) → Tus apps → icono `</>`** → registra una app web (sin Hosting por ahora).
   Copia el objeto `firebaseConfig` y pégalo en `web-cliente/js/firebase-config.js`.

## Paso 2 · Probar en tu computadora

Los archivos usan módulos de JavaScript, así que **no funcionan abriendo el HTML con doble clic**: hace falta un servidor local.

**Opción A (recomendada): Visual Studio Code + extensión *Live Server***
1. Abre la carpeta `sabor-limeno` en VS Code.
2. Instala la extensión **Live Server** (de Ritwick Dey).
3. Clic derecho sobre `herramientas/panel-pruebas.html` → **Open with Live Server**.

**Opción B: Python** (si lo tienes instalado), desde la carpeta `sabor-limeno`:
```
python -m http.server 5500
```
y abre <http://localhost:5500/herramientas/panel-pruebas.html>.

> Android Studio no se usa para esta parte; es solo para la app del personal.

### Recorrido de prueba
1. En el **panel de pruebas**, ingresa con el usuario administrador.
2. Pulsa **Cargar productos y 10 mesas de ejemplo**.
3. En la tabla **Mesas**, fila *Mesa 4*, pulsa **Abrir atención**. Luego haz clic en el enlace de la columna
   **Enlace del QR (web del cliente)**: se abre una pestaña nueva con la **web del cliente** (es lo que verá
   quien escanee el QR). No abras `web-cliente/index.html` directamente: sin el `?m=...` del enlace no sabe qué mesa es.
   Para verlo como en un celular, abre las herramientas de desarrollo de Chrome con cualquiera de estas formas:
   - `F12` (en muchas laptops hay que pulsar `Fn + F12`).
   - `Ctrl + Shift + I`.
   - Clic derecho sobre la página → **Inspeccionar**.
   - Menú `⋮` → **Más herramientas** → **Herramientas para desarrolladores**.

   Luego pulsa el icono de celular/tablet de la barra de herramientas (o `Ctrl + Shift + M`) y elige un modelo, por ejemplo *iPhone 12 Pro*.
4. En esa pestaña (web del cliente):
   - En **Nuestra carta**, elige una categoría (Entradas, Platos…) y pulsa **+ Agregar** en los productos.
   - Abajo, toca la pestaña **Mi pedido**: ajusta cantidades con `−` / `+`, escribe una observación y pulsa **Enviar pedido**.
   - La web pasa sola a **Mis pedidos**, donde el pedido aparece *Por confirmar*.
5. Vuelve a la pestaña del **panel de pruebas** y baja a la sección **Pedidos de atenciones abiertas**. En cada pedido pulsa,
   en orden, **Confirmar (mesero)**, **Iniciar preparación (cocina)**, **Marcar listo (cocina)** y **Entregar (mesero)**.
   Después de cada clic, la web del cliente se actualiza sola.
   > El botón **Solicitar cuenta** del cliente se activa cuando el mesero confirma al menos un pedido;
   > los pedidos *Por confirmar* todavía no se suman a la cuenta.
6. En el cliente, **Solicitar cuenta**; en el panel aparece “¡Cuenta solicitada!”.
7. En el panel, **Registrar pago y cerrar**: la web del cliente vuelve a la carta y ya no deja pedir.

También puedes probar: desmarcar *Disponible* de un producto que está en “Mi pedido”, cambiar un precio después de enviar un pedido, cancelar un pedido “Por confirmar” y abrir un enlace con un id inventado.

## Paso 3 · Publicar la web (Firebase Hosting)

Requiere [Node.js](https://nodejs.org) instalado. En una terminal, dentro de `sabor-limeno`:
```
npm install -g firebase-tools
firebase login
firebase use --add            (elige tu proyecto)
firebase deploy --only hosting,firestore:rules
```
La web quedará en `https://TU-PROYECTO.web.app`. El enlace de cada mesa será:
```
https://TU-PROYECTO.web.app/?m=ID_DE_LA_MESA
```
El panel de pruebas muestra el id de cada mesa. Con cada enlace genera el QR en cualquier generador gratuito e imprímelo.

## Notas

- **Seguridad**: las reglas de `firestore.rules` impiden que el cliente cambie estados, precios o mesas; solo puede crear pedidos *Por confirmar* en la atención abierta de su mesa, cancelarlos y solicitar la cuenta. Como no hay servidor propio, el precio que envía la web no se vuelve a calcular en la base de datos: el mesero lo ve antes de confirmar el pedido.
- **Lectura de atenciones**: para leer una atención hay que conocer su id, y este solo se obtiene desde el QR mientras la mesa está abierta. Las reglas no bloquean la lectura cuando la atención se cierra, porque si una página pierde el permiso mientras escucha cambios, el SDK de Firebase deja de recibir también los demás cambios de esa página.
- **Versión del SDK**: los `import` usan Firebase `10.12.2` desde `gstatic.com`; si la cambias, cámbiala en todos los archivos.
- **Emulador (opcional)**: con `firebase emulators:start --project demo-sabor-limeno` y `&emulador` al final de la URL puedes probar sin tocar tu proyecto real.
