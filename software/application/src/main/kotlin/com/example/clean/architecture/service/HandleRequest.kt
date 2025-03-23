package com.example.clean.architecture.service

import com.example.clean.architecture.model.HttpRequest
import com.example.clean.architecture.model.HttpResponse

fun interface HandleWireMockRequest {
    operator fun invoke(httpRequest: HttpRequest): HttpResponse
}
fun interface HandleAdminRequest {
    operator fun invoke(path: String, httpRequest: HttpRequest): HttpResponse
}