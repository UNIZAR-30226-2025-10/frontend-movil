import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.PersonasLike

class QuienLikeAdapter(private val lista: List<PersonasLike>) : RecyclerView.Adapter<QuienLikeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.imageView)
        val texto: TextView = view.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_artista, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val elemento = lista[position]
        holder.texto.text = elemento.nombreUsuario

        Glide.with(holder.itemView.context)
            .load(elemento.fotoPerfil)
            .circleCrop()
            .into(holder.imagen)
    }

    override fun getItemCount(): Int = lista.size
}
