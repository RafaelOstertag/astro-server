package ch.guengel.astro.server.catalogfetcher.ngc.notification

class NotificationError(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
