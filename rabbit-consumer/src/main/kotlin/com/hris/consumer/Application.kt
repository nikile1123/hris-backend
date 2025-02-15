package com.hris.consumer

import com.rabbitmq.client.*
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.*

class RabbitMQConsumer(private val queueNames: List<String>) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(RabbitMQConsumer::class.java)

    private val connectionFactory: ConnectionFactory = ConnectionFactory().apply {
        host = System.getenv("RABBITMQ_HOST") ?: "localhost"
        port = (System.getenv("RABBITMQ_PORT") ?: "5672").toInt()
        username = System.getenv("RABBITMQ_DEFAULT_USER") ?: "guest"
        password = System.getenv("RABBITMQ_DEFAULT_PASS") ?: "guest"
    }

    // Создаем одно соединение, которое можно использовать для создания множества каналов
    private val connection: Connection = connectionFactory.newConnection()

    // Для каждой очереди создаем отдельный канал и запускаем на нём потребителя
    private val channels: List<Channel> = queueNames.map { queueName ->
        val channel = connection.createChannel()
        // Объявляем очередь, если она ещё не существует
        channel.queueDeclare(queueName, true, false, false, null)
        logger.info("Waiting for messages in queue: $queueName")
        // Создаем потребителя для данной очереди
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope,
                properties: AMQP.BasicProperties?,
                body: ByteArray
            ) {
                val message = String(body, StandardCharsets.UTF_8)
                logger.info("Queue: [$queueName] | Received from exchange: ${envelope.exchange}, " +
                        "routingKey: ${envelope.routingKey}, message: $message")
                // Подтверждаем получение сообщения
                channel.basicAck(envelope.deliveryTag, false)
            }
        }
        // Запускаем потребителя для очереди (неавтоматическое подтверждение)
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
    // Пример списка очередей, для которых нужно запустить потребители
    val queues = listOf("ui_notifications", "email_notifications")
    val consumer = RabbitMQConsumer(queues)
    // Например, оставляем приложение работающим, чтобы потребители могли обрабатывать сообщения
    Runtime.getRuntime().addShutdownHook(Thread {
        consumer.close()
    })
    // Блокировка основного потока, чтобы приложение не завершалось сразу
    while (true) {
        Thread.sleep(1000)
    }
}
