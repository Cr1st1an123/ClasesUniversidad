package com.example.saborlimeno;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.saborlimeno.datos.Repositorio;
import com.example.saborlimeno.modelos.Usuario;
import com.example.saborlimeno.ui.Navegacion;
import com.google.android.material.textfield.TextInputEditText;

/** Pantalla de ingreso del personal (correo y contraseña de Firebase Authentication). */
public class MainActivity extends AppCompatActivity {

    private final Repositorio repositorio = new Repositorio();
    private TextInputEditText etCorreo;
    private TextInputEditText etClave;
    private Button btnIngresar;
    private ProgressBar progreso;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCorreo = findViewById(R.id.etCorreo);
        etClave = findViewById(R.id.etClave);
        btnIngresar = findViewById(R.id.btnIngresar);
        progreso = findViewById(R.id.progreso);
        tvError = findViewById(R.id.tvError);

        btnIngresar.setOnClickListener(v -> ingresar());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Si ya había una sesión abierta, entra directo.
        if (repositorio.uidActual() != null) verificarUsuario();
    }

    private void ingresar() {
        String correo = texto(etCorreo);
        String clave = texto(etClave);
        if (correo.isEmpty() || clave.isEmpty()) {
            mostrarError("Ingrese su correo y contraseña.");
            return;
        }
        cargando(true);
        repositorio.iniciarSesion(correo, clave)
                .addOnSuccessListener(resultado -> verificarUsuario())
                .addOnFailureListener(e -> {
                    cargando(false);
                    mostrarError("Correo o contraseña incorrectos, o no hay conexión a internet.");
                });
    }

    /** Lee el documento usuarios/{uid} para saber si la cuenta está activa y qué rol tiene. */
    private void verificarUsuario() {
        cargando(true);
        repositorio.obtenerUsuarioActual()
                .addOnSuccessListener(documento -> {
                    Usuario usuario = Repositorio.aUsuario(documento);
                    if (usuario == null) {
                        rechazar("Este usuario no está registrado en el sistema. Consulte con el administrador.");
                    } else if (!usuario.activo) {
                        rechazar("Su cuenta está desactivada. Consulte con el administrador.");
                    } else {
                        Intent pantalla = Navegacion.pantallaDeRol(this, usuario);
                        if (pantalla == null) {
                            rechazar("El módulo de " + usuario.rol + " estará disponible en la siguiente versión.");
                        } else {
                            startActivity(pantalla);
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Sin conexión no se cierra la sesión: el usuario puede reintentar con "Ingresar".
                    cargando(false);
                    mostrarError("No se pudo verificar el usuario. Revise su conexión a internet e intente de nuevo.");
                });
    }

    private void rechazar(String mensaje) {
        repositorio.cerrarSesion();
        cargando(false);
        mostrarError(mensaje);
    }

    private void cargando(boolean activo) {
        progreso.setVisibility(activo ? View.VISIBLE : View.GONE);
        btnIngresar.setEnabled(!activo);
        if (activo) tvError.setText("");
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
    }

    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
