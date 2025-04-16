package com.example.myapplication.Adapters.Notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.Novedad


class InteraccionesAdapter(
    private val lista:  MutableList<Interaccion>,
    private val onAceptarClick: (Interaccion) -> Unit,
) : RecyclerView.Adapter<InteraccionesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val btnAceptar: Button = view.findViewById(R.id.btnAceptar)
    }

    fun agregarInteraccion(interaccion: Interaccion) {
        lista.add(interaccion)
        notifyItemInserted(lista.size - 1)
    }

    fun eliminarInteraccion(interaccion: Interaccion) {
        val iterator = lista.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().noizzy == interaccion.noizzy) {
                iterator.remove()
            }
        }
        notifyDataSetChanged()
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion_interaccion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notificacion = lista[position]

        if (notificacion.tipo == "like") {
            holder.tvTitulo.text = notificacion.nombreUsuario + " le ha dado me gusta a tu Noizzy"
        } else {
            holder.tvTitulo.text = notificacion.nombreUsuario + " ha respondido a tu Noizzy"
        }

        holder.tvDescripcion.text = notificacion.texto

        holder.btnAceptar.setOnClickListener {
            onAceptarClick(notificacion)
        }

    }

    override fun getItemCount(): Int = lista.size
}
