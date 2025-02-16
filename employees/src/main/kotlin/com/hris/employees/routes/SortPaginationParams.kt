package com.hris.employees.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.SortOrder
import java.util.*

data class SortPaginationParams(
    val sortBy: String,
    val order: SortOrder,
    val page: Int,
    val pageSize: Int
)

fun ApplicationCall.getSortPaginationParams(defaultSortBy: String): SortPaginationParams {
    val sortBy = request.queryParameters["sortBy"] ?: defaultSortBy
    val orderParam = request.queryParameters["order"] ?: "asc"
    val order =
        if (orderParam.lowercase() == "desc") SortOrder.DESC else SortOrder.ASC
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 1
    val pageSize = request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
    return SortPaginationParams(sortBy, order, page, pageSize)
}


suspend fun ApplicationCall.getUUIDParam(paramName: String): UUID? {
    val param = parameters[paramName]
    return try {
        UUID.fromString(param)
    } catch (e: Exception) {
        respondText("Invalid ID", status = HttpStatusCode.BadRequest)
        null
    }
}
