package com.example.saborlimeno.modelos;

import com.google.firebase.firestore.IgnoreExtraProperties;

/** Un producto dentro de un pedido, con el precio que tenía al enviarse. */
@IgnoreExtraProperties
public class ItemPedido {
    public String productoId;
    public String nombre;
    public double precio;
    public int cantidad;

    public ItemPedido() {
        // Constructor vacío requerido por Firestore.
    }

    public double subtotal() {
        return precio * cantidad;
    }
}
