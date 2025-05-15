import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.PersonasLike
import com.example.myapplication.io.response.Publicas

class PublicasAdapter(private val lista: List<Publicas>) : RecyclerView.Adapter<PublicasAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.imageView)
        val texto: TextView = view.findViewById(R.id.textView)
        val user: TextView = view.findViewById(R.id.usuarioPlaylist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val elemento = lista[position]
        holder.texto.text = elemento.nombre
        holder.user.text = elemento.creador

        if (elemento.fotoPortada == "DEFAULT") {
            holder.imagen.setImageResource(R.drawable.no_cancion)
        } else {
            Glide.with(holder.itemView.context)
                .load(elemento.fotoPortada)
                .into(holder.imagen)
        }
    }

    override fun getItemCount(): Int = lista.size
}
