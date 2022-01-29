package ch.guengel.astro.server.resources

import ch.guengel.astro.server.api.OpenNgcApi
import ch.guengel.astro.server.ngc.OpenNGCService
import ch.guengel.astro.server.ngc.PagedList
import org.jboss.resteasy.reactive.NoCache
import java.time.OffsetDateTime
import javax.annotation.security.RolesAllowed
import javax.ws.rs.core.Response

class OpenNgcApiResource(private val openNGCService: OpenNGCService) : OpenNgcApi {
    @RolesAllowed("admin")
    @NoCache
    override fun fetchCatalog(): Response {
        openNGCService.fetchCatalog()
        return Response.noContent().build()
    }

    override fun getConstellations(): Response = Response.ok(openNGCService.constellations).build()

    override fun getLastCatalogUpdate(): Response = Response.ok(openNGCService.getLastCatalogUpdate()).build()

    override fun getObject(objectName: String): Response = Response.ok(openNGCService.getObject(objectName)).build()

    override fun getObjectExtended(
        longitude: Double,
        latitude: Double,
        localTime: OffsetDateTime,
        objectName: String,
    ): Response = Response.ok(openNGCService.getObjectExtended(longitude, latitude, localTime, objectName)).build()

    override fun getTypes(): Response = Response.ok(openNGCService.objectTypes).build()

    override fun listObjectsExtended(
        longitude: Double,
        latitude: Double,
        localTime: OffsetDateTime,
        pageSize: Int,
        pageIndex: Int,
        messier: Boolean?,
        catalog: String?,
        objects: MutableSet<String>?,
        constellations: MutableSet<String>?,
    ): Response {
        val pagedEntryList = openNGCService.listExtended(longitude,
            latitude,
            localTime,
            pageIndex,
            pageSize,
            messier,
            catalog,
            objects,
            constellations)
        return pagedEntryList.toResponse()
    }

    override fun listObjects(
        pageSize: Int,
        pageIndex: Int,
        messier: Boolean?,
        catalog: String?,
        objects: Set<String>?,
        constellations: Set<String>?,
    ): Response {
        val pagedEntryList = openNGCService.list(pageIndex, pageSize, messier, catalog, objects, constellations)
        return pagedEntryList.toResponse()
    }

    private fun <T> PagedList<T>.toResponse(): Response {
        val response = Response.ok(entryList)
            .header("x-page-size", pageSize)
            .header("x-page-index", pageIndex)
            .header("x-first-page", firstPage)
            .header("x-last-page", lastPage)
            .header("x-total-pages", numberOfPages)

        if (previousPageIndex != null) {
            response.header("x-previous-page-index", previousPageIndex)
        }

        if (nextPageIndex != null) {
            response.header("x-next-page-index", nextPageIndex)
        }

        return response.build()
    }
}
