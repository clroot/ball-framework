package io.clroot.ball.domain.model.core.paging

data class Order(
    val property: String,
    val direction: Direction
) {
    companion object {
        fun asc(property: String) = Order(property, Direction.ASC)
        fun desc(property: String) = Order(property, Direction.DESC)
    }
}