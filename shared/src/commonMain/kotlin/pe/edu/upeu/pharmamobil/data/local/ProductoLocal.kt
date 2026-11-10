
package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.Producto as ProductoFila
import pe.edu.upeu.pharmamobil.domain.model.EstadoEnvios
import pe.edu.upeu.pharmamobil.domain.model.FalloEnvio

data class ProductoPendiente(
    val producto: Producto,
    val intentos: Int
)

class ProductoLocal(base: PharmaDb) {

    private val consultas = base.productoQueries

    fun observar(): Flow<List<Producto>> =
        consultas.listarTodos()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas ->
                filas.map { it.aDominio() }
            }

    fun pendientes(): List<Producto> =
        consultas.listarPendientes()
            .executeAsList()
            .map { it.aDominio() }

    fun observarEstadoEnvios(maxIntentos: Int): Flow<EstadoEnvios> =
        consultas.listarPendientes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas ->

                val bloqueadas = filas.filter {
                    it.intentos >= maxIntentos.toLong()
                }

                EstadoEnvios(
                    pendientes = filas.size - bloqueadas.size,
                    fallidas = bloqueadas.size,
                    detallesFallidos = bloqueadas.map {
                        FalloEnvio(
                            id = it.id,
                            nombre = it.nombre,
                            motivo = it.ultimoError
                                ?: "No se recibió el detalle del error"
                        )
                    }
                )
            }

    fun pendientes(maxIntentos: Int): List<ProductoPendiente> =
        consultas.pendientesPorEnviar(maxIntentos.toLong())
            .executeAsList()
            .map { fila ->
                ProductoPendiente(
                    producto = fila.aDominio(),
                    intentos = fila.intentos.toInt()
                )
            }

    fun guardarPendiente(producto: Producto): Producto {
        val menor = consultas.siguienteIdLocal().executeAsOne()
        val local = producto.copy(
            id = minOf(menor, 0L) - 1L
        )

        consultas.insertar(
            local.id,
            local.nombre,
            local.precio,
            local.stock.toLong(),
            if (local.activo) 1L else 0L,
            1L
        )

        return local
    }

    fun sumarIntento(id: Long, motivo: String) {
        consultas.sumarIntento(motivo, id)
    }

    fun marcarFallida(
        id: Long,
        motivo: String,
        maxIntentos: Int
    ) {
        consultas.marcarFallida(
            maxIntentos.toLong(),
            motivo,
            id
        )
    }

    fun contarPendientes(maxIntentos: Int): Long =
        consultas.contarPendientes(
            maxIntentos.toLong()
        ).executeAsOne()

    fun contarFallidas(maxIntentos: Int): Long =
        consultas.contarFallidas(
            maxIntentos.toLong()
        ).executeAsOne()

    fun reiniciarIntentos(id: Long) {
        consultas.reiniciarIntentos(id)
    }

    fun eliminarLocal(id: Long) {
        consultas.borrarPorId(id)
    }

    fun reemplazarSincronizados(productos: List<Producto>) {
        consultas.transaction {
            consultas.borrarSincronizados()

            productos.forEach {
                consultas.insertar(
                    it.id,
                    it.nombre,
                    it.precio,
                    it.stock.toLong(),
                    if (it.activo) 1L else 0L,
                    0L
                )
            }
        }
    }
}

private fun ProductoFila.aDominio() = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock.toInt(),
    activo = activo == 1L
)
