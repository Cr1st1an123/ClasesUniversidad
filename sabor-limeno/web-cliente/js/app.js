// Lógica de las pantallas de la web del cliente: Carta, Mi pedido y Mis pedidos.
import * as datos from "./datos.js";

const ESTADOS = {
  POR_CONFIRMAR: "Por confirmar",
  PENDIENTE: "Pendiente",
  EN_PREPARACION: "En preparación",
  LISTO: "Listo",
  ENTREGADO: "Entregado",
  RECHAZADO: "Rechazado",
  CANCELADO: "Cancelado",
};
const SECUENCIA = ["POR_CONFIRMAR", "PENDIENTE", "EN_PREPARACION", "LISTO", "ENTREGADO"];
const ESTADOS_COBRABLES = ["PENDIENTE", "EN_PREPARACION", "LISTO", "ENTREGADO"];
const ORDEN_CATEGORIAS = ["Entradas", "Platos", "Bebidas", "Postres"];
const MAX_CANTIDAD = 20;
const TIEMPO_LIMITE_ENVIO = 15000;

const estado = {
  mesaId: new URLSearchParams(location.search).get("m"),
  mesa: null,
  atencionId: null,
  atencion: null,
  productos: [],
  pedidos: [],
  carrito: new Map(), // productoId -> cantidad
  categoria: null,
  vista: "carta",
  error: null,
  enviando: false,
  pedidoPendienteId: null,
  mensajeEnvio: "",
  estadosPrevios: new Map(),
  pedidoACancelar: null,
};
let dejarDeEscucharAtencion = null;
let dejarDeEscucharPedidos = null;

// ---------- Utilidades ----------

const $ = (id) => document.getElementById(id);
const redondear = (n) => Math.round(n * 100) / 100;
const soles = (n) => "S/ " + n.toFixed(2);

// Crea elementos HTML sin usar innerHTML (evita inyectar texto como código).
function el(etiqueta, atributos = {}, ...hijos) {
  const nodo = document.createElement(etiqueta);
  for (const [clave, valor] of Object.entries(atributos)) {
    if (valor === false || valor == null) continue;
    if (clave.startsWith("on")) nodo.addEventListener(clave.slice(2), valor);
    else if (clave === "class") nodo.className = valor;
    else nodo.setAttribute(clave, valor === true ? "" : valor);
  }
  for (const hijo of hijos.flat()) {
    if (hijo != null && hijo !== false && hijo !== "") nodo.append(hijo);
  }
  return nodo;
}

function hora(fecha) {
  return fecha?.toDate ? fecha.toDate().toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" }) : "";
}

let temporizadorToast;
function toast(mensaje) {
  const t = $("toast");
  t.textContent = mensaje;
  t.classList.add("visible");
  clearTimeout(temporizadorToast);
  temporizadorToast = setTimeout(() => t.classList.remove("visible"), 3500);
}

function conTiempoLimite(promesa, ms) {
  return Promise.race([
    promesa,
    new Promise((_, rechazar) => setTimeout(() => rechazar(new Error("tiempo")), ms)),
  ]);
}

const atencionAbierta = () => Boolean(estado.atencionId && estado.atencion?.abierta === true);

// ---------- Inicio y escucha de datos ----------

function iniciar() {
  configurarEventos();
  if (!estado.mesaId) {
    mostrarError("Escanee el QR de su mesa",
      "Este enlace no corresponde a ninguna mesa. Use la cámara de su celular para escanear el código QR de su mesa.");
    return;
  }
  datos.escucharProductos(
    (productos) => { estado.productos = productos; render(); },
    () => mostrarError("No se pudo cargar la carta", "Revise su conexión a internet y vuelva a escanear el QR."),
  );
  datos.escucharMesa(estado.mesaId, alCambiarMesa,
    () => mostrarError("No se pudo abrir la mesa", "Revise su conexión a internet y vuelva a escanear el QR."));
}

function alCambiarMesa(mesa) {
  if (!mesa || mesa.activa === false) {
    mostrarError("Código QR no válido", "Este código no corresponde a una mesa activa del restaurante. Por favor, llame al mesero.");
    return;
  }
  estado.mesa = mesa;
  const atencionId = mesa.atencionId || null;
  if (atencionId !== estado.atencionId) cambiarAtencion(atencionId);
  render();
}

