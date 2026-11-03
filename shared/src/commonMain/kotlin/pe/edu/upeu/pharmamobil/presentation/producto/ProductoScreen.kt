package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.util.ahoraEnMilisegundos
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.MensajeError
import pe.edu.upeu.pharmamobil.presentation.components.MensajeExito
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoUiState.Operacion
import androidx.compose.material3.TextButton
@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    onVerDetalle: (Long) -> Unit = {},
    puedeEscribir: Boolean = true,
    modifier: Modifier = Modifier
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var tabSeleccionada by remember { mutableStateOf(0) }
    val titulosTabs = listOf("Activos", "Inactivos", "Bajo stock")

    val tipoEnCurso = (uiState.operacion as? Operacion.EnCurso)?.tipo
    val guardando = tipoEnCurso == Operacion.Tipo.Crear ||
            tipoEnCurso == Operacion.Tipo.Actualizar
    val eliminando = tipoEnCurso == Operacion.Tipo.Eliminar

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if (puedeEscribir) {
            FormularioProductoCard(
                formulario = uiState.formulario,
                guardando = guardando,
                onNombreChange = viewModel::onNombreChange,
                onPrecioChange = viewModel::onPrecioChange,
                onStockChange = viewModel::onStockChange,
                onGuardar = viewModel::guardar,
                onCancelar = viewModel::cancelarEdicion
            )
        }

        uiState.mensajeExito?.let {
            MensajeExito(it)
        }

        (uiState.operacion as? Operacion.Fallida)?.let {
            MensajeError(it.mensaje)
        }

        uiState.avisoRed?.let { MensajeError(it) }

        EncabezadoInventario(
            fase = uiState.fase,
            sincronizando = uiState.sincronizando,
            onSincronizar = viewModel::sincronizar
        )

        PanelEstadoSincronizacion(uiState)

        TabRow(selectedTabIndex = tabSeleccionada) {
            titulosTabs.forEachIndexed { index, titulo ->
                Tab(
                    selected = tabSeleccionada == index,
                    onClick = { tabSeleccionada = index },
                    text = { Text(titulo) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            when (val fase = uiState.fase) {

                ProductoUiState.Fase.Cargando ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        CircularProgressIndicator()

                        Text(
                            text = "Cargando inventario…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                ProductoUiState.Fase.SinProductos ->
                    if (uiState.sincronizando) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Sincronizando inventario…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        EstadoVacio(
                            icono = Icons.Default.Inventory2,
                            titulo = "Todavía no hay productos",
                            descripcion = if (uiState.ultimaSincronizacion == null) {
                                "Aún no se ha sincronizado. Conéctate a internet para descargar el inventario."
                            } else {
                                "Registra el primero con el formulario de arriba."
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                is ProductoUiState.Fase.ConProductos -> {

                    val productosFiltrados = when (tabSeleccionada) {
                        0 -> fase.productos.filter { it.activo && !it.esBajoStock }
                        1 -> fase.productos.filter { !it.activo }
                        2 -> fase.productos.filter { it.esBajoStock }
                        else -> emptyList()
                    }

                    if (productosFiltrados.isEmpty()) {
                        EstadoVacio(
                            icono = Icons.Default.Inventory2,
                            titulo = "Nada en esta pestaña",
                            descripcion = "No hay productos que coincidan con este filtro.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = productosFiltrados,
                                key = { it.id }
                            ) { producto ->
                                ProductoItem(
                                    producto = producto,
                                    eliminando = eliminando,
                                    puedeEscribir = puedeEscribir,
                                    onEditar = { viewModel.editar(producto) },
                                    onEliminar = { viewModel.eliminar(producto.id) },
                                    onVerDetalle = { onVerDetalle(producto.id) }
                                )
                            }
                        }
                    }
                }

                is ProductoUiState.Fase.Error ->
                    EstadoVacio(
                        icono = Icons.Default.CloudOff,
                        titulo = "No pudimos cargar el inventario",
                        descripcion = fase.mensaje,
                        colorIcono = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                        accion = {
                            FilledTonalButton(onClick = viewModel::cargarProductos) {
                                Text("Reintentar")
                            }
                        }
                    )
            }
        }
    }
}


@Composable
private fun FormularioProductoCard(
    formulario: FormularioProducto,
    guardando: Boolean,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = if (formulario.editando) "Editar producto" else "Registrar producto",
                style = MaterialTheme.typography.titleMedium
            )

            ValidatedTextField(
                value = formulario.nombre,
                onValueChange = onNombreChange,
                label = "Nombre",
                error = formulario.nombreError,
                leadingIcon = Icons.Default.Medication,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ValidatedTextField(
                    value = formulario.precio,
                    onValueChange = onPrecioChange,
                    label = "Precio",
                    error = formulario.precioError,
                    ayuda = "En soles",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                ValidatedTextField(
                    value = formulario.stock,
                    onValueChange = onStockChange,
                    label = "Stock",
                    error = formulario.stockError,
                    ayuda = "Unidades",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = onGuardar,
                    enabled = !guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when {
                            guardando -> "Guardando…"
                            formulario.editando -> "Guardar cambios"
                            else -> "Registrar"
                        }
                    )
                }

                if (formulario.editando) {
                    OutlinedButton(
                        onClick = onCancelar,
                        enabled = !guardando
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}


@Composable
private fun EncabezadoInventario(
    fase: ProductoUiState.Fase,
    sincronizando: Boolean,
    onSincronizar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Inventario",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (fase is ProductoUiState.Fase.ConProductos) {
            val cantidad = fase.productos.size
            Text(
                text = if (cantidad == 1) "1 producto" else "$cantidad productos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (sincronizando) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onSincronizar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                }
            }
        }
    }
}


@Composable
private fun IndicadorAntiguedad(ultima: Long?) {
    val ahora by produceState(initialValue = ahoraEnMilisegundos(), key1 = ultima) {
        while (true) {
            delay(30_000)
            value = ahoraEnMilisegundos()
        }
    }
    Text(
        text = textoAntiguedad(ultima, ahora),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}


@Composable
private fun ProductoItem(
    producto: ProductoUi,
    eliminando: Boolean,
    puedeEscribir: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onVerDetalle: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = producto.id >= 0, onClick = onVerDetalle)
    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {

                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = "${producto.precio}  ·  ${producto.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (producto.requiereReposicion) {

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {

                    Text(
                        text = "Reponer",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }
            }

            val pendiente = producto.id < 0
            if (pendiente) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(
                        text = "Pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (puedeEscribir) {
                IconButton(
                    onClick = onEditar,
                    enabled = producto.id >= 0,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ${producto.nombre}"
                    )
                }

                IconButton(
                    onClick = onEliminar,
                    enabled = !eliminando && producto.id >= 0,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar ${producto.nombre}"
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelEstadoSincronizacion(
    estado: ProductoUiState
) {
    var mostrarErrores by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = when (estado.conectado) {
                true -> "Internet disponible"
                false -> "Sin conexión a internet"
                null -> "Verificando conexión..."
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Por enviar: ${estado.estadoEnvios.pendientes}" +
                    " · Fallidas: ${estado.estadoEnvios.fallidas}",
            style = MaterialTheme.typography.bodyMedium
        )

        IndicadorAntiguedad(
            estado.ultimaSincronizacion
        )

        if (estado.estadoEnvios.fallidas > 0) {

            TextButton(
                onClick = {
                    mostrarErrores = !mostrarErrores
                }
            ) {
                Text(
                    if (mostrarErrores)
                        "Ocultar errores"
                    else
                        "Revisar errores"
                )
            }

            if (mostrarErrores) {
                estado.estadoEnvios.detallesFallidos
                    .take(3)
                    .forEach { fallo ->
                        Text(
                            text = "${fallo.nombre}: ${fallo.motivo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
            }
        }
    }
}
