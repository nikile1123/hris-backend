package com.hris.employees.routes

import com.hris.employees.service.Employee
import com.hris.employees.service.EmployeesService
import com.hris.employees.service.Team
import com.hris.employees.service.TeamsService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.swagger.codegen.v3.generators.html.StaticHtmlCodegen
import org.kodein.di.DI
import org.kodein.di.instance

fun Application.registerRoutes(kodein: DI) {
    install(ContentNegotiation) { json() }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is IllegalArgumentException) {
                call.respondText(
                    "400: $cause",
                    status = HttpStatusCode.BadRequest
                )
            } else {
                call.respondText(
                    "500: $cause",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText("404: Page Not Found", status = status)
        }
    }

    val employeesService by kodein.instance<EmployeesService>()
    val teamsService by kodein.instance<TeamsService>()
    val appMicrometerRegistry by kodein.instance<PrometheusMeterRegistry>()

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/employees_documentation.yaml")
        openAPI(path = "openapi", swaggerFile = "openapi/employees_documentation.yaml") {
            codegen = StaticHtmlCodegen()
        }
        get("/metrics") { call.respond(appMicrometerRegistry.scrape()) }

        registerEmployeesRoutes(employeesService)
        registerTeamsRoutes(teamsService)
    }
}

private fun Routing.registerEmployeesRoutes(employeesService: EmployeesService) {
    route("/employees") {
        get {
            call.respond(HttpStatusCode.OK, employeesService.getAllEmployees())
        }
        get("{id}") {
            val id = call.getUUIDParam("id") ?: return@get
            employeesService.readEmployee(id)?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: call.respond(HttpStatusCode.NotFound, "Employee not found")
        }
        get("/paginated") {
            val params = call.getSortPaginationParams("joiningDate")
            val employees = employeesService.getEmployeesSortedPaginated(
                params.sortBy, params.order, params.page, params.pageSize
            )
            call.respond(HttpStatusCode.OK, employees)
        }
        get("{id}/manager") {
            val id = call.getUUIDParam("id") ?: return@get
            employeesService.getManager(id)?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: call.respond(HttpStatusCode.NotFound, "Manager not found")
        }
        get("{id}/subordinates") {
            val id = call.getUUIDParam("id") ?: return@get
            call.respond(
                HttpStatusCode.OK,
                employeesService.getSubordinates(id)
            )
        }
        get("{id}/colleagues") {
            val id = call.getUUIDParam("id") ?: return@get
            call.respond(HttpStatusCode.OK, employeesService.getColleagues(id))
        }
        get("{id}/hierarchy") {
            val id = call.getUUIDParam("id") ?: return@get
            call.respond(
                HttpStatusCode.OK,
                employeesService.getEmployeeHierarchy(id)
            )
        }
        post {
            val employee = call.receive<Employee>()
            val newId = employeesService.createEmployee(employee)
            call.respond(
                HttpStatusCode.Created,
                mapOf("id" to newId.toString())
            )
        }
        put("{id}") {
            val id = call.getUUIDParam("id") ?: return@put
            val employee = call.receive<Employee>()
            employeesService.updateEmployee(id, employee)
            call.respond(HttpStatusCode.OK, "Employee updated")
        }
        delete("{id}") {
            val id = call.getUUIDParam("id") ?: return@delete
            if (employeesService.deleteEmployee(id))
                call.respond(HttpStatusCode.OK, "Employee deleted")
            else
                call.respond(HttpStatusCode.NotFound, "Employee not found")
        }
    }
}

private fun Routing.registerTeamsRoutes(teamsService: TeamsService) {
    route("/teams") {
        get {
            call.respond(HttpStatusCode.OK, teamsService.getAllTeams())
        }
        get("{id}") {
            val id = call.getUUIDParam("id") ?: return@get
            teamsService.readTeam(id)?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: call.respond(HttpStatusCode.NotFound, "Team not found")
        }
        get("/paginated") {
            val params = call.getSortPaginationParams("createdAt")
            val teams = teamsService.getTeamsSortedPaginated(
                params.sortBy, params.order, params.page, params.pageSize
            )
            call.respond(HttpStatusCode.OK, teams)
        }
        post {
            val team = call.receive<Team>()
            val newId = teamsService.createTeam(team)
            call.respond(
                HttpStatusCode.Created,
                mapOf("id" to newId.toString())
            )
        }
        put("{id}") {
            val id = call.getUUIDParam("id") ?: return@put
            val team = call.receive<Team>()
            teamsService.updateTeam(id, team)
            call.respond(HttpStatusCode.OK, "Team updated")
        }
        delete("{id}") {
            val id = call.getUUIDParam("id") ?: return@delete
            if (teamsService.deleteTeam(id))
                call.respond(HttpStatusCode.OK, "Team deleted")
            else
                call.respond(HttpStatusCode.NotFound, "Team not found")
        }
    }
}
