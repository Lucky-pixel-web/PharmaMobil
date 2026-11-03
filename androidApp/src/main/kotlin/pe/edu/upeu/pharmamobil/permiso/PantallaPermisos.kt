
package pe.edu.upeu.pharmamobil.permiso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.permiso.*

@Composable
fun PantallaPermisos(
    gestor: GestorPermisos,
    onCerrar: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var estados by remember {
        mutableStateOf<Map<Permiso, EstadoPermiso>>(emptyMap())
    }

    var ocupado by remember {
        mutableStateOf<Permiso?>(null)
    }

    suspend fun actualizar() {
        estados = Permiso.entries.associateWith {
            gestor.estado(it)
        }
    }

    LaunchedEffect(Unit) {
        actualizar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Gestión de permisos",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Comprueba los permisos de Android. " +
                    "Solo se solicitarán cuando pulses el botón."
        )

        Permiso.entries.forEach { permiso ->

            val actual = estados[permiso]

            Text(
                permiso.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                when (actual) {
                    EstadoPermiso.Concedido -> "Concedido"
                    EstadoPermiso.Denegado -> "Denegado"
                    EstadoPermiso.DenegadoParaSiempre ->
                        "Denegado para siempre"
                    null -> "Comprobando..."
                }
            )

            Button(
                enabled = ocupado == null && actual != null,
                onClick = {
                    scope.launch {
                        ocupado = permiso
                        try {
                            if (actual == EstadoPermiso.DenegadoParaSiempre) {
                                gestor.abrirAjustes()
                            } else {
                                gestor.solicitar(permiso)
                            }
                            actualizar()
                        } finally {
                            ocupado = null
                        }
                    }
                }
            ) {
                Text(
                    if (actual == EstadoPermiso.DenegadoParaSiempre)
                        "Abrir ajustes"
                    else
                        "Solicitar permiso"
                )
            }
        }

        Button(
            onClick = {
                scope.launch { actualizar() }
            }
        ) {
            Text("Actualizar estados")
        }

        Button(onClick = onCerrar) {
            Text("Volver a PharmaMobil")
        }
    }
}
