package ch.guengel.astro.server.ngc

import ch.guengel.astro.openngc.Entry
import ch.guengel.astro.server.model.Constellation
import ch.guengel.astro.server.model.EquatorialCoordinates
import ch.guengel.astro.server.model.NGCEntry
import ch.guengel.astro.server.model.ObjectType
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CatalogEntryMapper {
    fun map(entry: Entry): NGCEntry = ngcEntry {
        catalogName = entry.catalogName.toString()
        name = entry.name
        type = map(entry.objectType)

        if (entry.equatorialCoordinates != null) {
            equatorialCoordinates = equatorialCoordinates {
                ra = entry.equatorialCoordinates!!.rightAscension.toString()
                dec = entry.equatorialCoordinates!!.declination.toString()
                raDecimal = entry.equatorialCoordinates!!.rightAscension.asDecimal()
                decDecimal = entry.equatorialCoordinates!!.declination.asDecimal()
            }
        }

        if (entry.constellation != null) {
            constellation = map(entry.constellation!!)
        }

        majorAxis = entry.majorAxis
        minorAxis = entry.minorAxis
        positionAngle = entry.positionAngle
        setbMag(entry.bMag)
        setvMag(entry.vMag)
        setjMag(entry.jMag)
        sethMag(entry.hMag)
        setkMag(entry.kMag)
        surfaceBrightness = entry.surfBr
        hubble = entry.hubble
        parallax = entry.pax
        properMotionRA = entry.pmRA
        properMotionDec = entry.pmDec
        radialVelocity = entry.radVel
        redshift = entry.redshift
        cstarUMag = entry.cstarUMag
        cstarBMag = entry.cstarBMag
        cstarVMag = entry.cstarVMag
        messier = entry.messier
        ngc = entry.ngc
        ic = entry.ic
        cstarNames = entry.cstarNames
        identifiers = entry.identifiers
        commonNames = entry.commonNames
        nedNotes = entry.nedNotes
        openNGCNotes = entry.openNGCNotes
    }

    fun map(objectType: ch.guengel.astro.openngc.ObjectType): ObjectType = objectType {
        abbrev = objectType.abbrev
        description = objectType.description
    }

    fun map(constellation: ch.guengel.astro.openngc.Constellation): Constellation = constellation {
        abbrev = constellation.abbrev
        fullname = constellation.fullname
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


