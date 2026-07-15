package com.luisete.queda.core.model.quantity

@Suppress("MagicNumber")
enum class ApproximateLevel(val order: Int) {
    EMPTY(0),
    ALMOST_EMPTY(1),
    LOW(2),
    MEDIUM(3),
    HIGH(4),
    FULL(5),
}
