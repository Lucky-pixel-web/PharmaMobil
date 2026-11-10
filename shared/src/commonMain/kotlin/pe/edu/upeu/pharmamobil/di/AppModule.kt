package pe.edu.upeu.pharmamobil.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.ProductoRemoto
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.session.SesionManager
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarClientesUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RecuperarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarClienteUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.local.AgroLocal
import pe.upeu.agrosierra.data.repository.AgroRepositorioOffline
import pe.upeu.agrosierra.presentation.AgroViewModel
import pe.upeu.agrosierra.domain.repository.AgroRepositorio
import pe.upeu.agrosierra.domain.usecase.BuscarLotesAgroUseCase
import pe.upeu.agrosierra.domain.usecase.RegistrarLaborAgroUseCase
import pe.upeu.agrosierra.domain.usecase.SincronizarAgroUseCase
import pe.upeu.agrosierra.domain.platform.MonitorConectividad
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.data.firebase.TelemetriaFirebase
import pe.upeu.agrosierra.data.local.AlmacenAgroLocal
import pe.upeu.agrosierra.data.remote.ServicioAgroRemoto
private const val CATEGORIA_POR_DEFECTO = 21L

val dataModule = module {
    single { SesionManager() }
    single { PharmaDb(get<CreadorDeDriver>().crear()) }
    single {
        val manager = get<SesionManager>()
        crearHttpClient(get(), get(named("urlBase")), get(), get(named("esDepuracion"))) { manager.expirar() }
    }
    single<AuthRepository> { AuthRepositorioRest(get(), get()) }
    single { ProductoApi(get()) }
    single { ProductoLocal(get()) }
    single<ProductoRepository> { ProductoRepositorioOffline(get(), get(), get()) }
    single<ProductoRemoto> { ProductoRepositorioRest(get(), CATEGORIA_POR_DEFECTO) }
    single<ClienteRepository> { ClienteRepositorioEnMemoria() }
    single<Telemetria> {
        TelemetriaFirebase()
    }
    single { AgroApi(get()) }

    single<ServicioAgroRemoto> {
        get<AgroApi>()
    }

    single { AgroLocal(get()) }

    single<AlmacenAgroLocal> {
        get<AgroLocal>()
    }

    single<AgroRepositorio> {
        AgroRepositorioOffline(get(), get())
    }
}

val domainModule = module {
    factory { BuscarLotesAgroUseCase(get()) }
    factory { RegistrarLaborAgroUseCase(get()) }
    factory { SincronizarAgroUseCase(get()) }
    factory { ListarProductosUseCase(get()) }
    factory { RegistrarProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
    factory { RegistrarClienteUseCase(get()) }
    factory { ListarClientesUseCase(get()) }
    factory { ObtenerProductoUseCase(get()) }
    factory { IniciarSesionUseCase(get()) }
    factory { RecuperarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { ObservarProductosUseCase(get()) }
    factory { SincronizarProductosUseCase(get()) }
    factory { ObservarUltimaSincronizacionUseCase(get()) }
}

val presentationModule = module {
    viewModel {
        ProductoViewModel(
            get(), get(), get(), get(),
            get(), get(), get(),
            getOrNull<MonitorConectividad>()
        )
    }
    viewModel { ClienteViewModel(get(), get()) }
    viewModel { DetalleProductoViewModel(get(), get()) }
    viewModel { SesionViewModel(get(), get(), get(), get()) }
    viewModel { AgroViewModel(get(), get(), get(), get(), get()) }
}

expect val platformModule: Module

fun initKoin(configuracionAdicional: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracionAdicional()
        modules(
            dataModule,
            domainModule,
            presentationModule,
            platformModule
        )
    }
}