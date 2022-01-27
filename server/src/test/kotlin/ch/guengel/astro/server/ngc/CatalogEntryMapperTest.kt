package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.isEqualTo
import ch.guengel.astro.openngc.Constellation
import ch.guengel.astro.openngc.Entry
import ch.guengel.astro.openngc.ExtendedEntry
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
        val entry = easyRandom.nextObject(Entry::class.java)
        val ngcEntry = catalogEntryMapper.map(entry)

        assertNgcEntry(ngcEntry, entry)
    }

    private fun assertNgcEntry(ngcEntry: NGCEntry, entry: Entry) {
        assertThat(ngcEntry.catalogName).isEqualTo(entry.catalogName.toString())
        assertThat(ngcEntry.name).isEqualTo(entry.name)
        assertThat(ngcEntry.type.abbrev).isEqualTo(entry.objectType.abbrev)
        assertThat(ngcEntry.type.description).isEqualTo(entry.objectType.description)
        assertThat(ngcEntry.equatorialCoordinates.dec).isEqualTo(entry.equatorialCoordinates!!.declination.toString())
        assertThat(ngcEntry.equatorialCoordinates.ra).isEqualTo(entry.equatorialCoordinates!!.rightAscension.toString())
        assertThat(ngcEntry.equatorialCoordinates.decDecimal).isEqualTo(entry.equatorialCoordinates!!.declination.asDecimal())
        assertThat(ngcEntry.equatorialCoordinates.raDecimal).isEqualTo(entry.equatorialCoordinates!!.rightAscension.asDecimal())
        assertThat(ngcEntry.constellation.fullname).isEqualTo(entry.constellation!!.fullname)
        assertThat(ngcEntry.constellation.abbrev).isEqualTo(entry.constellation!!.abbrev)
        assertThat(ngcEntry.majorAxis).isEqualTo(entry.majorAxis)
        assertThat(ngcEntry.minorAxis).isEqualTo(entry.minorAxis)
        assertThat(ngcEntry.getbMag()).isEqualTo(entry.bMag)
        assertThat(ngcEntry.getvMag()).isEqualTo(entry.vMag)
        assertThat(ngcEntry.getjMag()).isEqualTo(entry.jMag)
        assertThat(ngcEntry.gethMag()).isEqualTo(entry.hMag)
        assertThat(ngcEntry.getkMag()).isEqualTo(entry.kMag)
        assertThat(ngcEntry.surfaceBrightness).isEqualTo(entry.surfBr)
        assertThat(ngcEntry.hubble).isEqualTo(entry.hubble)
        assertThat(ngcEntry.parallax).isEqualTo(entry.pax)
        assertThat(ngcEntry.properMotionRA).isEqualTo(entry.pmRA)
        assertThat(ngcEntry.properMotionDec).isEqualTo(entry.pmDec)
        assertThat(ngcEntry.radialVelocity).isEqualTo(entry.radVel)
        assertThat(ngcEntry.redshift).isEqualTo(entry.redshift)
        assertThat(ngcEntry.cstarUMag).isEqualTo(entry.cstarUMag)
        assertThat(ngcEntry.cstarUMag).isEqualTo(entry.cstarUMag)
        assertThat(ngcEntry.cstarVMag).isEqualTo(entry.cstarVMag)
        assertThat(ngcEntry.messier).isEqualTo(entry.messier)
        assertThat(ngcEntry.ngc).isEqualTo(entry.ngc)
        assertThat(ngcEntry.ic).isEqualTo(entry.ic)
        assertThat(ngcEntry.cstarNames).isEqualTo(entry.cstarNames)
        assertThat(ngcEntry.identifiers).isEqualTo(entry.identifiers)
        assertThat(ngcEntry.commonNames).isEqualTo(entry.commonNames)
        assertThat(ngcEntry.nedNotes).isEqualTo(entry.nedNotes)
        assertThat(ngcEntry.openNGCNotes).isEqualTo(entry.openNGCNotes)
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
        val extendedEntry = easyRandom.nextObject(ExtendedEntry::class.java)
        val ngcEntryWithHorizonCoordinates = catalogEntryMapper.map(extendedEntry)

        assertNgcEntry(ngcEntryWithHorizonCoordinates.entry, extendedEntry.entry)
        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.alt).isEqualTo(extendedEntry.horizontalCoordinates.altitude.toString())
        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.altDec).isEqualTo(extendedEntry.horizontalCoordinates.altitude.asDecimal())

        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.az).isEqualTo(extendedEntry.horizontalCoordinates.azimuth.toString())
        assertThat(ngcEntryWithHorizonCoordinates.horizontalCoordinates.azDec).isEqualTo(extendedEntry.horizontalCoordinates.azimuth.asDecimal())
    }
}
