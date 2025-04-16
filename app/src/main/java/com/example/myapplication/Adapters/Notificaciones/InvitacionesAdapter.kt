package com.example.myapplication.Adapters.Notificaciones

import android.os.strictmode.IncorrectContextUseViolation
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.io.response.Interaccion
import com.example.myapplication.io.response.InvitacionPlaylist


class InvitacionesAdapter(
    private val lista:  MutableList<InvitacionPlaylist>,
    private val onAceptarClick: (InvitacionPlaylist) -> Unit,
    private val onRechazarClick: (InvitacionPlaylist) -> Unit
) : RecyclerView.Adapter<InvitacionesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val btnAceptar: Button = view.findViewById(R.id.btnAceptar)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazar)
    }

    fun agregarInvitacion(invitacion: InvitacionPlaylist) {
        lista.add(invitacion)
        notifyItemInserted(lista.size - 1)
    }

    fun eliminarInvitacion(invitacion: InvitacionPlaylist) {
        val index = lista.indexOf(invitacion)
        if (index != -1) {
            lista.removeAt(index)
            notifyItemRemoved(index)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion_invitacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notificacion = lista[position]

        holder.tvTitulo.text = "Te han invitado a colaborar en " + notificacion.nombre
        holder.tvDescripcion.text = "Podrás añadir canciones a esta lista de " + notificacion.nombreUsuario

        holder.btnAceptar.setOnClickListener {
            onAceptarClick(notificacion)
        }

        holder.btnRechazar.setOnClickListener {
            onRechazarClick(notificacion)
        }
    }

    override fun getItemCount(): Int = lista.size
}
