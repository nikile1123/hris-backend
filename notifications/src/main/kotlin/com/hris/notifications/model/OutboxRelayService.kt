package com.hris.notifications.service

import com.hris.notifications.model.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

object OutboxTable : Table("outbox") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val eventType = varchar("event_type", 50)
    val employeeId = uuid("employee_id")
    val message = varchar("message", 512)
    val processed = bool("processed").default(false)
    val createdAt =
        datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

data class OutboxEvent(
    val id: UUID,
    val employeeId: UUID,
    val eventType: String,
    val message: String,
    val processed: Boolean,
    val createdAt: String
)

class OutboxRelayService(
    private val database: Database,
    private val notificationSender: NotificationSender
) {
    private val logger = LoggerFactory.getLogger(OutboxRelayService::class.java)

    suspend fun processOutboxEvents() = newSuspendedTransaction(Dispatchers.IO, database) {
        val events = OutboxTable.selectAll()
            .where { OutboxTable.processed eq false }.map { row ->
            OutboxEvent(
                id = row[OutboxTable.id],
                employeeId = row[OutboxTable.employeeId],
                eventType = row[OutboxTable.eventType],
                message = row[OutboxTable.message],
                processed = row[OutboxTable.processed],
                createdAt = row[OutboxTable.createdAt].toString()
            )
        }
        events.forEach { event ->
            try {
                val routingKey = when (event.eventType.lowercase()) {
                    "review.created" -> "notification.review.created"
                    "review.updated" -> "notification.review.updated"
                    "review.deleted" -> "notification.review.deleted"
                    "employee.created" -> "notification.employee.created"
                    "employee.updated" -> "notification.employee.updated"
                    "employee.deleted" -> "notification.employee.deleted"
                    else -> throw IllegalArgumentException("Unknown event type: ${event.eventType}")
                }
                notificationSender.sendNotification(convertOutboxEventToNotification(event), routingKey)
                OutboxTable.update({ OutboxTable.id eq event.id }) { it[processed] = true }
                logger.info("Processed outbox event with id: ${event.id}")
            } catch (e: Exception) {
                logger.error("Failed to process outbox event with id: ${event.id}", e)
            }
        }
    }

    private fun convertOutboxEventToNotification(event: OutboxEvent): Notification {
        return Notification(
            id = UUID.randomUUID(),
            employeeId = event.employeeId,
            notificationType = event.eventType,
            message = event.message,
            createdAt = LocalDateTime.now().toString()
        )
    }
}
