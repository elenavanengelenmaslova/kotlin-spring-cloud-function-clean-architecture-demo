package com.example.clean.architecture.service.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class WebClientConfig(
    private val apiAuthInterceptor: ApiAuthInterceptor
) {

    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(apiAuthInterceptor)
        return restTemplate
    }
}