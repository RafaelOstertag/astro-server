package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.isEqualTo
import ch.guengel.astro.openngc.Constellation
import ch.guengel.astro.openngc.ExtendedNgcEntry
import ch.guengel.astro.openngc.NgcEntry
import ch.guengel.astro.openngc.ObjectType
import ch.guengel.astro.server.easyRandomParameters
import ch.guengel.astro.server.model.NGCEntry
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.Test

internal class CatalogEntryMapperTest {
    private val easyRandom = EasyRandom(easyRandomParameters)
    private val catalogEntryMapper = CatalogEntryMapper()

    @Test
    fun `should map entry`() {
        val ngcEntry = easyRandom.nextObject(NgcEntry::class.java)
        val ngcEntryModel = catalogEntryMapper.map(ngcEntry)

        assertNgcEntry(ngcEntryModel, ngcEntry)
    }

    private fun assertNgcEntry(ngcEntryModel: NGCEntry, ngcEntry: NgcEntry) {
        assertThat(ngcEntryModel.catalogName).isEqualTo(ngcEntry.id.catalogName.name)
        assertThat(ngcEntryModel.name).isEqualTo(ngcEntry.name)
        assertThat(ngcEntryModel.type.abbrev).isEqualTo(ngcEntry.objectType.abbrev)
        assertThat(ngcEntryModel.type.description).isEqualTo(ngcEntry.objectType.description)
        assertThat(ngcEntryModel.equatorialCoordinates.dec).isEqualTo(ngcEntry.equatorialCoordinates!!.declination.toString())
        assertThat(ngcEntryModel.equatorialCoordinates.ra).isEqualTo(ngcEntry.equatorialCoordinates!!.rightAscension.toString())
        assertThat(ngcEntryModel.equatorialCoordinates.decDecimal).isEqualTo(ngcEntry.equatorialCoordinates!!.declination.asDecimal())
        assertThat(ngcEntryModel.equatorialCoordinates.raDecimal).isEqualTo(ngcEntry.equatorialCoordinates!!.rightAscension.asDecimal())
        assertThat(ngcEntryModel.constellation.fullname).isEqualTo(ngcEntry.constellation!!.fullname)
        assertThat(ngcEntryModel.constellation.abbrev).isEqualTo(ngcEntry.constellation!!.abbrev)
        assertThat(ngcEntryModel.majorAxis).isEqualTo(ngcEntry.majorAxis)
        assertThat(ngcEntryModel.minorAxis).isEqualTo(ngcEntry.minorAxis)
        assertThat(ngcEntryModel.getbMag()).isEqualTo(ngcEntry.bMag)
        assertThat(ngcEntryModel.getvMag()).isEqualTo(ngcEntry.vMag)
        assertThat(ngcEntryModel.getjMag()).isEqualTo(ngcEntry.jMag)
        assertThat(ngcEntryModel.gethMag()).isEqualTo(ngcEntry.hMag)
        assertThat(ngcEntryModel.getkMag()).isEqualTo(ngcEntry.kMag)
        assertThat(ngcEntryModel.surfaceBrightness).isEqualTo(ngcEntry.surfBr)
        assertThat(ngcEntryModel.hubble).isEqualTo(ngcEntry.hubble)
        assertThat(ngcEntryModel.parallax).isEqualTo(ngcEntry.pax)
        assertThat(ngcEntryModel.properMotionRA).isEqualTo(ngcEntry.pmRA)
        assertThat(ngcEntryModel.properMotionDec).isEqualTo(ngcEntry.pmDec)
        assertThat(ngcEntryModel.radialVelocity).isEqualTo(ngcEntry.radVel)
        assertThat(ngcEntryModel.redshift).isEqualTo(ngcEntry.redshift)
        assertThat(ngcEntryModel.cstarUMag).isEqualTo(ngcEntry.cstarUMag)
        assertThat(ngcEntryModel.cstarUMag).isEqualTo(ngcEntry.cstarUMag)
        assertThat(ngcEntryModel.cstarVMag).isEqualTo(ngcEntry.cstarVMag)
        assertThat(ngcEntryModel.messier).isEqualTo(ngcEntry.messier)
        assertThat(ngcEntryModel.ngc).isEqualTo(ngcEntry.ngc)
        assertThat(ngcEntryModel.ic).isEqualTo(ngcEntry.ic)
        assertThat(ngcEntryModel.cstarNames).isEqualTo(ngcEntry.cstarNames)
        assertThat(ngcEntryModel.identifiers).isEqualTo(ngcEntry.identifiers)
        assertThat(ngcEntryModel.commonNames).isEqualTo(ngcEntry.commonNames)
        assertThat(ngcEntryModel.nedNotes).isEqualTo(ngcEntry.nedNotes)
        assertThat(ngcEntryModel.openNGCNotes).isEqualTo(ngcEntry.openNGCNotes)
    }

    @Test
    fun `should map object type`() {
        for (objectType in ObjectType.values()) {
            val objectTypeModel = catalogEntryMapper.map(objectType)
            assertThat(objectTypeModel.abbrev).isEqualTo(objectType.abbrev)
            assertThat(objectTypeModel.description).isEqualTo(objectType.description)
        }
    }

    @Test
    fun `should map constellation`() {
        for (constellation in Constellation.values()) {
            val constellationModel = catalogEntryMapper.map(constellation)
            assertThat(constellationModel.abbrev).isEqualTo(constellation.abbrev)
            assertThat(constellationModel.fullname).isEqualTo(constellation.fullname)
        }
    }

    @Test
    fun `should map entry with horizontal coordinates`() {
        val extendedNgcEntry = easyRandom.nextObject(ExtendedNgcEntry::class.java)
        val ngcEntryWithHorizonCoordinates = catalogEntryMapper.map(extendedNgcEntry)

        assertNgcEntry(ngcEntryWithHorizonCoordinates.entry, extendedNgcEntry.ngcEntry)
        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.alt).isEqualTo(extendedNgcEntry.horizontalCoordinates.altitude.toString())
        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.altDec).isEqualTo(extendedNgcEntry.horizontalCoordinates.altitude.asDecimal())

        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.az).isEqualTo(extendedNgcEntry.horizontalCoordinates.azimuth.toString())
        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.azDec).isEqualTo(extendedNgcEntry.horizontalCoordinates.azimuth.asDecimal())
    }
}
