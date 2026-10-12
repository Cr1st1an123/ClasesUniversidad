// Acceso a datos (Cloud Firestore). Las pantallas no hablan con Firestore directamente,
// solo a través de estas funciones.
//
// Colecciones:
//   productos/{id}                         nombre, descripcion, categoria, precio, disponible, activo, imagenUrl?
//   mesas/{idAleatorio}                    numero, activa, atencionId (null si está libre)
//   atenciones/{id}                        mesaId, mesaNumero, abierta, cuentaSolicitada, fechaApertura, pago...
//   atenciones/{id}/pedidos/{id}           items[], observaciones, total, estado, origen, fechaCreacion...
import { db } from "./firebase.js";
import {
  collection, doc, getDoc, onSnapshot, query, serverTimestamp, setDoc, updateDoc, where,
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

const conId = (snap) => ({ id: snap.id, ...snap.data({ serverTimestamps: "estimate" }) });

export function escucharMesa(mesaId, alCambiar, alError) {
  return onSnapshot(doc(db, "mesas", mesaId), (s) => alCambiar(s.exists() ? conId(s) : null), alError);
}

export function escucharProductos(alCambiar, alError) {
  const activos = query(collection(db, "productos"), where("activo", "==", true));
  return onSnapshot(activos, (s) => {
    alCambiar(s.docs.map(conId).sort((a, b) => a.nombre.localeCompare(b.nombre, "es")));
  }, alError);
}

export function escucharAtencion(atencionId, alCambiar, alError) {
  return onSnapshot(doc(db, "atenciones", atencionId), (s) => alCambiar(s.exists() ? conId(s) : null), alError);
}

// "pendiente" indica que el pedido aún no fue confirmado por el servidor (se está enviando).
export function escucharPedidos(atencionId, alCambiar, alError) {
  const pedidos = collection(db, "atenciones", atencionId, "pedidos");
  return onSnapshot(pedidos, { includeMetadataChanges: true }, (s) => {
    alCambiar(s.docs.map((d) => ({ ...conId(d), pendiente: d.metadata.hasPendingWrites })));
  }, alError);
}

// El id se genera antes de enviar para que un reintento no cree un pedido duplicado.
export function nuevoIdPedido(atencionId) {
  return doc(collection(db, "atenciones", atencionId, "pedidos")).id;
}

export function enviarPedido(atencionId, pedidoId, pedido) {
  return setDoc(doc(db, "atenciones", atencionId, "pedidos", pedidoId), {
    items: pedido.items,
    observaciones: pedido.observaciones,
    total: pedido.total,
    mesaId: pedido.mesaId,
    mesaNumero: pedido.mesaNumero,
    estado: "POR_CONFIRMAR",
    origen: "CLIENTE",
    fechaCreacion: serverTimestamp(),
  });
}

export async function existePedido(atencionId, pedidoId) {
  const s = await getDoc(doc(db, "atenciones", atencionId, "pedidos", pedidoId));
  return s.exists();
}

export function cancelarPedido(atencionId, pedidoId, motivo) {
  return updateDoc(doc(db, "atenciones", atencionId, "pedidos", pedidoId), {
    estado: "CANCELADO",
    motivo,
    canceladoPor: "CLIENTE",
    fechaCancelacion: serverTimestamp(),
  });
}

export function solicitarCuenta(atencionId) {
  return updateDoc(doc(db, "atenciones", atencionId), {
    cuentaSolicitada: true,
    fechaSolicitudCuenta: serverTimestamp(),
  });
}
