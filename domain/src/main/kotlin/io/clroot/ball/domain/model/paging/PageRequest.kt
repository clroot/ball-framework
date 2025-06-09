package io.clroot.ball.domain.model.paging

data class PageRequest(
    val page: Int = 0,
    val size: Int = DEFAULT_SIZE,
    val sort: Sort = Sort.unsorted()
) {
    companion object {
        const val DEFAULT_SIZE = 30
    }

    init {
        require(page >= 0) { "Page index must not be less than zero" }
        require(size >= 1) { "Page size must not be less than one" }
    }

    val offset: Long get() = (page * size).toLong()
}