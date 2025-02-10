package com.hris.employees

import com.hris.employees.model.EmployeesService
import com.hris.employees.monitoring.configureMonitoring
import com.hris.employees.routes.registerRoutes
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.time.Duration

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    registerRoutes(DI {
        bind<Database>() with singleton {
            Database.connect(
                url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/hris",
                user = System.getenv("DATABASE_USER") ?: "postgres",
                password = System.getenv("DATABASE_PASSWORD") ?: "Start#123"
            )
        }
        bind<EmployeesService>() with singleton { EmployeesService(instance()) }
    })
    configureMonitoring()
}
