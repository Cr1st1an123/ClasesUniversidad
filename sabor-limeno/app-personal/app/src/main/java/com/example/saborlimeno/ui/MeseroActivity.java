package com.example.saborlimeno.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saborlimeno.R;
import com.example.saborlimeno.datos.Repositorio;
import com.example.saborlimeno.modelos.Atencion;
import com.example.saborlimeno.modelos.EstadoPedido;
import com.example.saborlimeno.modelos.Mesa;
import com.example.saborlimeno.modelos.Pedido;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Pantalla principal del mesero: mesas del restaurante y avisos de cada una (prototipo P05). */
public class MeseroActivity extends AppCompatActivity {

    private final Repositorio repositorio = new Repositorio();
    private final List<ListenerRegistration> escuchas = new ArrayList<>();
    private List<Mesa> mesas = new ArrayList<>();
    private List<Atencion> atenciones = new ArrayList<>();
    private List<Pedido> pedidosConAviso = new ArrayList<>();
    private MesaAdapter adaptador;
    private TextView tvResumen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesero);

        TextView tvUsuario = findViewById(R.id.tvUsuario);
        tvUsuario.setText(getIntent().getStringExtra(Navegacion.EXTRA_NOMBRE)
                + " · " + getIntent().getStringExtra(Navegacion.EXTRA_ROL));
        findViewById(R.id.btnSalir).setOnClickListener(v -> Navegacion.cerrarSesion(this));
        tvResumen = findViewById(R.id.tvResumen);

        RecyclerView rvMesas = findViewById(R.id.rvMesas);
        rvMesas.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.columnas_mesas)));
        adaptador = new MesaAdapter(this::alTocarMesa);
        rvMesas.setAdapter(adaptador);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Escucha en tiempo real mientras la pantalla está visible.
        escuchas.add(repositorio.escucharMesas(new Repositorio.Escucha<List<Mesa>>() {
            @Override
            public void alCambiar(List<Mesa> datos) {
                mesas = datos;
                refrescar();
            }

            @Override
            public void alFallar(String mensaje) {
                mostrarError(mensaje);
            }
        }));
        escuchas.add(repositorio.escucharAtencionesAbiertas(new Repositorio.Escucha<List<Atencion>>() {
            @Override
            public void alCambiar(List<Atencion> datos) {
                atenciones = datos;
                refrescar();
            }

            @Override
            public void alFallar(String mensaje) {
                mostrarError(mensaje);
            }
        }));
        escuchas.add(repositorio.escucharPedidosPorConfirmarYListos(new Repositorio.Escucha<List<Pedido>>() {
            @Override
            public void alCambiar(List<Pedido> datos) {
                pedidosConAviso = datos;
                refrescar();
            }

            @Override
            public void alFallar(String mensaje) {
                mostrarError(mensaje);
            }
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (ListenerRegistration escucha : escuchas) escucha.remove();
        escuchas.clear();
    }

    private void refrescar() {
        adaptador.actualizar(mesas, atenciones, pedidosConAviso);
        int porConfirmar = 0;
        int listos = 0;
        for (Pedido p : pedidosConAviso) {
            if (EstadoPedido.LISTO.equals(p.estado)) listos++;
            else porConfirmar++;
        }
        int ocupadas = 0;
        for (Mesa m : mesas) if (!m.estaLibre()) ocupadas++;
        tvResumen.setText(ocupadas + " de " + mesas.size() + " mesas atendiéndose · "
                + porConfirmar + " pedido(s) por confirmar · " + listos + " listo(s) para entregar");
    }

    private void alTocarMesa(Mesa mesa) {
        if (mesa.estaLibre()) {
            Dialogos.confirmar(this, "Mesa " + mesa.numero,
                    "¿Abrir una atención en esta mesa? Su QR quedará habilitado para pedir.",
                    "Abrir atención",
                    () -> repositorio.abrirAtencion(mesa)
                            .addOnSuccessListener(v -> Toast.makeText(this,
                                    "Atención abierta en la mesa " + mesa.numero, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> mostrarError(e.getMessage())));
        } else {
            Intent intent = new Intent(this, MesaDetalleActivity.class);
            intent.putExtra(MesaDetalleActivity.EXTRA_ATENCION_ID, mesa.atencionId);
            intent.putExtra(MesaDetalleActivity.EXTRA_MESA_NUMERO, mesa.numero);
            startActivity(intent);
        }
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
