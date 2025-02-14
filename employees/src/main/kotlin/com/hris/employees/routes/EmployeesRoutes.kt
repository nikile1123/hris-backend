package com.hris.employees.routes

import com.hris.employees.model.Employee
import com.hris.employees.model.EmployeesService
import com.hris.employees.model.Team
import com.hris.employees.model.TeamsService
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
import org.jetbrains.exposed.sql.SortOrder
import org.kodein.di.DI
import org.kodein.di.instance
import java.util.*

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
                    text = "400: $cause",
                    status = HttpStatusCode.BadRequest
                )
            } else {
                call.respondText(
                    text = "500: $cause",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "404: Page Not Found", status = status)
        }
    }

    val employeesService by kodein.instance<EmployeesService>()
    val teamsService by kodein.instance<TeamsService>()
    val appMicrometerRegistry by kodein.instance<PrometheusMeterRegistry>()

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml") {
            codegen = StaticHtmlCodegen()
        }
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }
        route("/employees") {
            get {
                val allEmployees = employeesService.getAllEmployees()
                call.respond(HttpStatusCode.OK, allEmployees)
            }
            get("{id}") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val employee = employeesService.readEmployee(id)
                if (employee == null) {
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
                } else {
                    call.respond(HttpStatusCode.OK, employee)
                }
            }
            get("paginated") {
                val sortBy =
                    call.request.queryParameters["sortBy"] ?: "firstName"
                val orderParam = call.request.queryParameters["order"] ?: "asc"
                val order =
                    if (orderParam.lowercase() == "desc") SortOrder.DESC else SortOrder.ASC
                val page =
                    call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize =
                    call.request.queryParameters["pageSize"]?.toIntOrNull()
                        ?: 20

                val employees = employeesService.getEmployeesSortedPaginated(
                    sortBy,
                    order,
                    page,
                    pageSize
                )
                call.respond(HttpStatusCode.OK, employees)
            }
            get("{id}/manager") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val manager = employeesService.getManager(id)
                if (manager == null) {
                    call.respond(HttpStatusCode.NotFound, "Manager not found")
                } else {
                    call.respond(HttpStatusCode.OK, manager)
                }
            }
            get("{id}/subordinates") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val subordinates = employeesService.getSubordinates(id)
                call.respond(HttpStatusCode.OK, subordinates)
            }
            get("{id}/colleagues") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val colleagues = employeesService.getColleagues(id)
                call.respond(HttpStatusCode.OK, colleagues)
            }
            get("{id}/hierarchy") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val hierarchy = employeesService.getEmployeeHierarchy(id)
                call.respond(HttpStatusCode.OK, hierarchy)
            }
            post {
                val employee = call.receive<Employee>()
                val newEmployeeId = employeesService.createEmployee(employee)
                call.respond(
                    HttpStatusCode.Created,
                    mapOf("id" to newEmployeeId.toString())
                )
            }
            put("{id}") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@put
                }
                val employee = call.receive<Employee>()
                employeesService.updateEmployee(id, employee)
                call.respond(HttpStatusCode.OK, "Employee updated")
            }
            delete("{id}") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@delete
                }
                val deleted = employeesService.deleteEmployee(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, "Employee deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Employee not found")
                }
            }
        }
        route("/teams") {
            get {
                val teams = teamsService.getAllTeams()
                call.respond(HttpStatusCode.OK, teams)
            }
            get("{id}") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@get
                }
                val team = teamsService.readTeam(id)
                if (team == null) {
                    call.respond(HttpStatusCode.NotFound, "Team not found")
                } else {
                    call.respond(HttpStatusCode.OK, team)
                }
            }
            post {
                val team = call.receive<Team>()
                val newTeamId = teamsService.createTeam(team)
                call.respond(
                    HttpStatusCode.Created,
                    mapOf("id" to newTeamId.toString())
                )
            }
            put("{id}") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@put
                }
                val team = call.receive<Team>()
                teamsService.updateTeam(id, team)
                call.respond(HttpStatusCode.OK, "Team updated")
            }
            delete("{id}") {
                val idParam = call.parameters["id"]
                val id = try {
                    UUID.fromString(idParam)
                } catch (e: Exception) {
                    null
                }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    return@delete
                }
                val deleted = teamsService.deleteTeam(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, "Team deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Team not found")
                }
            }
        }
    }
}
