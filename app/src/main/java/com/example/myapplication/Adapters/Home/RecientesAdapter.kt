package com.example.myapplication.Adapters.Home

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.myapplication.R
import com.example.myapplication.io.response.*

class RecientesAdapter(private var listaRecientes: MutableList<HRecientes>) :
    RecyclerView.Adapter<RecientesAdapter.RecienteHViewHolder>() {
    // Cambia el método para actualizar la lista
    fun updateDataReciente(searchResponse: List<HRecientes>) {
        listaRecientes.clear()
        listaRecientes.addAll(searchResponse)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecienteHViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reciente, parent, false)
        return RecienteHViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecienteHViewHolder, position: Int) {
        val Reciente = listaRecientes[position]
        holder.nombre.text = Reciente.nombre
        holder.autor.text = Reciente.autor

        var foto: Any
        if (Reciente.fotoPortada == "DEFAULT") {
            foto = R.drawable.no_cancion
        } else {
            foto = Reciente.fotoPortada
        }

        Glide.with(holder.itemView.context)
            .load(foto)
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            12f,
                            holder.itemView.context.resources.displayMetrics
                        ).toInt()
                    )
                )
            )
            .into(holder.fotoPortada)
    }

    override fun getItemCount(): Int {
        return listaRecientes.size
    }

    class RecienteHViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.textViewReciente)
        val autor: TextView = itemView.findViewById(R.id.textViewReciente2)
        val fotoPortada: ImageView = itemView.findViewById(R.id.imageViewReciente)
    }
}