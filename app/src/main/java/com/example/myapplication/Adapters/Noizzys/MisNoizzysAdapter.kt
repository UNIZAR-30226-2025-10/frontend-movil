package com.example.myapplication.Adapters.Noizzys

import android.content.Intent
import android.util.Log
import com.bumptech.glide.Glide
import com.example.myapplication.io.response.Noizzy
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.activities.NoizzyDetail
import com.example.myapplication.io.response.InvitacionPlaylist
import com.example.myapplication.utils.Preferencias

class MisNoizzysAdapter(
    private val noizzys: MutableList<Noizzy>,
    private val pantalla: String,
    private val onItemClicked: (Noizzy) -> Unit,
    private val onLikeClicked: (Noizzy) -> Unit,
    private val onCommentClicked: (Noizzy) -> Unit,
    private val onDeleteClicked: (Noizzy) -> Unit
) : RecyclerView.Adapter<MisNoizzysAdapter.NoizzyViewHolder>() {

    class NoizzyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.noizzyProfileImage)
        val userName: TextView = view.findViewById(R.id.noizzyUserName)
        val content: TextView = view.findViewById(R.id.noizzyContent)
        val numLikes: TextView = view.findViewById(R.id.likeCount)
        val numComments: TextView = view.findViewById(R.id.commentCount)
        val recuadroCancion: LinearLayout = view.findViewById(R.id.cancionNoizzy)
        val fotoCancion: ImageView = view.findViewById(R.id.recuerdoImage)
        val nombreCancion: TextView = view.findViewById(R.id.recuerdoText1)
        val nombreArtista: TextView = view.findViewById(R.id.recuerdoText2)

        val likeButton: ImageButton = view.findViewById(R.id.likeButton)
        val commentButton: ImageButton = view.findViewById(R.id.commentButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButtonNoizzy)
    }

    fun agregarNoizzy(noizzy: Noizzy) {
        noizzys.add(0, noizzy)
        notifyItemInserted(0)
    }

    fun eliminarNoizzyPorId(id: Int) {
        val index = noizzys.indexOfFirst { it.id == id }
        if (index != -1) {
            noizzys.removeAt(index)
            notifyItemRemoved(index)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoizzyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_noizzy, parent, false)
        return NoizzyViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoizzyViewHolder, position: Int) {
        val noizzy = noizzys[position]

        Glide.with(holder.itemView.context)
            .load(noizzy.fotoPerfil)
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.profileImage)


        holder.userName.text = noizzy.nombre
        holder.content.text = noizzy.texto
        holder.numLikes.text = noizzy.num_likes.toString()
        holder.numComments.text = noizzy.num_comentarios.toString()


        if(noizzy.cancion != null) {
            holder.recuadroCancion.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(noizzy.cancion.fotoPortada)
                .placeholder(R.drawable.no_cancion)
                .error(R.drawable.no_cancion)
                .into(holder.fotoCancion)

            holder.nombreCancion.text = noizzy.cancion.nombre
            holder.nombreArtista.text = noizzy.cancion.nombreArtisticoArtista
        } else {
            holder.recuadroCancion.visibility = View.GONE
        }

        if (noizzy.like) {
            Glide.with(holder.itemView.context)
                .load(R.drawable.like_noizzy_selected)
                .placeholder(R.drawable.like_noizzy_selected)
                .error(R.drawable.like_noizzy_selected)
                .into(holder.likeButton)
        } else {
            Glide.with(holder.itemView.context)
                .load(R.drawable.like_noizzy)
                .placeholder(R.drawable.like_noizzy)
                .error(R.drawable.like_noizzy)
                .into(holder.likeButton)
        }

        //val miNombreUser = Preferencias.obtenerValorString("nombreUsuario","")
        if (pantalla == "misNoizzys") {
            holder.deleteButton.visibility = View.VISIBLE
        } else {
            holder.deleteButton.visibility = View.GONE
        }
        holder.likeButton.setOnClickListener { onLikeClicked(noizzy) }
        holder.commentButton.setOnClickListener { onCommentClicked(noizzy) }
        holder.deleteButton.setOnClickListener { onDeleteClicked(noizzy) }
        holder.itemView.setOnClickListener {
            //Log.d("NoizzyAdapter", "ID que se pasa: ${noizzy.id}")
            //val intent = Intent(holder.itemView.context, NoizzyDetail::class.java)
            //intent.putExtra("id", noizzy.id.toString())
            //holder.itemView.context.startActivity(intent)
            onItemClicked(noizzy)
        }
    }

    fun actualizarNoizzy(noizzyActualizado: Noizzy) {
        val index = noizzys.indexOfFirst { it.id == noizzyActualizado.id }
        if (index != -1) {
            noizzys[index] = noizzyActualizado
            notifyItemChanged(index)
        }
    }

    override fun getItemCount(): Int = noizzys.size
}
