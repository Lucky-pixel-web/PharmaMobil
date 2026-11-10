package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroAndroid
import android.content.pm.ApplicationInfo
import pe.edu.upeu.pharmamobil.data.local.AlmacenSincronizacionAndroid
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverAndroid
import pe.edu.upeu.pharmamobil.domain.repository.AlmacenSincronizacion
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.upeu.agrosierra.db.AgroDb
import pe.upeu.agrosierra.domain.platform.MonitorConectividad
import pe.upeu.agrosierra.platform.MonitorConectividadAndroid

actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<MonitorConectividad> { MonitorConectividadAndroid(androidContext()) }
    single(named("urlBase")) { "http://10.40.29.51:8080/api/v1/" }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
    single<AlmacenSincronizacion> { AlmacenSincronizacionAndroid(androidContext()) }
    single(named("esDepuracion")) {
        (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    single {
        AgroDb(
            AndroidSqliteDriver(
                schema = AgroDb.Schema,
                context = androidContext(),
                name = "agrosierra.db"
            )
        )
    }
}