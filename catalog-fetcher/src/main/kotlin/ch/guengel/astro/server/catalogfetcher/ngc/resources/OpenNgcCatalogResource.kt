package ch.guengel.astro.server.catalogfetcher.ngc.resources

import ch.guengel.astro.server.catalogfetcher.api.OpenNgcCatalogApi
import ch.guengel.astro.server.catalogfetcher.model.CatalogLastUpdate
import ch.guengel.astro.server.catalogfetcher.ngc.CatalogFetcherService
import ch.guengel.astro.server.common.ngc.CatalogFileService
import org.eclipse.microprofile.jwt.JsonWebToken
import org.jboss.resteasy.reactive.NoCache
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.ws.rs.core.Response

class OpenNgcCatalogResource(
    private val catalogFetcherService: CatalogFetcherService,
    private val catalogFileService: CatalogFileService,
) : OpenNgcCatalogApi {

    @Inject
    internal lateinit var jwt: JsonWebToken

    @RolesAllowed("admin")
    @NoCache
    override fun fetchCatalog(): Response {
        catalogFetcherService.fetchAndNotify(jwt.rawToken)
        return Response.noContent().build()
    }

    override fun getLastCatalogUpdate(): Response =
        Response.ok(CatalogLastUpdate().lastUpdated(catalogFileService.lastUpdated
            ?: throw CatalogNotLoadedError("Catalog not loaded yet"))).build()
}
