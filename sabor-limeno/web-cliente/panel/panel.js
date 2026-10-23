// Panel de pruebas: simula al personal (mesero, cocina y cajero) para probar la web del cliente.
// Será reemplazado por la aplicación Android.
import { app, db, modoEmulador } from "../js/firebase.js";
import {
  connectAuthEmulator, getAuth, onAuthStateChanged, signInWithEmailAndPassword, signOut,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";
import {
  collection, doc, getDoc, getDocs, onSnapshot, orderBy, query, runTransaction, serverTimestamp,
  updateDoc, where, writeBatch,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const auth = getAuth(app);
if (modoEmulador) connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });

const ESTADOS = {
  POR_CONFIRMAR: "Por confirmar", PENDIENTE: "Pendiente", EN_PREPARACION: "En preparación",
  LISTO: "Listo", ENTREGADO: "Entregado", RECHAZADO: "Rechazado", CANCELADO: "Cancelado",
};
// [texto del botón, estado nuevo, pide motivo]
const ACCIONES = {
  POR_CONFIRMAR: [["Confirmar (mesero)", "PENDIENTE"], ["Rechazar", "RECHAZADO", true], ["Cancelar", "CANCELADO", true]],
  PENDIENTE: [["Iniciar preparación (cocina)", "EN_PREPARACION"], ["Cancelar", "CANCELADO", true]],
  EN_PREPARACION: [["Marcar listo (cocina)", "LISTO"]],
  LISTO: [["Entregar (mesero)", "ENTREGADO"]],
};
const CAMPO_FECHA = {
  PENDIENTE: "fechaConfirmacion", EN_PREPARACION: "fechaInicioPreparacion", LISTO: "fechaListo",
  ENTREGADO: "fechaEntrega", RECHAZADO: "fechaRechazo", CANCELADO: "fechaCancelacion",
};
const ESTADOS_FINALES = ["ENTREGADO", "RECHAZADO", "CANCELADO"];
const ESTADOS_COBRABLES = ["PENDIENTE", "EN_PREPARACION", "LISTO", "ENTREGADO"];

const PRODUCTOS_EJEMPLO = [
  ["Papa a la huancaína", "Entradas", 12, "Papa amarilla con crema de ají amarillo y queso fresco."],
  ["Causa limeña", "Entradas", 14, "Masa de papa amarilla rellena de pollo y palta."],
  ["Lomo saltado", "Platos", 28, "Trozos de res salteados con cebolla, tomate y papas nativas."],
  ["Ají de gallina", "Platos", 22, "Pollo deshilachado en crema de ají amarillo."],
  ["Arroz con pollo", "Platos", 20, "Arroz verde al culantro con presa de pollo."],
  ["Seco de res", "Platos", 26, "Res guisada al culantro con frejoles.", false],
  ["Chicha morada", "Bebidas", 8, "Bebida tradicional de maíz morado."],
  ["Limonada", "Bebidas", 7, "Limonada fresca de limón peruano."],
  ["Inca Kola 500 ml", "Bebidas", 5, ""],
  ["Suspiro a la limeña", "Postres", 10, "Manjar blanco con merengue al oporto."],
];

let usuario = null;
let mesas = [];
let productos = [];
const pedidosPorAtencion = new Map(); // atencionId -> { mesaNumero, pedidos }
const escuchasPedidos = new Map();
let escuchas = [];

const $ = (id) => document.getElementById(id);
const soles = (n) => "S/ " + Number(n).toFixed(2);
const mensaje = (texto) => { $("mensaje").textContent = texto; };

function el(etiqueta, atributos = {}, ...hijos) {
  const nodo = document.createElement(etiqueta);
  for (const [k, v] of Object.entries(atributos)) {
    if (v === false || v == null) continue;
    if (k.startsWith("on")) nodo.addEventListener(k.slice(2), v);
    else if (k in nodo && k !== "list") nodo[k] = v;
    else nodo.setAttribute(k, v);
  }
  for (const h of hijos.flat()) if (h != null && h !== false) nodo.append(h);
  return nodo;
}

async function ejecutar(accion) {
  try {
    mensaje("");
    await accion();
  } catch (e) {
    console.error(e);
    mensaje("Error: " + (e.message || e.code));
  }
}

// Enlace que irá en el QR de cada mesa. Publicado en Hosting queda como https://TU-PROYECTO.web.app/?m=ID
function enlaceMesa(mesaId) {
  const url = new URL("../", location.href);
  url.search = `?m=${mesaId}${modoEmulador ? "&emulador" : ""}`;
  return url.href;
}

function idAleatorio(largo = 10) {
  const letras = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return Array.from(crypto.getRandomValues(new Uint8Array(largo)), (b) => letras[b % letras.length]).join("");
}

// ---------- Sesión ----------

$("form-login").addEventListener("submit", (e) => {
  e.preventDefault();
  ejecutar(() => signInWithEmailAndPassword(auth, $("correo").value, $("clave").value));
});
$("btn-salir").addEventListener("click", () => signOut(auth));

