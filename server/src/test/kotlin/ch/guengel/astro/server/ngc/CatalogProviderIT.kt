package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test
import java.io.File
import java.time.OffsetDateTime
import javax.inject.Inject

@QuarkusTest
internal class CatalogProviderIT {

    @InjectMock
    lateinit var catalogFetcherMock: CatalogFetcher

    @Inject
    lateinit var catalogProvider: CatalogProvider

    @Test
    fun `should not fetch catalog when locally available`() {
        val testCatalogFile = File("src/test/resources/testdata/catalog.csv")
        every { catalogFetcherMock.catalogFile } returns testCatalogFile
        every { catalogFetcherMock.fetch() } returns testCatalogFile

        val catalog = catalogProvider.loadCatalog()
        assertThat(catalog.entries).hasSize(9)

        verify { catalogFetcherMock.fetch() wasNot Called }
    }

    @Test
    fun `should fetch catalog when locally not available`() {
        val testCatalogFile = File("src/test/resources/testdata/catalog.csv")
        val nonExistingFile = File("/tmp/must-not-exist")
        every { catalogFetcherMock.catalogFile } returns nonExistingFile
        every { catalogFetcherMock.fetch() } returns testCatalogFile

        val catalog = catalogProvider.loadCatalog()
        assertThat(catalog.entries).hasSize(9)

        verify { catalogFetcherMock.fetch() }

    }

    @Test
    fun `should unconditionally fetch catalog`() {
        val testCatalogFile = File("src/test/resources/testdata/catalog.csv")
        every { catalogFetcherMock.catalogFile } returns testCatalogFile
        every { catalogFetcherMock.fetch() } returns testCatalogFile

        var catalog = catalogProvider.fetchCatalog()
        assertThat(catalog.entries).hasSize(9)

        catalog = catalogProvider.fetchCatalog()
        assertThat(catalog.entries).hasSize(9)

        verify(atLeast = 2, atMost = 2) { catalogFetcherMock.fetch() }
    }

    @Test
    fun `should return null as catalog last update datetime when non-existing`() {
        val nonExistingFile = File("/tmp/must-not-exist")
        every { catalogFetcherMock.catalogFile } returns nonExistingFile

        assertThat(catalogProvider.getLastUpdated()).isNull()
    }

    @Test
    fun `should return catalog last update datetime`() {
        val testCatalogFile = File("src/test/resources/testdata/catalog.csv")
        val now = OffsetDateTime.now().withNano(0)
        testCatalogFile.setLastModified(now.toEpochSecond() * 1_000)

        every { catalogFetcherMock.catalogFile } returns testCatalogFile

        assertThat(catalogProvider.getLastUpdated()).isEqualTo(now)
    }
}
