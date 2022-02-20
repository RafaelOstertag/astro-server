package ch.guengel.astro.server.catalogfetcher.ngc

import assertk.assertThat
import assertk.assertions.isSuccess
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verifySequence
import io.quarkus.oidc.client.OidcClient
import io.quarkus.oidc.client.Tokens
import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(MockKExtension::class)
internal class ScheduledCatalogFetcherTest {
    @MockK
    private lateinit var catalogFetcherService: CatalogFetcherService

    @MockK
    private lateinit var oidcClient: OidcClient

    @InjectMockKs
    private lateinit var scheduledCatalogFetcher: ScheduledCatalogFetcher

    @Test
    fun `should call catalog fetcher`() {
        every { catalogFetcherService.fetchAndNotify("test jwt") } just Runs
        every { oidcClient.tokens } returns Uni.createFrom().item(Tokens("test jwt", 0, Duration.ZERO, "", 0, null))

        assertThat { scheduledCatalogFetcher.fetchCatalog() }.isSuccess()

        verifySequence {
            oidcClient.tokens
            catalogFetcherService.fetchAndNotify("test jwt")
        }
    }

    @Test
    fun `should not die on exception`() {
        every { catalogFetcherService.fetchAndNotify(any()) } throws RuntimeException("test exception")
        every { oidcClient.tokens } returns Uni.createFrom().item(Tokens("test jwt", 0, Duration.ZERO, "", 0, null))

        assertThat { scheduledCatalogFetcher.fetchCatalog() }.isSuccess()

        verifySequence {
            oidcClient.tokens
            catalogFetcherService.fetchAndNotify("test jwt")
        }

    }

}
