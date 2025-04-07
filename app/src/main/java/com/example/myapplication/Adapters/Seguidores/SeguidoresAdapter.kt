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
    private val followers: List<Seguidores>
) : RecyclerView.Adapter<SeguidoresAdapter.FollowersViewHolder>() {

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

        // Load profile image
        Glide.with(holder.itemView.context)
            .load(user.fotoPerfil)
            .placeholder(R.drawable.ic_profile)
            .circleCrop()
            .into(holder.ivProfile)

        holder.tvName.text = user.nombreUsuario

        // Configure follow button
        holder.btnFollow.text = if (user.followBack) "✔" else "+"
        holder.btnFollow.setBackgroundResource(
            if (user.followBack) R.drawable.bg_following
            else R.drawable.bg_not_following
        )
    }

    override fun getItemCount() = followers.size
}