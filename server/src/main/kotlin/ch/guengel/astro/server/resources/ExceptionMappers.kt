package ch.guengel.astro.server.resources

import ch.guengel.astro.server.model.ErrorMessage
import ch.guengel.astro.server.ngc.NoObjectsFoundError
import ch.guengel.astro.server.ngc.ObjectNotFoundError
import ch.guengel.astro.server.ngc.PageOutOfBoundsError
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.ws.rs.core.Response

class ExceptionMappers {
    @ServerExceptionMapper
    fun mapIllegalArgumentException(e: IllegalArgumentException): RestResponse<ErrorMessage> =
        RestResponse.status(Response.Status.BAD_REQUEST, e.toError())

    @ServerExceptionMapper
    fun mapPageOutOfBoundsError(e: PageOutOfBoundsError): RestResponse<ErrorMessage> =
        RestResponse.status(Response.Status.NOT_FOUND, e.toError())

    @ServerExceptionMapper
    fun mapObjectNotFoundError(e: ObjectNotFoundError): RestResponse<ErrorMessage> =
        RestResponse.status(Response.Status.NOT_FOUND, e.toError())

    @ServerExceptionMapper
    fun mapNoObjectsFoundError(e: NoObjectsFoundError): RestResponse<ErrorMessage> =
        RestResponse.status(Response.Status.NOT_FOUND, e.toError())

    @ServerExceptionMapper
    fun mapException(e: Exception): RestResponse<ErrorMessage> {
        log.error("Caught internal error", e)
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, e.toError())
    }

    private fun Exception.toError(): ErrorMessage = ErrorMessage().reason(message ?: "no reason specified")

    private companion object {
        val log: Logger = Logger.getLogger(ExceptionMappers::class.java)
    }
}
