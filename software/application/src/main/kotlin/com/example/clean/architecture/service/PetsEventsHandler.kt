package com.example.clean.architecture.service

import com.example.clean.architecture.event.EventPublisherInterface
import com.example.clean.architecture.model.HttpRequest
import com.example.clean.architecture.model.HttpResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PetsEventsHandler(
    private val eventPublisher: EventPublisherInterface,
    @Value("\${eventbridge.source:pets-app}")
    private val eventSource: String,
    @Value("\${eventbridge.detail-type:PetsEvent}")
    private val detailType: String,
) : HandlePetsEventsRequest {

    override fun invoke(httpRequest: HttpRequest): HttpResponse {
        val body = httpRequest.body?.toString()
        if (body.isNullOrBlank()) {
            return HttpResponse(HttpStatus.BAD_REQUEST, body = "Request body is required")
        }
        return try {
            eventPublisher.publishEvent(eventSource, detailType, body)
            logger.info { "Published pets event to EventBridge: source=$eventSource, detailType=$detailType" }
            HttpResponse(HttpStatus.ACCEPTED, body = """{"status":"event_published"}""")
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish pets event to EventBridge" }
            HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR, body = "Failed to publish event: ${e.message}")
        }
    }
}
