package com.example.saborlimeno.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saborlimeno.R;
import com.example.saborlimeno.datos.Repositorio;
import com.example.saborlimeno.modelos.Atencion;
import com.example.saborlimeno.modelos.EstadoPedido;
import com.example.saborlimeno.modelos.Pedido;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Pedidos de una mesa con atención abierta: confirmar, rechazar, cancelar y entregar (prototipo P06). */
public class MesaDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_ATENCION_ID = "atencionId";
    public static final String EXTRA_MESA_NUMERO = "mesaNumero";

    private final Repositorio repositorio = new Repositorio();
    private final List<ListenerRegistration> escuchas = new ArrayList<>();
    private String atencionId;
    private int mesaNumero;
    private PedidoAdapter adaptador;
    private TextView tvCuenta;
    private TextView tvCuentaSolicitada;
    private TextView tvVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mesa_detalle);

        atencionId = getIntent().getStringExtra(EXTRA_ATENCION_ID);
        mesaNumero = getIntent().getIntExtra(EXTRA_MESA_NUMERO, 0);

        TextView tvTitulo = findViewById(R.id.tvTitulo);
        tvTitulo.setText("Mesa " + mesaNumero);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
        tvCuenta = findViewById(R.id.tvCuenta);
        tvCuentaSolicitada = findViewById(R.id.tvCuentaSolicitada);
        tvVacio = findViewById(R.id.tvVacio);

        RecyclerView rvPedidos = findViewById(R.id.rvPedidos);
        rvPedidos.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.columnas_pedidos)));
        adaptador = new PedidoAdapter(this::alAccionar);
        rvPedidos.setAdapter(adaptador);
    }

    @Override
    protected void onStart() {
        super.onStart();
        escuchas.add(repositorio.escucharAtencion(atencionId, new Repositorio.Escucha<Atencion>() {
            @Override
            public void alCambiar(Atencion atencion) {
                if (atencion == null || !atencion.abierta) {
                    Toast.makeText(MesaDetalleActivity.this,
                            "La atención de la mesa " + mesaNumero + " ya fue cerrada.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                tvCuentaSolicitada.setVisibility(atencion.cuentaSolicitada ? View.VISIBLE : View.GONE);
            }

            @Override
            public void alFallar(String mensaje) {
                mostrarError(mensaje);
            }
        }));
        escuchas.add(repositorio.escucharPedidosDeAtencion(atencionId, new Repositorio.Escucha<List<Pedido>>() {
            @Override
            public void alCambiar(List<Pedido> pedidos) {
                adaptador.actualizar(pedidos);
                tvVacio.setVisibility(pedidos.isEmpty() ? View.VISIBLE : View.GONE);
                mostrarCuenta(pedidos);
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

    /** Solo se suman los pedidos confirmados (RF12). */
    private void mostrarCuenta(List<Pedido> pedidos) {
        double total = 0;
        int porConfirmar = 0;
        for (Pedido p : pedidos) {
            if (EstadoPedido.esCobrable(p.estado)) total += p.total;
            if (EstadoPedido.POR_CONFIRMAR.equals(p.estado)) porConfirmar++;
        }
        String texto = "Cuenta: " + Formato.soles(total);
        if (porConfirmar > 0) texto += " · " + porConfirmar + " pedido(s) por confirmar";
        tvCuenta.setText(texto);
    }

    private void alAccionar(Pedido pedido, int numero, String nuevoEstado) {
        String titulo = "Pedido " + numero;
        switch (nuevoEstado) {
            case EstadoPedido.RECHAZADO:
                Dialogos.pedirMotivo(this, "Rechazar " + titulo, "Rechazar",
                        motivo -> cambiarEstado(pedido, numero, nuevoEstado, motivo));
                break;
            case EstadoPedido.CANCELADO:
                Dialogos.pedirMotivo(this, "Cancelar " + titulo, "Cancelar pedido",
                        motivo -> cambiarEstado(pedido, numero, nuevoEstado, motivo));
                break;
            case EstadoPedido.ENTREGADO:
                Dialogos.confirmar(this, titulo, "¿Se entregó el pedido completo en la mesa?", "Sí, entregado",
                        () -> cambiarEstado(pedido, numero, nuevoEstado, null));
                break;
            default:
                cambiarEstado(pedido, numero, nuevoEstado, null);
                break;
        }
    }

    private void cambiarEstado(Pedido pedido, int numero, String nuevoEstado, String motivo) {
        repositorio.cambiarEstado(pedido, nuevoEstado, motivo)
                .addOnSuccessListener(v -> Toast.makeText(this,
                        "Pedido " + numero + ": " + EstadoPedido.texto(nuevoEstado), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> mostrarError(e.getMessage()));
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }
}
