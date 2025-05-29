package io.clroot.ball.domain.model.core.paging

data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sort: Sort = Sort.unsorted()
) {
    init {
        require(page >= 0) { "Page index must not be less than zero" }
        require(size >= 1) { "Page size must not be less than one" }
    }

    val offset: Long get() = (page * size).toLong()
}