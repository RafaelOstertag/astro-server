package ch.guengel.astro.server.catalogfetcher.ngc.notification

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isFailure
import assertk.assertions.isSuccess
import ch.guengel.astro.server.catalogfetcher.ngc.AstroServerWiremockResource
import ch.guengel.astro.server.catalogfetcher.ngc.InjectAstroServerMock
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Test
import javax.inject.Inject

@QuarkusTest
@QuarkusTestResource(AstroServerWiremockResource::class)
internal class HttpNotificationIT {
    @field:InjectAstroServerMock
    lateinit var astroServer: WireMockServer

    @Inject
    lateinit var httpNotification: HttpNotification

    @Test
    fun `should notify to reload catalog`() {
        val response = ResponseDefinitionBuilder.responseDefinition()
            .withStatus(204)

        astroServer.stubFor(put(urlEqualTo("/v1/open-ngc/catalog/reload"))
            .withHeader("Authorization", equalTo("Bearer test jwt"))
            .willReturn(response))

        assertThat { httpNotification.notify(listOf("localhost:" + astroServer.port()), "test jwt") }.isSuccess()
    }

    @Test
    fun `should throw on failure`() {
        val response = ResponseDefinitionBuilder.responseDefinition()
            .withStatus(403)

        astroServer.stubFor(put(urlEqualTo("/v1/open-ngc/catalog/reload"))
            .withHeader("Authorization", equalTo("Bearer test jwt"))
            .willReturn(response))

        assertThat { httpNotification.notify(listOf("localhost:" + astroServer.port()), "test jwt") }.isFailure()
            .hasClass(NotificationError::class)
    }
}
