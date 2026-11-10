package pe.edu.upeu.pharmamobil.permiso

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso

class GestorPermisosAndroid(
    private val actividad: ComponentActivity,
    private val preferencias: SharedPreferences
) : GestorPermisos {

    var puente: PuentePermisos? = null

    private fun nombreSistema(permiso: Permiso): String? =
        when (permiso) {
            Permiso.CAMARA ->
                Manifest.permission.CAMERA

            Permiso.UBICACION ->
                Manifest.permission.ACCESS_COARSE_LOCATION

            Permiso.NOTIFICACIONES ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null
                }
        }

    override suspend fun estado(
        permiso: Permiso
    ): EstadoPermiso {
        val nombre = nombreSistema(permiso)
            ?: return EstadoPermiso.Concedido

        val concedido = actividad.checkSelfPermission(nombre) ==
                PackageManager.PERMISSION_GRANTED

        if (concedido) return EstadoPermiso.Concedido

        val yaSePidio = preferencias.getBoolean(
            "pedido_$nombre",
            false
        )

        val explicar = actividad
            .shouldShowRequestPermissionRationale(nombre)

        return when {
            !yaSePidio -> EstadoPermiso.Denegado
            explicar -> EstadoPermiso.Denegado
            else -> EstadoPermiso.DenegadoParaSiempre
        }
    }

    override suspend fun solicitar(
        permiso: Permiso
    ): EstadoPermiso {
        val nombre = nombreSistema(permiso)
            ?: return EstadoPermiso.Concedido

        val actual = estado(permiso)

        if (actual != EstadoPermiso.Denegado) {
            return actual
        }

        val lanzador = puente
            ?: return EstadoPermiso.Denegado

        lanzador.pedir(nombre)

        preferencias.edit()
            .putBoolean("pedido_$nombre", true)
            .apply()

        return estado(permiso)
    }

    override fun abrirAjustes() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts(
                "package",
                actividad.packageName,
                null
            )
        )
        actividad.startActivity(intent)
    }
}
