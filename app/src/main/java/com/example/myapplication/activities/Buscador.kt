import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.activities.Inicio
import com.example.myapplication.io.ApiService
import com.example.myapplication.io.response.BuscadorResponse
import com.example.myapplication.utils.Preferencias
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Buscador : AppCompatActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscador)

        // Inicialización de ApiService
        apiService = ApiService.create()

        searchEditText = findViewById(R.id.searchInput)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        searchAdapter = SearchAdapter()
        recyclerView.adapter = searchAdapter

        // Configurar el TextWatcher para realizar la búsqueda automáticamente
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val termino = searchEditText.text.toString().trim()
                if (termino.isNotEmpty()) {
                    search(termino)
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })
    }

    private fun search(termino: String) {
        val token = Preferencias.obtenerValorString("token", "")
        val authHeader = "Bearer $token"
        // Llamada a la API
        apiService.searchBuscador(authHeader,termino).enqueue(object : Callback<BuscadorResponse> {
            override fun onResponse(call: Call<BuscadorResponse>, response: Response<BuscadorResponse>) {
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse != null) {
                        Log.d("MiApp", "Respuesta exitosa: ${searchResponse}")
                        if(searchResponse.respuestaHTTP == 0){
                            Preferencias.borrarDatosUsuario()
                            showToast("Logout existoso")
                            navigateInicio()
                        } else{
                            handleErrorCode(searchResponse.respuestaHTTP)
                        }
                    } else {
                        showToast("busqueda fallido: Datos incorrectos")
                    }
                } else {
                    showToast("Error en la busqueda: Código ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BuscadorResponse>, t: Throwable) {
                showToast("Error en la solicitud: ${t.message}")
                Log.e("MiApp", "Error en la solicitud: ${t.message}")
            }
        })
    }

    private fun handleErrorCode(statusCode: Int) {
        val message = when (statusCode) {
            400 -> "Error: Correo o usuario en uso"
            500 -> "Error interno del servidor"
            else -> "Error desconocido ($statusCode)"
        }
        showToast(message)
    }

    private fun navigateInicio() {
        val intent = Intent(this, Inicio::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
