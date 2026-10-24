package com.example.saborlimeno.modelos;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

/** Documento usuarios/{uid}. */
@IgnoreExtraProperties
public class Usuario {
    public static final String MESERO = "mesero";
    public static final String COCINA = "cocina";
    public static final String CAJERO = "cajero";
    public static final String ADMINISTRADOR = "administrador";

    @DocumentId
    public String id;
    public String nombre;
    public String rol;
    public boolean activo;

    public Usuario() {
        // Constructor vacío requerido por Firestore.
    }
}
