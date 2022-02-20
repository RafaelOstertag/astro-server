package ch.guengel.astro.server.catalogfetcher.ngc

import ch.guengel.astro.server.catalogfetcher.ngc.catalog.CatalogFetcher
import ch.guengel.astro.server.catalogfetcher.ngc.k8s.PodLister
import ch.guengel.astro.server.catalogfetcher.ngc.notification.HttpNotification
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CatalogFetcherService(
    private val catalogFetcher: CatalogFetcher,
    private val podLister: PodLister,
    private val httpNotification: HttpNotification,
) {
    fun fetchAndNotify(jwt: String) {
        catalogFetcher.fetch()
        podLister.listAstroPodIPs().let {
            httpNotification.notify(it, jwt)
        }
    }
}
