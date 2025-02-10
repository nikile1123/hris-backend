package com.hris.notifications.service

import com.hris.notifications.model.Notification
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
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

    suspend fun processOutboxEvents() =
        newSuspendedTransaction(Dispatchers.IO, database) {
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
                        "review.created" -> "review.created"
                        "review.updated" -> "review.updated"
                        "review.deleted" -> "review.deleted"
                        "employee.created" -> "employee.created"
                        "employee.updated" -> "employee.updated"
                        "employee.deleted" -> "employee.deleted"
                        else -> throw IllegalArgumentException("Unknown event type: ${event.eventType}")
                    }
                    notificationSender.sendNotification(
                        convertOutboxEventToNotification(event),
                        routingKey
                    )
                    OutboxTable.update({ OutboxTable.id eq event.id }) {
                        it[processed] = true
                    }
                    logger.info("Processed outbox event with id: ${event.id}")
                } catch (e: Exception) {
                    logger.error(
                        "Failed to process outbox event with id: ${event.id}",
                        e
                    )
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
