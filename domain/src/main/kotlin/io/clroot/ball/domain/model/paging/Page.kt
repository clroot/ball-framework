package io.clroot.ball.domain.model.paging

data class Page<T>(
    val content: List<T>,
    val pageRequest: PageRequest,
    val totalElements: Long
) {
    val totalPages: Int =
        if (pageRequest.size == 0) 1
        else ((totalElements + pageRequest.size - 1) / pageRequest.size).toInt()

    val number: Int = pageRequest.page
    val size: Int = pageRequest.size
    val numberOfElements: Int = content.size
    val hasContent: Boolean = content.isNotEmpty()
    val hasNext: Boolean = number + 1 < totalPages
    val hasPrevious: Boolean = number > 0
    val isFirst: Boolean = !hasPrevious
    val isLast: Boolean = !hasNext

    inline fun <R> map(transform: (T) -> R): Page<R> =
        Page(content.map(transform), pageRequest, totalElements)
}