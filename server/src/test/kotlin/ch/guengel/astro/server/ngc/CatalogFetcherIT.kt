package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.hasClass
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isTrue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

private const val catalogPath = "/tmp/test"
private const val catalogFile = "${catalogPath}/ngc.csv"

@QuarkusTest
@QuarkusTestResource(CatalogWiremockResource::class)
internal class CatalogFetcherIT {
    val catalogData =
        """Name;Type;RA;Dec;Const;MajAx;MinAx;PosAng;B-Mag;V-Mag;J-Mag;H-Mag;K-Mag;SurfBr;Hubble;Pax;Pm-RA;Pm-Dec;RadVel;Redshift;Cstar U-Mag;Cstar B-Mag;Cstar V-Mag;M;NGC;IC;Cstar Names;Identifiers;Common names;NED notes;OpenNGC notes
IC0001;**;00:08:27.05;+27:43:03.6;Peg;;;;;;;;;;;;;;;;;;;;;;;;;;
IC0002;G;00:11:00.88;-12:49:22.3;Cet;0.98;0.32;142;15.46;;12.26;11.48;11.17;23.45;Sb;;;;6775;0.022860;;;;;;;;2MASX J00110081-1249206,IRAS 00084-1306,MCG -02-01-031,PGC 000778;;;
IC0003;G;00:12:06.09;-00:24:54.8;Psc;0.93;0.67;53;14.78;;11.53;10.79;10.54;23.50;E;;;;5398;0.018170;;;;;;;;2MASX J00120604-0024543,MCG +00-01-038,PGC 000836,SDSS J001206.08-002454.7,SDSS J001206.09-002454.7,SDSS J001206.09-002454.8,SDSS J001206.10-002454.8;;;
IC0004;G;00:13:26.94;+17:29:11.2;Peg;1.17;0.84;12;14.14;;11.51;10.65;10.50;23.01;Sc;;;;4956;0.016672;;;;;;;;2MASX J00132695+1729111,IRAS 00108+1712,MCG +03-01-029,PGC 000897,UGC 00123;;;
IC0005;G;00:17:34.93;-09:32:36.1;Cet;0.99;0.66;9;14.57;;11.50;10.85;10.50;23.40;E;;;;6617;0.022320;;;;;;;;2MASX J00173495-0932364,MCG -02-01-047,PGC 001145,SDSS J001734.93-093236.0,SDSS J001734.93-093236.1;;;
IC0006;G;00:18:55.04;-03:16:33.9;Psc;1.23;1.08;146;14.57;;11.03;10.32;10.08;23.89;E;;;;6186;0.020852;;;;;;;;2MASX J00185505-0316339,MCG -01-01-075,PGC 001228;;;
IC0007;G;00:18:53.16;+10:35:40.9;Psc;0.90;0.63;174;14.63;;11.33;10.57;10.26;23.22;S0;;;;5326;0.017926;;;;;;;;2MASX J00185316+1035410,PGC 001216;;;
IC0008;G;00:19:02.72;-03:13:19.5;Psc;0.82;0.34;129;15.16;;12.70;12.08;12.08;23.40;E?;;;;6090;0.020525;;;;;;;;2MASX J00190272-0313196,MCG -01-01-076,PGC 001234;;;
IC0009;G;00:19:43.98;-14:07:18.8;Cet;0.59;0.46;122;15.41;;12.38;11.71;11.28;22.88;Sa;;;;12622;0.043027;;;;;;;;2MASX J00194400-1407184,MCG -02-02-001,PGC 001271;;;"""

    @Inject
    lateinit var catalogFetcher: CatalogFetcher

    @field:InjectCatalogServerMock
    lateinit var catalogServer: WireMockServer

    @BeforeEach
    fun beforeEach() {
        removeTestFiles()
    }

    @AfterEach
    fun afterEach() {
        removeTestFiles()
    }

    private fun removeTestFiles() {
        val testPath = Path.of(catalogPath)
        if (testPath.exists() && testPath.isDirectory()) {
            testPath.listDirectoryEntries().forEach { testFile -> testFile.deleteExisting() }
        }
        testPath.deleteIfExists()
    }

    @Test
    fun `should fetch catalog`() {

        val response = ResponseDefinitionBuilder.responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody(catalogData.trimIndent())
        catalogServer.stubFor(get(urlEqualTo("/catalog")).willReturn(response))

        val catalog = catalogFetcher.fetch()
        assertThat(catalog.toString()).isEqualTo(catalogFile)
        assertThat(catalog.exists()).isTrue()
        assertThat(catalog.readText()).isEqualTo(catalogData)
    }

    @Test
    fun `should handle server error`() {
        `should fetch catalog`()

        val response = ResponseDefinitionBuilder.responseDefinition()
            .withStatus(401)

        catalogServer.stubFor(get(urlEqualTo("/catalog")).willReturn(response))

        assertThat { catalogFetcher.fetch() }.isFailure().hasClass(CatalogFetcherError::class)

        // The old catalog must still exist and have data in it
        val catalog = File(catalogFile)
        assertThat(catalog.exists()).isTrue()
        assertThat(catalog.readText()).isEqualTo(catalogData)
    }

}
