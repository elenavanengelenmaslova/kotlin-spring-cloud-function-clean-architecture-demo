package com.example.clean.architecture.aws.function

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Function

private val logger = KotlinLogging.logger {}
private const val ADMIN_PREFIX = "/__admin/"
private const val WIREMOCK_PREFIX = "/wiremock/"

@Configuration
class MockNestFunctions(
    //TODO: HandleWireMockRequest
    //TODO: HandleAdminRequest
) {
    @Bean
    fun router(): Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        return Function { event ->
            APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                // TODO: Add headers
                .withBody("Hello VoxxedDays Amsterdam!")
        }
    }
}


