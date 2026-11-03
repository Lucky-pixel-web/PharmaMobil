
package pe.edu.upeu.pharmamobil.data.repository

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.error.mensajeUsuario
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacion
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacionEnMemoria
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.util.ahoraEnMilisegundos

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoRemoto,
    private val almacen: AlmacenSincronizacion =
        AlmacenSincronizacionEnMemoria(),
    private val ahora: () -> Long = ::ahoraEnMilisegundos
) : ProductoRepository {

    private val candado = Mutex()

    override fun observar(): Flow<List<Producto>> =
        local.observar()

    override suspend fun registrar(producto: Producto): Producto {
        val guardado = local.guardarPendiente(producto)

        intentar {
            sincronizar()
        }

        return guardado
    }

    override suspend fun sincronizar() = candado.withLock {
        // Primero enviar las filas pendientes.
        subirPendientes()

        // Después descargar la información del servidor.
        local.reemplazarSincronizados(remoto.listar())

        // Guardar la fecha solo si la sincronización terminó.
        almacen.registrar(ahora())
    }

    override suspend fun listar(): List<Producto> {
        intentar {
            sincronizar()
        }
        return local.observar().first()
    }

    override suspend fun obtener(id: Long): Producto =
        remoto.obtener(id)

    override suspend fun actualizar(producto: Producto): Producto {
        val actualizado = remoto.actualizar(producto)

        intentar {
            sincronizar()
        }

        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        remoto.eliminar(id)

        intentar {
            sincronizar()
        }
    }

    private suspend fun subirPendientes() {
        val pendientes = local.pendientes(MAX_INTENTOS)

        pendientes.forEach { fila ->

            val producto = fila.producto
            val resultado = enviar(producto)

            when (resultado) {

                is ResultadoEnvio.Exito -> {
                    local.eliminarLocal(producto.id)
                }

                is ResultadoEnvio.Transitorio -> {
                    local.sumarIntento(
                        producto.id,
                        resultado.motivo
                    )

                    delay(espera(fila.intentos))
                }

                is ResultadoEnvio.Permanente -> {
                    local.marcarFallida(
                        producto.id,
                        resultado.motivo,
                        MAX_INTENTOS
                    )
                }
            }
        }
    }

    private suspend fun enviar(
        producto: Producto
    ): ResultadoEnvio {
        return try {
            val guardado = remoto.registrar(producto)

            ResultadoEnvio.Exito(
                idServidor = guardado.id
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: ErrorApiException) {
            e.error.aResultado()
        }
    }

    private fun espera(intentos: Int): Long =
        1_000L shl intentos.coerceIn(0, 4)

    private suspend fun intentar(
        bloque: suspend () -> Unit
    ) {
        try {
            bloque()

        } catch (e: CancellationException) {
            throw e

        } catch (e: ErrorApiException) {
            val sinRed =
                e.error == ErrorApi.SinConexion ||
                        e.error == ErrorApi.TiempoAgotado

            if (!sinRed) {
                throw e
            }
        }
    }

    companion object {
        const val MAX_INTENTOS = 5
    }
}

private fun ErrorApi.aResultado(): ResultadoEnvio =
    when (this) {

        ErrorApi.SinConexion,
        ErrorApi.TiempoAgotado,
        ErrorApi.Servidor ->
            ResultadoEnvio.Transitorio(
                mensajeUsuario()
            )

        is ErrorApi.Validacion,
        ErrorApi.NoEncontrado,
        is ErrorApi.Conflicto,
        ErrorApi.SinPermiso,
        ErrorApi.SesionExpirada ->
            ResultadoEnvio.Permanente(
                mensajeUsuario()
            )
    }
