package com.example.saborlimeno.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saborlimeno.R;
import com.example.saborlimeno.modelos.EstadoPedido;
import com.example.saborlimeno.modelos.ItemPedido;
import com.example.saborlimeno.modelos.Pedido;

import java.util.ArrayList;
import java.util.List;

/** Tarjetas de los pedidos de una mesa, con las acciones que le corresponden al mesero (prototipo P06). */
public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.Vista> {

    public interface AlAccionar {
        void alAccionar(Pedido pedido, int numero, String nuevoEstado);
    }

    private final AlAccionar alAccionar;
    private final List<Pedido> pedidos = new ArrayList<>();

    public PedidoAdapter(AlAccionar alAccionar) {
        this.alAccionar = alAccionar;
    }

    /** Recibe los pedidos del más reciente al más antiguo. */
    public void actualizar(List<Pedido> nuevos) {
        pedidos.clear();
        pedidos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_pedido, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista v, int posicion) {
        Pedido p = pedidos.get(posicion);
        int numero = pedidos.size() - posicion; // el más antiguo es el pedido 1
        Context contexto = v.itemView.getContext();

        String origen = "MESERO".equals(p.origen) ? "Mesero" : "Cliente";
        v.tvTitulo.setText("Pedido " + numero + " · " + Formato.hora(p.fechaCreacion) + " · " + origen);
        pintarEstado(v.tvEstado, p.estado, contexto);

        StringBuilder items = new StringBuilder();
        for (ItemPedido item : p.items) {
            if (items.length() > 0) items.append('\n');
            items.append(item.cantidad).append(" × ").append(item.nombre)
                    .append("   ").append(Formato.soles(item.subtotal()));
        }
        v.tvItems.setText(items);
        mostrarSiHay(v.tvObservaciones, TextUtils.isEmpty(p.observaciones) ? null : "Observaciones: " + p.observaciones);
        mostrarSiHay(v.tvMotivo, TextUtils.isEmpty(p.motivo) ? null : "Motivo: " + p.motivo);
        v.tvTotal.setText("Total: " + Formato.soles(p.total));

        // Acciones del mesero según el estado (RF08, RF10 y RF11).
        ocultar(v.btnPrincipal, v.btnSecundario, v.btnCancelar);
        String nota = null;
        switch (p.estado == null ? "" : p.estado) {
            case EstadoPedido.POR_CONFIRMAR:
                accion(v.btnPrincipal, "Confirmar", p, numero, EstadoPedido.PENDIENTE);
                accion(v.btnSecundario, "Rechazar", p, numero, EstadoPedido.RECHAZADO);
                accion(v.btnCancelar, "Cancelar", p, numero, EstadoPedido.CANCELADO);
                break;
            case EstadoPedido.PENDIENTE:
                accion(v.btnCancelar, "Cancelar", p, numero, EstadoPedido.CANCELADO);
                nota = "Enviado a cocina; aún no empiezan a prepararlo.";
                break;
            case EstadoPedido.EN_PREPARACION:
                nota = "Cocina lo está preparando. Ya no se puede cancelar.";
                break;
            case EstadoPedido.LISTO:
                accion(v.btnPrincipal, "Confirmar entrega", p, numero, EstadoPedido.ENTREGADO);
                break;
            default:
                break;
        }
        mostrarSiHay(v.tvNota, nota);
    }

    private void accion(Button boton, String texto, Pedido pedido, int numero, String nuevoEstado) {
        boton.setText(texto);
        boton.setVisibility(View.VISIBLE);
        boton.setOnClickListener(x -> alAccionar.alAccionar(pedido, numero, nuevoEstado));
    }

    private static void ocultar(View... vistas) {
        for (View vista : vistas) vista.setVisibility(View.GONE);
    }

    private static void mostrarSiHay(TextView vista, String texto) {
        vista.setText(texto);
        vista.setVisibility(texto == null ? View.GONE : View.VISIBLE);
    }

    /** El estado siempre se muestra con texto; el color solo lo acompaña (RNF07). */
    static void pintarEstado(TextView etiqueta, String estado, Context contexto) {
        int fondo;
        int texto;
        switch (estado == null ? "" : estado) {
            case EstadoPedido.POR_CONFIRMAR:
                fondo = R.color.ambar_fondo;
                texto = R.color.ambar;
                break;
            case EstadoPedido.PENDIENTE:
                fondo = R.color.azul_fondo;
                texto = R.color.azul;
                break;
            case EstadoPedido.EN_PREPARACION:
                fondo = R.color.naranja_fondo;
                texto = R.color.terracota_oscuro;
                break;
            case EstadoPedido.LISTO:
            case EstadoPedido.ENTREGADO:
                fondo = R.color.verde_fondo;
                texto = R.color.verde;
                break;
            default:
                fondo = R.color.rojo_fondo;
                texto = R.color.rojo;
                break;
        }
        etiqueta.setText(EstadoPedido.texto(estado));
        etiqueta.setTextColor(ContextCompat.getColor(contexto, texto));
        etiqueta.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(contexto, fondo)));
    }

    @Override
    public int getItemCount() {
        return pedidos.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView tvTitulo;
        final TextView tvEstado;
        final TextView tvItems;
        final TextView tvObservaciones;
        final TextView tvMotivo;
        final TextView tvTotal;
        final TextView tvNota;
        final Button btnPrincipal;
        final Button btnSecundario;
        final Button btnCancelar;

        Vista(View vista) {
            super(vista);
            tvTitulo = vista.findViewById(R.id.tvTitulo);
            tvEstado = vista.findViewById(R.id.tvEstado);
            tvItems = vista.findViewById(R.id.tvItems);
            tvObservaciones = vista.findViewById(R.id.tvObservaciones);
            tvMotivo = vista.findViewById(R.id.tvMotivo);
            tvTotal = vista.findViewById(R.id.tvTotal);
            tvNota = vista.findViewById(R.id.tvNota);
            btnPrincipal = vista.findViewById(R.id.btnPrincipal);
            btnSecundario = vista.findViewById(R.id.btnSecundario);
            btnCancelar = vista.findViewById(R.id.btnCancelar);
        }
    }
}
