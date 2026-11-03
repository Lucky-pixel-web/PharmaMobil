package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.EstadoEnvios

data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val operacion: Operacion = Operacion.Inactiva,
    val mensajeExito: String? = null,
    val sincronizando: Boolean = false,
    val avisoRed: String? = null,
    val ultimaSincronizacion: Long? = null,
    val estadoEnvios: EstadoEnvios = EstadoEnvios(),
    val conectado: Boolean? = null
) {
    sealed interface Fase {
        data object Cargando : Fase

        data object SinProductos : Fase

        data class ConProductos(val productos: List<ProductoUi>) : Fase

        data class Error(val mensaje: String) : Fase
    }

    sealed interface Operacion {
        data object Inactiva : Operacion

        data class EnCurso(val tipo: Tipo) : Operacion

        data class Fallida(val mensaje: String) : Operacion

        enum class Tipo { Crear, Actualizar, Eliminar, Obtener }
    }
}

data class FormularioProducto(
    val idEnEdicion: Long? = null,
    val activoEnEdicion: Boolean = true,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null
) {
    val editando: Boolean
        get() = idEnEdicion != null
}