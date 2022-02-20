package ch.guengel.astro.server.ngc

import ch.guengel.astro.server.api.OpenNgcApi
import org.jboss.resteasy.reactive.NoCache
import java.time.OffsetDateTime
import javax.annotation.security.RolesAllowed
import javax.ws.rs.core.Response

class OpenNgcApiResource(private val openNGCService: OpenNGCService) : OpenNgcApi {

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

    @RolesAllowed("admin")
    @NoCache
    override fun reloadCatalog(): Response {
        openNGCService.loadCatalog()
        return Response.noContent().build()
    }

    override fun listObjectsExtended(
        longitude: Double,
        latitude: Double,
        localTime: OffsetDateTime,
        pageSize: Int,
        pageIndex: Int,
        messier: Boolean?,
        catalog: String?,
        objects: Set<String>?,
        constellations: Set<String>?,
        vMagMax: Double?,
        vMagMin: Double?,
        altMax: Double?,
        altMin: Double?,
        types: Set<String>?,
    ): Response = OpenNGCService.ListExtendedArguments(
        OpenNGCService.ListArguments(
            pageIndex = pageIndex,
            pageSize = pageSize,
            messier = messier,
            catalog = catalog,
            objects = objects,
            types = types,
            constellations = constellations,
            vMagnitudeMax = vMagMax,
            vMagnitudeMin = vMagMin,
        ),
        longitude = longitude,
        latitude = latitude,
        localTime = localTime,
        altitudeMax = altMax,
        altitudeMin = altMin
    ).let { openNGCService.listExtended(it) }.toResponse()

    override fun listObjects(
        pageSize: Int,
        pageIndex: Int,
        messier: Boolean?,
        catalog: String?,
        objects: Set<String>?,
        constellations: Set<String>?,
        vMagMax: Double?,
        vMagMin: Double?,
        types: Set<String>?,
    ): Response = OpenNGCService.ListArguments(
        pageIndex = pageIndex,
        pageSize = pageSize,
        messier = messier,
        catalog = catalog,
        objects = objects,
        types = types,
        constellations = constellations,
        vMagnitudeMax = vMagMax,
        vMagnitudeMin = vMagMin
    ).let { openNGCService.list(it) }.toResponse()

    private fun <T> PagedList<T>.toResponse(): Response {
        val response = Response.ok(entryList)
            .header("x-page-size", pageSize)
            .header("x-page-index", pageIndex)
            .header("x-first-page", firstPage)
            .header("x-last-page", lastPage)
            .header("x-total-pages", numberOfPages)
            .header("x-total-entries", numberOfEntries)

        if (previousPageIndex != null) {
            response.header("x-previous-page-index", previousPageIndex)
        }

        if (nextPageIndex != null) {
            response.header("x-next-page-index", nextPageIndex)
        }

        return response.build()
    }
}

