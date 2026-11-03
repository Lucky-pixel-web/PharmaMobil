package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlinx.coroutines.flow.flowOf
import pe.edu.upeu.pharmamobil.domain.model.EstadoEnvios

interface ProductoRepository {

    fun observar(): Flow<List<Producto>>

    suspend fun registrar(producto: Producto): Producto

    suspend fun sincronizar()

    suspend fun listar(): List<Producto>

    suspend fun obtener(id: Long): Producto

    suspend fun actualizar(producto: Producto): Producto

    suspend fun eliminar(id: Long)

    fun observarEstadoEnvios(): Flow<EstadoEnvios> =
        flowOf(EstadoEnvios())

}