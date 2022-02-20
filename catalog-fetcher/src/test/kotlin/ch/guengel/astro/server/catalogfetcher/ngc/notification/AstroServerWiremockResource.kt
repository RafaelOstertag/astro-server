package ch.guengel.astro.server.catalogfetcher.ngc

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class InjectAstroServerMock

class AstroServerWiremockResource : QuarkusTestResourceLifecycleManager {
    var wireMockServer: WireMockServer? = null

    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer!!.start()

        return mapOf("astro-server.catalog-fetcher.notification.scheme" to "http")
    }

    @Synchronized
    override fun stop() {
        if (wireMockServer != null) {
            wireMockServer!!.stop()
            wireMockServer = null
        }
    }

    override fun inject(testInjector: TestInjector) {
        testInjector.injectIntoFields(wireMockServer,
            AnnotatedAndMatchesType(InjectAstroServerMock::class.java, WireMockServer::class.java))
    }
}
