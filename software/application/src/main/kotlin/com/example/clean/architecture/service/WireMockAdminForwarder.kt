package com.example.clean.architecture.service

import com.example.clean.architecture.model.HttpRequest
import com.example.clean.architecture.model.HttpResponse
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.InvalidInputException
import com.github.tomakehurst.wiremock.common.Json
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import wiremock.org.apache.hc.core5.http.ContentType
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class WireMockAdminForwarder(
    private val wireMockServer: WireMockServer,
    //TODO: WireMockMappingRepository,
) : HandleAdminRequest {

    override fun invoke(
        path: String,
        httpRequest: HttpRequest,
    ): HttpResponse {
        val contentType = ContentType.APPLICATION_JSON.toString()
        return when {
            path == "requests/unmatched/near-misses" && httpRequest.method == HttpMethod.GET -> {
                logger.info { "Retrieving near misses" }
                wireMockServer.runCatching {
                    val mappings = Json.getObjectMapper().writeValueAsString(findNearMissesForUnmatchedRequests())
                    HttpResponse(
                        HttpStatusCode.valueOf(200),
                        HttpHeaders().apply { add(HttpHeaders.CONTENT_TYPE, contentType) },
                        body = mappings
                    )
                }.getOrElse {
                    handleAdminException(it)
                }
            }

            path == "mappings/reset" && httpRequest.method == HttpMethod.POST -> {
                logger.info { "Resetting WireMock mappings" }
                wireMockServer.runCatching {
                    resetToDefaultMappings()

                    // TODO: Delete all mappings from storage
                    HttpResponse(HttpStatusCode.valueOf(200), body = "Mappings reset successfully")
                }.getOrElse { handleAdminException(it) }
            }

            path == "mappings" && httpRequest.method == HttpMethod.POST -> {
                logger.info { "Creating new WireMock stub mapping" }
                wireMockServer.runCatching {
                    val addedMapping = httpRequest.body?.let { body ->
                        // Convert body to string once
                        val bodyString = body.toString()

                        // Parse the mapping
                        val mapping = bodyString.toStubMapping()
                        // Add the mapping to WireMock
                        addStubMapping(mapping)
                        // TODO: Check if mapping is persistent and save it
                        "Success"
                    }

                    // Return the response
                    HttpResponse(httpStatusCode = HttpStatusCode.valueOf(201), body = addedMapping)
                }.getOrElse {
                    handleAdminException(it)
                }

            }

            path.startsWith("mappings/") -> {
                val mappingIdAsString = path.removePrefix("mappings/")
                val mappingId = UUID.fromString(mappingIdAsString)
                when (httpRequest.method) {
                    HttpMethod.GET -> {
                        logger.info { "Retrieving WireMock mapping with ID: $mappingId" }
                        HttpResponse(HttpStatusCode.valueOf(200), body = wireMockServer.getStubMapping(mappingId))
                    }

                    HttpMethod.PUT -> {
                        logger.info { "Updating WireMock mapping with ID: $mappingId" }
                        val updatedMapping = httpRequest.body?.let { body ->
                            // Convert body to string once
                            val bodyString = body.toString()

                            // Parse the mapping
                            val mapping = bodyString.toStubMapping()

                            // Update the mapping in WireMock
                            wireMockServer.editStubMapping(mapping)
                            // TODO: Check if mapping is persistent and save it
                            "Success"
                        }
                        HttpResponse(HttpStatusCode.valueOf(200), body = updatedMapping)
                    }

                    HttpMethod.DELETE -> {
                        logger.info { "Deleting WireMock mapping with ID: $mappingId" }
                        wireMockServer.removeStubMapping(mappingId)
                        // TODO: Remove from persistent storage
                        HttpResponse(HttpStatusCode.valueOf(200), body = "Stub mapping deleted successfully")
                    }

                    else -> {
                        logger.warn { "Unsupported method for admin request: ${httpRequest.method}" }
                        HttpResponse(HttpStatusCode.valueOf(405), body = "Method not allowed")
                    }
                }
            }

            else -> {
                logger.warn { "Unknown WireMock admin API request: $path" }
                HttpResponse(
                    HttpStatusCode.valueOf(404), body = "Unknown admin request: $path"
                )
            }
        }
    }

    private fun handleAdminException(exception: Throwable) = when (exception) {
        is InvalidInputException ->
            HttpResponse(
                HttpStatusCode.valueOf(400),
                body = exception.message
            ).also { logger.info { "WireMock error: $exception" } }

        else -> HttpResponse(
            HttpStatusCode.valueOf(500),
            body = exception.message
        ).also { logger.error(exception) { "WireMock error: $exception" } }
    }

    private fun String.toStubMapping(): StubMapping = Json.getObjectMapper().readValue(this, StubMapping::class.java)
}
