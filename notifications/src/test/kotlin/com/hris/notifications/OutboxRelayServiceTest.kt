package com.hris.notifications

import com.hris.notifications.service.NotificationSender
import com.hris.notifications.service.OutboxRelayService
import com.hris.notifications.service.OutboxTable
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxRelayServiceTest {

    private lateinit var database: Database
    private lateinit var service: OutboxRelayService
    private lateinit var notificationSender: NotificationSender

    private val dummyEmployeeId: UUID = UUID.randomUUID()
    private val dummyTeamId: UUID = UUID.randomUUID()

    @BeforeAll
    fun setupAll() {
        database = Database.connect(
            "jdbc:h2:mem:testNotifications;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        transaction(database) {
            SchemaUtils.create(OutboxTable)
        }
    }

    @BeforeEach
    fun setup() {
        transaction(database) {
            OutboxTable.deleteAll()
        }
        notificationSender = mockk(relaxed = true)
        service = OutboxRelayService(database, notificationSender)
    }

    @AfterAll
    fun tearDownAll() {
        transaction(database) {
            SchemaUtils.drop(OutboxTable)
        }
    }

    @Test
    fun testProcessOutboxEventsSuccess() = runBlocking {
        transaction(database) {
            OutboxTable.insert {
                it[employeeId] = dummyEmployeeId
                it[teamId] = dummyTeamId
                it[eventType] = "review.created"
                it[message] = "Test review created."
                it[processed] = false
            }
        }
        service.processOutboxEvents()
        verify(exactly = 1) {
            notificationSender.sendNotification(
                match {
                    it.notificationType == "review.created" && it.message.contains(
                        "Test review created."
                    )
                },
                match { it.startsWith("notification.review.employee.") }
            )
        }
        transaction(database) {
            val processedCount =
                OutboxTable.selectAll().count { it[OutboxTable.processed] }
            assertEquals(
                1,
                processedCount,
                "Outbox event should be marked as processed"
            )
        }
    }

    @Test
    fun testProcessOutboxEventsFailure() = runBlocking {
        every {
            notificationSender.sendNotification(
                any(),
                any()
            )
        } throws RuntimeException("Test exception")
        transaction(database) {
            OutboxTable.insert {
                it[employeeId] = dummyEmployeeId
                it[teamId] = dummyTeamId
                it[eventType] = "employee.created"
                it[message] = "Test employee created."
                it[processed] = false
            }
        }
        service.processOutboxEvents()
        transaction(database) {
            val unprocessedCount =
                OutboxTable.selectAll().count { !it[OutboxTable.processed] }
            assertTrue(
                unprocessedCount >= 1,
                "At least one outbox event should remain unprocessed due to failure"
            )
        }
    }
}
