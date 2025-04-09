package com.example.myapplication.Adapters.SubirCancion

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.MiAlbum
import android.widget.ImageView
import android.widget.TextView


class MisAlbumesListAdapter(
    context: Context,
    private val albums: MutableList<MiAlbum>
) : ArrayAdapter<MiAlbum>(context, 0, albums) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_spinneralbum, parent, false)
        val album = albums[position]

        val imageView = view.findViewById<ImageView>(R.id.albumImage)
        val textView = view.findViewById<TextView>(R.id.albumName)

        textView.text = album.nombre

        if (album.id == "") {
            imageView.visibility = View.GONE
        } else {
            imageView.visibility = View.VISIBLE
            Glide.with(context)
                .load(album.fotoPortada)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imageView)
        }

        return view
    }
}
