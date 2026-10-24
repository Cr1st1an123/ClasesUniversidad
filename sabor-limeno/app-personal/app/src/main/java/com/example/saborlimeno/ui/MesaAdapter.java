package com.example.saborlimeno.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.saborlimeno.R;
import com.example.saborlimeno.modelos.Atencion;
import com.example.saborlimeno.modelos.EstadoPedido;
import com.example.saborlimeno.modelos.Mesa;
import com.example.saborlimeno.modelos.Pedido;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tarjetas de la cuadrícula de mesas del mesero. */
public class MesaAdapter extends RecyclerView.Adapter<MesaAdapter.Vista> {

    public interface AlTocar {
        void alTocar(Mesa mesa);
    }

    private final AlTocar alTocar;
    private final List<Mesa> mesas = new ArrayList<>();
    private final Map<String, Atencion> atencionesPorId = new HashMap<>();
    private final Map<String, Integer> porConfirmar = new HashMap<>(); // atencionId -> cantidad
    private final Map<String, Integer> listos = new HashMap<>();

    public MesaAdapter(AlTocar alTocar) {
        this.alTocar = alTocar;
    }

    public void actualizar(List<Mesa> nuevasMesas, List<Atencion> atenciones, List<Pedido> pedidos) {
        mesas.clear();
        mesas.addAll(nuevasMesas);
        atencionesPorId.clear();
        for (Atencion a : atenciones) atencionesPorId.put(a.id, a);
        porConfirmar.clear();
        listos.clear();
        for (Pedido p : pedidos) {
            Map<String, Integer> conteo = EstadoPedido.LISTO.equals(p.estado) ? listos : porConfirmar;
            Integer actual = conteo.get(p.atencionId);
            conteo.put(p.atencionId, actual == null ? 1 : actual + 1);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext()).inflate(R.layout.item_mesa, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista v, int posicion) {
        Mesa mesa = mesas.get(posicion);
        v.tvNumero.setText("Mesa " + mesa.numero);

        if (mesa.estaLibre()) {
            v.tvEstado.setText("Libre");
            v.tvAvisos.setVisibility(View.GONE);
            v.tarjeta.setStrokeColor(ContextCompat.getColor(v.itemView.getContext(), R.color.borde));
        } else {
            v.tvEstado.setText("Atendiendo");
            v.tarjeta.setStrokeColor(ContextCompat.getColor(v.itemView.getContext(), R.color.terracota));
            StringBuilder avisos = new StringBuilder();
            agregarAviso(avisos, porConfirmar.get(mesa.atencionId), " por confirmar");
            agregarAviso(avisos, listos.get(mesa.atencionId), " listo(s) para entregar");
            Atencion atencion = atencionesPorId.get(mesa.atencionId);
            if (atencion != null && atencion.cuentaSolicitada) {
                if (avisos.length() > 0) avisos.append('\n');
                avisos.append("Cuenta solicitada");
            }
            v.tvAvisos.setText(avisos);
            v.tvAvisos.setVisibility(avisos.length() > 0 ? View.VISIBLE : View.GONE);
        }
        v.itemView.setOnClickListener(x -> alTocar.alTocar(mesa));
    }

    private static void agregarAviso(StringBuilder avisos, Integer cantidad, String texto) {
        if (cantidad == null || cantidad == 0) return;
        if (avisos.length() > 0) avisos.append('\n');
        avisos.append(cantidad).append(texto);
    }

    @Override
    public int getItemCount() {
        return mesas.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final MaterialCardView tarjeta;
        final TextView tvNumero;
        final TextView tvEstado;
        final TextView tvAvisos;

        Vista(View vista) {
            super(vista);
            tarjeta = (MaterialCardView) vista;
            tvNumero = vista.findViewById(R.id.tvNumero);
            tvEstado = vista.findViewById(R.id.tvEstado);
            tvAvisos = vista.findViewById(R.id.tvAvisos);
        }
    }
}
