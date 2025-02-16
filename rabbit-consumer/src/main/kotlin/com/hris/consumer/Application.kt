package com.hris.consumer

import com.hris.consumer.service.RabbitMQConsumer
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val queues = listOf("ui_notifications", "email_notifications")
    val consumer = RabbitMQConsumer(queues)

    monitor.subscribe(ApplicationStopped) {
        consumer.close()
    }

    launch {
        consumer.start()
    }
}

