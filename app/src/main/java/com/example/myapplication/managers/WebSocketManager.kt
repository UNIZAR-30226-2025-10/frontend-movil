import android.util.Log
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

class WebSocketManager{

    companion object {
        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }

    private var webSocket: WebSocket? = null
    private var sid: String? = null // Almacena el sid
    private val client = OkHttpClient()

    fun connectWebSocket(token: String, onMessageReceived: (String) -> Unit) {
        if (webSocket != null) {
            Log.d("WebSocket", "Ya hay una conexión WebSocket activa")
            return
        }

        sid = UUID.randomUUID().toString() // Genera un sid único

        val request = Request.Builder()
            .url("wss://api-noizz.onrender.com/socket.io/?EIO=4&transport=websocket") // URL del servidor WebSocket
            .addHeader("Authorization", "Bearer $token") // Token JWT
            .addHeader("sid", sid!!) // Envía el sid
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Conexión abierta con sid: $sid")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Mensaje recibido: $text")

                // Verifica si el mensaje es la respuesta con el SID
                if (text.startsWith("0{")) {
                    try {
                        val json = JSONObject(text.substring(1)) // Elimina el primer "0"
                        sid = json.getString("sid") // Almacena el SID recibido
                        Log.d("WebSocket", "SID asignado desde el servidor: $sid")
                    } catch (e: JSONException) {
                        Log.e("WebSocket", "Error al parsear el SID: ${e.message}")
                    }
                }

                onMessageReceived(text)
            }


            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Conexión cerrada: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error en la conexión: ${t.message}")
            }
        }

        webSocket = client.newWebSocket(request, listener)
    }

    fun getSid(): String? {
        return sid
    }

    fun closeWebSocket() {
        webSocket?.close(1000, "Cierre normal")
        webSocket = null
        sid = null
    }
}

