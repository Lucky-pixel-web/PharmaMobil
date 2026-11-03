
package pe.edu.upeu.pharmamobil.domain.model

data class FalloEnvio(
    val id: Long,
    val nombre: String,
    val motivo: String
)

data class EstadoEnvios(
    val pendientes: Int = 0,
    val fallidas: Int = 0,
    val detallesFallidos: List<FalloEnvio> = emptyList()
)
