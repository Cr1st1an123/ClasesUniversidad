package com.example.saborlimeno.modelos;

/** Estados de un pedido. Deben coincidir con los que usa la web del cliente. */
public final class EstadoPedido {
    public static final String POR_CONFIRMAR = "POR_CONFIRMAR";
    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_PREPARACION = "EN_PREPARACION";
    public static final String LISTO = "LISTO";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String CANCELADO = "CANCELADO";

    private EstadoPedido() {
    }

    /** Texto que se muestra en pantalla para cada estado. */
    public static String texto(String estado) {
        if (estado == null) return "";
        switch (estado) {
            case POR_CONFIRMAR: return "Por confirmar";
            case PENDIENTE: return "Pendiente";
            case EN_PREPARACION: return "En preparación";
            case LISTO: return "Listo";
            case ENTREGADO: return "Entregado";
            case RECHAZADO: return "Rechazado";
            case CANCELADO: return "Cancelado";
            default: return estado;
        }
    }

    /** Nombre del campo de fecha que se guarda al pasar a cada estado. */
    public static String campoFecha(String estado) {
        switch (estado) {
            case PENDIENTE: return "fechaConfirmacion";
            case EN_PREPARACION: return "fechaInicioPreparacion";
            case LISTO: return "fechaListo";
            case ENTREGADO: return "fechaEntrega";
            case RECHAZADO: return "fechaRechazo";
            case CANCELADO: return "fechaCancelacion";
            default: return "fechaActualizacion";
        }
    }

    /** Pedidos que se suman a la cuenta. */
    public static boolean esCobrable(String estado) {
        return PENDIENTE.equals(estado) || EN_PREPARACION.equals(estado)
                || LISTO.equals(estado) || ENTREGADO.equals(estado);
    }
}
