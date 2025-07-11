package io.clroot.ball.domain.model.paging

data class Sort(
    val orders: List<Order> = emptyList()
) {
    companion object {
        fun by(vararg properties: String): Sort =
            Sort(properties.map { Order.asc(it) })

        fun unsorted(): Sort = Sort()
    }

    fun and(other: Sort): Sort = Sort(orders + other.orders)
    fun ascending(property: String): Sort = Sort(orders + Order.asc(property))
    fun descending(property: String): Sort = Sort(orders + Order.desc(property))

    val isSorted: Boolean get() = orders.isNotEmpty()

    val isUnsorted: Boolean get() = !isSorted


    fun isEmpty(): Boolean {
        return orders.isEmpty()
    }
}