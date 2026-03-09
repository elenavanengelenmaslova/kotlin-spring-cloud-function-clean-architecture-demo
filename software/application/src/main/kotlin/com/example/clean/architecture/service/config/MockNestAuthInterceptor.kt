package com.example.clean.architecture.service.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Interceptor that adds X-Api-Key header to API requests when tokens are configured.
 * Each API (bored, petstore, etc.) can have its own token.
 * If a token is present for an API, the X-Api-Key header is added to matching requests.
 */
@Component
class ApiAuthInterceptor(
    @Value("\${api.bored.url}")
    private val boredApiUrl: String,
    @Value("\${api.bored.token:}")
    private val boredApiToken: String?,
    @Value("\${api.petstore.url}")
    private val petstoreApiUrl: String,
    @Value("\${api.petstore.token:}")
    private val petstoreApiToken: String?
) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val requestUrl = request.uri.toString()

        // Check which API this request is for and add the appropriate token
        when {
            requestUrl.startsWith(boredApiUrl) && !boredApiToken.isNullOrBlank() -> {
                logger.debug { "Adding X-Api-Key header to Bored API request: ${request.uri}" }
                request.headers.add("X-Api-Key", boredApiToken)
            }
            requestUrl.startsWith(petstoreApiUrl) && !petstoreApiToken.isNullOrBlank() -> {
                logger.debug { "Adding X-Api-Key header to Petstore API request: ${request.uri}" }
                request.headers.add("X-Api-Key", petstoreApiToken)
            }
        }

        return execution.execute(request, body)
    }
}
