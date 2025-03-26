import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class WebSocketManager {

    companion object {
        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }

    private var socket: Socket? = null
    private var sid: String? = null // Guardamos el SID aquí

    fun connectWebSocket(token: String, onMessageReceived: (String) -> Unit, onConnectError: (String) -> Unit) {
        if (socket != null && socket!!.connected()) {
            Log.d("WebSocket", "Ya hay una conexión activa")
            return
        }

        try {
            val options = IO.Options()
            options.transports = arrayOf("websocket", "polling")
            options.forceNew = true
            options.reconnection = true
            options.extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))

            socket = IO.socket("http://172.20.10.4:5000", options)
            //socket = IO.socket("http://api-noizz.onrender.com", options)
            //socket = IO.socket("http://192.1.65.102:5000", options)

            // Evento de conexión
            socket?.on(Socket.EVENT_CONNECT) {
                sid = socket?.id() // Guardamos el SID
                Log.d("WebSocket", "Conectado al servidor con SID: $sid")
            }

            // Manejo de errores
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("WebSocket", "Error de conexión: ${args[0]}")
                onConnectError(args[0].toString())
            }

            // Escuchar mensajes
            socket?.on("mensaje") { args ->
                val mensaje = args[0].toString()
                Log.d("WebSocket", "Mensaje recibido: $mensaje")
                onMessageReceived(mensaje)
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("WebSocket", "Error al conectar: ${e.message}")
            onConnectError(e.message ?: "Error desconocido")
        }
    }

    // Método para obtener el SID
    fun getSid(): String? {
        return sid
    }

    fun closeWebSocket() {
        socket?.disconnect()
        socket = null
        sid = null
    }
}
