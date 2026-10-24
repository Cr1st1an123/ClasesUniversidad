package com.example.saborlimeno.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.example.saborlimeno.MainActivity;
import com.example.saborlimeno.datos.Repositorio;
import com.example.saborlimeno.modelos.Usuario;

/** Decide a qué pantalla va cada rol y cómo se cierra la sesión. */
public final class Navegacion {
    public static final String EXTRA_NOMBRE = "nombre";
    public static final String EXTRA_ROL = "rol";

    private Navegacion() {
    }

    /** Devuelve la pantalla principal del rol, o null si ese módulo aún no existe. */
    public static Intent pantallaDeRol(Context contexto, Usuario usuario) {
        if (usuario.rol == null) return null;
        Intent intent;
        switch (usuario.rol) {
            case Usuario.MESERO:
            case Usuario.ADMINISTRADOR:
                intent = new Intent(contexto, MeseroActivity.class);
                break;
            default:
                // Cocina y cajero se agregan en la siguiente fase.
                return null;
        }
        intent.putExtra(EXTRA_NOMBRE, usuario.nombre);
        intent.putExtra(EXTRA_ROL, usuario.rol);
        return intent;
    }

    public static void cerrarSesion(Activity actividad) {
        new Repositorio().cerrarSesion();
        Intent intent = new Intent(actividad, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        actividad.startActivity(intent);
        actividad.finish();
    }
}
