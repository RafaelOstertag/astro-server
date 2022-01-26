package ch.guengel.astro.server.resources

import ch.guengel.astro.server.api.OpenNgcApi
import ch.guengel.astro.server.ngc.OpenNGCService
import org.jboss.resteasy.reactive.NoCache
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

    override fun getTypes(): Response = Response.ok(openNGCService.objectTypes).build()

    override fun listObjects(
        pageSize: Int,
        pageIndex: Int,
        messier: Boolean?,
        catalog: String?,
        objects: Set<String>?,
        constellations: Set<String>?,
    ): Response {
        val pagedEntryList = openNGCService.list(pageIndex, pageSize, messier, catalog, objects, constellations)
        val response = Response.ok(pagedEntryList.entryList)
            .header("x-page-size", pagedEntryList.pageSize)
            .header("x-page-index", pagedEntryList.pageIndex)
            .header("x-first-page", pagedEntryList.firstPage)
            .header("x-last-page", pagedEntryList.lastPage)
            .header("x-total-pages", pagedEntryList.numberOfPages)

        if (pagedEntryList.previousPageIndex != null) {
            response.header("x-previous-page-index", pagedEntryList.previousPageIndex)
        }

        if (pagedEntryList.nextPageIndex != null) {
            response.header("x-next-page-index", pagedEntryList.nextPageIndex)
        }

        return response.build()
    }
}
