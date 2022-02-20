package ch.guengel.astro.server.ngc

import ch.guengel.astro.openngc.Catalog
import ch.guengel.astro.openngc.parser.CSVParser
import ch.guengel.astro.server.common.ngc.CatalogFileService
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CatalogProvider(private val catalogFileService: CatalogFileService) {

    /**
     * Load catalog. If the catalog does not exist, it will be fetched.
     */
    fun loadCatalog(): Catalog {
        log.info("Load catalog from ${catalogFileService.catalogFile}")
        return CSVParser.parse(catalogFileService.catalogFile)
    }

    /**
     * Get date and time of last catalog update
     */
    fun getLastUpdated(): OffsetDateTime? = catalogFileService.lastUpdated

    private companion object {
        private val log: Logger = Logger.getLogger(CatalogProvider::class.java)
    }

}
