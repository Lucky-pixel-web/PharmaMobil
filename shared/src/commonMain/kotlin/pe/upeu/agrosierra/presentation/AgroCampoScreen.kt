
package pe.upeu.agrosierra.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.agrosierra.domain.model.*
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import kotlin.time.Instant

@Composable
fun AgroCampoScreen(
    sesion: Sesion,
    onCerrarSesion: () -> Unit
) {
    val vm: AgroViewModel = koinViewModel(
        key = sesion.usuario
    )

    val lotes by vm.lotes.collectAsStateWithLifecycle()
    val pendientesPorLote by vm.pendientesPorLote.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val ultimaSincronizacion by vm.ultimaSincronizacion.collectAsStateWithLifecycle()

    var seleccionado by remember {
        mutableStateOf<Lote?>(null)
    }

    var perfil by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var valle by remember { mutableStateOf("") }
    var abiertas by remember { mutableStateOf(false) }

    LaunchedEffect(sesion.usuario) {
        vm.sincronizar()
    }

    // CONTENEDOR PRINCIPAL
    // Respeta los bordes seguros del dispositivo.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // 1. ENCABEZADO
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "AgroCampo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Usuario: ${sesion.usuario}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = ultimaSincronizacion?.let { instante ->
                        "Última sincronización: " +
                                Instant.fromEpochMilliseconds(instante)
                                    .toString()
                                    .replace('T', ' ')
                                    .replace("Z", " UTC")
                    } ?: "Última sincronización: todavía no realizada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. NAVEGACIÓN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    perfil = false
                    seleccionado = null
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Lotes")
            }

            OutlinedButton(
                onClick = {
                    perfil = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Perfil")
            }
        }

        // 3. SINCRONIZACIÓN Y SESIÓN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.sincronizar() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Sincronizar")
            }

            OutlinedButton(
                onClick = onCerrarSesion,
                modifier = Modifier.weight(1f)
            ) {
                Text("Salir")
            }
        }

        // 4. ESTADO DE OPERACIONES
        when (val e = estado) {
            AgroUiState.Cargando -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )

                    Text(
                        text = "Procesando...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is AgroUiState.Exito -> {
                Text(
                    text = e.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is AgroUiState.Error -> {
                Text(
                    text = e.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            AgroUiState.Listo -> Unit
        }

        // 5. CONTENIDO
        if (perfil) {

            // PERFIL DEL USUARIO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Mi perfil",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text("Nombre: ${sesion.nombreCompleto}")
                        Text("Rol: ${sesion.rol}")

                        HorizontalDivider()

                        Text(
                            text = "Privilegios:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        sesion.permisos.sorted().forEach { permiso ->
                            Text("• $permiso")
                        }
                    }
                }
            }

        } else if (seleccionado == null) {

            // 6. FILTROS DE LOTES
            OutlinedTextField(
                value = texto,
                onValueChange = {
                    texto = it
                    vm.cambiarBusqueda(it)
                },
                label = { Text("Buscar nombre o código") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = valle,
                onValueChange = {
                    valle = it
                    vm.cambiarValle(it)
                },
                label = { Text("Valle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = abiertas,
                    onCheckedChange = {
                        abiertas = it
                        vm.cambiarSoloAbiertas(it)
                    }
                )

                Text(
                    text = "Solo campañas abiertas",
                    modifier = Modifier.clickable {
                        abiertas = !abiertas
                        vm.cambiarSoloAbiertas(abiertas)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            TextButton(
                onClick = {
                    texto = ""
                    valle = ""
                    abiertas = false

                    vm.cambiarBusqueda("")
                    vm.cambiarValle("")
                    vm.cambiarSoloAbiertas(false)
                }
            ) {
                Text("Limpiar filtros")
            }

            // 7. LISTADO DE LOTES
            if (lotes.isEmpty()) {
                Text(
                    text = "No hay lotes almacenados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = lotes,
                        key = { it.id }
                    ) { lote ->

                        val totalPendientes =
                            pendientesPorLote[lote.id] ?: 0L

                        val estadoCampania =
                            if (lote.campaniaCerrada) {
                                "Cerrada"
                            } else {
                                "Abierta"
                            }

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = lote.nombre,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "${lote.codigo} · " +
                                            "${lote.valle} · $estadoCampania"
                                )
                            },
                            trailingContent = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Badge(
                                        containerColor =
                                            if (totalPendientes > 0) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            },
                                        contentColor =
                                            if (totalPendientes > 0) {
                                                MaterialTheme.colorScheme.onError
                                            } else {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            }
                                    ) {
                                        Text(totalPendientes.toString())
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = "Pendientes",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    seleccionado = lote
                                }
                        )

                        HorizontalDivider()
                    }
                }
            }

        } else {

            // 8. DETALLE DEL LOTE
            val lote = seleccionado!!

            DetalleAgro(
                lote = lote,
                sesion = sesion,
                vm = vm,
                erroresServidor =
                    (estado as? AgroUiState.Error)?.campos.orEmpty(),
                onVolver = {
                    seleccionado = null
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DetalleAgro(
    lote: Lote,
    sesion: Sesion,
    vm: AgroViewModel,
    erroresServidor: Map<String, String>,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flujoLabores = remember(lote.id) {
        vm.observarLabores(lote.id)
    }

    val flujoPendientes = remember(lote.id) {
        vm.contarPendientes(lote.id)
    }

    val labores by flujoLabores.collectAsState(
        initial = emptyList()
    )

    val pendientes by flujoPendientes.collectAsState(
        initial = 0L
    )

    var edicion by remember {
        mutableStateOf<Labor?>(null)
    }

    var fecha by remember {
        mutableStateOf("")
    }

    var cantidad by remember {
        mutableStateOf("")
    }

    var jornales by remember {
        mutableStateOf("1")
    }

    var observacion by remember {
        mutableStateOf("")
    }

    var tipo by remember {
        mutableStateOf(TipoLabor.RIEGO)
    }

    var erroresLocales by remember {
        mutableStateOf(emptyMap<String, String>())
    }

    val errores = erroresServidor + erroresLocales

    fun limpiarErrores() {
        erroresLocales = emptyMap()
        vm.limpiarMensaje()
    }

    // CONTENEDOR DEL DETALLE
    // Todo el contenido se puede desplazar.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        TextButton(
            onClick = onVolver
        ) {
            Text("Volver a lotes")
        }

        // INFORMACIÓN GENERAL DEL LOTE
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = lote.nombre,
                    style = MaterialTheme.typography.titleLarge
                )

                Text("Valle: ${lote.valle}")
                Text("Cultivo: ${lote.cultivo}")
                Text("Hectáreas: ${lote.hectareas}")

                Text(
                    text = "Pendientes de sincronizar: $pendientes",
                    color = MaterialTheme.colorScheme.primary
                )

                if (
                    sesion.puede("CAMPANIA_CERRAR") &&
                    !lote.campaniaCerrada
                ) {
                    Button(
                        onClick = {
                            vm.cerrarCampania(lote.id)
                        }
                    ) {
                        Text("Cerrar campaña")
                    }
                }
            }
        }

        HorizontalDivider()

        // LABORES REGISTRADAS
        Text(
            text = "Labores registradas",
            style = MaterialTheme.typography.titleMedium
        )

        labores.forEach { labor ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${labor.tipo} · ${labor.fecha}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text("Responsable: ${labor.responsable}")
                    Text("Cantidad: ${labor.cantidad}")
                    Text("Jornales: ${labor.cantidadJornales}")

                    if (labor.pendiente) {
                        Text(
                            text = "Pendiente de sincronizar",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // CORREGIR LABOR
                    if (sesion.puede("LABOR_EDITAR")) {
                        TextButton(
                            onClick = {
                                edicion = labor
                                fecha = labor.fecha
                                cantidad = labor.cantidad.toString()
                                jornales = labor.cantidadJornales.toString()
                                observacion = labor.observacion
                                tipo = labor.tipo
                            }
                        ) {
                            Text("Corregir")
                        }
                    }

                    // ELIMINAR LABOR
                    if (
                        sesion.puede("LABOR_ELIMINAR") ||
                        labor.id < 0L
                    ) {
                        TextButton(
                            onClick = {
                                vm.eliminar(labor.id)
                            }
                        ) {
                            Text("Eliminar")
                        }
                    }
                }
            }
        }

        // FORMULARIO PARA REGISTRAR O CORREGIR
        if (
            !lote.campaniaCerrada &&
            (
                    sesion.puede("LABOR_CREAR") ||
                            (
                                    edicion != null &&
                                            sesion.puede("LABOR_EDITAR")
                                    )
                    )
        ) {

            HorizontalDivider()

            Text(
                text = if (edicion == null) {
                    "Registrar labor"
                } else {
                    "Corregir labor"
                },
                style = MaterialTheme.typography.titleMedium
            )

            // SELECCIÓN DEL TIPO DE LABOR
            // Permite desplazamiento horizontal.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TipoLabor.entries.forEach { opcion ->
                    FilterChip(
                        selected = tipo == opcion,
                        onClick = {
                            tipo = opcion
                        },
                        label = {
                            Text(opcion.name)
                        }
                    )
                }
            }

            // FECHA
            OutlinedTextField(
                value = fecha,
                onValueChange = {
                    fecha = it
                    limpiarErrores()
                },
                label = {
                    Text("Fecha AAAA-MM-DD")
                },
                isError = errores.containsKey("fecha"),
                supportingText = {
                    errores["fecha"]?.let {
                        Text(it)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // CANTIDAD
            OutlinedTextField(
                value = cantidad,
                onValueChange = {
                    cantidad = it
                    limpiarErrores()
                },
                label = {
                    Text("Cantidad")
                },
                isError = errores.containsKey("cantidad"),
                supportingText = {
                    errores["cantidad"]?.let {
                        Text(it)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // JORNALES
            OutlinedTextField(
                value = jornales,
                onValueChange = {
                    jornales = it
                    limpiarErrores()
                },
                label = {
                    Text("Cantidad de jornales")
                },
                isError = errores.containsKey("cantidadJornales"),
                supportingText = {
                    errores["cantidadJornales"]?.let {
                        Text(it)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // OBSERVACIÓN
            OutlinedTextField(
                value = observacion,
                onValueChange = {
                    observacion = it
                    limpiarErrores()
                },
                label = {
                    Text("Observación")
                },
                isError = errores.containsKey("observacion"),
                supportingText = {
                    errores["observacion"]?.let {
                        Text(it)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // GUARDAR LABOR
            Button(
                onClick = {
                    val numero =
                        cantidad.toDoubleOrNull() ?: 0.0

                    val numeroJornales =
                        jornales.toIntOrNull() ?: 0

                    erroresLocales = validarLaborFormulario(
                        lote,
                        tipo,
                        fecha,
                        cantidad,
                        jornales,
                        observacion
                    )

                    if (erroresLocales.isEmpty()) {

                        val labor = Labor(
                            id = edicion?.id ?: -1L,
                            loteId = lote.id,
                            tipo = tipo,
                            fecha = fecha,
                            responsable =
                                edicion?.responsable ?: sesion.usuario,
                            cantidad = numero,
                            cantidadJornales = numeroJornales,
                            observacion = observacion,
                            pendiente = true
                        )

                        if (edicion == null) {
                            vm.registrar(labor)
                        } else {
                            vm.corregir(labor)
                        }

                        edicion = null
                        fecha = ""
                        cantidad = ""
                        observacion = ""
                        jornales = "1"
                        erroresLocales = emptyMap()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (edicion == null) {
                        "Guardar labor"
                    } else {
                        "Guardar corrección"
                    }
                )
            }

            // CANCELAR EDICIÓN
            if (edicion != null) {
                TextButton(
                    onClick = {
                        edicion = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar edición")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
