package com.example.saborlimeno.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

/** Documento atenciones/{atencionId}/pedidos/{id}. */
@IgnoreExtraProperties
public class Pedido {
    @DocumentId
    public String id;
    public List<ItemPedido> items = new ArrayList<>();
    public String observaciones;
    public double total;
    public String estado;
    /** "CLIENTE" (desde la web) o "MESERO" (pedido asistido). */
    public String origen;
    public String motivo;
    public String mesaId;
    public int mesaNumero;
    public Timestamp fechaCreacion;

    /** Id de la atención a la que pertenece (no se guarda: sale de la ruta del documento). */
    @Exclude
    public String atencionId;

    public Pedido() {
        // Constructor vacío requerido por Firestore.
    }

    /** Convierte un documento de Firestore en Pedido, completando el id de su atención. */
    public static Pedido desde(DocumentSnapshot documento) {
        Pedido pedido = documento.toObject(Pedido.class);
        if (pedido != null && documento.getReference().getParent().getParent() != null) {
            pedido.atencionId = documento.getReference().getParent().getParent().getId();
        }
        return pedido;
    }
}
