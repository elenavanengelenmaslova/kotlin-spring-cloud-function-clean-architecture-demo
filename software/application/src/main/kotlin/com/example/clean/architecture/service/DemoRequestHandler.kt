package com.example.clean.architecture.service

import com.example.clean.architecture.model.BoredActivity
import com.example.clean.architecture.model.HttpResponse
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
class DemoRequestHandler(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate,
    private val emailSender: DocumentNotificationInterface,
    @Value("\${bored.api.url}")
    private val boredApiUrl: String
) : HandleDemoRequest {

    /**
     * Formats a BoredActivity as HTML for email
     */
    private fun formatActivityAsHtml(activity: BoredActivity): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Bored Activity Suggestion</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    h1 { color: #2c3e50; }
                    .activity { background-color: #f8f9fa; border-left: 4px solid #4CAF50; padding: 15px; margin: 20px 0; }
                    .label { font-weight: bold; color: #555; }
                    .footer { margin-top: 30px; font-size: 0.8em; color: #777; border-top: 1px solid #eee; padding-top: 10px; }
                </style>
            </head>
            <body>
                <h1>Your Activity Suggestion</h1>
                <p>Here's an activity you might enjoy:</p>

                <div class="activity">
                    <p><span class="label">Activity:</span> ${activity.activity ?: "N/A"}</p>
                    <p><span class="label">Type:</span> ${activity.type ?: "N/A"}</p>
                    <p><span class="label">Participants:</span> ${activity.participants ?: "N/A"}</p>
                    <p><span class="label">Price:</span> ${activity.price ?: "N/A"}</p>
                    <p><span class="label">Accessibility:</span> ${activity.accessibility ?: "N/A"}</p>
                    <p><span class="label">Duration:</span> ${activity.duration ?: "N/A"}</p>
                    <p><span class="label">Kid Friendly:</span> ${activity.kidFriendly ?: "N/A"}</p>
                    ${if (activity.link != null) "<p><span class=\"label\">Link:</span> <a href=\"${activity.link}\">${activity.link}</a></p>" else ""}
                    <p><span class="label">Key:</span> ${activity.key ?: "N/A"}</p>
                </div>

                <div class="footer">
                    <p>This email was sent automatically from the Bored API demo application.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    override fun invoke(): HttpResponse {
        logger.info { "Calling Bored API at $boredApiUrl" }

        try {
            val responseType = object : ParameterizedTypeReference<List<BoredActivity>>() {}
            val response = restTemplate.exchange(
                boredApiUrl,
                HttpMethod.GET,
                null,
                responseType
            )

            val activities = response.body ?: emptyList()
            logger.info { "Received ${activities.size} activities from Bored API: $activities" }

            // Send a random activity via email if there are any activities
            if (activities.isNotEmpty()) {
                val randomActivity = activities.random()
                val htmlContent = formatActivityAsHtml(randomActivity)
                emailSender.sendEmail(htmlContent)
                logger.info { "Sent email with random activity: $randomActivity" }
            }

            return HttpResponse(
                httpStatusCode = HttpStatus.OK,
                body = activities
            )
        } catch (e: RestClientResponseException) {
            logger.error { "Failed to get response from Bored API: ${e.rawStatusCode} - ${e.responseBodyAsString}" }
            return HttpResponse(
                httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
                body = "Failed to get response from Bored API"
            )
        } catch (e: Exception) {
            logger.error(e) { "Error calling Bored API" }
            return HttpResponse(
                httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR,
                body = "Error calling Bored API: ${e.message}"
            )
        }
    }
}
