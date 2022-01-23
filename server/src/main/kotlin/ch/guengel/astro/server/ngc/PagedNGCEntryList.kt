package ch.guengel.astro.server.ngc

import ch.guengel.astro.server.model.NGCEntry

data class PagedNGCEntryList(
    val entryList: List<NGCEntry>,
    val pageIndex: Int,
    val pageSize: Int,
    val numberOfPages: Int,
    val nextPageIndex: Int?,
    val previousPageIndex: Int?,
    val firstPage: Boolean,
    val lastPage: Boolean,
)
