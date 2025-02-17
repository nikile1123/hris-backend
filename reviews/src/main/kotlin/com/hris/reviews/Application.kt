package com.hris.reviews


import com.hris.reviews.service.PerformanceReviewService
import com.hris.reviews.monitoring.configureMonitoring
import com.hris.reviews.routes.registerRoutes
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

////For openapi gen
//fun main() {
//    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
//}

fun createHikariDataSource(): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = System.getenv("DATABASE_URL")
            ?: "jdbc:postgresql://localhost:5432/hris"
        username = System.getenv("DATABASE_USER") ?: "postgres"
        password = System.getenv("DATABASE_PASSWORD") ?: "Start#123"
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 20
        minimumIdle = 5
        connectionTimeout = 30000
        idleTimeout = 600000
        maxLifetime = 1800000
    }
    return HikariDataSource(config)
}

fun Application.module() {
    val kodein = DI {
        bind<Database>() with singleton {
            Database.connect(
                createHikariDataSource()
            )
        }
        bind<PerformanceReviewService>() with singleton {
            PerformanceReviewService(
                instance()
            )
        }
        bind<PrometheusMeterRegistry>() with singleton {
            PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT
            )
        }
    }
    registerRoutes(kodein)
    configureMonitoring(kodein)
}
