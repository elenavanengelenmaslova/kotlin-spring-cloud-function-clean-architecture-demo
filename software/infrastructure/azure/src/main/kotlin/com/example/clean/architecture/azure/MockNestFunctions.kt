package com.example.clean.architecture.azure

import com.example.clean.architecture.model.HttpRequest
import com.example.clean.architecture.model.HttpResponse
import com.example.clean.architecture.service.HandleAdminRequest
import com.example.clean.architecture.service.HandleClientRequest
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import org.springframework.stereotype.Component
import org.springframework.http.HttpMethod as SpringHttpMethod

@Component
class MockNestFunctions(
    // TODO: HandleClientRequest,
    // TODO: HandleAdminRequest,
) {

    @FunctionName("RequestForwarder")
    fun forwardClientRequest(
        @HttpTrigger(
            name = "request",
            methods = [HttpMethod.POST, HttpMethod.GET, HttpMethod.PATCH, HttpMethod.PUT, HttpMethod.DELETE],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "mocknest/{*route}"
        ) request: HttpRequestMessage<String>,
        @BindingName("route") route: String?,
        context: ExecutionContext,
    ): HttpResponseMessage {
        // TODO handle client azure

        return buildResponse(request)
    }

    @FunctionName("Admin")
    fun forwardAdminRequest(
        @HttpTrigger(
            name = "request",
            methods = [HttpMethod.POST, HttpMethod.GET, HttpMethod.PATCH, HttpMethod.PUT, HttpMethod.DELETE],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "__admin/{*route}"
        ) request: HttpRequestMessage<String>,
        @BindingName("route") route: String?,
        context: ExecutionContext,
    ): HttpResponseMessage {
        // TODO handle admin azure

        return buildResponse(request)
    }

    private fun buildResponse(
        request: HttpRequestMessage<String>,
        //TODO: take response
    ): HttpResponseMessage {
        return request
            .createResponseBuilder(HttpStatus.valueOf(200))
            .body("Hello VoxxedDays Amsterdam 2025!")
            .build()
        // TODO: map response azure
    }

}