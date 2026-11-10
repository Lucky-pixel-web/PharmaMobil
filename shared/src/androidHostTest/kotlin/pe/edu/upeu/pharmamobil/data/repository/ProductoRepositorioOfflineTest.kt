package pe.edu.upeu.pharmamobil.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeProductoRemoto : ProductoRemoto {

    var errorEnRegistro: ErrorApi? = null
    var llamadasRegistro = 0
    val servidor = mutableListOf<Producto>()
    var sinConexion = false
    private var siguienteId = 100L

    private fun red() {
        if (sinConexion) throw ErrorApiException(ErrorApi.SinConexion)
    }

    override suspend fun listar(): List<Producto> { red(); return servidor.toList() }

    override suspend fun obtener(id: Long): Producto {
        red(); return servidor.first { it.id == id }
    }

    override suspend fun registrar(producto: Producto): Producto {
        red()
        llamadasRegistro++
        errorEnRegistro?.let { throw ErrorApiException(it) }

        val guardado = producto.copy(id = siguienteId++)
        servidor.add(guardado)
        return guardado
    }

    override suspend fun actualizar(producto: Producto): Producto { red(); return producto }

    override suspend fun eliminar(id: Long) { red() }
}

class ProductoRepositorioOfflineTest {

    private val remoto = FakeProductoRemoto()
    private val local = ProductoLocal(
        PharmaDb(
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PharmaDb.Schema.create(it) }
        )
    )
    private val repositorio = ProductoRepositorioOffline(local, remoto)

    private val aspirina = Producto(id = 0L, nombre = "Aspirina", precio = 5.0, stock = 10)

    @Test
    fun registrarSinConexionGuardaPendienteConIdNegativo() = runTest {
        remoto.sinConexion = true

        repositorio.registrar(aspirina)

        val lista = repositorio.observar().first()
        assertEquals(1, lista.size)
        assertTrue(lista.single().id < 0)
        assertEquals(1, local.pendientes().size)
        assertEquals(0, remoto.servidor.size)
    }

    @Test
    fun sincronizarSubeLosPendientesSinDejarDuplicados() = runTest {
        remoto.sinConexion = true
        repositorio.registrar(aspirina)

        remoto.sinConexion = false
        repositorio.sincronizar()

        val lista = repositorio.observar().first()
        assertEquals(1, lista.size)
        assertTrue(lista.single().id > 0)
        assertEquals(0, local.pendientes().size)
        assertEquals(1, remoto.servidor.size)
    }

    @Test
    fun sincronizarReemplazaLoSincronizadoPorLaListaDelServidor() = runTest {
        remoto.servidor += Producto(id = 1L, nombre = "Paracetamol", precio = 8.5, stock = 100)
        remoto.servidor += Producto(id = 2L, nombre = "Ibuprofeno", precio = 12.0, stock = 50)
        repositorio.sincronizar()
        assertEquals(2, repositorio.observar().first().size)

        remoto.servidor.removeAt(0)
        repositorio.sincronizar()

        assertEquals(listOf("Ibuprofeno"), repositorio.observar().first().map { it.nombre })
    }

    @Test
    fun sincronizarSinConexionConservaLosPendientes() = runTest {
        remoto.sinConexion = true
        repositorio.registrar(aspirina)

        assertFailsWith<ErrorApiException> { repositorio.sincronizar() }

        assertEquals(1, local.pendientes().size)
        assertEquals(1, repositorio.observar().first().size)
    }

    @Test
    fun falloTransitorioIncrementaIntentoYConservaPendiente() = runTest {
        local.guardarPendiente(aspirina)
        remoto.errorEnRegistro = ErrorApi.Servidor

        repositorio.sincronizar()

        val pendientes = local.pendientes(
            ProductoRepositorioOffline.MAX_INTENTOS
        )

        val estado = local.observarEstadoEnvios(
            ProductoRepositorioOffline.MAX_INTENTOS
        ).first()

        assertEquals(1, pendientes.size)
        assertEquals(1, pendientes.single().intentos)
        assertEquals(1, estado.pendientes)
        assertEquals(0, estado.fallidas)
    }

    @Test
    fun falloPermanenteMarcaFallidaYSinReintentos() = runTest {
        local.guardarPendiente(aspirina)
        remoto.errorEnRegistro =
            ErrorApi.Conflicto("Registro duplicado")

        repositorio.sincronizar()

        val estado = local.observarEstadoEnvios(
            ProductoRepositorioOffline.MAX_INTENTOS
        ).first()

        assertEquals(0, estado.pendientes)
        assertEquals(1, estado.fallidas)
        assertEquals(1, remoto.llamadasRegistro)

        remoto.errorEnRegistro = null
        repositorio.sincronizar()

        assertEquals(1, remoto.llamadasRegistro)
    }
}