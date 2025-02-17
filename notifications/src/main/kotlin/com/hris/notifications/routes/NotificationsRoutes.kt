package com.hris.notifications.routes

import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.kodein.di.DI
import org.kodein.di.instance

fun Application.registerRoutes(kodein: DI) {
    val appMicrometerRegistry by kodein.instance<PrometheusMeterRegistry>()

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/notifications_documentation.yaml")
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }
    }
}