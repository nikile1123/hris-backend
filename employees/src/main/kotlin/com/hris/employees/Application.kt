package com.hris.employees

import com.hris.employees.model.EmployeesService
import com.hris.employees.monitoring.configureMonitoring
import com.hris.employees.routes.registerRoutes
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

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