onAuthStateChanged(auth, async (u) => {
  escuchas.forEach((detener) => detener());
  escuchas = [];
  escuchasPedidos.forEach((detener) => detener());
  escuchasPedidos.clear();
  pedidosPorAtencion.clear();
  usuario = u;
  $("form-login").hidden = Boolean(u);
  $("sesion-activa").hidden = !u;
  $("contenido").hidden = true;
  if (!u) return;

  const perfil = await getDoc(doc(db, "usuarios", u.uid)).catch(() => null);
  if (!perfil?.exists() || perfil.data().activo !== true) {
    $("info-usuario").textContent = `Sesión iniciada como ${u.email}.`;
    mensaje(`Falta registrar este usuario en Firestore: crea el documento usuarios/${u.uid} con los campos `
      + `nombre (string), rol = "administrador" (string) y activo = true (boolean). Luego recarga la página.`);
    return;
  }
  $("info-usuario").textContent = `${perfil.data().nombre} (${perfil.data().rol})`;
  $("contenido").hidden = false;
  escuchar();
});

// ---------- Escucha de datos ----------

function escuchar() {
  escuchas.push(onSnapshot(query(collection(db, "mesas"), orderBy("numero")), (s) => {
    mesas = s.docs.map((d) => ({ id: d.id, ...d.data() }));
    renderMesas();
  }));
  escuchas.push(onSnapshot(collection(db, "productos"), (s) => {
    productos = s.docs.map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => a.categoria.localeCompare(b.categoria) || a.nombre.localeCompare(b.nombre));
    renderProductos();
  }));
  escuchas.push(onSnapshot(query(collection(db, "atenciones"), where("abierta", "==", true)), (s) => {
    const abiertas = new Map(s.docs.map((d) => [d.id, d.data()]));
    for (const [id, detener] of escuchasPedidos) {
      if (!abiertas.has(id)) { detener(); escuchasPedidos.delete(id); pedidosPorAtencion.delete(id); }
    }
    for (const [id, atencion] of abiertas) {
      const previo = pedidosPorAtencion.get(id);
      pedidosPorAtencion.set(id, { ...atencion, pedidos: previo?.pedidos ?? [] });
      if (escuchasPedidos.has(id)) continue;
      escuchasPedidos.set(id, onSnapshot(collection(db, "atenciones", id, "pedidos"), (sp) => {
        const datos = pedidosPorAtencion.get(id);
        if (!datos) return;
        datos.pedidos = sp.docs.map((d) => ({ id: d.id, ...d.data({ serverTimestamps: "estimate" }) }))
          .sort((a, b) => (a.fechaCreacion?.toMillis() ?? 0) - (b.fechaCreacion?.toMillis() ?? 0));
        renderPedidos();
        renderMesas();
      }));
    }
    renderPedidos();
    renderMesas();
  }));
}

// ---------- Datos de ejemplo ----------

$("btn-semilla").addEventListener("click", () => ejecutar(async () => {
  const lote = writeBatch(db);
  const existentes = await getDocs(collection(db, "productos"));
  const nombres = new Set(existentes.docs.map((d) => d.data().nombre));
  for (const [nombre, categoria, precio, descripcion, disponible = true] of PRODUCTOS_EJEMPLO) {
    if (nombres.has(nombre)) continue;
    lote.set(doc(collection(db, "productos")), { nombre, categoria, precio, descripcion, disponible, activo: true });
  }
  const mesasExistentes = await getDocs(collection(db, "mesas"));
  const numeros = new Set(mesasExistentes.docs.map((d) => d.data().numero));
  for (let numero = 1; numero <= 10; numero++) {
    if (numeros.has(numero)) continue;
    lote.set(doc(db, "mesas", idAleatorio()), { numero, activa: true, atencionId: null });
  }
  await lote.commit();
  mensaje("Datos de ejemplo cargados.");
}));

// ---------- Mesero y cajero: abrir y cerrar atenciones ----------

function abrirAtencion(mesa) {
  return ejecutar(async () => {
    await runTransaction(db, async (t) => {
      const refMesa = doc(db, "mesas", mesa.id);
      const actual = await t.get(refMesa);
      if (actual.data().atencionId) throw new Error(`La mesa ${mesa.numero} ya tiene una atención abierta.`);
      const refAtencion = doc(collection(db, "atenciones"));
      t.set(refAtencion, {
        mesaId: mesa.id, mesaNumero: mesa.numero, abierta: true, cuentaSolicitada: false,
        fechaApertura: serverTimestamp(), abiertaPor: usuario.uid,
      });
      t.update(refMesa, { atencionId: refAtencion.id });
    });
    mensaje(`Atención abierta en la mesa ${mesa.numero}.`);
  });
}