// Cuando la mesa cambia de atención (se abre o se cierra), se descarta todo lo de la anterior.
function cambiarAtencion(atencionId) {
  dejarDeEscucharAtencion?.();
  dejarDeEscucharPedidos?.();
  dejarDeEscucharAtencion = dejarDeEscucharPedidos = null;
  Object.assign(estado, {
    atencionId, atencion: null, pedidos: [], pedidoPendienteId: null, mensajeEnvio: "",
  });
  estado.carrito.clear();
  estado.estadosPrevios.clear();
  $("observaciones").value = "";
  if (!atencionId) {
    estado.vista = "carta";
    return;
  }
  dejarDeEscucharAtencion = datos.escucharAtencion(atencionId,
    (atencion) => { estado.atencion = atencion; render(); },
    () => { estado.atencion = null; render(); });
  dejarDeEscucharPedidos = datos.escucharPedidos(atencionId,
    (pedidos) => { avisarCambios(pedidos); estado.pedidos = pedidos; render(); },
    () => { estado.pedidos = []; render(); });
}

// Muestra un aviso cuando un pedido cambia de estado (por ejemplo, cuando está Listo).
function avisarCambios(pedidos) {
  const numerados = numerarPedidos(pedidos);
  for (const p of numerados) {
    const anterior = estado.estadosPrevios.get(p.id);
    if (anterior && anterior !== p.estado) toast(`Pedido ${p.numero}: ${ESTADOS[p.estado]}`);
    estado.estadosPrevios.set(p.id, p.estado);
  }
}

function numerarPedidos(pedidos) {
  return [...pedidos]
    .sort((a, b) => (a.fechaCreacion?.toMillis?.() ?? Infinity) - (b.fechaCreacion?.toMillis?.() ?? Infinity))
    .map((p, i) => ({ ...p, numero: i + 1 }));
}

// ---------- Acciones del cliente ----------

function agregar(producto) {
  const cantidad = estado.carrito.get(producto.id) || 0;
  if (cantidad >= MAX_CANTIDAD) return toast(`Máximo ${MAX_CANTIDAD} unidades por producto`);
  estado.carrito.set(producto.id, cantidad + 1);
  estado.pedidoPendienteId = null;
  toast(`${producto.nombre} agregado a su pedido`);
  render();
}

function cambiarCantidad(productoId, diferencia) {
  const cantidad = (estado.carrito.get(productoId) || 0) + diferencia;
  if (cantidad < 1 || cantidad > MAX_CANTIDAD) return;
  estado.carrito.set(productoId, cantidad);
  estado.pedidoPendienteId = null;
  render();
}

function retirar(productoId) {
  estado.carrito.delete(productoId);
  estado.pedidoPendienteId = null;
  render();
}

function lineasCarrito() {
  return [...estado.carrito].map(([productoId, cantidad]) => {
    const producto = estado.productos.find((p) => p.id === productoId);
    return { productoId, cantidad, producto, disponible: Boolean(producto?.disponible) };
  });
}

function motivoBloqueoEnvio(lineas) {
  if (!atencionAbierta()) return "La mesa no tiene una atención activa. Llame al mesero.";
  if (!navigator.onLine) return "Sin conexión a internet.";
  if (lineas.some((l) => !l.disponible)) return "Retire los productos que ya no están disponibles.";
  return "";
}

async function enviarPedido() {
  const lineas = lineasCarrito();
  if (estado.enviando || lineas.length === 0 || motivoBloqueoEnvio(lineas)) return;

  estado.enviando = true;
  estado.mensajeEnvio = "";
  render();

  const atencionId = estado.atencionId;
  estado.pedidoPendienteId ??= datos.nuevoIdPedido(atencionId);
  const pedidoId = estado.pedidoPendienteId;
  // Se usa el precio vigente en este momento; queda guardado en el pedido.
  const items = lineas.map((l) => ({
    productoId: l.productoId,
    nombre: l.producto.nombre,
    precio: l.producto.precio,
    cantidad: l.cantidad,
  }));
  const pedido = {
    items,
    observaciones: $("observaciones").value.trim(),
    total: redondear(items.reduce((suma, i) => suma + i.precio * i.cantidad, 0)),
    mesaId: estado.mesaId,
    mesaNumero: estado.mesa.numero,
  };

  try {
    await conTiempoLimite(datos.enviarPedido(atencionId, pedidoId, pedido), TIEMPO_LIMITE_ENVIO);
    pedidoEnviado();
  } catch (error) {
    // Si un intento anterior sí llegó, el reintento es rechazado pero el pedido ya existe.
    const yaExiste = error.code === "permission-denied"
      && await datos.existePedido(atencionId, pedidoId).catch(() => false);
    if (yaExiste) pedidoEnviado();
    else if (error.message === "tiempo") estado.mensajeEnvio = "No pudimos confirmar el envío. Revise su conexión e intente de nuevo; su pedido no se duplicará.";
    else if (error.code === "permission-denied") estado.mensajeEnvio = "No se pudo enviar: la mesa ya no tiene una atención activa. Llame al mesero.";
    else estado.mensajeEnvio = "No se pudo enviar el pedido. Intente de nuevo.";
  } finally {
    estado.enviando = false;
    render();
  }
}

