package com.example.myapplication.Adapters.Notificaciones

import android.os.strictmode.IncorrectContextUseViolation
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.Seguidor


class SeguidoresAdapter(
    private val lista:  MutableList<Seguidor>,
    private val onAceptarClick: (Seguidor) -> Unit,
    private val onCerrarClick: (Seguidor) -> Unit
) : RecyclerView.Adapter<SeguidoresAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val btnAceptar: Button = view.findViewById(R.id.btnAceptar)
        val btnCerrar: ImageButton = view.findViewById(R.id.btnCerrar)
    }

    fun agregarSeguidor(seguidor: Seguidor) {
        lista.add(seguidor)
        notifyItemInserted(lista.size - 1)
    }

    fun eliminarSeguidor(seguidor: Seguidor) {
        val index = lista.indexOf(seguidor)
        if (index != -1) {
            lista.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion_seguidor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notificacion = lista[position]

        holder.tvTitulo.text = notificacion.nombre + " ha comenzado ha seguirte"

        holder.btnAceptar.setOnClickListener {
            onAceptarClick(notificacion)
        }

        holder.btnCerrar.setOnClickListener {
            onCerrarClick(notificacion)
        }

    }

    override fun getItemCount(): Int = lista.size
}
