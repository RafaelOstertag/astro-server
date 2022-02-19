package ch.guengel.astro.server.catalogfetcher.ngc.k8s

import io.fabric8.kubernetes.client.KubernetesClient
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class PodLister(
    private val kubernetesClient: KubernetesClient,
    @ConfigProperty(name = "astro-server.catalog-fetcher.notification.pod-prefix") private val podPrefix: String,
) {
    fun listAstroPodIPs(): List<String> =
        kubernetesClient
            .pods()
            .list().items.filter { pod ->
                pod.metadata.name.startsWith(podPrefix)
            }
            .map { pod ->
                pod.status.podIP
            }
            .apply {
                log.info("Identified following IP(s): ${this.joinToString()}")
            }

    private companion object {
        private val log: Logger = Logger.getLogger(PodLister::class.java)
    }
}
