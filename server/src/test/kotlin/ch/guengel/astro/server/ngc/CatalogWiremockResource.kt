package ch.guengel.astro.server.ngc

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class InjectCatalogServerMock

class CatalogWiremockResource : QuarkusTestResourceLifecycleManager {
    var wireMockServer: WireMockServer? = null
    override fun start(): Map<String, String> {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer!!.start()

        // create some stubs
        return mapOf("astro-server.catalog-fetch.catalog-url" to "http://localhost:" + wireMockServer!!.port() + "/catalog")
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
            AnnotatedAndMatchesType(InjectCatalogServerMock::class.java, WireMockServer::class.java))
    }
}
