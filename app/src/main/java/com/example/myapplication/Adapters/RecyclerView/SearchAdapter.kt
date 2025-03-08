import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.io.response.Cancion

class SearchAdapter : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    private var results: List<Cancion> = emptyList()

    // Método para actualizar los datos en el adaptador
    fun updateData(searchResponse: BuscadorResponse) {
        results = searchResponse.canciones  // Aquí puedes combinar los distintos tipos de resultados si lo necesitas
        notifyDataSetChanged()  // Notifica que los datos han cambiado
    }

    // Crea y devuelve una nueva vista para cada ítem
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false)
        return ViewHolder(view)
    }

    // Asocia los datos del modelo con la vista del ítem
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        holder.textView.text = item.nombre  // Seteamos el nombre de la canción
        Glide.with(holder.itemView.context)  // Usamos Glide para cargar la imagen
            .load(item.fotoPortada)
            .into(holder.imageView)  // Cargamos la imagen en el ImageView
    }

    // Devuelve el número total de ítems en el adaptador
    override fun getItemCount(): Int = results.size

    // Clase ViewHolder que mantiene las vistas para un ítem
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)  // Imagen de la portada
        val textView: TextView = view.findViewById(R.id.textView)  // Texto para el nombre de la canción
    }
}
