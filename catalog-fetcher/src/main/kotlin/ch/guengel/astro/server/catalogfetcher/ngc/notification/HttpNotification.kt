package ch.guengel.astro.server.catalogfetcher.ngc.notification

import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class HttpNotification(
    @ConfigProperty(name = "astro-server.catalog-fetcher.notification.path") private val notificationPath: String,
    @ConfigProperty(name = "astro-server.catalog-fetcher.notification.scheme") private val scheme: String,
    @ConfigProperty(name = "astro-server.catalog-fetcher.notification.timeout") private val timeout: Duration,
) {
    fun notify(hostnames: List<String>, jwt: String) {
        val client = createClient()

        log.info("Start notifying hosts to reload catalog")
        val errorList = mutableListOf<String>()
        hostnames.forEach {
            try {
                notifyHost(it, client, jwt)
            } catch (e: Exception) {
                errorList += e.message ?: "No message"
            }
        }
        log.info("Done notifying hosts to reload catalog")

        if (errorList.isNotEmpty()) {
            throw NotificationError(errorList.joinToString("; "))
        }
    }

    private fun createClient() = HttpClient.newBuilder().connectTimeout(timeout).build()

    private fun notifyHost(hostname: String, httpClient: HttpClient, jwt: String) {
        val notificationURI = getNotificationURIForHostname(hostname)
        log.info("Notify ${notificationURI.toASCIIString()} to reload catalog")
        val httpRequest = HttpRequest.newBuilder()
            .uri(notificationURI)
            .header("Authorization", "Bearer $jwt")
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build()

        try {
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding())
            val statusCode = response.statusCode()
            if (statusCode != 204) {
                val message = "Received status code $statusCode from $hostname when notifying to reload catalog"
                log.error(message)
                throw NotificationError(message)
            }

            log.info("Successfully notified ${notificationURI.toASCIIString()} to reload catalog")
        } catch (e: NotificationError) {
            throw e
        } catch (e: Exception) {
            val message = "Error notifying '${hostname}' to reload catalog"
            log.error(message, e)
            throw NotificationError(message, e)
        }


    }

    private fun getNotificationURIForHostname(hostname: String): URI =
        URI.create("${scheme}://${hostname}/${notificationPath}").normalize()

    private companion object {
        private val log: Logger = Logger.getLogger(HttpNotification::class.java)
    }

}
