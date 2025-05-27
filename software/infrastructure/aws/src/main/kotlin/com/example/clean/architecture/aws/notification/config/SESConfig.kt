package com.example.clean.architecture.aws.notification.config

import aws.sdk.kotlin.services.ses.SesClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!local")
class SESConfig {

    @Bean
    fun sesClient(): SesClient = SesClient { region = "eu-west-1" }
}