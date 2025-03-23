package com.example.clean.architecture.service.config

import com.example.clean.architecture.persistence.WireMockMappingRepository
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn


private val logger = KotlinLogging.logger {}

@Configuration
class WireMockConfig {

    @Value("\${wiremock.root-dir:wiremock}")
    private val rootDir: String = "wiremock"

    @Bean
    fun directCallHttpServerFactory() = DirectCallHttpServerFactory()

    @Bean
    fun wireMockServer(directCallHttpServerFactory: DirectCallHttpServerFactory, mappingRepository: WireMockMappingRepository): WireMockServer {
        val wireMockServer = WireMockServer(
            wireMockConfig()
                .usingFilesUnderClasspath(rootDir)
                .notifier(ConsoleNotifier(true))
                .httpServerFactory(directCallHttpServerFactory)
        )
        wireMockServer.start()
        wireMockServer.loadMappingsFromStorage(mappingRepository)
        logger.info { "WireMock server started with root dir: $rootDir" }
        return wireMockServer
    }


    @Bean
    @DependsOn("wireMockServer")
    fun directCallHttpServer(directCallHttpServerFactory: DirectCallHttpServerFactory): DirectCallHttpServer {
        return directCallHttpServerFactory.httpServer
    }

    private fun WireMockServer.loadMappingsFromStorage(mappingRepository: WireMockMappingRepository) {
        logger.info { "Loading mappings from storage..." }

        val mappings = mappingRepository.listMappings()
        if (mappings.isEmpty()) {
            logger.info { "No mappings found in storage." }
            return
        }

        mappings.forEach { mappingId ->
            mappingRepository.getMapping(mappingId)?.let {
                val stubMapping = StubMapping.buildFrom(it)
                addStubMapping(stubMapping)
                logger.info { "Loaded mapping: $mappingId into WireMock memory" }
            }?: logger.warn { "Failed to load mapping: $mappingId" }
        }
        logger.info { "Finished loading mappings from storage." }
    }
}
