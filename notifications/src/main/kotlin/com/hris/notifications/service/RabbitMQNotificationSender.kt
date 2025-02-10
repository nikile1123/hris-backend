package com.hris.notifications.service

import com.hris.notifications.model.Notification
import kotlinx.serialization.json.Json

interface NotificationSender {
    fun sendNotification(notification: Notification, routingKey: String)
}

class RabbitMQNotificationSender(private val rabbitMQService: RabbitMQService) : NotificationSender {
    override fun sendNotification(notification: Notification, routingKey: String) {
        val messageJson = Json.encodeToString(Notification.serializer(), notification)
        rabbitMQService.publishNotification(routingKey, messageJson)
    }
}
