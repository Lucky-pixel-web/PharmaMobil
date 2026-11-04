package pe.edu.upeu.pharmamobil.domain.permiso

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GestorPermisosContratoTest {

    private class GestorFalso : GestorPermisos {
        var estadoActual: EstadoPermiso = EstadoPermiso.Denegado
        var ajustesAbiertos = false

        override suspend fun estado(
            permiso: Permiso
        ): EstadoPermiso = estadoActual

        override suspend fun solicitar(
            permiso: Permiso
        ): EstadoPermiso = estadoActual

        override fun abrirAjustes() {
            ajustesAbiertos = true
        }
    }

    @Test
    fun permisoDenegadoSePuedeConsultar() = runTest {
        val gestor = GestorFalso()

        assertEquals(
            EstadoPermiso.Denegado,
            gestor.estado(Permiso.CAMARA)
        )
    }

    @Test
    fun permisoConcedidoSeReconoce() = runTest {
        val gestor = GestorFalso()
        gestor.estadoActual = EstadoPermiso.Concedido

        assertEquals(
            EstadoPermiso.Concedido,
            gestor.solicitar(Permiso.UBICACION)
        )
    }

    @Test
    fun permisoBloqueadoPermiteAbrirAjustes() = runTest {
        val gestor = GestorFalso()
        gestor.estadoActual = EstadoPermiso.DenegadoParaSiempre

        assertEquals(
            EstadoPermiso.DenegadoParaSiempre,
            gestor.estado(Permiso.NOTIFICACIONES)
        )

        gestor.abrirAjustes()

        assertTrue(gestor.ajustesAbiertos)
    }
}
