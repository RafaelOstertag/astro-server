package ch.guengel.astro.server.common.ngc

import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CatalogFileService(
    @ConfigProperty(name = "astro-server.catalog-file-path")
    val catalogFilePath: String,
) {
    val catalogFile = File(catalogFilePath)

    /**
     * Get date and time of last catalog update
     */
    val lastUpdated: OffsetDateTime?
        get() {
            if (!catalogFile.exists()) {
                return null
            }

            val modifiedInstant = Instant.ofEpochMilli(catalogFile.lastModified())
            return ZonedDateTime.ofInstant(modifiedInstant, ZoneId.systemDefault()).toOffsetDateTime()
        }
}
