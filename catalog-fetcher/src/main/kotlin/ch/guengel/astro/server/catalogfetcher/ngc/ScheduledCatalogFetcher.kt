package ch.guengel.astro.server.catalogfetcher.ngc

import io.quarkus.oidc.client.OidcClient
import io.quarkus.scheduler.Scheduled
import org.jboss.logging.Logger
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class ScheduledCatalogFetcher(
    private val catalogFetcherService: CatalogFetcherService,
    private val oidcClient: OidcClient,
) {
    @Scheduled(cron = "{astro-server.catalog-fetcher.cron.expression}")
    fun fetchCatalog() {
        log.info("Start Fetching catalog scheduled")
        try {
            log.debug("Get access token")
            val token = oidcClient.tokens.await().atMost(Duration.of(10, ChronoUnit.SECONDS))
            log.debug("Fetch catalog and notify")
            catalogFetcherService.fetchAndNotify(token.accessToken)
        } catch (e: Exception) {
            log.error("Error while fetching catalog", e)
        }
    }

    private companion object {
        private val log: Logger = Logger.getLogger(ScheduledCatalogFetcher::class.java)
    }
}
