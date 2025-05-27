package com.example.clean.architecture.service

import com.example.clean.architecture.model.BoredActivity
import com.example.clean.architecture.model.HttpResponse
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

@Service
class DemoRequestHandler(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate,
    @Value("\${bored.api.url}")
    private val boredApiUrl: String
) : HandleDemoRequest {

    override fun invoke(): HttpResponse {
        logger.info { "Calling Bored API at $boredApiUrl" }

        try {
            val responseType = object : ParameterizedTypeReference<List<BoredActivity>>() {}
            val response = restTemplate.exchange(
                boredApiUrl,
                HttpMethod.GET,
                null,
                responseType
            )

            val activities = response.body ?: emptyList()
            logger.info { "Received ${activities.size} activities from Bored API: $activities" }

            return HttpResponse(
                httpStatusCode = HttpStatus.OK,
                body = activities
            )
        } catch (e: RestClientResponseException) {
            logger.error { "Failed to get response from Bored API: ${e.rawStatusCode} - ${e.responseBodyAsString}" }
            return HttpResponse(
                httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
                body = "Failed to get response from Bored API"
            )
        } catch (e: Exception) {
            logger.error(e) { "Error calling Bored API" }
            return HttpResponse(
                httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
                body = "Error calling Bored API: ${e.message}"
            )
        }
    }
}
