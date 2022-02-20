package ch.guengel.astro.server.ngc

class PageOutOfBoundsError(message: String) : RuntimeException(message)
class ObjectNotFoundError(message: String) : RuntimeException(message)
class NoObjectsFoundError(message: String) : RuntimeException(message)
class CatalogNotLoadedError(message: String) : RuntimeException(message)

