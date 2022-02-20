package ch.guengel.astro.server.common.ngc

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class CatalogFileServiceTest {
    private lateinit var catalogFileService: CatalogFileService
    private val testFile = File("/tmp/test-" + UUID.randomUUID().toString())

    @BeforeEach
    fun beforeEach() {
        testFile.delete()
        catalogFileService = CatalogFileService(testFile.canonicalPath)
    }

    @AfterEach
    fun afterEach() {
        testFile.delete()
    }

    @Test
    fun `should get correct file`() {
        assertThat(catalogFileService.catalogFile).isEqualTo(testFile)
    }

    @Test
    fun `should get last updated date time`() {
        val now = OffsetDateTime.now()
        testFile.createNewFile()

        assertThat(ChronoUnit.SECONDS.between(catalogFileService.lastUpdated, now)).isBetween(-2, 2)
    }

    @Test
    fun `should get null for last updated date time of non-existing file`() {
        assertThat(catalogFileService.lastUpdated).isNull()
    }

    @Test
    fun `should get correct file path`() {
        assertThat(catalogFileService.catalogFilePath).isEqualTo(testFile.canonicalPath)
    }
}
