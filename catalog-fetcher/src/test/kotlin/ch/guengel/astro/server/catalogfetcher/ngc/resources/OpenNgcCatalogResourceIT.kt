package ch.guengel.astro.server.catalogfetcher.ngc.resources

import ch.guengel.astro.server.catalogfetcher.ngc.CatalogFetcherService
import ch.guengel.astro.server.common.ngc.CatalogFileService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@QuarkusTest
@TestHTTPEndpoint(OpenNgcCatalogResource::class)
internal class OpenNgcCatalogResourceIT {
    @InjectMock
    lateinit var catalogFetcherService: CatalogFetcherService

    @InjectMock
    lateinit var catalogFileService: CatalogFileService

    @Test
    @TestSecurity(user = "someuser", roles = ["admin"])
    fun `should fetch catalog when authenticated with correct role`() {
        every { catalogFetcherService.fetchAndNotify(any()) } just Runs

        When {
            put("/fetch")
        } Then {
            statusCode(204)
        }

        verify(exactly = 1) { catalogFetcherService.fetchAndNotify(any()) }
    }

    @Test
    @TestSecurity(user = "someuser", roles = ["some-rule"])
    fun `should not fetch catalog when authenticated with incorrect role`() {
        every { catalogFetcherService.fetchAndNotify(any()) } just Runs

        When {
            put("/fetch")
        } Then {
            statusCode(403)
        }

        verify(exactly = 0) { catalogFetcherService.fetchAndNotify(any()) }
    }

    @Test
    fun `should not fetch catalog when not authenticated`() {
        every { catalogFetcherService.fetchAndNotify(any()) } just Runs

        When {
            put("/fetch")
        } Then {
            statusCode(401)
        }

        verify(exactly = 0) { catalogFetcherService.fetchAndNotify(any()) }
    }

    @Test
    fun `should return last catalog update`() {
        val now = OffsetDateTime.now()
        every { catalogFileService.lastUpdated } returns now

        When {
            get("/last-update")
        } Then {
            statusCode(200)
            body("lastUpdated", `is`(DateTimeFormatter.ISO_DATE_TIME.format(now)))
        }

        verify(exactly = 1) { catalogFileService.lastUpdated }
    }

    @Test
    fun `should return 404 on no last update`() {
        every { catalogFileService.lastUpdated } returns null

        When {
            get("/last-update")
        } Then {
            statusCode(404)
        }

        verify(exactly = 1) { catalogFileService.lastUpdated }
    }
}
