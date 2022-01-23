package ch.guengel.astro.server.ngc

class CatalogFetcherError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class PageOutOfBoundsError(message: String) : RuntimeException(message)
class ObjectNotFoundError(message: String) : RuntimeException(message)
class CatalogNotLoadedError(message: String) : RuntimeException(message)

