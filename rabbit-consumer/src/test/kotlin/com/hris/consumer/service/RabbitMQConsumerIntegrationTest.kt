package com.hris.consumer.service

import com.rabbitmq.client.ConnectionFactory
import nl.altindag.log.LogCaptor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.StandardCharsets

@Testcontainers
class RabbitMQConsumerIntegrationTest {

    companion object {
        @Container
        val rabbitMQContainer = RabbitMQContainer("rabbitmq:3-management").apply {
            withExposedPorts(5672, 15672)
        }
    }

    private var consumer: RabbitMQConsumer? = null

    @BeforeEach
    fun setup() {
        System.setProperty("RABBITMQ_HOST", rabbitMQContainer.host)
        System.setProperty("RABBITMQ_PORT", rabbitMQContainer.getMappedPort(5672).toString())
        System.setProperty("RABBITMQ_DEFAULT_USER", rabbitMQContainer.adminUsername)
        System.setProperty("RABBITMQ_DEFAULT_PASS", rabbitMQContainer.adminPassword)

        println("Connecting to RabbitMQ at ${rabbitMQContainer.host}:${rabbitMQContainer.getMappedPort(5672)}")

        Thread.sleep(10000)

        consumer = RabbitMQConsumer(listOf("test_queue")).apply { start() }
        Thread.sleep(5000)
    }

    @AfterEach
    fun tearDown() {
        consumer?.close()
    }

    @Test
    @Disabled
    fun testMessageConsumption() {
        val logCaptor = LogCaptor.forClass(RabbitMQConsumer::class.java)

        val factory = ConnectionFactory().apply {
            host = rabbitMQContainer.host
            port = rabbitMQContainer.getMappedPort(5672)
            username = rabbitMQContainer.adminUsername
            password = rabbitMQContainer.adminPassword
        }
        val connection = factory.newConnection()
        val channel = connection.createChannel()
        val testMessage = "Hello, Test Queue!"
        channel.basicPublish(
            "",
            "test_queue",
            null,
            testMessage.toByteArray(StandardCharsets.UTF_8)
        )
        Thread.sleep(5000)
        channel.close()
        connection.close()

        val logs = logCaptor.logs
        println("Captured logs:")
        logs.forEach { println(it) }
        assertTrue(
            logs.any { it.contains("Received from exchange") && it.contains(testMessage) },
            "Test message should be consumed"
        )
    }
}
