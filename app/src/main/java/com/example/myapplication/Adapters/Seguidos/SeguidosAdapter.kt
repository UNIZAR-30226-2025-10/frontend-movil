package com.example.myapplication.Adapters.Seguidos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.Seguidos

class SeguidosAdapter(
    private val following: MutableList<Seguidos>,
    private val unfollowListener: OnUnfollowListener
) : RecyclerView.Adapter<SeguidosAdapter.FollowingViewHolder>() {

    interface OnUnfollowListener {
        fun onUnfollow(userId: String, position: Int)
    }

    class FollowingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val btnUnfollow: Button = view.findViewById(R.id.btnFollow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seguidos, parent, false)
        return FollowingViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowingViewHolder, position: Int) {
        val user = following[position]

        Glide.with(holder.itemView.context)
            .load(user.fotoPerfil)
            .placeholder(R.drawable.ic_profile)
            .circleCrop()
            .into(holder.ivProfile)

        holder.tvName.text = user.nombreUsuario

        holder.btnUnfollow.setOnClickListener {
            unfollowListener.onUnfollow(user.nombreUsuario, position)
        }
    }

    override fun getItemCount() = following.size
}