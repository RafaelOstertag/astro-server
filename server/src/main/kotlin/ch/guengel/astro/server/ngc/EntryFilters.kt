package ch.guengel.astro.server.ngc

import ch.guengel.astro.openngc.ExtendedNgcEntry
import ch.guengel.astro.openngc.NgcEntry

internal typealias EntryFilter = (NgcEntry) -> Boolean
internal typealias ExtendedEntryFilter = (ExtendedNgcEntry) -> Boolean

internal object EntryFilters {
    fun compileEntryPredicates(listArguments: OpenNGCService.ListArguments): List<EntryFilter> = with(listArguments) {
        val predicates = mutableListOf<EntryFilter>()

        addMessierFilter(predicates)
        addCatalogFilter(predicates)
        addObjectsFilter(predicates)
        addConstellationFilter(predicates)
        addVMagnitudeMaxFilter(predicates)
        addVMagnitudeMinFilter(predicates)
        addTypesFilter(predicates)

        return predicates
    }

    private fun OpenNGCService.ListArguments.addTypesFilter(predicates: MutableList<EntryFilter>) {
        if (types != null && types.isNotEmpty()) {
            predicates.add { entry -> types.contains(entry.objectType.abbrev) }
        }
    }

    private fun OpenNGCService.ListArguments.addVMagnitudeMinFilter(predicates: MutableList<EntryFilter>) {
        if (vMagnitudeMin != null) {
            predicates.add { entry -> entry.vMag != null && entry.vMag!! <= vMagnitudeMin }
        }
    }

    private fun OpenNGCService.ListArguments.addVMagnitudeMaxFilter(predicates: MutableList<EntryFilter>) {
        if (vMagnitudeMax != null) {
            predicates.add { entry -> entry.vMag != null && entry.vMag!! >= vMagnitudeMax }
        }
    }

    private fun OpenNGCService.ListArguments.addConstellationFilter(predicates: MutableList<EntryFilter>) {
        if ((constellations != null) && constellations.isNotEmpty()) {
            predicates.add { entry ->
                (entry.constellation != null) &&
                        (constellations.contains(entry.constellation!!.abbrev) ||
                                constellations.contains(entry.constellation!!.fullname))
            }
        }
    }

    private fun OpenNGCService.ListArguments.addObjectsFilter(predicates: MutableList<EntryFilter>) {
        if ((objects != null) && objects.isNotEmpty()) {
            predicates.add { entry -> objects.contains(entry.name) }
        }
    }

    private fun OpenNGCService.ListArguments.addCatalogFilter(predicates: MutableList<EntryFilter>) {
        if (catalog != null) {
            predicates.add { entry -> entry.id.catalogName.name == catalog }
        }
    }

    private fun OpenNGCService.ListArguments.addMessierFilter(predicates: MutableList<EntryFilter>) {
        if (messier != null) {
            predicates.add { entry -> entry.isMessier() == messier }
        }
    }

    fun compileExtendedEntryPredicates(arguments: OpenNGCService.ListExtendedArguments): List<ExtendedEntryFilter> {
        val predicates = mutableListOf<ExtendedEntryFilter>()

        if (arguments.altitudeMax != null) {
            predicates.add { extendedEntry -> extendedEntry.horizontalCoordinates.altitude.asDecimal() <= arguments.altitudeMax }
        }

        if (arguments.altitudeMin != null) {
            predicates.add { extendedEntry -> extendedEntry.horizontalCoordinates.altitude.asDecimal() >= arguments.altitudeMin }
        }

        return predicates
    }
}
