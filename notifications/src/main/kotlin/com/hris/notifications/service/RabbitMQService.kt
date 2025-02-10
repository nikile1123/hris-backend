package com.hris.notifications.service

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.LoggerFactory

class RabbitMQService {
    private val logger = LoggerFactory.getLogger(RabbitMQService::class.java)
    private val exchangeName = "notifications_exchange"
    private val connectionFactory: ConnectionFactory = ConnectionFactory().apply {
        host = System.getenv("RABBITMQ_HOST") ?: "localhost"
        port = (System.getenv("RABBITMQ_PORT") ?: "5672").toInt()
        username = System.getenv("RABBITMQ_DEFAULT_USER") ?: "guest"
        password = System.getenv("RABBITMQ_DEFAULT_PASS") ?: "guest"
    }
    private val connection: Connection = connectionFactory.newConnection()
    val channel: Channel = connection.createChannel()

    init {
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, true)
        channel.queueDeclare("ui_notifications", true, false, false, null)
        channel.queueDeclare("email_notifications", true, false, false, null)
        channel.queueBind("ui_notifications", exchangeName, "review.#")
        channel.queueBind("ui_notifications", exchangeName, "employee.#")
        channel.queueBind("email_notifications", exchangeName, "review.#")
        channel.queueBind("email_notifications", exchangeName, "employee.#")
        logger.info("RabbitMQService initialized")
    }

    //TODO: goes only in 1 queue, need to all
    fun publishNotification(routingKey: String, message: String) {
        channel.basicPublish(exchangeName, routingKey, null, message.toByteArray(Charsets.UTF_8))
        logger.info("Published notification with routingKey: $routingKey")
    }

    fun close() {
        channel.close()
        connection.close()
        logger.info("RabbitMQService closed")
    }
}
