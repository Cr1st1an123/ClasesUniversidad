package com.example.saborlimeno.datos;

import com.example.saborlimeno.modelos.Atencion;
import com.example.saborlimeno.modelos.EstadoPedido;
import com.example.saborlimeno.modelos.Mesa;
import com.example.saborlimeno.modelos.Pedido;
import com.example.saborlimeno.modelos.Usuario;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a Firebase (Authentication y Cloud Firestore).
 * Las pantallas no usan Firestore directamente: siempre pasan por esta clase.
 */
public class Repositorio {

    /** Recibe los datos cada vez que cambian en la base de datos (tiempo real). */
    public interface Escucha<T> {
        void alCambiar(T datos);

        void alFallar(String mensaje);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // ---------------- Sesión ----------------

    public Task<?> iniciarSesion(String correo, String clave) {
        return auth.signInWithEmailAndPassword(correo, clave);
    }

    public void cerrarSesion() {
        auth.signOut();
    }

    public String uidActual() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public Task<DocumentSnapshot> obtenerUsuarioActual() {
        return db.collection("usuarios").document(uidActual()).get();
    }

    public static Usuario aUsuario(DocumentSnapshot documento) {
        return documento.exists() ? documento.toObject(Usuario.class) : null;
    }

    // ---------------- Mesas y atenciones ----------------

    public ListenerRegistration escucharMesas(Escucha<List<Mesa>> escucha) {
        return db.collection("mesas").orderBy("numero").addSnapshotListener((snap, error) -> {
            if (error != null) {
                escucha.alFallar(error.getMessage());
                return;
            }
            List<Mesa> mesas = new ArrayList<>();
            for (QueryDocumentSnapshot d : snap) {
                Mesa mesa = d.toObject(Mesa.class);
                if (mesa.activa) mesas.add(mesa);
            }
            escucha.alCambiar(mesas);
        });
    }

    public ListenerRegistration escucharAtencionesAbiertas(Escucha<List<Atencion>> escucha) {
        return db.collection("atenciones").whereEqualTo("abierta", true).addSnapshotListener((snap, error) -> {
            if (error != null) {
                escucha.alFallar(error.getMessage());
                return;
            }
            escucha.alCambiar(snap.toObjects(Atencion.class));
        });
    }

    public ListenerRegistration escucharAtencion(String atencionId, Escucha<Atencion> escucha) {
        return db.collection("atenciones").document(atencionId).addSnapshotListener((snap, error) -> {
            if (error != null) {
                escucha.alFallar(error.getMessage());
                return;
            }
            escucha.alCambiar(snap.exists() ? snap.toObject(Atencion.class) : null);
        });
    }

    /** Abre una atención en una mesa libre. La transacción impide abrir dos a la vez en la misma mesa. */
    public Task<Void> abrirAtencion(Mesa mesa) {
        DocumentReference refMesa = db.collection("mesas").document(mesa.id);
        DocumentReference refAtencion = db.collection("atenciones").document();
        String uid = uidActual();
        return db.runTransaction(transaccion -> {
            DocumentSnapshot actual = transaccion.get(refMesa);
            String abierta = actual.getString("atencionId");
            if (abierta != null && !abierta.isEmpty()) {
                throw new FirebaseFirestoreException("La mesa " + mesa.numero + " ya tiene una atención abierta.",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            Map<String, Object> atencion = new HashMap<>();
            atencion.put("mesaId", mesa.id);
            atencion.put("mesaNumero", mesa.numero);
            atencion.put("abierta", true);
            atencion.put("cuentaSolicitada", false);
            atencion.put("fechaApertura", FieldValue.serverTimestamp());
            atencion.put("abiertaPor", uid);
            transaccion.set(refAtencion, atencion);
            transaccion.update(refMesa, "atencionId", refAtencion.getId());
            return null;
        });
    }

    // ---------------- Pedidos ----------------

    /** Pedidos de una atención, del más reciente al más antiguo. */
    public ListenerRegistration escucharPedidosDeAtencion(String atencionId, Escucha<List<Pedido>> escucha) {
        return db.collection("atenciones").document(atencionId).collection("pedidos")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        escucha.alFallar(error.getMessage());
                        return;
                    }
                    List<Pedido> pedidos = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) pedidos.add(Pedido.desde(d));
                    escucha.alCambiar(pedidos);
                });
    }

    /** Pedidos de TODAS las mesas que están en alguno de los estados indicados. */
    public ListenerRegistration escucharPedidosPorEstado(List<String> estados, Escucha<List<Pedido>> escucha) {
        return db.collectionGroup("pedidos").whereIn("estado", estados).addSnapshotListener((snap, error) -> {
            if (error != null) {
                escucha.alFallar(error.getMessage());
                return;
            }
            List<Pedido> pedidos = new ArrayList<>();
            for (QueryDocumentSnapshot d : snap) pedidos.add(Pedido.desde(d));
            escucha.alCambiar(pedidos);
        });
    }

    public ListenerRegistration escucharPedidosPorConfirmarYListos(Escucha<List<Pedido>> escucha) {
        return escucharPedidosPorEstado(Arrays.asList(EstadoPedido.POR_CONFIRMAR, EstadoPedido.LISTO), escucha);
    }

    /**
     * Cambia el estado de un pedido solo si sigue en el estado que vio el usuario.
     * Así, si dos personas actúan a la vez sobre el mismo pedido, solo vale la primera.
     */
    public Task<Void> cambiarEstado(Pedido pedido, String nuevoEstado, String motivo) {
        DocumentReference ref = db.collection("atenciones").document(pedido.atencionId)
                .collection("pedidos").document(pedido.id);
        String estadoVisto = pedido.estado;
        String uid = uidActual();
        return db.runTransaction(transaccion -> {
            DocumentSnapshot actual = transaccion.get(ref);
            if (!estadoVisto.equals(actual.getString("estado"))) {
                throw new FirebaseFirestoreException("El pedido cambió de estado. Revise la lista.",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            Map<String, Object> cambios = new HashMap<>();
            cambios.put("estado", nuevoEstado);
            cambios.put(EstadoPedido.campoFecha(nuevoEstado), FieldValue.serverTimestamp());
            cambios.put("actualizadoPor", uid);
            if (motivo != null) cambios.put("motivo", motivo);
            if (EstadoPedido.CANCELADO.equals(nuevoEstado)) cambios.put("canceladoPor", "MESERO");
            transaccion.update(ref, cambios);
            return null;
        });
    }
}
