package ch.guengel.astro.server.resources

import ch.guengel.astro.server.model.CatalogLastUpdate
import ch.guengel.astro.server.model.NGCEntry
import ch.guengel.astro.server.model.ObjectType
import ch.guengel.astro.server.ngc.NoObjectsFoundError
import ch.guengel.astro.server.ngc.ObjectNotFoundError
import ch.guengel.astro.server.ngc.OpenNGCService
import ch.guengel.astro.server.ngc.PageOutOfBoundsError
import ch.guengel.astro.server.ngc.PagedList
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.streams.toList

@QuarkusTest
@TestHTTPEndpoint(OpenNgcApiResource::class)
internal class OpenNgcApiResourceIT {
    val numberOfCatalogEntries = 100
    private var easyRandom = EasyRandom()

    lateinit var catalogEntries: List<NGCEntry>

    @InjectMock
    lateinit var openNGCService: OpenNGCService

    @BeforeEach
    fun beforeEach() {
        catalogEntries = easyRandom.objects(NGCEntry::class.java, numberOfCatalogEntries).toList()
    }

    @Test
    @TestSecurity(user = "someuser", roles = ["admin"])
    fun `should fetch when authenticated with correct role catalog`() {
        justRun { openNGCService.fetchCatalog() }

        When {
            put("/fetch")
        } Then {
            statusCode(204)
        }

        verify { openNGCService.fetchCatalog() }
    }

    @Test
    @TestSecurity(user = "someuser", roles = ["any-role"])
    fun `should not allow fetching without the correct role`() {
        justRun { openNGCService.fetchCatalog() }

        When {
            put("/fetch")
        } Then {
            statusCode(403)
        }

        verify(exactly = 0) { openNGCService.fetchCatalog() }
    }

    @Test
    fun `should not allow fetching without authentication`() {
        justRun { openNGCService.fetchCatalog() }

        When {
            put("/fetch")
        } Then {
            statusCode(401)
        }

        verify(exactly = 0) { openNGCService.fetchCatalog() }
    }

    @Test
    fun `should get constellations`() {
        every { openNGCService.constellations } returns emptySet()

        When {
            get("/constellations")
        } Then {
            statusCode(200)
        }

        verify { openNGCService.constellations }
    }

    @Test
    fun `should get catalog last update date`() {
        val catalogLastUpdate = CatalogLastUpdate()
        every { openNGCService.getLastCatalogUpdate() } returns catalogLastUpdate

        When {
            get("/last-update")
        } Then {
            statusCode(200)
        }

        verify { openNGCService.getLastCatalogUpdate() }
    }

    @Test
    fun `should get object`() {
        val ngcEntry = easyRandom.nextObject(NGCEntry::class.java)
        every { openNGCService.getObject(ngcEntry.name) } returns ngcEntry

        When {
            get("/{objectName}", mapOf("objectName" to ngcEntry.name))
        } Then {
            statusCode(200)
        }

        verify { openNGCService.getObject(ngcEntry.name) }
    }

    @Test
    fun `should handle non-existing objects`() {
        every { openNGCService.getObject("non-existing") } throws ObjectNotFoundError("object not found")

        When {
            get("/{objectName}", mapOf("objectName" to "non-existing"))
        } Then {
            statusCode(404)
        }

        verify { openNGCService.getObject("non-existing") }
    }

    @Test
    fun `should get object types`() {
        every { openNGCService.objectTypes } returns setOf(ObjectType())

        When {
            get("/types")
        } Then {
            statusCode(200)
        }

        verify { openNGCService.objectTypes }
    }

    @Test
    fun `should list objects`() {
        every {
            openNGCService.list(0, 25, constellations = emptySet(),
                objects = emptySet())
        } returns PagedList(listOf(), 2, 4, 5, 6, 7, true, true)

        When {
            get()
        } Then {
            statusCode(200)
            header("x-page-index", "2")
            header("x-page-size", "4")
            header("x-total-pages", "5")
            header("x-next-page-index", "6")
            header("x-previous-page-index", "7")
            header("x-first-page", "true")
            header("x-last-page", "true")
        }

        verify {
            openNGCService.list(0, 25,
                constellations = emptySet(),
                objects = emptySet())
        }
    }

