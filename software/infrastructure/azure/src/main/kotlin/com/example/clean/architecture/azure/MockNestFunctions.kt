package com.example.clean.architecture.azure

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import org.springframework.stereotype.Component

@Component
class MockNestFunctions(
    //TODO: HandleWireMockRequest
    //TODO: HandleAdminRequest
) {

    @FunctionName("WiremockForwarder")
    fun forwardToWiremock(
        @HttpTrigger(
            name = "request",
            methods = [HttpMethod.POST, HttpMethod.GET, HttpMethod.PATCH, HttpMethod.PUT, HttpMethod.DELETE],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "wiremock/{*route}"
        ) request: HttpRequestMessage<String>,
        @BindingName("route") route: String?,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return buildResponse(request)
    }

    @FunctionName("WiremockAdmin")
    fun forwardToWiremockAdmin(
        @HttpTrigger(
            name = "request",
            methods = [HttpMethod.POST, HttpMethod.GET, HttpMethod.PATCH, HttpMethod.PUT, HttpMethod.DELETE],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "__admin/{*route}"
        ) request: HttpRequestMessage<String>,
        @BindingName("route") route: String?,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return buildResponse(request)
    }

    private fun buildResponse(
        request: HttpRequestMessage<String>,
        //TODO: take response
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.valueOf(200))
            //TODO: Add headers
            .body("Hello VoxxedDays Amsterdam!")
            .build()
    }

}