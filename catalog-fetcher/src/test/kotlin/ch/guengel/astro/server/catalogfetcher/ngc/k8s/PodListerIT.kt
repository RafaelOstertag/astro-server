package ch.guengel.astro.server.catalogfetcher.ngc.k8s

import assertk.assertThat
import assertk.assertions.containsAll
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.kubernetes.client.KubernetesTestServer
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.inject.Inject

@WithKubernetesTestServer
@QuarkusTest
internal class PodListerIT {
    @KubernetesTestServer
    lateinit var mockServer: KubernetesServer

    @Inject
    lateinit var podLister: PodLister

    @BeforeEach
    fun beforeEach() {
        val pod1 = PodBuilder()
            .withNewMetadata()
            .withName("astro-server-${UUID.randomUUID()}")
            .withNamespace("test")
            .endMetadata()
            .withNewStatus()
            .withPodIP("127.0.0.2")
            .and()
            .build()
        val pod2 = PodBuilder()
            .withNewMetadata()
            .withName("astro-server-${UUID.randomUUID()}")
            .withNamespace("test")
            .endMetadata()
            .withNewStatus()
            .withPodIP("127.0.0.3")
            .and()
            .build()

        mockServer.client.pods().create(pod1)
        mockServer.client.pods().create(pod2)
    }

    @Test
    fun `should list pod IPs`() {
        val podIPs = podLister.listAstroPodIPs()
        assertThat(podIPs)
            .containsAll("127.0.0.2", "127.0.0.3")
    }
}
