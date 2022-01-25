package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import ch.guengel.astro.openngc.Catalog
import ch.guengel.astro.openngc.Constellation
import ch.guengel.astro.openngc.Entry
import ch.guengel.astro.openngc.ObjectType
import ch.guengel.astro.server.model.CatalogLastUpdate
import io.mockk.every
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.streams.toList

@QuarkusTest
internal class OpenNGCServiceIT {
    val numberOfCatalogEntries = 100
    private var easyRandom = EasyRandom()

    lateinit var catalogEntries: List<Entry>

    @InjectMock
    lateinit var catalogProvider: CatalogProvider

    @Inject
    lateinit var catalogEntryMapper: CatalogEntryMapper

    @Inject
    lateinit var openNGCService: OpenNGCService

    @BeforeEach
    fun beforeEach() {
        catalogEntries = easyRandom.objects(Entry::class.java, numberOfCatalogEntries).toList()
        every { catalogProvider.loadCatalog() } returns Catalog(catalogEntries)
    }

    @Test
    fun `should get all object types`() {
        assertThat(openNGCService.objectTypes).hasSize(ObjectType.values().size)
    }

    @Test
    fun `should get all constellations`() {
        assertThat(openNGCService.constellations).hasSize(Constellation.values().size)
    }


    @Test
    fun `list pages correctly first page`() {
        var result = openNGCService.list(0, 1)
        assertThat(result.entryList).hasSize(1)
        assertThat(result.firstPage).isTrue()
        assertThat(result.lastPage).isFalse()
        assertThat(result.numberOfPages).isEqualTo(numberOfCatalogEntries)
        assertThat(result.pageIndex).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(1)
        assertThat(result.nextPageIndex).isNotNull().isEqualTo(1)
        assertThat(result.previousPageIndex).isNull()

        result = openNGCService.list(0, 24)
        assertThat(result.entryList).hasSize(24)
        assertThat(result.firstPage).isTrue()
        assertThat(result.lastPage).isFalse()
        assertThat(result.numberOfPages).isEqualTo(ceil(numberOfCatalogEntries / 24.0).toInt())
        assertThat(result.pageIndex).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(24)
        assertThat(result.nextPageIndex).isNotNull().isEqualTo(1)
        assertThat(result.previousPageIndex).isNull()

        result = openNGCService.list(0, numberOfCatalogEntries)
        assertThat(result.entryList).hasSize(numberOfCatalogEntries)
        assertThat(result.firstPage).isTrue()
        assertThat(result.lastPage).isTrue()
        assertThat(result.numberOfPages).isEqualTo(1)
        assertThat(result.pageIndex).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(numberOfCatalogEntries)
        assertThat(result.nextPageIndex).isNull()
        assertThat(result.previousPageIndex).isNull()

        result = openNGCService.list(0, numberOfCatalogEntries + 1)
        assertThat(result.entryList).hasSize(numberOfCatalogEntries)
        assertThat(result.firstPage).isTrue()
        assertThat(result.lastPage).isTrue()
        assertThat(result.numberOfPages).isEqualTo(1)
        assertThat(result.pageIndex).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(numberOfCatalogEntries + 1)
        assertThat(result.nextPageIndex).isNull()
        assertThat(result.previousPageIndex).isNull()
    }

    @Test
    fun `list pages correctly last page`() {
        val result = openNGCService.list(4, 24)
        assertThat(result.entryList).hasSize(4)
        assertThat(result.firstPage).isFalse()
        assertThat(result.lastPage).isTrue()
        assertThat(result.numberOfPages).isEqualTo(ceil(numberOfCatalogEntries / 24.0).toInt())
        assertThat(result.pageIndex).isEqualTo(4)
        assertThat(result.pageSize).isEqualTo(24)
        assertThat(result.nextPageIndex).isNull()
        assertThat(result.previousPageIndex).isNotNull().isEqualTo(3)
    }

