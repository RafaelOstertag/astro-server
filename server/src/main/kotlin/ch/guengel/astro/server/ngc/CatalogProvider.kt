package ch.guengel.astro.server.ngc

import ch.guengel.astro.openngc.Catalog
import ch.guengel.astro.openngc.parser.CSVParser
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CatalogProvider(private val catalogFetcher: CatalogFetcher) {

    /**
     * Load catalog. If the catalog does not exist, it will be fetched.
     */
    fun loadCatalog(): Catalog {
        val catalogFile = fetchIfNonExisting(catalogFetcher.catalogFile)

        return CSVParser.parse(catalogFile)
    }

    /**
     * Fetch catalog unconditionally.
     */
    fun fetchCatalog(): Catalog {
        val catalogFile = catalogFetcher.fetch()
        return CSVParser.parse(catalogFile)
    }

    /**
     * Get date and time of last catalog update
     */
    fun getLastUpdated(): OffsetDateTime? {
        val catalogFile = catalogFetcher.catalogFile
        if (!catalogFile.exists()) {
            return null
        }

        val modifiedInstant = Instant.ofEpochMilli(catalogFile.lastModified())
        return ZonedDateTime.ofInstant(modifiedInstant, ZoneId.systemDefault()).toOffsetDateTime()
    }

    private fun fetchIfNonExisting(catalogFile: File): File {
        if (catalogFile.exists()) return catalogFile

        return catalogFetcher.fetch()
    }
}
