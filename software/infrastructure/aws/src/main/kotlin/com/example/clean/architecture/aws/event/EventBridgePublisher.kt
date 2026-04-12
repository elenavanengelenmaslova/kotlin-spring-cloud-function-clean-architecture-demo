package com.example.clean.architecture.aws.event

import aws.sdk.kotlin.services.eventbridge.EventBridgeClient
import aws.sdk.kotlin.services.eventbridge.model.PutEventsRequest
import aws.sdk.kotlin.services.eventbridge.model.PutEventsRequestEntry
import com.example.clean.architecture.event.EventPublisherInterface
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class EventBridgePublisher(
    private val eventBridgeClient: EventBridgeClient,
    @Value("\${eventbridge.bus-name:demo-pets-events-bus}")
    private val eventBusName: String,
) : EventPublisherInterface {

    override fun publishEvent(source: String, detailType: String, detail: String): Unit = runBlocking {
        logger.info { "Publishing event to EventBridge bus: $eventBusName" }
        val entry = PutEventsRequestEntry {
            this.eventBusName = this@EventBridgePublisher.eventBusName
            this.source = source
            this.detailType = detailType
            this.detail = detail
        }
        val request = PutEventsRequest {
            entries = listOf(entry)
        }
        val response = eventBridgeClient.putEvents(request)
        if (response.failedEntryCount > 0) {
            val errors = response.entries?.map { "${it.errorCode}: ${it.errorMessage}" }
            throw RuntimeException("Failed to publish events: $errors")
        }
        logger.info { "Successfully published event to EventBridge" }
    }
}