    @Test
    fun `should list extended objects`() {
        val now = OffsetDateTime.now()
        every {
            openNGCService.listExtended(
                8.83,
                47.32,
                now,
                0,
                25,
                constellations = emptySet(),
                objects = emptySet())
        } returns PagedList(listOf(), 2, 4, 5, 6, 7, true, true)

        When {
            get("{longitude}/{latitude}/{localTime}", mapOf(
                "longitude" to "8.83",
                "latitude" to "47.32",
                "localTime" to now.toString()))
        } Then {
            statusCode(200)
            header("x-page-index", "2")
            header("x-page-size", "4")
            header("x-total-pages", "5")
            header("x-next-page-index", "6")
            header("x-previous-page-index", "7")
            header("x-first-page", "true")
            header("x-last-page", "true")
        }

        verify {
            openNGCService.listExtended(8.83, 47.32, now, 0, 25, constellations = emptySet(),
                objects = emptySet())
        }
    }

    @Test
    fun `should list objects with query parameters`() {
        every {
            openNGCService.list(2, 4,
                messier = true,
                catalog = "IC",
                objects = setOf("object1", "object2"),
                constellations = setOf("cons1", "cons2"))
        } returns PagedList(listOf(), 2, 4, 5, 6, 7, true, true)

        Given {
            queryParam("page-index", "2")
            queryParam("page-size", "4")
            queryParam("messier", "true")
            queryParam("catalog", "IC")
            queryParam("objects", "object1")
            queryParam("objects", "object2")
            queryParam("constellations", "cons1")
            queryParam("constellations", "cons2")
        } When {
            get()
        } Then {
            statusCode(200)
        }

        verify {
            openNGCService.list(2, 4,
                messier = true,
                catalog = "IC",
                objects = setOf("object1", "object2"),
                constellations = setOf("cons1", "cons2"))
        }
    }

    @Test
    fun `should list extended objects with query parameters`() {
        val now = OffsetDateTime.now()
        every {
            openNGCService.listExtended(8.83,
                47.32,
                now,
                2,
                4,
                messier = true,
                catalog = "IC",
                objects = setOf("object1", "object2"),
                constellations = setOf("cons1", "cons2"))
        } returns PagedList(listOf(), 2, 4, 5, 6, 7, true, true)

        Given {
            queryParam("page-index", "2")
            queryParam("page-size", "4")
            queryParam("messier", "true")
            queryParam("catalog", "IC")
            queryParam("objects", "object1")
            queryParam("objects", "object2")
            queryParam("constellations", "cons1")
            queryParam("constellations", "cons2")
        } When {
            get("{longitude}/{latitude}/{localTime}", mapOf(
                "longitude" to "8.83",
                "latitude" to "47.32",
                "localTime" to now.toString()))
        } Then {
            statusCode(200)
        }

        verify {
            openNGCService.listExtended(8.83,
                47.32,
                now,
                2,
                4,
                messier = true,
                catalog = "IC",
                objects = setOf("object1", "object2"),
                constellations = setOf("cons1", "cons2"))
        }
    }

    @Test
    fun `should handle paging error`() {
        every {
            openNGCService.list(0, 25, constellations = emptySet(),
                objects = emptySet())
        } throws PageOutOfBoundsError("")

        When {
            get()
        } Then {
            statusCode(404)
        }

        verify {
            openNGCService.list(0, 25,
                constellations = emptySet(),
                objects = emptySet())
        }
    }

    @Test
    fun `should handle illegal argument exception`() {
        every {
            openNGCService.list(0, 25, constellations = emptySet(),
                objects = emptySet())
        } throws IllegalArgumentException()

        When {
            get()
        } Then {
            statusCode(400)
        }

        verify {
            openNGCService.list(0, 25,
                constellations = emptySet(),
                objects = emptySet())
        }
    }

    @Test
    fun `should handle exception`() {
        every {
            openNGCService.list(0, 25, constellations = emptySet(),
                objects = emptySet())
        } throws Exception()

        When {
            get()
        } Then {
            statusCode(500)
        }

        verify {
            openNGCService.list(0, 25,
                constellations = emptySet(),
                objects = emptySet())
        }
    }

    @Test
    fun `should handle no objects found`() {
        every {
            openNGCService.list(0, 25, constellations = emptySet(),
                objects = emptySet())
        } throws NoObjectsFoundError("")

        When {
            get()
        } Then {
            statusCode(404)
        }

        verify {
            openNGCService.list(0, 25,
                constellations = emptySet(),
                objects = emptySet())
        }
    }
}