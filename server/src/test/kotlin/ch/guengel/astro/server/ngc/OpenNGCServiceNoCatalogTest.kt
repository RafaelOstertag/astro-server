package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import ch.guengel.astro.openngc.ExtendedNgcEntry
import ch.guengel.astro.openngc.NgcEntry
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class OpenNGCServiceNoCatalogTest {
    lateinit var catalogEntryMapper: CatalogEntryMapper

    lateinit var openNGCService: OpenNGCService

    @BeforeEach
    fun beforeEach() {
        val catalogProvider = mockk<CatalogProvider>()
        every { catalogProvider.loadCatalog() } throws RuntimeException("test exception")

        catalogEntryMapper = spyk<CatalogEntryMapper>()
        openNGCService = OpenNGCService(catalogProvider, catalogEntryMapper)
        openNGCService.postConstruct()
    }

    @Test
    fun `should return empty list on no catalog`() {
        val arguments = OpenNGCService.ListArguments(0, 25)
        val list = openNGCService.list(arguments)
        assertThat(list.entryList).isEmpty()
        assertThat(list.firstPage).isFalse()
        assertThat(list.pageIndex).isEqualTo(0)
        assertThat(list.pageSize).isEqualTo(25)
        assertThat(list.lastPage).isFalse()
        assertThat(list.nextPageIndex).isNull()
        assertThat(list.previousPageIndex).isNull()
        assertThat(list.numberOfPages).isEqualTo(0)
        assertThat(list.numberOfEntries).isEqualTo(0)

        verify {
            listOf(
                catalogEntryMapper.map(any<ExtendedNgcEntry>()),
                catalogEntryMapper.map(any<NgcEntry>())
            ) wasNot Called
        }
    }

    @Test
    fun `should return empty extended list on no catalog`() {
        val arguments = OpenNGCService.ListExtendedArguments(
            OpenNGCService.ListArguments(0, 25),
            2.0, 3.0, OffsetDateTime.now()
        )
        val list = openNGCService.listExtended(arguments)
        assertThat(list.entryList).isEmpty()
        assertThat(list.firstPage).isFalse()
        assertThat(list.pageIndex).isEqualTo(0)
        assertThat(list.pageSize).isEqualTo(25)
        assertThat(list.lastPage).isFalse()
        assertThat(list.nextPageIndex).isNull()
        assertThat(list.previousPageIndex).isNull()
        assertThat(list.numberOfPages).isEqualTo(0)
        assertThat(list.numberOfEntries).isEqualTo(0)

        verify {
            listOf(
                catalogEntryMapper.map(any<ExtendedNgcEntry>()),
                catalogEntryMapper.map(any<NgcEntry>())
            ) wasNot Called
        }
    }
}
