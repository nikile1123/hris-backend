package com.hris.employees.routes

import com.hris.employees.model.Employee
import com.hris.employees.model.EmployeesService
import com.hris.employees.model.Team
import com.hris.employees.model.TeamsService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.kodein.di.DI
import org.kodein.di.instance
import java.util.*

fun Application.registerRoutes(kodein: DI) {
    install(ContentNegotiation) { json() }
    val employeesService by kodein.instance<EmployeesService>()
    val teamsService by kodein.instance<TeamsService>()
    val appMicrometerRegistry by kodein.instance<PrometheusMeterRegistry>()

    routing {
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
            get("{id}/hierarchy") {
                val idParam = call.parameters["id"]
                val id = try { UUID.fromString(idParam) } catch (e: Exception) { null }
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
