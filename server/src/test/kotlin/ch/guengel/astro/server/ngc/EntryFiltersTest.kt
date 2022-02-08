package ch.guengel.astro.server.ngc

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import ch.guengel.astro.coordinates.Angle
import ch.guengel.astro.coordinates.GeographicCoordinates
import ch.guengel.astro.coordinates.HorizontalCoordinates
import ch.guengel.astro.openngc.CatalogName
import ch.guengel.astro.openngc.Constellation
import ch.guengel.astro.openngc.ExtendedNgcEntry
import ch.guengel.astro.openngc.NgcEntry
import ch.guengel.astro.openngc.NgcEntryId
import ch.guengel.astro.openngc.ObjectType
import ch.guengel.astro.server.ngc.OpenNGCService.ListArguments
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

internal class EntryFiltersTest {
    @Test
    fun `should add all filters`() {
        val listArguments = ListArguments(0,
            0,
            messier = false,
            catalog = "wdc",
            objects = setOf("wdc"),
            constellations = setOf("wdc"),
            types = setOf("wdc"),
            vMagnitudeMax = 3.0,
            vMagnitudeMin = 8.0
        )

        val predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).hasSize(7)
    }

    @Test
    fun `should add messier filter`() {
        var listArguments = ListArguments(0, 0, messier = null)
        var predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, messier = true)
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        var ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()
        ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP, messier = 1)
        assertThat(predicates[0](ngcEntry)).isTrue()

        listArguments = ListArguments(0, 0, messier = false)
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isTrue()
        ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP, messier = 1)
        assertThat(predicates[0](ngcEntry)).isFalse()
    }

    @Test
    fun `should add catalog filter`() {
        var listArguments = ListArguments(0, 0, catalog = null)
        var predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, catalog = "IC")
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        var ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isTrue()
        ngcEntry = NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()

        listArguments = ListArguments(0, 0, catalog = "non-existing")
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()
        ngcEntry = NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()
    }

    @Test
    fun `should add objects filter`() {
        var listArguments = ListArguments(0, 0, objects = setOf())
        var predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, objects = null)
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, objects = setOf("NGC0001", "IC0001"))
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        var ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isTrue()
        ngcEntry = NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isTrue()

        ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0002"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()
        ngcEntry = NgcEntry(NgcEntryId(CatalogName.NGC, "0002"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()
    }

    @Test
    fun `should add constellations filter`() {
        var listArguments = ListArguments(0, 0, constellations = setOf())
        var predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, constellations = null)
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, constellations = setOf("And", "Peg"))
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        var ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP, constellation = Constellation.AND)
        assertThat(predicates[0](ngcEntry)).isTrue()
        ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP, constellation = Constellation.PEG)
        assertThat(predicates[0](ngcEntry)).isTrue()

        ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.IC, "0002"), ObjectType.GALAXY_GROUP, constellation = Constellation.CAE)
        assertThat(predicates[0](ngcEntry)).isFalse()
    }

    @Test
    fun `should add v-mag max filter`() {
        var listArguments = ListArguments(0, 0, vMagnitudeMax = null)
        var predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, vMagnitudeMax = 8.0)
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        var ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP, vMag = 8.1)
        assertThat(predicates[0](ngcEntry)).isTrue()
        ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP, vMag = 7.9)
        assertThat(predicates[0](ngcEntry)).isFalse()

    }

    @Test
    fun `should add v-mag min filter`() {
        var listArguments = ListArguments(0, 0, vMagnitudeMin = null)
        var predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, vMagnitudeMin = 8.0)
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        var ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP, vMag = 8.1)
        assertThat(predicates[0](ngcEntry)).isFalse()
        ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP, vMag = 7.9)
        assertThat(predicates[0](ngcEntry)).isTrue()

    }

    @Test
    fun `should add types filter`() {
        var listArguments = ListArguments(0, 0, types = emptySet())
        var predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, types = null)
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isEmpty()

        listArguments = ListArguments(0, 0, types = setOf("**", "*"))
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()

        var ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.STAR)
        assertThat(predicates[0](ngcEntry)).isTrue()
        ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.DOUBLE_STAR)
        assertThat(predicates[0](ngcEntry)).isTrue()
        ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()


        listArguments = ListArguments(0, 0, types = setOf("non-existing"))
        predicates = EntryFilters.compileEntryPredicates(listArguments)
        assertThat(predicates).isNotEmpty()
        ngcEntry =
            NgcEntry(NgcEntryId(CatalogName.NGC, "0001"), ObjectType.GALAXY_GROUP)
        assertThat(predicates[0](ngcEntry)).isFalse()
    }

    @Test
    fun `should add all extended filters`() {
        val listExtendedArguments = OpenNGCService.ListExtendedArguments(ListArguments(0, 0),
            longitude = 0.0,
            latitude = 0.0,
            OffsetDateTime.now(),
            altitudeMin = 5.0,
            altitudeMax = 10.0)
        val predicates = EntryFilters.compileExtendedEntryPredicates(listExtendedArguments)
        assertThat(predicates).hasSize(2)
    }

    @Test
    fun `should add altitude min filter`() {
        var listExtendedArguments = OpenNGCService.ListExtendedArguments(ListArguments(0, 0),
            longitude = 0.0,
            latitude = 0.0,
            OffsetDateTime.now(),
            altitudeMin = null)
        var predicates = EntryFilters.compileExtendedEntryPredicates(listExtendedArguments)
        assertThat(predicates).isEmpty()

        listExtendedArguments = OpenNGCService.ListExtendedArguments(ListArguments(0, 0),
            longitude = 0.0,
            latitude = 0.0,
            OffsetDateTime.now(),
            altitudeMin = 2.0)
        predicates = EntryFilters.compileExtendedEntryPredicates(listExtendedArguments)
        assertThat(predicates).isNotEmpty()

        val ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP)
        var ngcExtendedNgcEntry = ExtendedNgcEntry(ngcEntry, HorizontalCoordinates(Angle.of(2.1), Angle.of(0.0)),
            OffsetDateTime.now(), GeographicCoordinates(Angle.of(0.0), Angle.of(0.0)))
        assertThat(predicates[0](ngcExtendedNgcEntry)).isTrue()
        ngcExtendedNgcEntry = ExtendedNgcEntry(ngcEntry, HorizontalCoordinates(Angle.of(1.9), Angle.of(0.0)),
            OffsetDateTime.now(), GeographicCoordinates(Angle.of(0.0), Angle.of(0.0)))
        assertThat(predicates[0](ngcExtendedNgcEntry)).isFalse()
    }

    @Test
    fun `should add altitude max filter`() {
        var listExtendedArguments = OpenNGCService.ListExtendedArguments(ListArguments(0, 0),
            longitude = 0.0,
            latitude = 0.0,
            OffsetDateTime.now(),
            altitudeMax = null)
        var predicates = EntryFilters.compileExtendedEntryPredicates(listExtendedArguments)
        assertThat(predicates).isEmpty()

        listExtendedArguments = OpenNGCService.ListExtendedArguments(ListArguments(0, 0),
            longitude = 0.0,
            latitude = 0.0,
            OffsetDateTime.now(),
            altitudeMax = 2.0)
        predicates = EntryFilters.compileExtendedEntryPredicates(listExtendedArguments)
        assertThat(predicates).isNotEmpty()

        val ngcEntry = NgcEntry(NgcEntryId(CatalogName.IC, "0001"), ObjectType.GALAXY_GROUP)
        var ngcExtendedNgcEntry = ExtendedNgcEntry(ngcEntry, HorizontalCoordinates(Angle.of(2.1), Angle.of(0.0)),
            OffsetDateTime.now(), GeographicCoordinates(Angle.of(0.0), Angle.of(0.0)))
        assertThat(predicates[0](ngcExtendedNgcEntry)).isFalse()
        ngcExtendedNgcEntry = ExtendedNgcEntry(ngcEntry, HorizontalCoordinates(Angle.of(1.9), Angle.of(0.0)),
            OffsetDateTime.now(), GeographicCoordinates(Angle.of(0.0), Angle.of(0.0)))
        assertThat(predicates[0](ngcExtendedNgcEntry)).isTrue()
    }
}
