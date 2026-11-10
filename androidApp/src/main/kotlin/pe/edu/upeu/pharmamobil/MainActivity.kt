
package pe.edu.upeu.pharmamobil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.permiso.GestorPermisosAndroid
import pe.edu.upeu.pharmamobil.permiso.PantallaPermisos
import pe.edu.upeu.pharmamobil.permiso.PuentePermisos
import android.content.pm.ApplicationInfo
import org.koin.core.context.GlobalContext
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val telemetria = GlobalContext.get().get<Telemetria>()

        val esDepuracion =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val gestor = GestorPermisosAndroid(
            this,
            getSharedPreferences("permisos_pharma", MODE_PRIVATE)
        )

        gestor.puente = PuentePermisos(this)

        setContent {
            var mostrarPermisos by remember {
                mutableStateOf(false)
            }

            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    App()

                    if (esDepuracion) {
                        Button(
                            onClick = {
                                telemetria.registrarError(
                                    IllegalStateException(
                                        "Prueba controlada de Crashlytics"
                                    ),
                                    mapOf(
                                        "pantalla" to "MainActivity",
                                        "operacion" to "verificacion_sesion13"
                                    )
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Text("Probar Crashlytics")
                        }
                    }

                    if (mostrarPermisos) {
                        Surface(Modifier.fillMaxSize()) {
                            PantallaPermisos(
                                gestor = gestor,
                                onCerrar = {
                                    mostrarPermisos = false
                                }
                            )
                        }
                    } else {
                        Button(
                            onClick = { mostrarPermisos = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Text("Permisos")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
