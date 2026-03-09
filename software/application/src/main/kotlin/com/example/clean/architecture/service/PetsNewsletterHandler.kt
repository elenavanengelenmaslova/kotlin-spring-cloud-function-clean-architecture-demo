package com.example.clean.architecture.service

import com.example.clean.architecture.model.HttpResponse
import com.example.clean.architecture.model.Pet
import com.example.clean.architecture.notification.DocumentNotificationInterface
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

@Service
class PetsNewsletterHandler(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate,
    private val emailSender: DocumentNotificationInterface,
    @Value("\${api.petstore.url}")
    private val petstoreApiUrl: String
) : HandlePetsNewsletterRequest {

    /**
     * Formats pets newsletter as HTML for email
     */
    private fun formatNewsletterAsHtml(newPet: Pet?, availablePets: List<Pet>): String {
        val featuredSection = if (newPet != null) {
            val photoUrls = newPet.photoUrls
            val tags = newPet.tags
            """
            <div class="featured">
                <h2>🌟 New Pet Alert!</h2>
                <div class="pet-card featured-pet">
                    ${if (!photoUrls.isNullOrEmpty()) "<img src=\"${photoUrls[0]}\" alt=\"${newPet.name}\" class=\"pet-image\">" else ""}
                    <h3>${newPet.name ?: "Unknown"}</h3>
                    <p><span class="label">Status:</span> ${newPet.status ?: "N/A"}</p>
                    <p><span class="label">ID:</span> ${newPet.id ?: "N/A"}</p>
                    ${if (!tags.isNullOrEmpty()) "<p><span class=\"label\">Tags:</span> ${tags.joinToString(", ")}</p>" else ""}
                </div>
            </div>
            """
        } else {
            ""
        }

        val petsList = availablePets.joinToString("") { pet ->
            val petTags = pet.tags
            """
            <div class="pet-card">
                <h4>${pet.name ?: "Unknown"}</h4>
                <p><span class="label">ID:</span> ${pet.id ?: "N/A"}</p>
                <p><span class="label">Status:</span> ${pet.status ?: "N/A"}</p>
                ${if (!petTags.isNullOrEmpty()) "<p><span class=\"label\">Tags:</span> ${petTags.joinToString(", ")}</p>" else ""}
            </div>
            """
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Pet Adoption Newsletter</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f5f5f5; }
                    h1 { color: #2c3e50; text-align: center; }
                    h2 { color: #e74c3c; }
                    .featured { background-color: #fff3cd; border: 2px solid #ffc107; padding: 20px; margin: 20px 0; border-radius: 8px; }
                    .pet-card { background-color: #ffffff; border: 1px solid #ddd; padding: 15px; margin: 10px 0; border-radius: 5px; }
                    .featured-pet { border: 2px solid #e74c3c; }
                    .pet-image { max-width: 100%; height: auto; border-radius: 5px; margin: 10px 0; }
                    .label { font-weight: bold; color: #555; }
                    .pets-list { background-color: #ffffff; padding: 20px; margin: 20px 0; border-radius: 8px; }
                    .footer { margin-top: 30px; font-size: 0.8em; color: #777; border-top: 1px solid #eee; padding-top: 10px; text-align: center; }
                    h4 { margin: 0 0 10px 0; color: #2c3e50; }
                </style>
            </head>
            <body>
                <h1>🐾 Pet Adoption Newsletter 🐾</h1>

                $featuredSection

                <div class="pets-list">
                    <h2>Available Pets for Adoption</h2>
                    <p>Here are all the pets currently available and looking for their forever home:</p>
                    $petsList
                </div>

                <div class="footer">
                    <p>This email was sent automatically from the Pet Store API demo application.</p>
                    <p>Give a pet a loving home today! 🏡</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    override fun invoke(): HttpResponse {
        logger.info { "Fetching pets data from $petstoreApiUrl" }

        try {
            val responseType = object : ParameterizedTypeReference<List<Pet>>() {}

            // Fetch pets with "new" tag
            val newPetsUrl = "$petstoreApiUrl/findByTags?tags=new"
            logger.info { "Calling Pet Store API for new pets at $newPetsUrl" }
            val newPetsResponse = restTemplate.exchange(
                newPetsUrl,
                HttpMethod.GET,
                null,
                responseType
            )
            val newPets = newPetsResponse.body ?: emptyList()
            logger.info { "Received ${newPets.size} new pets: $newPets" }

            // Fetch all available pets
            val availablePetsUrl = "$petstoreApiUrl/findByStatus?status=available"
            logger.info { "Calling Pet Store API for available pets at $availablePetsUrl" }
            val availablePetsResponse = restTemplate.exchange(
                availablePetsUrl,
                HttpMethod.GET,
                null,
                responseType
            )
            val availablePets = availablePetsResponse.body ?: emptyList()
            logger.info { "Received ${availablePets.size} available pets: $availablePets" }

            // Generate and send email
            val featuredPet = newPets.randomOrNull()
            val htmlContent = formatNewsletterAsHtml(featuredPet, availablePets)
            emailSender.sendEmail(htmlContent)
            logger.info { "Sent pet adoption newsletter email with ${availablePets.size} available pets" }

            return HttpResponse(
                httpStatusCode = HttpStatus.OK,
                body = mapOf(
                    "newPets" to newPets,
                    "availablePets" to availablePets,
                    "emailSent" to true
                )
            )
        } catch (e: RestClientResponseException) {
            logger.error { "Failed to get response from Pet Store API: ${e.rawStatusCode} - ${e.responseBodyAsString}" }
            return HttpResponse(
                httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
                body = "Failed to get response from Pet Store API"
            )
        } catch (e: Exception) {
            logger.error(e) { "Error calling Pet Store API" }
            return HttpResponse(
                httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
                body = "Error calling Pet Store API: ${e.message}"
            )
        }
    }
}
