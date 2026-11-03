
package pe.edu.upeu.pharmamobil.permiso

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PuentePermisos(
    actividad: ComponentActivity
) {
    private var pendiente: CompletableDeferred<Boolean>? = null
    private val mutex = Mutex()

    private val lanzador = actividad.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        pendiente?.complete(concedido)
    }

    suspend fun pedir(nombre: String): Boolean =
        mutex.withLock {
            val resultado = CompletableDeferred<Boolean>()
            pendiente = resultado

            try {
                lanzador.launch(nombre)
                resultado.await()
            } finally {
                if (pendiente === resultado) {
                    pendiente = null
                }
            }
        }
}
