package ch.guengel.astro.server.catalogfetcher.ngc.catalog

import ch.guengel.astro.server.common.ngc.CatalogFileService
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import javax.enterprise.context.ApplicationScoped
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

@ApplicationScoped
class CatalogFetcher(
    private val catalogFileService: CatalogFileService,
    @ConfigProperty(name = "astro-server.catalog-fetcher.catalog-url")
    private val catalogUrl: String,
) {
    fun fetch(): File {
        val catalogDirectory = Path.of(catalogFileService.catalogFilePath).parent
        ensurePath(catalogDirectory)

        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("catalog-server", null, catalogDirectory.toFile())
            downloadCatalogIntoFile(tempFile!!)

            tempFile.renameTo(catalogFileService.catalogFile)
            tempFile = null
            log.info("Catalog in '${catalogFileService.catalogFile}'")
            return catalogFileService.catalogFile
        } finally {
            tempFile?.delete()
        }
    }

    private fun downloadCatalogIntoFile(file: File) {
        try {
            val httpRequest = HttpRequest.newBuilder().uri(URI.create(catalogUrl)).GET().build()
            val response = HttpClient.newBuilder().build().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
            when (response.statusCode()) {
                200 -> {
                    response.body().use { inputStream ->
                        file.outputStream().use { outputStream ->
                            writeData(inputStream, outputStream)

                        }
                    }
                    log.info("Downloaded '$catalogUrl' into '$file'")
                }
                else -> {
                    val message = "Got HTTP Status Code ${response.statusCode()} when trying to download catalog"
                    log.error(message)
                    throw CatalogFetcherError(message)
                }
            }
        } catch (e: CatalogFetcherError) {
            throw e
        } catch (e: Exception) {
            val message = "Error while downloading catalog to '$file' from '$catalogUrl'"
            log.error(message, e)
            throw CatalogFetcherError(message, e)
        }

    }

    private fun writeData(inputStream: InputStream, outputStream: OutputStream) {
        val byteArray = ByteArray(bufferSize)
        var bytesRead = inputStream.read(byteArray)
        var totalBytesRead = bytesRead
        while (bytesRead != -1) {
            outputStream.write(byteArray, 0, bytesRead)
            bytesRead = inputStream.read(byteArray)
            totalBytesRead += bytesRead
        }
    }

    private fun ensurePath(directory: Path) {
        if (!directory.exists()) {
            directory.createDirectories()
            log.info("'${directory}' does not exist. Created it")
        }

        if (!directory.isDirectory()) {
            val message = "'${directory}' is not a directory"
            log.error(message)
            throw CatalogFetcherError(message)
        }
    }

    private companion object {
        private val log: Logger = Logger.getLogger(CatalogFetcher::class.java)
        private const val bufferSize = 1024 * 8
    }
}

