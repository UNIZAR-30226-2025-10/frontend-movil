import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class ParticipantesAdapter(
    private val participantes: MutableList<String>,
    private val creador: String,
    private val onExpulsarClick: (String) -> Unit
) : RecyclerView.Adapter<ParticipantesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreUser: TextView = itemView.findViewById(R.id.nombreUser)
        val btnExpulsar: Button = itemView.findViewById(R.id.btnExpulsar)
        val numero: TextView = itemView.findViewById(R.id.numero)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participantes, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = participantes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario = participantes[position]
        holder.nombreUser.text = usuario
        holder.numero.text = (position + 1).toString()

        // Oculta el botón si es el creador
        if (usuario == creador) {
            holder.btnExpulsar.visibility = View.GONE
        } else {
            holder.btnExpulsar.visibility = View.VISIBLE
            holder.btnExpulsar.setOnClickListener {
                onExpulsarClick(usuario)
                removeParticipante(usuario) // <- aquí se elimina de la lista y se actualiza
            }
        }
    }

    fun removeParticipante(nombre: String) {
        val index = participantes.indexOf(nombre)
        if (index != -1) {
            participantes.removeAt(index)
            notifyItemRemoved(index)
            notifyItemRangeChanged(index, participantes.size)
        }
    }
}
