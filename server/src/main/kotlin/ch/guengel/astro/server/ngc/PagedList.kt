package ch.guengel.astro.server.ngc

data class PagedList<T>(
    val entryList: List<T>,
    val pageIndex: Int,
    val pageSize: Int,
    val numberOfPages: Int,
    val nextPageIndex: Int?,
    val previousPageIndex: Int?,
    val firstPage: Boolean,
    val lastPage: Boolean,
)