function pedidoEnviado() {
  estado.carrito.clear();
  estado.pedidoPendienteId = null;
  estado.mensajeEnvio = "";
  $("observaciones").value = "";
  estado.vista = "seguimiento";
  toast("Pedido enviado. El mesero lo confirmará en breve.");
}

function abrirDialogoCancelar(pedido) {
  estado.pedidoACancelar = pedido;
  $("motivo-cancelacion").value = "";
  $("dialogo-cancelar").showModal();
}

async function alCerrarDialogoCancelar() {
  const dialogo = $("dialogo-cancelar");
  const pedido = estado.pedidoACancelar;
  estado.pedidoACancelar = null;
  if (dialogo.returnValue !== "confirmar" || !pedido) return;
  const motivo = $("motivo-cancelacion").value.trim();
  if (!motivo) return toast("Indique el motivo para cancelar");
  try {
    await datos.cancelarPedido(estado.atencionId, pedido.id, motivo);
    toast(`Pedido ${pedido.numero} cancelado`);
  } catch {
    toast("No se pudo cancelar: es posible que el mesero ya lo haya confirmado.");
  }
}

async function solicitarCuenta() {
  const boton = $("btn-solicitar-cuenta");
  boton.disabled = true;
  try {
    await datos.solicitarCuenta(estado.atencionId);
    toast("Cuenta solicitada. El cajero se acercará a su mesa.");
  } catch {
    toast("No se pudo solicitar la cuenta. Intente de nuevo.");
    boton.disabled = false;
  }
}

function configurarEventos() {
  for (const boton of document.querySelectorAll("#pestanas button")) {
    boton.addEventListener("click", () => { estado.vista = boton.dataset.vista; render(); window.scrollTo(0, 0); });
  }
  $("btn-enviar").addEventListener("click", enviarPedido);
  $("btn-solicitar-cuenta").addEventListener("click", solicitarCuenta);
  $("observaciones").addEventListener("input", () => { estado.pedidoPendienteId = null; });
  $("dialogo-cancelar").addEventListener("close", alCerrarDialogoCancelar);
  window.addEventListener("online", render);
  window.addEventListener("offline", render);
}

// ---------- Pantallas ----------

function mostrarError(titulo, detalle) {
  estado.error = { titulo, detalle };
  render();
}

function render() {
  const { error } = estado;
  const cargando = !error && !estado.mesa;
  const vistaActual = error ? "error" : cargando ? "cargando" : estado.vista;

  for (const vista of ["cargando", "error", "carta", "pedido", "seguimiento"]) {
    $(`vista-${vista}`).hidden = vista !== vistaActual;
  }
  $("aviso-conexion").hidden = navigator.onLine;

  if (error) {
    $("error-titulo").textContent = error.titulo;
    $("error-detalle").textContent = error.detalle;
    $("pestanas").hidden = true;
    $("aviso-mesa").hidden = true;
    $("etiqueta-mesa").textContent = "";
    return;
  }
  if (cargando) return;

  const abierta = atencionAbierta();
  // Si la atención se cerró mientras el cliente veía otra pestaña, vuelve a la carta.
  if (!abierta && estado.vista !== "carta") {
    estado.vista = "carta";
    return render();
  }

  $("etiqueta-mesa").textContent = `Mesa ${estado.mesa.numero}`;
  $("aviso-mesa").hidden = abierta || !estado.mesa || (estado.atencionId && !estado.atencion);
  renderPestanas(abierta);

  if (vistaActual === "carta") renderCarta(abierta);
  if (vistaActual === "pedido") renderPedido();
  if (vistaActual === "seguimiento") renderSeguimiento();
}

