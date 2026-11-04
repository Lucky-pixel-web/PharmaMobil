package pe.edu.upeu.pharmamobil.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

class TelemetriaFirebase : Telemetria {

    private val clavesPermitidas = setOf(
        "pantalla",
        "operacion",
        "codigo_estado"
    )

    override fun registrarError(
        e: Throwable,
        contexto: Map<String, String>
    ) {
        val contextoSeguro = contexto.filterKeys {
            it in clavesPermitidas
        }

        Firebase.crashlytics.recordException(
            e,
            contextoSeguro
        )
    }

    override fun registrarEvento(
        nombre: String,
        datos: Map<String, String>
    ) {
        val datosSeguros = datos.filterKeys {
            it in clavesPermitidas
        }

        Firebase.crashlytics.log(
            "evento=$nombre; contexto=$datosSeguros"
        )
    }
}
