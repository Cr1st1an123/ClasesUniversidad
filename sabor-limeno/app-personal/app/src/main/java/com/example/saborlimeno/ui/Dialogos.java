package com.example.saborlimeno.ui;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Diálogos de confirmación que usan varias pantallas. */
public final class Dialogos {

    public interface AlConfirmar {
        void alConfirmar();
    }

    public interface AlIngresarMotivo {
        void alIngresar(String motivo);
    }

    private Dialogos() {
    }

    public static void confirmar(Context contexto, String titulo, String mensaje, String boton, AlConfirmar accion) {
        new MaterialAlertDialogBuilder(contexto)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setNegativeButton("Volver", null)
                .setPositiveButton(boton, (dialogo, cual) -> accion.alConfirmar())
                .show();
    }

    /** Pide un motivo obligatorio (para rechazar o cancelar un pedido). */
    public static void pedirMotivo(Context contexto, String titulo, String boton, AlIngresarMotivo accion) {
        EditText campo = new EditText(contexto);
        campo.setHint("Motivo (lo verá el cliente)");
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        campo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});

        FrameLayout contenedor = new FrameLayout(contexto);
        int margen = (int) (20 * contexto.getResources().getDisplayMetrics().density);
        contenedor.setPadding(margen, margen / 2, margen, 0);
        contenedor.addView(campo);

        new MaterialAlertDialogBuilder(contexto)
                .setTitle(titulo)
                .setView(contenedor)
                .setNegativeButton("Volver", null)
                .setPositiveButton(boton, (dialogo, cual) -> {
                    String motivo = campo.getText().toString().trim();
                    if (motivo.isEmpty()) {
                        Toast.makeText(contexto, "Debe indicar un motivo.", Toast.LENGTH_SHORT).show();
                    } else {
                        accion.alIngresar(motivo);
                    }
                })
                .show();
    }
}
