package com.example.saborlimeno.modelos;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

/** Documento mesas/{idAleatorio}. El id es el que va en el QR de la mesa. */
@IgnoreExtraProperties
public class Mesa {
    @DocumentId
    public String id;
    public int numero;
    public boolean activa;
    /** Id de la atención abierta, o null si la mesa está libre. */
    public String atencionId;

    public Mesa() {
        // Constructor vacío requerido por Firestore.
    }

    public boolean estaLibre() {
        return atencionId == null || atencionId.isEmpty();
    }
}
