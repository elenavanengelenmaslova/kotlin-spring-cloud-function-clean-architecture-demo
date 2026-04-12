package com.example.clean.architecture.aws.event

import aws.sdk.kotlin.services.eventbridge.EventBridgeClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!local")
class EventBridgeConfig {

    @Bean
    fun eventBridgeClient(): EventBridgeClient = EventBridgeClient { region = "eu-west-1" }
}