function renderPestanas(abierta) {
  $("pestanas").hidden = false;
  for (const boton of document.querySelectorAll("#pestanas button")) {
    const activa = boton.dataset.vista === estado.vista;
    boton.classList.toggle("activa", activa);
    boton.setAttribute("aria-current", activa ? "page" : "false");
    boton.disabled = boton.dataset.vista !== "carta" && !abierta;
  }
  const unidades = [...estado.carrito.values()].reduce((a, b) => a + b, 0);
  $("contador-carrito").hidden = unidades === 0;
  $("contador-carrito").textContent = unidades;
}

function compararCategorias(a, b) {
  const ia = ORDEN_CATEGORIAS.indexOf(a);
  const ib = ORDEN_CATEGORIAS.indexOf(b);
  return (ia === -1 ? 99 : ia) - (ib === -1 ? 99 : ib) || a.localeCompare(b, "es");
}

function renderCarta(puedePedir) {
  const categorias = [...new Set(estado.productos.map((p) => p.categoria))].sort(compararCategorias);
  if (!categorias.includes(estado.categoria)) estado.categoria = categorias[0] ?? null;

  $("categorias").replaceChildren(...categorias.map((c) => el("button", {
    type: "button",
    class: "chip" + (c === estado.categoria ? " activo" : ""),
    "aria-pressed": String(c === estado.categoria),
    onclick: () => { estado.categoria = c; render(); },
  }, c)));

  const productos = estado.productos.filter((p) => p.categoria === estado.categoria);
  $("lista-productos").replaceChildren(...productos.map((p) => tarjetaProducto(p, puedePedir)));
  $("carta-vacia").hidden = estado.productos.length > 0;
}

function imagenProducto(producto) {
  if (producto.imagenUrl) {
    return el("img", { class: "producto-imagen", src: producto.imagenUrl, alt: "", loading: "lazy" });
  }
  return el("div", { class: "producto-imagen producto-inicial", "aria-hidden": "true" }, producto.nombre.charAt(0));
}

function tarjetaProducto(producto, puedePedir) {
  const enPedido = estado.carrito.get(producto.id) || 0;
  let accion = null;
  if (!producto.disponible) accion = el("span", { class: "etiqueta etiqueta-gris" }, "No disponible");
  else if (puedePedir) {
    accion = el("button", {
      type: "button",
      class: "btn btn-primario btn-chico",
      "aria-label": `Agregar ${producto.nombre}`,
      onclick: () => agregar(producto),
    }, enPedido ? `+ Agregar (${enPedido})` : "+ Agregar");
  }
  return el("li", { class: "producto" + (producto.disponible ? "" : " agotado") },
    imagenProducto(producto),
    el("div", { class: "producto-info" },
      el("h2", { class: "producto-nombre" }, producto.nombre),
      producto.descripcion && el("p", { class: "producto-descripcion" }, producto.descripcion),
      el("div", { class: "producto-pie" },
        el("span", { class: "precio" }, soles(producto.precio)),
        accion)));
}

