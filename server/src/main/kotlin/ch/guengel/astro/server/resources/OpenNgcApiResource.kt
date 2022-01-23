package ch.guengel.astro.server.resources

import ch.guengel.astro.server.api.OpenNgcApi
import ch.guengel.astro.server.ngc.OpenNGCService
import javax.ws.rs.core.Response

class OpenNgcApiResource(private val openNGCService: OpenNGCService) : OpenNgcApi {
    override fun fetchCatalog(): Response {
        openNGCService.fetchCatalog()
        return Response.noContent().build()
    }

    override fun getLastCatalogUpdate(): Response = Response.ok(openNGCService.getLastCatalogUpdate()).build()

    override fun getObject(objectName: String): Response = Response.ok(openNGCService.getObject(objectName)).build()

    override fun listObjects(pageSize: Int, pageIndex: Int): Response {
        val pagedEntryList = openNGCService.list(pageIndex, pageSize)
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
