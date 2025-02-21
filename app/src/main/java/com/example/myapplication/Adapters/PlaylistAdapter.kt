package com.example.myapplication.Adapters
/*class PlaylistAdapter(private val playlistList: List<Playlist>) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlistList[position]
        holder.bind(playlist)
    }

    override fun getItemCount(): Int = playlistList.size

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.playlistName)
        private val songCount: TextView = itemView.findViewById(R.id.playlistSongCount)

        fun bind(playlist: Playlist) {
            name.text = playlist.name
            songCount.text = "${playlist.songCount} canciones"
        }
    }
}*/