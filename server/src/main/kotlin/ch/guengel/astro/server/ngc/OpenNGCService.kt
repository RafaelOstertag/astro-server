package ch.guengel.astro.server.ngc

import ch.guengel.astro.coordinates.Angle
import ch.guengel.astro.coordinates.GeographicCoordinates
import ch.guengel.astro.openngc.Catalog
import ch.guengel.astro.openngc.Entry
import ch.guengel.astro.openngc.ExtendedEntry
import ch.guengel.astro.server.model.CatalogLastUpdate
import ch.guengel.astro.server.model.Constellation
import ch.guengel.astro.server.model.NGCEntry
import ch.guengel.astro.server.model.NGCEntryWithHorizontalCoordinates
import ch.guengel.astro.server.model.ObjectType
import io.quarkus.scheduler.Scheduled
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import kotlin.math.ceil

private typealias EntryFilter = (Entry) -> Boolean

@ApplicationScoped
class OpenNGCService(private val catalogProvider: CatalogProvider, private val catalogEntryMapper: CatalogEntryMapper) {
    private val catalogReference: AtomicReference<Catalog> = AtomicReference(null)
    val objectTypes: Set<ObjectType> =
        ch.guengel.astro.openngc.ObjectType.values().map { objectType -> catalogEntryMapper.map(objectType) }.toSet()
    val constellations: Set<Constellation> =
        ch.guengel.astro.openngc.Constellation.values().map { constellation -> catalogEntryMapper.map(constellation) }
            .toSet()

    @PostConstruct
    fun postConstruct() {
        try {
            val catalog = catalogProvider.loadCatalog()
            catalogReference.set(catalog)
        } catch (e: Exception) {
            log.error("Error while loading catalog", e)
        }
    }

    fun list(
        pageIndex: Int,
        pageSize: Int,
        messier: Boolean? = null,
        catalog: String? = null,
        objects: Set<String>? = null,
        constellations: Set<String>? = null,
    ): PagedList<NGCEntry> {
        require(pageIndex >= 0) { "page index must be equal or greater than 0" }
        require(pageSize > 0) { "page size must be greater than 1" }

        val openNgcCatalog: Catalog? = catalogReference.get()
        if (openNgcCatalog == null) {
            log.error("No catalog loaded")
            return PagedList(
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

        val entryPredicates = compileEntryPredicates(messier, catalog, objects, constellations)

        val allEntries =
            if (entryPredicates.isEmpty()) openNgcCatalog.find { true } else openNgcCatalog.find { entry: Entry ->
                entryPredicates
                    .map { predicate -> predicate(entry) }
                    .reduce { acc, b -> acc && b }
            }

        return listToPage(allEntries, pageSize, pageIndex, catalogEntryMapper::map)
    }

    fun listExtended(
        longitude: Double,
        latitude: Double,
        localTime: OffsetDateTime,
        pageIndex: Int,
        pageSize: Int,
        messier: Boolean? = null,
        catalog: String? = null,
        objects: Set<String>? = null,
        constellations: Set<String>? = null,
    ): PagedList<NGCEntryWithHorizontalCoordinates> {
        require(pageIndex >= 0) { "page index must be equal or greater than 0" }
        require(pageSize > 0) { "page size must be greater than 1" }

        val openNgcCatalog: Catalog? = catalogReference.get()
        if (openNgcCatalog == null) {
            log.error("No catalog loaded")
            return PagedList(
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

        val entryPredicates = compileEntryPredicates(messier, catalog, objects, constellations)

        val geographicCoordinates = GeographicCoordinates(Angle.of(latitude), Angle.of(longitude))

        val allEntries =
            if (entryPredicates.isEmpty()) openNgcCatalog.findExtendedEntries(geographicCoordinates,
                localTime) { true } else openNgcCatalog.findExtendedEntries(geographicCoordinates,
                localTime) { extendedEntry: ExtendedEntry ->
                entryPredicates
                    .map { predicate -> predicate(extendedEntry.entry) }
                    .reduce { acc, b -> acc && b }
            }

        return listToPage(allEntries, pageSize, pageIndex, catalogEntryMapper::map)
    }

    private fun <O, I> listToPage(
        allEntries: List<I>,
        pageSize: Int,
        pageIndex: Int,
        mapper: (I) -> O,
    ): PagedList<O> {
        val catalogSize = allEntries.size
        if (catalogSize == 0) {
            throw NoObjectsFoundError("No objects found for specified criteria")
        }

        val numberOfPages = ceil(catalogSize / pageSize.toDouble()).toInt()
        if (pageIndex >= numberOfPages) {
            throw PageOutOfBoundsError("Page index $pageIndex out of bounds. Number of pages is $numberOfPages. Hint: Page index is zero-based and must be less than number of pages")
        }

        val fromIndex = pageIndex * pageSize
        val toIndex = fromIndex + pageSize
        val entries = allEntries
            .subList(fromIndex, if (toIndex >= catalogSize) catalogSize else toIndex)
            .map { mapper(it) }

        val isFirstPage = pageIndex == 0
        val isLastPage = pageIndex == numberOfPages - 1
        return PagedList(
            entries,
            pageIndex = pageIndex,
            pageSize = pageSize,
            numberOfPages = numberOfPages,
            nextPageIndex = if (!isLastPage) pageIndex + 1 else null,
            previousPageIndex = if (!isFirstPage) pageIndex - 1 else null,
            firstPage = isFirstPage,
            lastPage = isLastPage)
    }

    private fun compileEntryPredicates(
        messier: Boolean?,
        catalog: String?,
        objects: Set<String>?,
        constellations: Set<String>?,
    ): List<EntryFilter> {
        val predicates = mutableListOf<EntryFilter>()
        if (messier != null) {
            predicates.add { entry -> entry.isMessier() == messier }
        }

        if (catalog != null) {
            predicates.add { entry -> entry.catalogName.toString() == catalog }
        }

        if (objects != null && objects.isNotEmpty()) {
            predicates.add { entry -> objects.contains(entry.name) }
        }

        if (constellations != null && constellations.isNotEmpty()) {
            predicates.add { entry ->
                entry.constellation != null &&
                        (constellations.contains(entry.constellation!!.abbrev) ||
                                constellations.contains(entry.constellation!!.fullname))
            }
        }

        return predicates
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
