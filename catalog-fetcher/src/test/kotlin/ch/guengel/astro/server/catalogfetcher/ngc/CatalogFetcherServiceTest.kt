package ch.guengel.astro.server.catalogfetcher.ngc

import ch.guengel.astro.server.catalogfetcher.ngc.catalog.CatalogFetcher
import ch.guengel.astro.server.catalogfetcher.ngc.k8s.PodLister
import ch.guengel.astro.server.catalogfetcher.ngc.notification.HttpNotification
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
internal class CatalogFetcherServiceTest {
    @MockK
    private lateinit var catalogFetcher: CatalogFetcher

    @MockK
    private lateinit var podLister: PodLister

    @MockK
    private lateinit var httpNotification: HttpNotification

    @InjectMockKs
    private lateinit var catalogFetcherService: CatalogFetcherService

    @Test
    fun fetchAndNotify() {
        every { catalogFetcher.fetch() } returns File("/tmp/test")
        val ipLists = listOf("a", "b", "c")
        every { podLister.listAstroPodIPs() } returns ipLists
        every { httpNotification.notify(any(), any()) } just Runs

        catalogFetcherService.fetchAndNotify("test jwt")

        verifySequence {
            catalogFetcher.fetch()
            podLister.listAstroPodIPs()
            httpNotification.notify(ipLists, "test jwt")
        }
    }
}