    @Test
    fun `list should correctly handle invalid page input`() {
        assertThat { openNGCService.list(-1, 25) }
            .isFailure().hasClass(IllegalArgumentException::class)
        assertThat { openNGCService.list(0, 0) }
            .isFailure().hasClass(IllegalArgumentException::class)
        assertThat { openNGCService.list(4, 25) }
            .isFailure().hasClass(PageOutOfBoundsError::class)
    }

    @RepeatedTest(value = 10, name = RepeatedTest.LONG_DISPLAY_NAME)
    fun `list should correctly filter list`() {
        val needle = catalogEntries.random()

        val result = openNGCService.list(0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.fullname)
        )
        assertThat(result.entryList).hasSize(1)
        assertThat(result.entryList.first()).isEqualTo(catalogEntryMapper.map(needle))
    }

    @Test
    fun `list should throw correct exception when no objects are found`() {
        assertThat {
            openNGCService.list(0,
                numberOfCatalogEntries,
                objects = setOf(UUID.randomUUID().toString())
            )
        }.isFailure().hasClass(NoObjectsFoundError::class)
    }

    @Test
    fun `list should filter by constellation abbreviation and full name`() {
        val needle = catalogEntries.random()

        var result = openNGCService.list(0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.fullname, UUID.randomUUID().toString())
        )
        assertThat(result.entryList).hasSize(1)
        assertThat(result.entryList.first()).isEqualTo(catalogEntryMapper.map(needle))

        result = openNGCService.list(0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.abbrev, UUID.randomUUID().toString())
        )
        assertThat(result.entryList).hasSize(1)
        assertThat(result.entryList.first()).isEqualTo(catalogEntryMapper.map(needle))

        result = openNGCService.list(0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(
                needle.constellation!!.abbrev,
                needle.constellation!!.fullname,
                UUID.randomUUID().toString())
        )
        assertThat(result.entryList).hasSize(1)
        assertThat(result.entryList.first()).isEqualTo(catalogEntryMapper.map(needle))
    }


    @Test
    fun `should get object`() {
        val objectEntry = catalogEntries.random()
        val ngcEntry = openNGCService.getObject(objectEntry.name)
        assertThat(ngcEntry).isEqualTo(catalogEntryMapper.map(objectEntry))
    }

    @Test
    fun `get should throw proper exception on non-existing object`() {
        assertThat { openNGCService.getObject(UUID.randomUUID().toString()) }.isFailure()
            .hasClass(ObjectNotFoundError::class)
    }

    @Test
    fun `should get correct catalog update date`() {
        val date = OffsetDateTime.now()
        every { catalogProvider.getLastUpdated() } returns date

        val expectedLastUpdate = CatalogLastUpdate()
        expectedLastUpdate.lastUpdated = date
        assertThat(openNGCService.getLastCatalogUpdate()).isEqualTo(expectedLastUpdate)

        verify { catalogProvider.getLastUpdated() }
    }

    @Test
    fun `should handle null catalog last update date`() {
        every { catalogProvider.getLastUpdated() } returns null

        assertThat { openNGCService.getLastCatalogUpdate() }.isFailure().hasClass(CatalogNotLoadedError::class)

        verify { catalogProvider.getLastUpdated() }
    }

    @Test
    fun `fetch catalog should call catalog provider`() {
        val newEntry = easyRandom.nextObject(Entry::class.java)
        every { catalogProvider.fetchCatalog() } returns Catalog(listOf(newEntry))

        openNGCService.fetchCatalog()

        val result = openNGCService.list(0, 2)
        assertThat(result.entryList).hasSize(1)

        assertThat(result.entryList.first()).isEqualTo(catalogEntryMapper.map(newEntry))
        verify { catalogProvider.fetchCatalog() }
    }

    @Test
    fun `fetch catalog should not die on exception`() {
        every { catalogProvider.fetchCatalog() } throws RuntimeException("Test exception")

        assertThat { openNGCService.fetchCatalog() }.isSuccess()

        verify { catalogProvider.fetchCatalog() }
    }

    @Test
    fun `post construct should not die on exception`() {
        every { catalogProvider.loadCatalog() } throws RuntimeException("Test exception")

        assertThat { openNGCService.postConstruct() }.isSuccess()

        verify { catalogProvider.loadCatalog() }
    }
}
