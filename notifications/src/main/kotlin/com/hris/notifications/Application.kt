package com.hris.notifications

import com.hris.notifications.service.NotificationSender
import com.hris.notifications.service.OutboxRelayService
import com.hris.notifications.service.RabbitMQNotificationSender
import com.hris.notifications.service.RabbitMQService
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    launchOutboxRelay(DI {
        bind<Database>() with singleton {
            Database.connect(
                url = System.getenv("DATABASE_URL")
                    ?: "jdbc:postgresql://localhost:5432/hris",
                user = System.getenv("DATABASE_USER") ?: "postgres",
                password = System.getenv("DATABASE_PASSWORD") ?: "Start#123"
            )
        }
        bind<RabbitMQService>() with singleton { RabbitMQService() }
        bind<NotificationSender>() with singleton {
            RabbitMQNotificationSender(
                instance()
            )
        }
        bind<OutboxRelayService>() with singleton {
            OutboxRelayService(
                instance(),
                instance()
            )
        }
    })
}

fun Application.launchOutboxRelay(kodein: DI) {
    val outboxRelayService by kodein.instance<OutboxRelayService>()
    launch {
        while (true) {
            try {
                outboxRelayService.processOutboxEvents()
            } catch (e: Exception) {
                log.error("Failed to process outbox events", e)
            }
            delay(60000L)
        }
    }
}
