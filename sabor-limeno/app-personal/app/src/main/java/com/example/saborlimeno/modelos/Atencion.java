package com.example.saborlimeno.modelos;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Map;

/** Documento atenciones/{id}: el servicio a una mesa desde que se abre hasta que se cobra. */
@IgnoreExtraProperties
public class Atencion {
    @DocumentId
    public String id;
    public String mesaId;
    public int mesaNumero;
    public boolean abierta;
    public boolean cuentaSolicitada;
    public Timestamp fechaApertura;
    public String abiertaPor;
    public Timestamp fechaCierre;
    /** medio, importe, fecha, cajero (lo registra el cajero al cerrar). */
    public Map<String, Object> pago;

    public Atencion() {
        // Constructor vacío requerido por Firestore.
    }
}
