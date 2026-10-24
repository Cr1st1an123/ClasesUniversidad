package com.example.saborlimeno.ui;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Formatos de texto que se repiten en varias pantallas. */
public final class Formato {
    private Formato() {
    }

    public static String soles(double monto) {
        return String.format(Locale.US, "S/ %.2f", monto);
    }

    public static String hora(Timestamp fecha) {
        if (fecha == null) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(fecha.toDate());
    }
}
