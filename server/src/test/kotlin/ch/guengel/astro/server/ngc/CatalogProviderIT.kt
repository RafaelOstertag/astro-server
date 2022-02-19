package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import ch.guengel.astro.server.common.ngc.CatalogFileService
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
    lateinit var catalogFilesServiceMock: CatalogFileService

    @Inject
    lateinit var catalogProvider: CatalogProvider

    @Test
    fun `should load catalog`() {
        val testCatalogFile = File("src/test/resources/testdata/catalog.csv")
        every { catalogFilesServiceMock.catalogFile } returns testCatalogFile

        val catalog = catalogProvider.loadCatalog()
        assertThat(catalog.entries).hasSize(9)

        verify { catalogFilesServiceMock.catalogFile }
    }

    @Test
    fun `should return null as catalog last update datetime when non-existing`() {
        every { catalogFilesServiceMock.lastUpdated } returns null

        assertThat(catalogProvider.getLastUpdated()).isNull()
    }

    @Test
    fun `should return catalog last update datetime`() {
        val now = OffsetDateTime.now()

        every { catalogFilesServiceMock.lastUpdated } returns now

        assertThat(catalogProvider.getLastUpdated()).isEqualTo(now)
    }
}
