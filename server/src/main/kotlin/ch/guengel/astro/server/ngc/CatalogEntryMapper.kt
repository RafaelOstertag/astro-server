package ch.guengel.astro.server.ngc

import ch.guengel.astro.openngc.ExtendedNgcEntry
import ch.guengel.astro.openngc.NgcEntry
import ch.guengel.astro.server.model.Constellation
import ch.guengel.astro.server.model.EquatorialCoordinates
import ch.guengel.astro.server.model.HorizontalCoordinates
import ch.guengel.astro.server.model.NGCEntry
import ch.guengel.astro.server.model.NGCEntryWithHorizontalCoordinates
import ch.guengel.astro.server.model.ObjectType
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CatalogEntryMapper {
    fun map(ngcEntry: NgcEntry): NGCEntry = ngcEntry {
        catalogName = ngcEntry.id.catalogName.name
        name = ngcEntry.name
        type = map(ngcEntry.objectType)

        if (ngcEntry.equatorialCoordinates != null) {
            equatorialCoordinates = equatorialCoordinates {
                ra = ngcEntry.equatorialCoordinates!!.rightAscension.toString()
                dec = ngcEntry.equatorialCoordinates!!.declination.toString()
                raDecimal = ngcEntry.equatorialCoordinates!!.rightAscension.asDecimal()
                decDecimal = ngcEntry.equatorialCoordinates!!.declination.asDecimal()
            }
        }

        if (ngcEntry.constellation != null) {
            constellation = map(ngcEntry.constellation!!)
        }

        majorAxis = ngcEntry.majorAxis
        minorAxis = ngcEntry.minorAxis
        positionAngle = ngcEntry.positionAngle
        setbMag(ngcEntry.bMag)
        setvMag(ngcEntry.vMag)
        setjMag(ngcEntry.jMag)
        sethMag(ngcEntry.hMag)
        setkMag(ngcEntry.kMag)
        surfaceBrightness = ngcEntry.surfBr
        hubble = ngcEntry.hubble
        parallax = ngcEntry.pax
        properMotionRA = ngcEntry.pmRA
        properMotionDec = ngcEntry.pmDec
        radialVelocity = ngcEntry.radVel
        redshift = ngcEntry.redshift
        cstarUMag = ngcEntry.cstarUMag
        cstarBMag = ngcEntry.cstarBMag
        cstarVMag = ngcEntry.cstarVMag
        messier = ngcEntry.messier
        ngc = ngcEntry.ngc
        ic = ngcEntry.ic
        cstarNames = ngcEntry.cstarNames
        identifiers = ngcEntry.identifiers
        commonNames = ngcEntry.commonNames
        nedNotes = ngcEntry.nedNotes
        openNGCNotes = ngcEntry.openNGCNotes
    }

    fun map(extendedNgcEntry: ExtendedNgcEntry): NGCEntryWithHorizontalCoordinates = ngcEntryWithHorizontalCoordinates {
        entry = map(extendedNgcEntry.ngcEntry)
        horizontalCoordinates = map(extendedNgcEntry.horizontalCoordinates)
    }

    fun map(horizontalCoordinates: ch.guengel.astro.coordinates.HorizontalCoordinates) = horizontalCoordinates {
        alt = horizontalCoordinates.altitude.toString()
        az = horizontalCoordinates.azimuth.toString()
        altDec = horizontalCoordinates.altitude.asDecimal()
        azDec = horizontalCoordinates.azimuth.asDecimal()
    }

    fun map(objectType: ch.guengel.astro.openngc.ObjectType): ObjectType = objectType {
        abbrev = objectType.abbrev
        description = objectType.description
    }

    fun map(constellation: ch.guengel.astro.openngc.Constellation): Constellation = constellation {
        abbrev = constellation.abbrev
        fullname = constellation.fullname
    }

    private fun ngcEntryWithHorizontalCoordinates(init: NGCEntryWithHorizontalCoordinates.() -> Unit): NGCEntryWithHorizontalCoordinates {
        val ngcEntryWithHorizonCoordinates = NGCEntryWithHorizontalCoordinates()
        ngcEntryWithHorizonCoordinates.init()
        return ngcEntryWithHorizonCoordinates
    }

    private fun ngcEntry(init: NGCEntry.() -> Unit): NGCEntry {
        val ngcEntry = NGCEntry()
        ngcEntry.init()
        return ngcEntry
    }

    private fun equatorialCoordinates(init: EquatorialCoordinates.() -> Unit): EquatorialCoordinates {
        val equatorialCoordinates = EquatorialCoordinates()
        equatorialCoordinates.init()
        return equatorialCoordinates
    }

    private fun horizontalCoordinates(init: HorizontalCoordinates.() -> Unit): HorizontalCoordinates {
        val horizonCoordinates = HorizontalCoordinates()
        horizonCoordinates.init()
        return horizonCoordinates
    }

    private fun objectType(init: ObjectType.() -> Unit): ObjectType {
        val objectType = ObjectType()
        objectType.init()
        return objectType
    }

    private fun constellation(init: Constellation.() -> Unit): Constellation {
        val constellation = Constellation()
        constellation.init()
        return constellation
    }
}


