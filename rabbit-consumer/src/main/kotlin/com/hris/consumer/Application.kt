package com.hris.consumer

import com.rabbitmq.client.*
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

class RabbitMQConsumer(private val queueNames: List<String>) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(RabbitMQConsumer::class.java)

    private val connectionFactory: ConnectionFactory =
        ConnectionFactory().apply {
            host = System.getenv("RABBITMQ_HOST") ?: "localhost"
            port = (System.getenv("RABBITMQ_PORT") ?: "5672").toInt()
            username = System.getenv("RABBITMQ_DEFAULT_USER") ?: "guest"
            password = System.getenv("RABBITMQ_DEFAULT_PASS") ?: "guest"
        }

    private val connection: Connection = connectionFactory.newConnection()

    private val channels: List<Channel> = queueNames.map { queueName ->
        val channel = connection.createChannel()
        channel.queueDeclare(queueName, true, false, false, null)
        logger.info("Waiting for messages in queue: $queueName")
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope,
                properties: AMQP.BasicProperties?,
                body: ByteArray
            ) {
                val message = String(body, StandardCharsets.UTF_8)
                logger.info(
                    "Queue: [$queueName] | Received from exchange: ${envelope.exchange}, " +
                            "routingKey: ${envelope.routingKey}, message: $message"
                )
                channel.basicAck(envelope.deliveryTag, false)
            }
        }
        channel.basicConsume(queueName, false, consumer)
        channel
    }

    override fun close() {
        channels.forEach { channel ->
            try {
                channel.close()
            } catch (e: Exception) {
                logger.error("Error closing channel", e)
            }
        }
        try {
            connection.close()
            logger.info("RabbitMQConsumer connection closed")
        } catch (e: Exception) {
            logger.error("Error closing connection", e)
        }
    }
}

fun main() {
    val queues = listOf("ui_notifications", "email_notifications")
    val consumer = RabbitMQConsumer(queues)
    Runtime.getRuntime().addShutdownHook(Thread {
        consumer.close()
    })
    while (true) {
        Thread.sleep(1000)
    }
}
