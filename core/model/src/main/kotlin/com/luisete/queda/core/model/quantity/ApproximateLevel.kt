package com.luisete.queda.core.model.quantity

private const val ORDER_EMPTY = 0
private const val ORDER_ALMOST_EMPTY = 1
private const val ORDER_LOW = 2
private const val ORDER_MEDIUM = 3
private const val ORDER_HIGH = 4
private const val ORDER_FULL = 5

enum class ApproximateLevel(val order: Int) {
    EMPTY(ORDER_EMPTY),
    ALMOST_EMPTY(ORDER_ALMOST_EMPTY),
    LOW(ORDER_LOW),
    MEDIUM(ORDER_MEDIUM),
    HIGH(ORDER_HIGH),
    FULL(ORDER_FULL),
}
