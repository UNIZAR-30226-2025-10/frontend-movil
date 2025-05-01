package com.example.myapplication.Adapters.Noizzys

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.activities.NoizzyDetail
import com.example.myapplication.io.response.NoizzitoData
import com.example.myapplication.io.response.Noizzy
import com.example.myapplication.io.response.NoizzyDetailResponse

class NoizzyDetailAdapter(
    private val noizzys: MutableList<NoizzitoData>,
    private val onItemClicked: (NoizzitoData) -> Unit,
    private val onLikeClicked: (NoizzitoData) -> Unit,
    private val onCommentClicked: (NoizzitoData) -> Unit,
    private val onDeleteClicked: (NoizzitoData) -> Unit
) : RecyclerView.Adapter<NoizzyDetailAdapter.NoizzyViewHolder>() {

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

    fun agregarNoizzito(noizzito: NoizzitoData) {
        noizzys.add(0, noizzito)
        notifyItemInserted(0)
    }

    fun eliminarNoizzitoPorId (id: String) {
        val index = noizzys.indexOfFirst { it.id == id}
        if (index != -1) {
            noizzys.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun actualizarNoizzy(noizzy: NoizzitoData) {
        val index = noizzys.indexOfFirst { it.id == noizzy.id }
        if (index != -1) {
            noizzys[index] = noizzy
            notifyItemChanged(index)
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

        if (noizzy.mio) {
            holder.deleteButton.visibility = View.VISIBLE
        }

        if (noizzy.cancion != null) {
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

        val likeDrawable = if (noizzy.like) {
            R.drawable.like_noizzy_selected
        } else {
            R.drawable.like_noizzy
        }

        Glide.with(holder.itemView.context)
            .load(likeDrawable)
            .placeholder(likeDrawable)
            .error(likeDrawable)
            .into(holder.likeButton)

        holder.likeButton.setOnClickListener { onLikeClicked(noizzy) }
        holder.commentButton.setOnClickListener { onCommentClicked(noizzy) }
        holder.deleteButton.setOnClickListener { onDeleteClicked(noizzy) }
        holder.itemView.setOnClickListener {
            //val intent = Intent(holder.itemView.context, NoizzyDetail::class.java)
            //intent.putExtra("id", noizzy.id)
            //holder.itemView.context.startActivity(intent)
            onItemClicked(noizzy)
        }
    }

    override fun getItemCount(): Int = noizzys.size
}