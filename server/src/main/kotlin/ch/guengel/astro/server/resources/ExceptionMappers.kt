package ch.guengel.astro.server.resources

import ch.guengel.astro.server.model.Error
import ch.guengel.astro.server.ngc.NoObjectsFoundError
import ch.guengel.astro.server.ngc.ObjectNotFoundError
import ch.guengel.astro.server.ngc.PageOutOfBoundsError
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import javax.ws.rs.core.Response

class ExceptionMappers {
    @ServerExceptionMapper
    fun mapIllegalArgument(e: IllegalArgumentException): RestResponse<Error> =
        RestResponse.status(Response.Status.BAD_REQUEST, e.toError())

    @ServerExceptionMapper
    fun mapPageOutOfBoundsError(e: PageOutOfBoundsError): RestResponse<Error> =
        RestResponse.status(Response.Status.NOT_FOUND, e.toError())

    @ServerExceptionMapper
    fun mapObjectNotFoundError(e: ObjectNotFoundError): RestResponse<Error> =
        RestResponse.status(Response.Status.NOT_FOUND, e.toError())

    @ServerExceptionMapper
    fun mapNoObjectsFoundError(e: NoObjectsFoundError): RestResponse<Error> =
        RestResponse.status(Response.Status.NOT_FOUND, e.toError())

    @ServerExceptionMapper
    fun mapException(e: Exception): RestResponse<Error> =
        RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, e.toError())

    private fun Exception.toError(): Error = Error().also { errorResponse ->
        errorResponse.reason = message ?: "no reason specified"
    }
}
