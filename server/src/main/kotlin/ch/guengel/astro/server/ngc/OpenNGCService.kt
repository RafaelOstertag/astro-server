package ch.guengel.astro.server.ngc

import ch.guengel.astro.openngc.Catalog
import ch.guengel.astro.server.model.CatalogLastUpdate
import ch.guengel.astro.server.model.NGCEntry
import io.quarkus.scheduler.Scheduled
import org.jboss.logging.Logger
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import kotlin.math.ceil

@ApplicationScoped
class OpenNGCService(private val catalogProvider: CatalogProvider, private val catalogEntryMapper: CatalogEntryMapper) {
    private val catalogReference: AtomicReference<Catalog> = AtomicReference<Catalog>(null)

    @PostConstruct
    fun postConstruct() {
        try {
            catalogReference.set(catalogProvider.loadCatalog())
        } catch (e: Exception) {
            log.error("Error while loading catalog", e)
        }
    }

    fun list(pageIndex: Int, pageSize: Int): PagedNGCEntryList {
        require(pageIndex >= 0) { "page index must be equal or greater than 0" }
        require(pageSize > 0) { "page size must be greater than 1" }

        val catalog: Catalog? = catalogReference.get()
        if (catalog == null) {
            log.error("No catalog loaded")
            return PagedNGCEntryList(
                entryList = emptyList(),
                pageIndex = 0,
                pageSize = pageSize,
                numberOfPages = -1,
                nextPageIndex = null,
                previousPageIndex = null,
                firstPage = false,
                lastPage = false
            )
        }

        val catalogSize = catalog.entries.size
        val numberOfPages = ceil(catalogSize / pageSize.toDouble()).toInt()
        if (pageIndex >= numberOfPages) {
            throw PageOutOfBoundsError("Page index $pageIndex out of bounds. Number of pages is $numberOfPages. Hint: Page index is zero-based and must be less than number of pages")
        }

        val fromIndex = pageIndex * pageSize
        val toIndex = fromIndex + pageSize
        val entries = catalog.entries
            .subList(fromIndex, if (toIndex >= catalogSize) catalogSize else toIndex)
            .map { catalogEntryMapper.map(it) }

        val isFirstPage = pageIndex == 0
        val isLastPage = pageIndex == numberOfPages - 1
        return PagedNGCEntryList(
            entries,
            pageIndex = pageIndex,
            pageSize = pageSize,
            numberOfPages = numberOfPages,
            nextPageIndex = if (!isLastPage) pageIndex + 1 else null,
            previousPageIndex = if (!isFirstPage) pageIndex - 1 else null,
            firstPage = isFirstPage,
            lastPage = isLastPage)
    }

    fun getObject(objectName: String): NGCEntry {
        val entries = catalogReference.get()?.entries ?: listOf()
        return entries.firstOrNull { entry -> entry.name == objectName }?.let { catalogEntryMapper.map(it) }
            ?: throw ObjectNotFoundError("Object '$objectName' not found")
    }

    fun getLastCatalogUpdate(): CatalogLastUpdate {
        val catalogLastUpdated =
            catalogProvider.getLastUpdated() ?: throw CatalogNotLoadedError("Catalog not loaded yet")
        return CatalogLastUpdate().apply { lastUpdated = catalogLastUpdated }
    }

    @Scheduled(cron = "{astro-server.catalog-fetch.cron.expression}")
    fun fetchCatalog() {
        log.info("Reload catalog")
        try {
            val catalog = catalogProvider.fetchCatalog()
            catalogReference.set(catalog)
        } catch (e: Exception) {
            log.error("Error while fetching catalog", e)
        }
    }

    private companion object {
        private val log: Logger = Logger.getLogger(OpenNGCService::class.java)
    }

}
