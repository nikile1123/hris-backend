package com.hris.reviews.routes

import com.hris.reviews.model.PerformanceReview
import com.hris.reviews.model.PerformanceReviewService
import com.hris.reviews.model.UUIDSerializer
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.swagger.codegen.v3.generators.html.StaticHtmlCodegen
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.exposed.sql.SortOrder
import org.kodein.di.DI
import org.kodein.di.instance
import java.util.*

fun Application.registerRoutes(kodein: DI) {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = SerializersModule {
                contextual(UUID::class, UUIDSerializer)
            }
        })
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    val reviewService by kodein.instance<PerformanceReviewService>()
    val appMicrometerRegistry by kodein.instance<PrometheusMeterRegistry>()

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        openAPI(path="openapi", swaggerFile = "openapi/documentation.yaml") {
            codegen = StaticHtmlCodegen()
        }
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }
        route("/performance_reviews") {
            get {
                val allReviews = reviewService.getAllReviews()
                call.respond(HttpStatusCode.OK, allReviews)
            }
            get("paginated") {
                val sortBy =
                    call.request.queryParameters["sortBy"] ?: "joiningDate"
                val orderParam = call.request.queryParameters["order"] ?: "asc"
                val order =
                    if (orderParam.lowercase() == "desc") SortOrder.DESC else SortOrder.ASC
                val page =
                    call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize =
                    call.request.queryParameters["pageSize"]?.toIntOrNull()
                        ?: 20

                val reviews = reviewService.getReviewsSortedPaginated(
                    sortBy,
                    order,
                    page,
                    pageSize
                )
                call.respond(HttpStatusCode.OK, reviews)
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
                val review = reviewService.getReviewById(id)
                if (review == null) {
                    call.respond(HttpStatusCode.NotFound, "Review not found")
                } else {
                    call.respond(HttpStatusCode.OK, review)
                }
            }
            get("/employee/{employeeId}") {
                val empIdParam = call.parameters["employeeId"]
                val employeeId = try {
                    UUID.fromString(empIdParam)
                } catch (e: Exception) {
                    null
                }
                if (employeeId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid employee ID"
                    )
                    return@get
                }
                val reviews = reviewService.getReviewsByEmployeeId(employeeId)
                call.respond(HttpStatusCode.OK, reviews)
            }
            post {
                val review = call.receive<PerformanceReview>()
                val newReviewId = reviewService.createReview(review)
                call.respond(
                    HttpStatusCode.Created,
                    mapOf("id" to newReviewId.toString())
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
                val review = call.receive<PerformanceReview>()
                reviewService.updateReview(id, review)
                call.respond(HttpStatusCode.OK, "Review updated")
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
                val deleted = reviewService.deleteReview(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, "Review deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Review not found")
                }
            }
        }
    }
}