function renderPedido() {
  const lineas = lineasCarrito();
  $("lista-carrito").replaceChildren(...lineas.map((l) => {
    const nombre = l.producto?.nombre ?? "Producto retirado de la carta";
    const precio = l.producto?.precio ?? 0;
    return el("li", { class: "linea" + (l.disponible ? "" : " agotado") },
      el("div", { class: "linea-info" },
        el("span", { class: "linea-nombre" }, nombre),
        el("span", { class: "linea-precio" }, `${soles(precio)} c/u`),
        !l.disponible && el("span", { class: "linea-alerta" }, "Ya no está disponible. Retírelo para continuar.")),
      el("div", { class: "cantidad" },
        el("button", { type: "button", class: "btn-cantidad", "aria-label": `Quitar uno de ${nombre}`,
          disabled: l.cantidad <= 1 || !l.disponible, onclick: () => cambiarCantidad(l.productoId, -1) }, "−"),
        el("span", { class: "cantidad-valor", "aria-live": "polite" }, String(l.cantidad)),
        el("button", { type: "button", class: "btn-cantidad", "aria-label": `Agregar uno de ${nombre}`,
          disabled: l.cantidad >= MAX_CANTIDAD || !l.disponible, onclick: () => cambiarCantidad(l.productoId, 1) }, "+")),
      el("span", { class: "linea-subtotal" }, soles(precio * l.cantidad)),
      el("button", { type: "button", class: "btn-retirar", "aria-label": `Retirar ${nombre}`,
        onclick: () => retirar(l.productoId) }, "Retirar"));
  }));
  $("carrito-vacio").hidden = lineas.length > 0;

  const total = redondear(lineas.reduce((s, l) => s + (l.disponible ? l.producto.precio * l.cantidad : 0), 0));
  $("total-carrito").textContent = soles(total);

  const bloqueo = motivoBloqueoEnvio(lineas);
  const boton = $("btn-enviar");
  boton.disabled = estado.enviando || lineas.length === 0 || Boolean(bloqueo);
  boton.textContent = estado.enviando ? "Enviando…" : "Enviar pedido";
  $("mensaje-envio").textContent = estado.mensajeEnvio || (lineas.length ? bloqueo : "");
}

function renderSeguimiento() {
  const pedidos = numerarPedidos(estado.pedidos).reverse();
  $("lista-pedidos").replaceChildren(...pedidos.map(tarjetaPedido));
  $("sin-pedidos").hidden = pedidos.length > 0;

  const suma = (lista) => redondear(lista.reduce((s, p) => s + p.total, 0));
  const cobrables = pedidos.filter((p) => ESTADOS_COBRABLES.includes(p.estado));
  const porConfirmar = suma(pedidos.filter((p) => p.estado === "POR_CONFIRMAR"));
  $("total-cuenta").textContent = soles(suma(cobrables));
  $("nota-cuenta").textContent = porConfirmar > 0
    ? `Además tiene ${soles(porConfirmar)} en pedidos por confirmar; se sumarán cuando el mesero los confirme.`
    : "Solo se cobran los pedidos confirmados por el mesero.";

  const solicitada = estado.atencion?.cuentaSolicitada === true;
  $("btn-solicitar-cuenta").hidden = solicitada;
  $("btn-solicitar-cuenta").disabled = cobrables.length === 0;
  $("cuenta-solicitada").hidden = !solicitada;
}

function tarjetaPedido(pedido) {
  const terminado = pedido.estado === "RECHAZADO" || pedido.estado === "CANCELADO";
  const etiqueta = pedido.pendiente && !pedido.fechaCreacion ? "Enviando…" : ESTADOS[pedido.estado];
  return el("article", { class: "pedido" + (terminado ? " pedido-terminado" : "") },
    el("header", { class: "pedido-cabecera" },
      el("h2", {}, `Pedido ${pedido.numero}`),
      el("span", { class: "hora" }, hora(pedido.fechaCreacion)),
      el("span", { class: `etiqueta estado-${pedido.estado.toLowerCase()}` }, etiqueta)),
    el("ul", { class: "pedido-items" }, pedido.items.map((i) => el("li", {},
      el("span", {}, `${i.cantidad} × ${i.nombre}`),
      el("span", {}, soles(i.precio * i.cantidad))))),
    pedido.observaciones && el("p", { class: "observacion" }, `Observaciones: ${pedido.observaciones}`),
    terminado
      ? el("p", { class: "motivo" }, `${ESTADOS[pedido.estado]}${pedido.motivo ? ": " + pedido.motivo : ""}`)
      : progreso(pedido.estado),
    el("div", { class: "pedido-pie" },
      el("span", {}, terminado ? "Total (no se cobra)" : "Total"),
      el("strong", {}, soles(pedido.total))),
    pedido.estado === "POR_CONFIRMAR" && el("button", {
      type: "button", class: "btn btn-secundario btn-bloque", onclick: () => abrirDialogoCancelar(pedido),
    }, "Cancelar pedido"));
}

function progreso(estadoPedido) {
  const actual = SECUENCIA.indexOf(estadoPedido);
  return el("ol", { class: "progreso", "aria-label": "Avance del pedido" }, SECUENCIA.map((e, i) => el("li", {
    class: i < actual ? "hecho" : i === actual ? "actual" : "",
    "aria-current": i === actual ? "step" : false,
  }, ESTADOS[e])));
}

iniciar();
