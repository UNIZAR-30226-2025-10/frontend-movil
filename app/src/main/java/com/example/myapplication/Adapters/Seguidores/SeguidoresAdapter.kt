package com.example.myapplication.Adapters.Seguidores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.Seguidores

class SeguidoresAdapter(
    private var followers: MutableList<Seguidores>,
    private val followListener: OnFollowListener
) : RecyclerView.Adapter<SeguidoresAdapter.FollowersViewHolder>() {

    interface OnFollowListener {
        fun onFollowStatusChanged(userId: String, isFollowing: Boolean, position: Int)
    }

    class FollowersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val btnFollow: Button = view.findViewById(R.id.btnFollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowersViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seguidos, parent, false)
        return FollowersViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowersViewHolder, position: Int) {
        val user = followers[position]

        Glide.with(holder.itemView.context)
            .load(user.fotoPerfil)
            .placeholder(R.drawable.ic_profile)
            .circleCrop()
            .into(holder.ivProfile)

        holder.tvName.text = user.nombreUsuario
        updateFollowButton(holder.btnFollow, user.followBack)

        holder.btnFollow.setOnClickListener {
            val newFollowStatus = !user.followBack
            // Actualizar visualmente inmediatamente
            user.followBack = newFollowStatus
            updateFollowButton(holder.btnFollow, newFollowStatus)
            // Notificar al activity para llamar a la API
            followListener.onFollowStatusChanged(user.nombreUsuario, newFollowStatus, position)
        }
    }

    private fun updateFollowButton(button: Button, isFollowing: Boolean) {
        button.text = if (isFollowing) "Siguiendo" else "Seguir"
        button.setBackgroundResource(
            if (isFollowing) R.drawable.bg_following
            else R.drawable.bg_not_following
        )
    }

    override fun getItemCount() = followers.size

    fun updateList(newFollowers: List<Seguidores>) {
        followers.clear()
        followers.addAll(newFollowers)
        notifyDataSetChanged()
    }

    fun updateItem(position: Int, isFollowing: Boolean) {
        if (position in 0 until followers.size) {
            followers[position].followBack = isFollowing
            notifyItemChanged(position)
        }
    }
}