package com.example.clean.architecture.aws.persistence.config

import aws.sdk.kotlin.services.s3.S3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!local")
class S3Config(
    @Value("\${aws.s3.bucket-name}") private val backetName: String,
) {

    @Bean
    fun s3Client(): S3Client = S3Client { region = "eu-west-1" }
}