function cerrarAtencion(mesa, medio) {
  return ejecutar(async () => {
    const atencionId = mesa.atencionId;
    const pedidos = (await getDocs(collection(db, "atenciones", atencionId, "pedidos"))).docs.map((d) => d.data());
    if (pedidos.some((p) => !ESTADOS_FINALES.includes(p.estado))) {
      throw new Error("Hay pedidos que aún no están entregados, rechazados o cancelados.");
    }
    const importe = Math.round(pedidos.filter((p) => ESTADOS_COBRABLES.includes(p.estado))
      .reduce((s, p) => s + p.total, 0) * 100) / 100;
    await runTransaction(db, async (t) => {
      const refAtencion = doc(db, "atenciones", atencionId);
      const atencion = await t.get(refAtencion);
      if (!atencion.data().abierta) throw new Error("La atención ya estaba cerrada.");
      t.update(refAtencion, {
        abierta: false,
        fechaCierre: serverTimestamp(),
        pago: { medio, importe, fecha: serverTimestamp(), cajero: usuario.uid },
      });
      t.update(doc(db, "mesas", mesa.id), { atencionId: null });
    });
    mensaje(`Mesa ${mesa.numero}: pago de ${soles(importe)} registrado y atención cerrada.`);
  });
}

// ---------- Mesero y cocina: cambiar estados ----------

function cambiarEstado(atencionId, pedido, nuevo, pideMotivo) {
  let motivo = null;
  if (pideMotivo) {
    motivo = prompt(`Motivo (${ESTADOS[nuevo]}):`)?.trim();
    if (!motivo) return;
  }
  return ejecutar(() => runTransaction(db, async (t) => {
    const ref = doc(db, "atenciones", atencionId, "pedidos", pedido.id);
    const actual = await t.get(ref);
    if (actual.data().estado !== pedido.estado) throw new Error("El pedido cambió de estado; revise la lista.");
    const cambios = { estado: nuevo, [CAMPO_FECHA[nuevo]]: serverTimestamp(), actualizadoPor: usuario.uid };
    if (motivo) cambios.motivo = motivo;
    if (nuevo === "CANCELADO") cambios.canceladoPor = "MESERO";
    t.update(ref, cambios);
  }));
}

// ---------- Pantalla ----------

function renderMesas() {
  $("tabla-mesas").replaceChildren(...mesas.map((m) => {
    const atencion = m.atencionId && pedidosPorAtencion.get(m.atencionId);
    const medio = el("select", {}, ["Efectivo", "Tarjeta", "Transferencia"].map((x) => el("option", { value: x }, x)));
    let acciones;
    if (!m.atencionId) acciones = el("button", { onclick: () => abrirAtencion(m) }, "Abrir atención");
    else acciones = el("span", {}, medio, el("button", { onclick: () => cerrarAtencion(m, medio.value) }, "Registrar pago y cerrar"));
    const enlace = enlaceMesa(m.id);
    return el("tr", {},
      el("td", {}, `Mesa ${m.numero}`),
      el("td", {}, el("a", { href: enlace, target: "_blank" }, enlace)),
      el("td", {}, m.atencionId ? "Abierta" : "Libre",
        atencion?.cuentaSolicitada ? el("div", { className: "alerta" }, "¡Cuenta solicitada!") : null),
      el("td", {}, acciones));
  }));
}

function renderPedidos() {
  const bloques = [];
  for (const [atencionId, atencion] of pedidosPorAtencion) {
    for (const p of atencion.pedidos) {
      bloques.push(el("div", { className: "pedido" },
        el("div", {}, el("strong", {}, `Mesa ${atencion.mesaNumero}`), ` · ${p.origen} · `,
          el("span", { className: "estado" }, ESTADOS[p.estado]), ` · ${soles(p.total)}`),
        el("div", {}, p.items.map((i) => `${i.cantidad} × ${i.nombre} (${soles(i.precio)})`).join(", ")),
        p.observaciones ? el("div", {}, `Obs.: ${p.observaciones}`) : null,
        p.motivo ? el("div", {}, `Motivo: ${p.motivo}`) : null,
        el("div", {}, (ACCIONES[p.estado] ?? []).map(([texto, nuevo, pideMotivo]) =>
          el("button", { onclick: () => cambiarEstado(atencionId, p, nuevo, pideMotivo) }, texto)))));
    }
  }
  $("lista-pedidos").replaceChildren(...(bloques.length ? bloques : [el("p", {}, "No hay pedidos.")]));
}

function renderProductos() {
  $("tabla-productos").replaceChildren(...productos.map((p) => el("tr", {},
    el("td", {}, p.nombre),
    el("td", {}, p.categoria),
    el("td", {}, el("input", {
      type: "number", min: "0.5", step: "0.5", value: p.precio, style: "width:6rem",
      onchange: (e) => ejecutar(() => updateDoc(doc(db, "productos", p.id), { precio: Number(e.target.value) })),
    })),
    el("td", {}, el("input", {
      type: "checkbox", checked: p.disponible,
      onchange: (e) => ejecutar(() => updateDoc(doc(db, "productos", p.id), { disponible: e.target.checked })),
    })))));
}
