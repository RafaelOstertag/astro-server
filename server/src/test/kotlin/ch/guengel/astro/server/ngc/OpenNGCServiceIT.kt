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
import ch.guengel.astro.coordinates.Angle
import ch.guengel.astro.coordinates.GeographicCoordinates
import ch.guengel.astro.coordinates.toHorizontalCoordinates
import ch.guengel.astro.openngc.Catalog
import ch.guengel.astro.openngc.Constellation
import ch.guengel.astro.openngc.NgcEntry
import ch.guengel.astro.openngc.ObjectType
import ch.guengel.astro.server.easyRandomParameters
import ch.guengel.astro.server.model.CatalogLastUpdate
import ch.guengel.astro.server.model.NGCEntryWithHorizontalCoordinates
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

private const val longitude = 8.0
private const val latitude = 47.0

@QuarkusTest
internal class OpenNGCServiceIT {
    private val numberOfCatalogEntries = 100
    private var easyRandom = EasyRandom(easyRandomParameters)

    private lateinit var catalogEntries: List<NgcEntry>

    @InjectMock
    lateinit var catalogProvider: CatalogProvider

    @Inject
    lateinit var catalogEntryMapper: CatalogEntryMapper

    @Inject
    lateinit var openNGCService: OpenNGCService

    @BeforeEach
    fun beforeEach() {
        catalogEntries = easyRandom.objects(NgcEntry::class.java, numberOfCatalogEntries).toList()
        every { catalogProvider.loadCatalog() } returns Catalog(catalogEntries)
        openNGCService.postConstruct()
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
            catalog = needle.id.catalogName.name,
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
            catalog = needle.id.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.fullname, UUID.randomUUID().toString())
        )
        assertThat(result.entryList).hasSize(1)
        assertThat(result.entryList.first()).isEqualTo(catalogEntryMapper.map(needle))

        result = openNGCService.list(0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.id.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.abbrev, UUID.randomUUID().toString())
        )
        assertThat(result.entryList).hasSize(1)
        assertThat(result.entryList.first()).isEqualTo(catalogEntryMapper.map(needle))

        result = openNGCService.list(0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.id.catalogName.name,
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
    fun `list extended pages correctly first page`() {
        var result = OpenNGCService.ListExtendedArguments(
            longitude,
            latitude,
            OffsetDateTime.now(),
            0,
            1).let { openNGCService.listExtended(it) }

        assertThat(result.entryList).hasSize(1)
        assertThat(result.firstPage).isTrue()
        assertThat(result.lastPage).isFalse()
        assertThat(result.numberOfPages).isEqualTo(numberOfCatalogEntries)
        assertThat(result.pageIndex).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(1)
        assertThat(result.nextPageIndex).isNotNull().isEqualTo(1)
        assertThat(result.previousPageIndex).isNull()

        result = OpenNGCService.ListExtendedArguments(longitude, latitude, OffsetDateTime.now(), 0, 24)
            .let { openNGCService.listExtended(it) }
        assertThat(result.entryList).hasSize(24)
        assertThat(result.firstPage).isTrue()
        assertThat(result.lastPage).isFalse()
        assertThat(result.numberOfPages).isEqualTo(ceil(numberOfCatalogEntries / 24.0).toInt())
        assertThat(result.pageIndex).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(24)
        assertThat(result.nextPageIndex).isNotNull().isEqualTo(1)
        assertThat(result.previousPageIndex).isNull()

        result =
            OpenNGCService.ListExtendedArguments(longitude, latitude, OffsetDateTime.now(), 0, numberOfCatalogEntries)
                .let { openNGCService.listExtended(it) }
        assertThat(result.entryList).hasSize(numberOfCatalogEntries)
        assertThat(result.firstPage).isTrue()
        assertThat(result.lastPage).isTrue()
        assertThat(result.numberOfPages).isEqualTo(1)
        assertThat(result.pageIndex).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(numberOfCatalogEntries)
        assertThat(result.nextPageIndex).isNull()
        assertThat(result.previousPageIndex).isNull()

        result = OpenNGCService.ListExtendedArguments(longitude,
            latitude,
            OffsetDateTime.now(),
            0,
            numberOfCatalogEntries + 1).let { openNGCService.listExtended(it) }
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
    fun `list extended pages correctly last page`() {
        val result = OpenNGCService.ListExtendedArguments(longitude, latitude, OffsetDateTime.now(), 4, 24)
            .let { openNGCService.listExtended(it) }
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
    fun `list extended should correctly handle invalid page input`() {
        var arguments = OpenNGCService.ListExtendedArguments(longitude, latitude, OffsetDateTime.now(), -1, 25)
        assertThat { openNGCService.listExtended(arguments) }
            .isFailure().hasClass(IllegalArgumentException::class)

        arguments = OpenNGCService.ListExtendedArguments(longitude, latitude, OffsetDateTime.now(), 0, 0)
        assertThat { openNGCService.listExtended(arguments) }
            .isFailure().hasClass(IllegalArgumentException::class)

        arguments = OpenNGCService.ListExtendedArguments(longitude, latitude, OffsetDateTime.now(), 4, 25)
        assertThat { openNGCService.listExtended(arguments) }
            .isFailure().hasClass(PageOutOfBoundsError::class)
    }

    @RepeatedTest(value = 10, name = RepeatedTest.LONG_DISPLAY_NAME)
    fun `list extended should correctly filter list`() {
        val needle = catalogEntries.random()

        val localTime = OffsetDateTime.now()
        val result = OpenNGCService.ListExtendedArguments(longitude, latitude, localTime, 0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.id.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.fullname)).let {
            openNGCService.listExtended(it)
        }
        assertThat(result.entryList).hasSize(1)
        val ngcEntryWithHorizonCoordinates = result.entryList.first()
        assertNgcEntryWithHorzonCoordinates(ngcEntryWithHorizonCoordinates, needle, localTime)
    }

    @Test
    fun `list extended should throw correct exception when no objects are found`() {
        val arguments = OpenNGCService.ListExtendedArguments(longitude, latitude, OffsetDateTime.now(), 0,
            numberOfCatalogEntries,
            objects = setOf(UUID.randomUUID().toString()))
        assertThat {
            openNGCService.listExtended(arguments)
        }.isFailure().hasClass(NoObjectsFoundError::class)
    }

    @Test
    fun `list extended should filter by constellation abbreviation and full name`() {
        val needle = catalogEntries.random()

        val localTime = OffsetDateTime.now()
        var result = OpenNGCService.ListExtendedArguments(longitude,
            latitude,
            localTime,
            0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.id.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.fullname, UUID.randomUUID().toString()))
            .let { openNGCService.listExtended(it) }
        assertThat(result.entryList).hasSize(1)
        assertNgcEntryWithHorzonCoordinates(result.entryList.first(), needle, localTime)

        result = OpenNGCService.ListExtendedArguments(longitude,
            latitude,
            localTime,
            0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.id.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(needle.constellation!!.abbrev, UUID.randomUUID().toString()))
            .let { openNGCService.listExtended(it) }
        assertThat(result.entryList).hasSize(1)
        assertNgcEntryWithHorzonCoordinates(result.entryList.first(), needle, localTime)

        result = OpenNGCService.ListExtendedArguments(longitude,
            latitude,
            localTime,
            0,
            numberOfCatalogEntries,
            messier = needle.isMessier(),
            catalog = needle.id.catalogName.name,
            objects = setOf(needle.name),
            constellations = setOf(
                needle.constellation!!.abbrev,
                needle.constellation!!.fullname,
                UUID.randomUUID().toString())).let { openNGCService.listExtended(it) }
        assertThat(result.entryList).hasSize(1)
        assertNgcEntryWithHorzonCoordinates(result.entryList.first(), needle, localTime)
    }

    private fun assertNgcEntryWithHorzonCoordinates(
        ngcEntryWithHorizonCoordinates: NGCEntryWithHorizontalCoordinates,
        ngcEntry: NgcEntry,
        localTime: OffsetDateTime,
    ) {
        assertThat(ngcEntryWithHorizonCoordinates.entry).isEqualTo(catalogEntryMapper.map(ngcEntry))
        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates)
            .isEqualTo(catalogEntryMapper.map(ngcEntry.equatorialCoordinates!!.toHorizontalCoordinates(
                GeographicCoordinates(Angle.of(latitude), Angle.of(longitude)), localTime)))
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
    fun `should get object extended`() {
        val localTime = OffsetDateTime.now()
        val objectEntry = catalogEntries.random()
        val ngcEntryWithHorizontalCoordinates =
            openNGCService.getObjectExtended(longitude, latitude, localTime, objectEntry.name)
        assertThat(ngcEntryWithHorizontalCoordinates.entry).isEqualTo(catalogEntryMapper.map(objectEntry))
        assertThat(ngcEntryWithHorizontalCoordinates.horizontalCoordinates).isEqualTo(
            catalogEntryMapper.map(objectEntry.equatorialCoordinates
            !!.toHorizontalCoordinates(GeographicCoordinates(Angle.of(latitude), Angle.of(longitude)), localTime)))
    }

    @Test
    fun `get should throw proper exception on non-existing object extended`() {
        assertThat {
            openNGCService.getObjectExtended(longitude,
                latitude,
                OffsetDateTime.now(),
                UUID.randomUUID().toString())
        }.isFailure()
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
        val newEntry = easyRandom.nextObject(NgcEntry::class.java)
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
